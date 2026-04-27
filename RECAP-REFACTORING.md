# Recap Dettagliato — Refactoring SQL/JPA Best Practices

## Panoramica

Refactoring completo delle interazioni con il database nel progetto WIAM.  
**14 file modificati, 2 file nuovi creati. Tutti i 16 test JUnit passano.**

---

## 1. `@Transactional` su tutti i servizi

**File modificati:**
- `service/utente/RegistraUtenteService.java` → `@Transactional`
- `service/utente/LoginUtenteService.java` → `@Transactional(readOnly = true)`
- `service/securety/RetrivePswStep1Service.java` → `@Transactional`
- `service/securety/RetrivePswStep2Service.java` → `@Transactional`
- `service/securety/RetrivePswStep3Service.java` → `@Transactional`
- `service/securety/ChangePswService.java` → `@Transactional`

**Cosa c'era prima:**  
Nessun servizio aveva `@Transactional`. Ogni chiamata a `repository.save()` faceva auto-commit immediato.

**Cosa ho fatto:**  
Aggiunto `@Transactional` a livello di classe su tutti i servizi che scrivono dati. Su `LoginUtenteService` ho usato `@Transactional(readOnly = true)` perché fa solo lettura.

**Perché:**  
Senza `@Transactional`, ogni `repository.save()` è un'operazione atomica indipendente. Se un servizio fa più operazioni (es. creare Utente + UtenteSecurety), un crash a metà lascia dati parziali nel DB — l'utente esiste ma non ha il record di sicurezza.

Problema concreto — **race condition nella registrazione:**

```
Thread A                                    Thread B
────────────────────────────────────────────────────
findByUsername("john") → NON trovato
                                            findByUsername("john") → NON trovato
                                            save(john) → SUCCESSO
save(john) → DataIntegrityViolationException (500 al client!)
```

Senza transazione, il check `findByUsername()` e il `save()` non sono atomici. Due richieste concorrenti possono entrambe passare il check.

Stesso problema per:
- **OTP counter** (`RetrivePswStep1Service`): due richieste simultanee leggono entrambe counter=4, entrambe passano il check <5, entrambe scrivono counter=5 → rate limit bypassato
- **Cambio password** (`ChangePswService`): due cambi simultanei, l'ultimo vince silenziosamente (lost update)

`@Transactional(readOnly = true)` su `LoginUtenteService` abilita ottimizzazioni Hibernate (skip dirty checking) e previene scritture accidentali.

---

## 2. `GlobalExceptionHandler.java` (file nuovo)

**File creato:** `config/GlobalExceptionHandler.java`

**Cosa c'era prima:**  
Nessun `@ControllerAdvice`. Le eccezioni di dominio (`UtenteException`, `UtenteSecuretyException`) propagavano come errore 500 con stack trace raw. Un `DataIntegrityViolationException` da violazione unique constraint arrivava al client come errore interno senza messaggio utile.

**Cosa ho fatto:**  
Creato un `@RestControllerAdvice` che intercetta:
- `UtenteException` → estrae HTTP status dal codice errore (es. `ERR-UT-400` → 400)
- `UtenteSecuretyException` → stessa logica
- `InventarioException` → stessa logica
- `DataIntegrityViolationException` → 409 CONFLICT con messaggio strutturato

Ogni risposta è un JSON `{"messaggio": "...", "errorCode": "..."}`.

**Perché:**  
Il client deve ricevere risposte strutturate, non stack trace Java. La logica di estrazione dello status dal codice errore (`ERR-{DOMAIN}-{HTTP_STATUS}`) sfrutta il pattern già presente nel codebase per mappare automaticamente al codice HTTP corretto. Il 409 CONFLICT per `DataIntegrityViolationException` è la rete di sicurezza: se il check applicativo fallisce (race condition), il vincolo DB cattura il duplicato e il client riceve un errore leggibile.

---

## 3. `InventarioException.java` (fix stub)

**File modificato:** `exception/InventarioException.java`

**Cosa c'era prima:**
```java
public class InventarioException {
}
```
Classe vuota — non estendeva nemmeno `Exception`.

**Cosa ho fatto:**  
Trasformata in:
```java
@Data
public class InventarioException extends RuntimeException {
    private String messaggio;
    private String errorCode;

    public InventarioException(String messaggio, String errorCode) {
        this.messaggio = messaggio;
        this.errorCode = errorCode;
    }
}
```

**Perché:**  
Allineata al pattern di `UtenteException` e `UtenteSecuretyException`. Il `GlobalExceptionHandler` ha bisogno che abbia la stessa struttura (campi `messaggio` e `errorCode`) per poterla gestire. Senza `extends RuntimeException`, non era nemmeno un'eccezione lanciabile.

---

## 4. `application.yaml` — WAL mode + HikariCP timeouts

**File modificato:** `src/main/resources/application.yaml`

**Cosa c'era prima:**
```yaml
datasource:
  url: jdbc:sqlite:./data/wiam.db
  hikari:
    maximum-pool-size: 1
```

**Cosa ho fatto:**
```yaml
datasource:
  url: jdbc:sqlite:./data/wiam.db?journal_mode=WAL&busy_timeout=30000
  hikari:
    maximum-pool-size: 1
    connection-timeout: 30000
    idle-timeout: 600000
    max-lifetime: 1800000
```

**Perché — WAL mode:**  
SQLite di default usa il "rollback journal mode" — quando un thread scrive, **tutti i lettori sono bloccati** fino a fine scrittura. WAL (Write-Ahead Logging) separa le scritture in un file di log separato, permettendo letture concorrenti anche durante una scrittura attiva. Per un'API web con richieste concorrenti, questo è fondamentale.

**Perché — busy_timeout=30000:**  
Senza questa impostazione, se il DB è locked da un'altra operazione, SQLite fallisce **immediatamente** con "database is locked". Con `busy_timeout=30000`, SQLite riprova per fino a 30 secondi prima di arrendersi.

**Perché — HikariCP timeouts:**
- `connection-timeout: 30000` (30s) — tempo massimo per ottenere una connessione dal pool. Senza, una richiesta potrebbe aspettare indefinitamente.
- `idle-timeout: 600000` (10 min) — chiude connessioni inattive dopo 10 minuti. Evita connessioni "zombie" che occupano risorse.
- `max-lifetime: 1800000` (30 min) — ricicla ogni connessione dopo 30 minuti. Previene problemi con connessioni stale (es. timeout lato SQLite).

---

## 5. `application-prod.yaml` (file nuovo)

**File creato:** `src/main/resources/application-prod.yaml`

**Cosa c'era prima:**  
Nessun profilo produzione. `application.yaml` ha `ddl-auto: update` e `h2.console.enabled: true` con solo un commento che dice "in produzione va disabilitata".

**Cosa ho fatto:**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  h2:
    console:
      enabled: false
```

Si attiva con `--spring.profiles.active=prod`.

**Perché — ddl-auto: validate:**  
`ddl-auto: update` in produzione è pericoloso:
- Hibernate **può aggiungere** colonne, ma **mai rimuoverle**
- **Mai rinominare** colonne (ne crea una nuova, la vecchia resta)
- **Ignora silenziosamente** cambiamenti di tipo (es. String → Integer resta String nel DB)
- Nessun versioning — impossibile fare rollback

Col tempo lo schema nel DB diverge dal modello Java. Con `validate`, Hibernate verifica **solo** che lo schema corrisponda alle entity all'avvio, senza modificarlo. Se non corrisponde, l'app non parte — fail fast.

**Perché — H2 console disabled:**  
La H2 console espone un'interfaccia web su `/h2-console` dove chiunque può eseguire query SQL arbitrarie sul database. In produzione questo è una vulnerabilità critica.

---

## 6. `Utente.java` — Constraint, audit, indici, sync helpers

**File modificato:** `entity/Utente.java`

### 6a. `@Column` constraints

**Prima:**
```java
private String nome;
private String cognome;
```

**Dopo:**
```java
@Column(nullable = false, length = 100)
private String nome;
@Column(nullable = false, length = 100)
private String cognome;
```

Stessa cosa per `email` (length = 150), `username` (length = 50), `ruolo` (length = 20).

**Perché:**  
Senza vincoli, il DB accetta qualsiasi cosa — `nome` potrebbe essere `null`, una stringa vuota, o un testo di 10.000 caratteri. I constraint `@Column`:
- **Documentano il modello dati** — leggendo l'entity sai esattamente cosa il campo accetta
- **Generano DDL corretto** — Hibernate crea la colonna con `NOT NULL` e `VARCHAR(100)`
- **Proteggono l'integrità** — il DB rifiuta dati invalidi prima ancora che il codice Java li veda

### 6b. `@Table(indexes = ...)`

**Aggiunto:**
```java
@Table(indexes = {
    @Index(name = "idx_utente_username", columnList = "username"),
    @Index(name = "idx_utente_email", columnList = "email")
})
```

**Perché:**  
SQLite crea indici automatici per vincoli `UNIQUE`, ma l'annotazione esplicita rende l'intento chiaro e funziona indipendentemente dal DB. Senza indice, ogni `findByUsername()` fa un full table scan — O(n). Con indice, O(log n).

### 6c. `@EqualsAndHashCode(exclude = {"utenteSecurety", "inventario"})`

**Perché:**  
Lombok `@Data` genera `equals()` e `hashCode()` che includono **tutti** i campi — incluse le relazioni. Con entità bidirezionali:
```
utente.equals() → chiama utenteSecurety.hashCode()
    → utenteSecurety.hashCode() chiama utente.hashCode()
        → utente.hashCode() chiama utenteSecurety.hashCode()
            → ... StackOverflowError!
```
Escludendo le relazioni dal calcolo, il loop infinito non accade.

### 6d. Campi audit `createdAt` / `updatedAt`

**Aggiunto:**
```java
@CreationTimestamp
@Column(nullable = false, updatable = false)
private LocalDateTime createdAt;

@UpdateTimestamp
@Column(nullable = false)
private LocalDateTime updatedAt;
```

**Perché:**  
Senza audit fields non c'è modo di sapere quando un record è stato creato o l'ultima volta che è stato modificato. Essenziale per:
- Debug ("quando è stato creato questo utente problematico?")
- Audit ("chi ha modificato cosa e quando?")
- Business logic ("utenti registrati nell'ultimo mese", "utenti inattivi da 6 mesi")

`@CreationTimestamp` setta il valore automaticamente al primo `persist()`. `updatable = false` impedisce che venga sovrascritto.  
`@UpdateTimestamp` si aggiorna automaticamente ad ogni `merge()`/`save()`.

### 6e. Helper per sincronizzazione bidirezionale

**Aggiunto:**
```java
public void impostaUtenteSecurety(UtenteSecurety sec) {
    this.utenteSecurety = sec;
    if (sec != null) {
        sec.setUtente(this);
    }
}

public void aggiungiInventarioItem(UtenteInventario item) {
    this.inventario.add(item);
    item.setUtente(this);
}
```

**Perché:**  
In JPA, le relazioni bidirezionali richiedono di settare **entrambi i lati** manualmente. Se un service fa `utente.setUtenteSecurety(sec)` ma dimentica `sec.setUtente(utente)`, il grafo in memoria è inconsistente — `sec.getUtente()` restituisce `null` anche se nel DB la FK è corretta. I metodi helper centralizzano la sincronizzazione e rendono impossibile dimenticarsene.

---

## 7. `UtenteSecurety.java` — Constraint, tipo data, audit

**File modificato:** `entity/UtenteSecurety.java`

### 7a. `otpCounter` — `@Column(nullable = false)`

**Perché:**  
Un record di sicurezza **deve sempre** avere un contatore OTP. Il valore iniziale è 0 (settato in `RegistraUtenteService`). Senza il vincolo, era possibile salvare un record senza counter — cosa che il test `ChangePswServiceTest` faceva (e che ho corretto).

### 7b. `otp` — `@Column(length = 6)`

**Perché:**  
L'OTP è sempre esattamente 6 caratteri (generato in `generaOtp()`). Il vincolo `length = 6` documenta questo fatto e genera un `VARCHAR(6)` nel DB.

### 7c. `chiaveStep3` — `@Column(length = 36)`

**Perché:**  
La chiave step3 è un `UUID.randomUUID().toString()` — sempre 36 caratteri (formato `8-4-4-4-12`). Il vincolo documenta e limita.

### 7d. `lastPaswordChange` — da `String` a `LocalDateTime`

**Prima:**
```java
private String lastPaswordChange;
// Usato come: LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
```

**Dopo:**
```java
private LocalDateTime lastPaswordChange;
// Usato come: LocalDateTime.now()
```

**Perché:**  
Con `String`:
- Nessuna validazione DB — qualsiasi stringa è accettata ("ciao" è un `lastPaswordChange` valido)
- Sorting rotto — ordinamento lessicografico ≠ ordinamento cronologico
- Query temporali impossibili — non puoi fare `WHERE lastPaswordChange > '2024-01-01'` con garanzia di correttezza
- Nessun supporto timezone/locale

Con `LocalDateTime`:
- Il DB valida il tipo
- L'ordinamento funziona correttamente
- Le query temporali funzionano (`WHERE lastPaswordChange > :cutoff`)
- JPA/Hibernate gestisce la serializzazione automaticamente

### 7e. `@EqualsAndHashCode(exclude = {"utente"})` e audit fields

Stesse ragioni spiegate per `Utente.java` (sezioni 6c e 6d).

---

## 8. `UtenteInventario.java` — Tipo date, BigDecimal, constraint, indici

**File modificato:** `entity/UtenteInventario.java`

### 8a. `dataAcquisto` e `dataVendita` — da `String` a `LocalDate`

**Prima:**
```java
private String dataAcquisto;   // riga 41
private String dataVendita;    // riga 68
```

**Dopo:**
```java
@Column(nullable = false)
private LocalDate dataAcquisto;

private LocalDate dataVendita;
```

**Perché:**  
Identico al ragionamento per `lastPaswordChange` — le date salvate come stringa impediscono sorting, filtraggio temporale, e validazione. `dataAcquisto` è `nullable = false` perché ogni item di inventario deve avere una data di acquisto. `dataVendita` è nullable perché l'item potrebbe non essere ancora venduto.

### 8b. `prezzoVendita`, `costiAccessori`, `prezzoNetto` — da `Double` a `BigDecimal`

**Prima:**
```java
private Double prezzoVendita;
private Double costiAccessori;
private Double prezzoNetto;
```

**Dopo:**
```java
@Column(precision = 10, scale = 2)
private BigDecimal prezzoVendita;

@Column(precision = 10, scale = 2)
private BigDecimal costiAccessori;

@Column(precision = 10, scale = 2)
private BigDecimal prezzoNetto;
```

**Perché:**  
`Double` usa aritmetica floating-point IEEE 754 che ha errori di precisione:
```java
System.out.println(0.1 + 0.2);  // Output: 0.30000000000000004
```

Per valori monetari, anche errori microscopici si accumulano. Se calcoli il `prezzoNetto` come `prezzoVendita - costiAccessori`, con `Double` puoi ottenere €99.989999999999 invece di €99.99.

`BigDecimal` usa aritmetica decimale **esatta**. `precision = 10, scale = 2` supporta valori fino a €99.999.999,99 — adeguato per un inventario di collezionabili.

### 8c. `@Column` constraints sui campi stringa

- `nomeAcquisto`: `@Column(nullable = false, length = 200)` — ogni item deve avere un nome
- `codiceAcquisto`: `@Column(length = 100)` — nullable (non tutti i prodotti hanno codice)
- `descrizione`: `@Column(length = 500)` — testo lungo ma limitato
- `piattaformaVendita`: `@Column(length = 100)` — es. "eBay", "Vinted"
- Tutti gli enum: `@Column(length = 30)` — dimensione sufficiente per qualsiasi valore enum

### 8d. `@Table(indexes = @Index(... "utente_id"))`

**Perché:**  
La colonna `utente_id` (FK verso Utente) è usata in ogni `findByUtente()` e in ogni JOIN tra Utente e inventario. A differenza dei vincoli `UNIQUE` (che SQLite indicizza automaticamente), le FK **non** sono indicizzate automaticamente. Senza indice, ogni `findByUtente()` fa un full table scan su tutta la tabella inventario.

### 8e. Audit fields `createdAt` / `updatedAt`

Stesse ragioni spiegate per `Utente.java` (sezione 6d).

---

## 9. `RegistraUtenteService.java` — Fix tipo data

**File modificato:** `service/utente/RegistraUtenteService.java`

**Prima:**
```java
utenteSecurety.setLastPaswordChange(
    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
);
```

**Dopo:**
```java
utenteSecurety.setLastPaswordChange(LocalDateTime.now());
```

**Perché:**  
Il campo `lastPaswordChange` è ora `LocalDateTime` (non più `String`), quindi non serve più formattarlo come stringa. JPA/Hibernate si occupa della serializzazione nel DB. Rimosso anche l'import `DateTimeFormatter` diventato inutilizzato.

---

## 10. `ChangePswService.java` — Fix tipo data

**File modificato:** `service/securety/ChangePswService.java`

Identica modifica del punto 9:

**Prima:**
```java
utente.getUtenteSecurety().setLastPaswordChange(
    LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
);
```

**Dopo:**
```java
utente.getUtenteSecurety().setLastPaswordChange(LocalDateTime.now());
```

Rimosso import `DateTimeFormatter`.

---

## 11. `InventarioRepository.java` — Pagination

**File modificato:** `repository/InventarioRepository.java`

**Prima:**
```java
List<UtenteInventario> findByUtente(Utente utente);
```

**Dopo:**
```java
List<UtenteInventario> findByUtente(Utente utente);
Page<UtenteInventario> findByUtente(Utente utente, Pageable pageable);
```

**Perché:**  
Il metodo originale restituisce `List<UtenteInventario>` — carica **tutto** l'inventario in memoria. Se un utente ha 10.000 item, tutti vengono caricati in un'unica query e tenuti in RAM.

Con l'overload `Pageable`, il controller può richiedere `?page=0&size=20` e il DB restituisce solo 20 record. Spring Data JPA genera automaticamente la query `LIMIT`/`OFFSET` corretta.

Ho mantenuto il metodo `List<>` per retrocompatibilità — i servizi esistenti che lo usano continuano a funzionare.

---

## 12. `ChangePswServiceTest.java` — Fix test

**File modificato:** `test/.../ChangePswServiceTest.java`

**Prima:**
```java
utente.setUtenteSecurety(new UtenteSecurety());
```

**Dopo:**
```java
var sec = new UtenteSecurety();
sec.setOtpCounter(0);
utente.setUtenteSecurety(sec);
```

**Perché:**  
Ho aggiunto `@Column(nullable = false)` su `otpCounter` in `UtenteSecurety`. Il test creava un `UtenteSecurety` senza settare il counter → `null`. Con il nuovo vincolo, il `save()` fallisce con `DataIntegrityViolationException: not-null property references a null or transient value`. Settando `otpCounter = 0` (valore iniziale corretto), il test passa.

---

## Cosa NON ho implementato (e perché)

| Cosa | Motivo |
|------|--------|
| **Flyway/Liquibase** | Richiede una sessione dedicata — prima bisogna stabilizzare tutte le modifiche schema, poi creare una migration baseline che cattura lo schema attuale |
| **Password hashing (bcrypt)** | È un tema di sicurezza applicativa, non di best practice SQL/JPA. Richiederebbe Spring Security dependency e migrazione dati esistenti |
| **FetchType.LAZY su Utente→UtenteSecurety** | Il commento nel codice spiega la scelta ("pochi campi, non impatta performance"). Cambiarlo richiederebbe JOIN FETCH in ogni query che accede alla security |
