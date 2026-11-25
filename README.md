# Instructions for candidates

This is the Java version of the Payment Gateway challenge. If you haven't already read this [README.md](https://github.com/cko-recruitment/) on the details of this exercise, please do so now.

## Requirements
- JDK 17
- Docker

## Template structure

src/ - A skeleton SpringBoot Application

test/ - Some simple JUnit tests

imposters/ - contains the bank simulator configuration. Don't change this

.editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code

docker-compose.yml - configures the bank simulator


## Design considerations and assumptions
See [DESIGN.md](DESIGN.md).

## Security considerations and known gaps

**What the service already does**
- The full card number and CVV are never stored, logged or returned. Only the last four digits are kept.
- `toString()` of the request is redacted, error messages never repeat the submitted values, and
  the 400 message is fixed text.
- All input is validated before the bank is called, and the service uses no SQL.

**What is not done, because the brief did not ask for it, and how I would fix it**

| Gap | Risk | Fix |
| --- | --- | --- |
| **Authentication** | Anyone who can reach the API can create payments | One API key per merchant in the `Authorization` header. A filter checks it before the controller. Only a hash of the key is stored, and keys can be rotated (two active keys at once) and revoked |
| **Authorization (IDOR)** | Any caller who knows a payment id can read it. A random UUID is not access control | Store the merchant id with each payment and query by payment id **and** merchant id. Return 404 for other merchants' payments. On `POST`, take the merchant id from the authenticated key, never from the request body |
| **Idempotency and replay** | A retry creates a second payment | `Idempotency-Key` header with a unique constraint, so the same key returns the first result |
| **Transport security** | The app speaks plain HTTP | Terminate TLS at the load balancer or API gateway, and use TLS to the bank |
| **Rate limiting and abuse** | A stolen key or a bot can flood the API or test stolen cards | Rate limit per merchant, alert on many declines, limit the request size |
| **Stolen API keys** | A leaked key can create payments | HTTPS only, never log keys, scopes, IP allow-list, per-key limits, expiry, usage monitoring, instant revoke. For stronger security, request signing (HMAC) so the secret is never sent |
| **Card data at rest** | A database leak would expose card data (PCI scope) | Keep only the last four digits. If full numbers are ever needed, use a separate PCI-compliant vault and give the gateway a token |
| **Secrets** | The bank URL is in `application.properties`, and later there will be credentials | Load secrets from a secret manager or environment variables, never from Git |
| **Logging and monitoring** | No automated check that logs stay free of card data, no request id, no alerts | A test that fails if a card number appears in logs, a request id on every log line, alerts on bank error rate |
| **Swagger UI** | It shows the API to everyone | Disable it in production or keep it on the internal network |
| **Dependencies** | A library with a known vulnerability | Dependency scan in CI |

The order I would fix them in: authentication and per-merchant access first, then idempotency, then
rate limiting and monitoring.

## API Documentation
For documentation openAPI is included, and it can be found under the following url: **http://localhost:8090/swagger-ui/index.html**

**Feel free to change the structure of the solution, use a different library etc.**