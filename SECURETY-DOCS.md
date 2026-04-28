# SECURETY-DOCS — Documentazione Sicurezza TSM Resell

## Panoramica Architettura

L'ecosistema TSM Resell usa **OAuth2 con JWT** per proteggere le API.
L'architettura è composta da due microservizi:

```
┌─────────────┐     POST /oauth2/token      ┌──────────────┐
│   Client     │ ──────────────────────────► │  Auth Server  │
│ (app/browser)│ ◄────────────────────────── │  (porta 9000) │
│              │     { access_token: JWT }    │               │
└──────┬───────┘                             └───────┬───────┘
       │                                             │
       │  Authorization: Bearer <JWT>                │ POST /api/internal/v1/
       │                                             │   utente/verifica-credenziali
       ▼                                             ▼
┌──────────────────────────────────────────────────────────┐
│                    WIAM (porta 8080)                       │
│              OAuth2 Resource Server                        │
│                                                            │
│  ┌──────────────────┐    ┌───────────────────────────┐    │
│  │ Endpoint Pubblici │    │ Endpoint Protetti (JWT)   │    │
│  │ - /registra       │    │ - /inventario/**          │    │
│  │ - /login          │    │ - /utente/delete          │    │
│  │ - /retrivestep*   │    │ - /utente/changepsw       │    │
│  │ - /api/internal/** │    │ - /securety/changepsw    │    │
│  └──────────────────┘    └───────────────────────────┘    │
└──────────────────────────────────────────────────────────┘
```

## Flusso di Autenticazione

### 1. Ottenere un Token

```bash
POST http://localhost:9000/oauth2/token
Content-Type: application/json

{
  "username": "ajeje",
  "password": "MiaPassword123"
}
```

**Cosa succede internamente:**
1. L'auth-server riceve username e password
2. Chiama WIAM `POST /api/internal/v1/utente/verifica-credenziali` con le credenziali
3. WIAM cerca l'utente nel DB, verifica la password con BCrypt
4. Se valida, restituisce i dati utente (username, ruolo, email)
5. L'auth-server genera un JWT firmato con RSA contenente i claim dell'utente
6. Restituisce il token al client

**Risposta:**
```json
{
  "accessToken": "eyJhbGciOiJSUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### 2. Usare il Token

```bash
GET http://localhost:8080/api/v1/inventario/inquiry
Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
Content-Type: application/json

{ ... request body ... }
```

**Cosa succede internamente:**
1. WIAM riceve la richiesta con il Bearer token
2. Spring Security estrae il JWT dall'header `Authorization`
3. Scarica la chiave pubblica RSA dall'auth-server (`GET /.well-known/jwks.json`)
4. Verifica la firma del JWT — se la firma è valida, il token è autentico
5. Controlla che il token non sia scaduto (`exp` claim)
6. Se tutto è valido, la richiesta procede al controller

### 3. Token Scaduto

Dopo 1 ora il token scade. Il client riceve un 401 e deve richiedere un nuovo token.

## Struttura del JWT

```json
{
  "header": {
    "alg": "RS256",
    "kid": "uuid-della-chiave"
  },
  "payload": {
    "sub": "ajeje",
    "iss": "tsm-auth-server",
    "iat": 1714245600,
    "exp": 1714249200,
    "ruolo": "User",
    "email": "ajeje@gmail.com"
  }
}
```

| Claim | Descrizione |
|-------|-------------|
| `sub` | Username dell'utente (subject) |
| `iss` | Identificativo dell'auth-server che ha emesso il token |
| `iat` | Timestamp di emissione |
| `exp` | Timestamp di scadenza (1 ora dopo emissione) |
| `ruolo` | Ruolo utente: Admin, User, Collaborator |
| `email` | Email dell'utente |

## Endpoint Pubblici vs Protetti

### Pubblici (non richiedono JWT)

| Endpoint | Motivo |
|----------|--------|
| `POST /api/v1/utente/registra` | L'utente non è ancora registrato |
| `POST /api/v1/utente/login` | L'utente non ha ancora un token |
| `POST /api/v1/utente/securety/retrivestep1` | Recupero password (utente non può autenticarsi) |
| `POST /api/v1/utente/securety/retrivestep2` | Recupero password step 2 |
| `POST /api/v1/utente/securety/retrivestep3` | Recupero password step 3 |
| `POST /api/internal/**` | Comunicazione interna tra microservizi |
| `/h2-console/**` | Console H2 per sviluppo (disabilitata in prod) |

### Protetti (richiedono JWT valido)

| Endpoint | Metodo |
|----------|--------|
| `/api/v1/utente/delete` | DELETE |
| `/api/v1/utente/changepsw` | POST |
| `/api/v1/utente/securety/changepsw` | POST |
| `/api/v1/inventario/acquisto` | POST |
| `/api/v1/inventario/vendita` | PUT |
| `/api/v1/inventario/cancella-vendita` | PUT |
| `/api/v1/inventario/cancella-acquisto` | PUT |
| `/api/v1/inventario/inquiry` | POST |
| `/api/v1/inventario/inquiry-v2` | POST |

## Password Hashing (BCrypt)

Le password sono hashate con **BCrypt** prima di essere salvate nel database.
Non viene mai salvata la password in chiaro.

### Come funziona

```
Registrazione:
  "MiaPassword123" → BCrypt.encode() → "$2a$10$N9qo8uLOickgx2ZMRZoMye..."

Login:
  "MiaPassword123" → BCrypt.matches("MiaPassword123", "$2a$10$N9q...") → true
  "PasswordSbagliata" → BCrypt.matches("PasswordSbagliata", "$2a$10$N9q...") → false
```

### Dove è applicato

| Servizio | Operazione |
|----------|------------|
| `RegistraUtenteService` | Hash della password alla registrazione |
| `LoginUtenteService` | Verifica con `matches()` al login |
| `RegistraUtenteService.cancellaUtente()` | Verifica con `matches()` prima di cancellare |
| `ChangePswService` | Verifica vecchia + hash nuova password |
| `RetrivePswStep3Service` | Hash della nuova password nel recupero |

### ⚠️ Migrazione dati esistenti

Le password salvate in chiaro **prima** di questa implementazione non sono più valide.
Per migrarle, eseguire uno script che:
1. Legge ogni utente dal DB
2. Hasha la password con BCrypt
3. Aggiorna il record

```sql
-- Le password in chiaro nel DB non funzionano più con BCrypt.
-- Serve una migrazione manuale o un reset password per gli utenti esistenti.
```

## Firma JWT (RSA)

L'auth-server usa una **coppia di chiavi RSA 2048-bit**:
- **Chiave privata**: usata dall'auth-server per firmare i JWT. Mai esposta.
- **Chiave pubblica**: esposta su `/.well-known/jwks.json`. Usata da WIAM per verificare la firma.

### Sviluppo
Le chiavi vengono generate in memoria all'avvio. Ogni restart le rigenera, invalidando tutti i token attivi.

### Produzione
Le chiavi devono essere:
- Caricate da file, KeyStore, o servizio di gestione segreti
- Persistenti tra restart
- Ruotate periodicamente

## Come Eseguire

### Avvio in sviluppo

```bash
# Terminale 1 — WIAM (porta 8080)
cd wiam/
./mvnw spring-boot:run

# Terminale 2 — Auth Server (porta 9000)
cd auth-server/
./mvnw spring-boot:run
```

### Test rapido

```bash
# 1. Registra un utente
curl -X POST http://localhost:8080/api/v1/utente/registra \
  -H "Content-Type: application/json" \
  -d '{"nome":"Test","cognome":"User","username":"testuser","email":"test@test.com","password":"MyPassword123"}'

# 2. Ottieni un token
curl -X POST http://localhost:9000/oauth2/token \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"MyPassword123"}'

# 3. Usa il token per accedere a un endpoint protetto
curl http://localhost:8080/api/v1/inventario/inquiry \
  -H "Authorization: Bearer <token-dal-passo-2>" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","page":0,"size":10}'
```

## Configurazione

### WIAM (`wiam/src/main/resources/application.yaml`)

```yaml
spring.security.oauth2.resourceserver.jwt.jwk-set-uri: http://localhost:9000/.well-known/jwks.json
```

### Auth Server (`auth-server/src/main/resources/application.yaml`)

```yaml
server.port: 9000
auth-server.wiam.base-url: http://localhost:8080
```

### Variabili d'ambiente per produzione

| Variabile | Default | Descrizione |
|-----------|---------|-------------|
| `WIAM_BASE_URL` | `http://localhost:8080` | URL di WIAM (per auth-server) |

## Sicurezza dell'endpoint interno

L'endpoint `/api/internal/**` è `permitAll` in Spring Security perché l'auth-server non ha un JWT
(è lui che li emette). In produzione, proteggere questi endpoint a livello di rete:

- **Firewall**: permettere solo il traffico dall'IP dell'auth-server
- **Service mesh** (Istio, Linkerd): policy di rete che limita l'accesso
- **VPN/rete privata**: i microservizi comunicano su rete interna non esposta
