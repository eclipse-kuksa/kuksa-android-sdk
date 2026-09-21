# Test Authentication Tokens & TLS Certificates

This directory contains cryptographic assets used for integration and unit testing with KUKSA Databroker instances (e.g., in Docker containers).

## Validity & Contents

### Authentication Tokens (`authentication/`)

All JWT tokens are signed using RS256 with a 4096-bit RSA private key matching `jwt.key.pub`.

* **Validity**: Valid until **2045-01-01T00:00:00Z** (`exp`: `2366841600`).
* **Audience**: `kuksa.val`
* **Issuer**: `createToken.py`
* **Subject**: `local dev`

| File | Scope | Purpose |
| --- | --- | --- |
| `jwt.key.pub` | N/A | Public key mounted into Databroker container (`--jwt-public-key`) to verify client tokens |
| `actuate-provide-all.token` | `actuate provide` | Token with full read, provide (feed), and actuate permissions |
| `provide-all.token` | `provide` | Token with feed/provider permissions |
| `read-all.token` | `read` | Token with read-only permissions |

### TLS Certificates (`tls/`)

* **Validity**: Valid for 20 years (until **August 2046**).
* **Subject Alternative Names (SAN)**: `DNS:Server`, `DNS:localhost`, `IP:127.0.0.1`.

| File | Type | Purpose |
| --- | --- | --- |
| `CA.pem` | Certificate | Self-signed Root CA certificate, used by clients as trust store |
| `Server.pem` | Certificate | Server certificate signed by `CA.pem`, mounted into Databroker container (`--tls-cert`) |
| `Server.key` | Private Key | 2048-bit RSA private key for the server certificate, mounted into Databroker (`--tls-private-key`) |

---

## Regenerating Tokens and Certificates

A helper script is provided at `buildscripts/generate-test-certificates-and-tokens.sh`.

### Prerequisites

* `openssl`
* `python3` (standard library, no extra dependencies needed)

### Execution

Run the script from the repository root:

```bash
./buildscripts/generate-test-certificates-and-tokens.sh
```

This will automatically generate a new RSA key pair, create the signed JWT tokens, generate the CA and Server TLS certificates with valid SAN extensions, and update the test resource files directly.
