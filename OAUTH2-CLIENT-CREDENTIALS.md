# OAuth2 Client Credentials — Protezione Endpoint Interni WIAM

## Panoramica

Questo documento spiega nel dettaglio come abbiamo protetto gli endpoint interni di WIAM (`/api/internal/**`) con **OAuth2 Client Credentials**, perché lo abbiamo fatto, e quali best practice abbiamo seguito.

### Il Problema

Prima di questa implementazione, gli endpoint interni di WIAM erano configurati come `permitAll` in Spring Security:

```java
// PRIMA — chiunque poteva chiamare questi endpoint!
.requestMatchers("/api/internal/**").permitAll()
```

Questo significava che **qualsiasi client** che conoscesse l'URL poteva invocare l'endpoint `POST /api/internal/v1/utente/verifica-credenziali` — un endpoint che accetta username e password e restituisce i dati dell'utente. Un attaccante avrebbe potuto:

1. Fare brute-force sulle password degli utenti
2. Enumerare gli username esistenti (risposte diverse per utente non trovato vs password errata)
3. Estrarre dati sensibili (email, ruolo) senza autenticazione

La protezione era delegata solo al livello di rete (firewall), che non è sufficiente in un'architettura moderna dove i servizi potrebbero essere esposti accidentalmente.

### La Soluzione

Abbiamo implementato il pattern **OAuth2 Client Credentials** per la comunicazione B2B (Business-to-Business / machine-to-machine):

```
┌─────────────────┐                           ┌──────────────────┐
│   Auth Server    │                           │       WIAM       │
│   (porta 9000)  │                           │   (porta 8080)   │
│                  │                           │                  │
│ 1. Genera JWT   │   Authorization:          │ 3. Valida JWT    │
│    B2B con      │   Bearer <jwt-b2b>        │    (firma RSA +  │
│    scope=       │──────────────────────────►│    scadenza +    │
│    "internal"   │                           │    scope)        │
│                  │                           │                  │
│ 2. Chiama WIAM  │   200 OK + dati utente    │ 4. Se scope=     │
│    con Bearer   │◄──────────────────────────│    "internal"    │
│    token        │                           │    → accesso OK  │
└─────────────────┘                           └──────────────────┘
```

---

## Architettura: Doppio OAuth2

Il sistema ora ha **due livelli di protezione OAuth2** che coesistono:

### 1. Client Credentials (B2B — machine-to-machine)

| Aspetto | Dettaglio |
|---------|-----------|
| **Chi lo usa** | Auth-server → WIAM (comunicazione tra servizi) |
| **Cosa protegge** | `/api/internal/**` |
| **Subject (sub)** | `auth-server-client` (identifica il servizio, non un utente) |
| **Scope** | `internal` |
| **Durata** | 5 minuti (short-lived) |
| **Chi lo emette** | Auth-server per sé stesso (self-issued) |

### 2. Bearer Token Utente (user-facing)

| Aspetto | Dettaglio |
|---------|-----------|
| **Chi lo usa** | Client (app/browser) → WIAM |
| **Cosa protegge** | `/api/v1/inventario/**`, `/api/v1/utente/delete`, ecc. |
| **Subject (sub)** | Username dell'utente (es. `ajeje`) |
| **Scope** | Nessuno (usa claim custom `ruolo`) |
| **Durata** | 1 ora |
| **Chi lo emette** | Auth-server (dopo verifica credenziali via WIAM) |

### Come WIAM Distingue i Due Tipi di Token

WIAM non ha bisogno di sapere "chi" ha emesso il token — verifica solo i claim:

```java
// In SecurityConfig.java di WIAM:

// Endpoint interni → richiede scope "internal"
.requestMatchers("/api/internal/**").hasAuthority("SCOPE_internal")

// Endpoint utente → richiede qualsiasi JWT valido
.anyRequest().authenticated()
```

Spring Security converte automaticamente il claim `scope` del JWT in authorities con prefisso `SCOPE_`:
- JWT con `"scope": "internal"` → authority `SCOPE_internal` → accede a `/api/internal/**`
- JWT con `"sub": "ajeje"` (senza scope internal) → **non** accede a `/api/internal/**` (403)

---

## Flusso Completo: Da Richiesta Token a Risposta

```
Client                    Auth-Server (9000)              WIAM (8080)
  │                            │                             │
  │ POST /oauth2/token         │                             │
  │ {username, password}       │                             │
  │───────────────────────────►│                             │
  │                            │                             │
  │                            │ STEP 0: Genera JWT B2B      │
  │                            │ (sub=auth-server-client,    │
  │                            │  scope=internal, exp=5min)  │
  │                            │                             │
  │                            │ POST /api/internal/v1/      │
  │                            │   utente/verifica-credenziali
  │                            │ Authorization: Bearer <B2B> │
  │                            │ Body: {username, password}  │
  │                            │────────────────────────────►│
  │                            │                             │
  │                            │                             │ Valida JWT B2B:
  │                            │                             │ ✓ Firma RSA OK
  │                            │                             │ ✓ Non scaduto
  │                            │                             │ ✓ scope=internal
  │                            │                             │
  │                            │                             │ Verifica credenziali:
  │                            │                             │ ✓ Utente esiste
  │                            │                             │ ✓ BCrypt match
  │                            │                             │
  │                            │ 200 OK                      │
  │                            │ {username, ruolo, email}    │
  │                            │◄────────────────────────────│
  │                            │                             │
  │                            │ STEP 1: Genera JWT Utente   │
  │                            │ (sub=username, ruolo,       │
  │                            │  email, exp=1h)             │
  │                            │                             │
  │ 200 OK                     │                             │
  │ {access_token, Bearer, 3600}                             │
  │◄───────────────────────────│                             │
  │                            │                             │
  │ GET /api/v1/inventario     │                             │
  │ Authorization: Bearer <JWT-UTENTE>                       │
  │─────────────────────────────────────────────────────────►│
  │                            │                             │
  │                            │                             │ Valida JWT Utente:
  │                            │                             │ ✓ Firma RSA OK
  │                            │                             │ ✓ Non scaduto
  │                            │                             │ (no scope check)
  │                            │                             │
  │ 200 OK                     │                             │
  │ {inventario data}          │                             │
  │◄─────────────────────────────────────────────────────────│
```

---

## Dettaglio Implementativo

### 1. `ClientCredentialsService` (auth-server)

**File:** `auth-server/src/main/java/.../service/ClientCredentialsService.java`

Questo servizio genera un JWT "B2B" che l'auth-server usa per autenticarsi verso WIAM.

**Perché self-issued (l'auth-server genera il token per sé stesso)?**

In un sistema classico, un client ottiene un token da un Authorization Server esterno. Ma nel nostro caso l'auth-server È l'Authorization Server — non può chiedere un token a sé stesso tramite un endpoint (sarebbe circolare e inutile). Invece:

1. Ha già la chiave privata RSA per firmare JWT
2. WIAM già scarica la chiave pubblica dall'auth-server (JWK Set)
3. Genera il token in-memory senza rete, senza endpoint, senza latenza

**Claim del token B2B:**

```json
{
  "sub": "auth-server-client",   // Identifica il servizio (non un utente)
  "iss": "tsm-auth-server",      // Chi ha emesso il token
  "iat": 1700000000,             // Quando è stato emesso
  "exp": 1700000300,             // Scade dopo 5 minuti
  "scope": "internal"            // Permette accesso a /api/internal/**
}
```

**Perché 5 minuti di durata?**

- Il token serve per UNA singola chiamata HTTP (verifica credenziali)
- Se la chiamata dura più di 5 minuti, c'è un problema più grave (timeout rete)
- Se il token viene intercettato, l'attaccante ha una finestra molto breve
- L'auth-server ne genera uno nuovo per ogni richiesta — nessun impatto sulle performance

### 2. `WiamClientService` modificato (auth-server)

**File:** `auth-server/src/main/java/.../service/WiamClientService.java`

**Cosa è cambiato:** Prima faceva una semplice chiamata POST senza autenticazione. Ora:

1. Inietta `ClientCredentialsService` nel costruttore
2. Prima di ogni chiamata, genera un token B2B
3. Aggiunge l'header `Authorization: Bearer <token>` alla richiesta HTTP

```java
// Prima di ogni chiamata a WIAM:
var tokenB2B = clientCredentialsService.generaTokenB2B();

// Nella richiesta HTTP:
.header("Authorization", "Bearer " + tokenB2B)
```

**Perché non cacheamo il token?**

- La generazione è istantanea (~1ms, è solo una firma RSA in-memory)
- Un token nuovo è sempre valido (nessun rischio di usare un token scaduto dalla cache)
- Semplifica il codice (niente logica di cache/invalidazione/refresh)
- In scenari ad alto traffico si potrebbe aggiungere una cache con TTL < 5 minuti

### 3. `SecurityConfig` modificata (WIAM)

**File:** `wiam/src/main/java/.../config/SecurityConfig.java`

**Cosa è cambiato:**

```java
// PRIMA:
.requestMatchers("/api/internal/**").permitAll()

// DOPO:
.requestMatchers("/api/internal/**").hasAuthority("SCOPE_internal")
```

**Come funziona `hasAuthority("SCOPE_internal")`?**

Quando WIAM riceve una richiesta con un JWT, Spring Security:

1. Estrae il token dall'header `Authorization: Bearer <jwt>`
2. Scarica la chiave pubblica dal JWK Set endpoint dell'auth-server
3. Verifica la firma RSA del JWT (il token è autentico?)
4. Verifica la scadenza (il token è ancora valido?)
5. Estrae i claim dal payload JSON del JWT
6. **Converte il claim `scope` in authorities con prefisso `SCOPE_`:**
   - `"scope": "internal"` → authority `SCOPE_internal`
   - `"scope": "read write"` → authorities `SCOPE_read`, `SCOPE_write`
7. Verifica che l'authority richiesta (`SCOPE_internal`) sia presente
8. Se tutto OK → la richiesta passa al controller
9. Se manca l'authority → 403 Forbidden
10. Se il token è invalido/mancante → 401 Unauthorized

---

## Best Practice Applicate

### 1. Principio del Least Privilege (Minimo Privilegio)

Il token B2B ha SOLO lo scope `internal` — non ha accesso a nient'altro. Se venisse compromesso, l'attaccante potrebbe solo chiamare endpoint interni, non accedere ai dati di un utente specifico.

### 2. Token Short-Lived (Breve Durata)

5 minuti di vita per il token B2B. Best practice di sicurezza:
- Riduce la finestra di esposizione in caso di compromissione
- Non richiede meccanismi di revoca (il token scade da solo)
- L'OWASP raccomanda token a breve durata per comunicazione M2M

### 3. Defense in Depth (Difesa in Profondità)

La protezione ora opera su **più livelli**:
- **Livello rete:** firewall/service mesh (già esistente)
- **Livello applicativo:** OAuth2 Client Credentials (nuovo)
- **Livello JWT:** firma RSA + scadenza + scope

Anche se un livello viene bypassato (es. l'attaccante è nella rete interna), gli altri lo bloccano.

### 4. Separazione dei Concern

- Auth-server: emette token (sia utente che B2B)
- WIAM: valida token e applica autorizzazione
- Nessun servizio ha responsabilità miste

### 5. Zero Trust Architecture

Non ci fidiamo di nessuno solo perché è "nella rete interna". Ogni richiesta deve provare la propria identità con un token valido. Questo è il principio Zero Trust: "never trust, always verify".

### 6. Stessa Chiave RSA per Tutti i Token

Vantaggi:
- WIAM ha UN SOLO JWK Set da configurare
- Nessuna complessità aggiuntiva di gestione chiavi
- Lo scope nel token è sufficiente per distinguere i tipi di accesso

### 7. Subject Identificabile

`sub: "auth-server-client"` rende immediatamente visibile nei log chi ha fatto la richiesta. Se in futuro altri servizi avessero bisogno di accesso interno, avrebbero subject diversi (es. `monitoring-service`, `batch-processor`).

---

## Scenari di Errore

| Scenario | Cosa succede | HTTP Status |
|----------|-------------|-------------|
| Nessun token nell'header | WIAM rifiuta la richiesta | 401 Unauthorized |
| Token con scope errato (es. `scope=read`) | Token valido ma senza authority richiesta | 403 Forbidden |
| Token utente (senza scope) | Token valido ma non ha `SCOPE_internal` | 403 Forbidden |
| Token B2B scaduto (>5 minuti) | Token invalido | 401 Unauthorized |
| Token firmato con chiave diversa | Firma non verificabile | 401 Unauthorized |
| Token B2B valido + credenziali utente errate | Token OK, ma WIAM restituisce errore utente | 401 (errore business) |

---

## Come Testare

### Test Automatici (auth-server)

```bash
cd auth-server/
./mvnw test -Dtest=ClientCredentialsServiceTest
```

Verifica che:
- Il token B2B viene generato correttamente
- I claim (sub, iss, scope, exp) sono corretti
- Ogni chiamata genera un token diverso

### Test Manuale (sistema completo)

1. Avvia WIAM: `cd wiam/ && ./mvnw spring-boot:run`
2. Avvia auth-server: `cd auth-server/ && ./mvnw spring-boot:run`

**Chiamata SENZA token (deve fallire con 401):**
```bash
curl -X POST http://localhost:8080/api/internal/v1/utente/verifica-credenziali \
  -H "Content-Type: application/json" \
  -d '{"username": "test", "password": "test"}'
# Risposta: 401 Unauthorized
```

**Chiamata con token utente (deve fallire con 403):**
```bash
# Ottieni un token utente normale
TOKEN=$(curl -s -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/json" \
  -d '{"username": "ajeje", "password": "password"}' | jq -r .access_token)

# Prova a chiamare l'endpoint interno con il token utente
curl -X POST http://localhost:8080/api/internal/v1/utente/verifica-credenziali \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"username": "test", "password": "test"}'
# Risposta: 403 Forbidden (il token utente non ha scope "internal")
```

**Chiamata tramite auth-server (flusso normale — deve funzionare):**
```bash
# Il flusso normale passa dall'auth-server che aggiunge il token B2B automaticamente
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/json" \
  -d '{"username": "ajeje", "password": "MiaPassword123"}'
# Risposta: 200 OK con access_token (se l'utente esiste)
```

---

## Evoluzione Futura

### Possibili Miglioramenti

1. **Cache del token B2B** — Se il traffico è molto alto, cacheare il token con TTL di 4 minuti (sotto la scadenza di 5) per evitare di generare un nuovo token ad ogni richiesta.

2. **Audience claim** — Aggiungere `"aud": "wiam"` per specificare a quale servizio è destinato il token. WIAM potrebbe validare che il token sia destinato a lui.

3. **Scopes granulari** — Invece di un unico scope `internal`, usare scopes più specifici:
   - `internal:verifica-credenziali` — solo verifica credenziali
   - `internal:admin` — operazioni amministrative
   
4. **Client Registration** — Se altri servizi avranno bisogno di accesso interno, creare un registro di client con i loro scopes permessi.

5. **mTLS** — Per sicurezza massima, aggiungere Mutual TLS tra i servizi oltre al token JWT (doppio livello di autenticazione).

---

## File Modificati / Creati

| File | Azione | Descrizione |
|------|--------|-------------|
| `auth-server/.../service/ClientCredentialsService.java` | **Nuovo** | Genera JWT B2B |
| `auth-server/.../service/WiamClientService.java` | Modificato | Aggiunge Bearer token B2B |
| `wiam/.../config/SecurityConfig.java` | Modificato | `/api/internal/**` → `hasAuthority("SCOPE_internal")` |
| `wiam/.../controller/InternalUtenteController.java` | Modificato | Commenti aggiornati |
| `wiam/.../config/SecurityTestConfig.java` (test) | Modificato | Commenti con esempio per test futuri |
| `auth-server/.../service/ClientCredentialsServiceTest.java` | **Nuovo** | Test del token B2B |
