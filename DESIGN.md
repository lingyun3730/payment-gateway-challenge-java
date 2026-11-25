# Design considerations and assumptions

## Overview

A small payment gateway: a merchant submits a card payment, the gateway validates it, asks the
acquiring bank (simulator), stores the outcome and returns it. A merchant can fetch a payment by id.

| Endpoint | Result |
| --- | --- |
| `POST /payment` | `200` Authorized or Declined, `422` Rejected, `400` malformed JSON, `502` bank unavailable |
| `GET /payment/{id}` | `200` payment, `404` unknown id, `400` id is not a UUID |

Flow of `POST /payment`: validate (collect every error) -> call bank -> map the bank answer to
Authorized or Declined -> store (last four digits only) -> respond.

Layers: `controller` (HTTP), `service` (flow), `validation`, `bank` (client behind an interface so
tests use a fake), `repository` (in-memory), `exception` (mapped to HTTP in one place).

## Key design decisions

| Decision | Why |
| --- | --- |
| Invalid content returns **422** with **all** errors at once; unreadable JSON or wrong types return **400** | The merchant can fix everything in one round trip. 400 means "cannot read your request", 422 means "readable, but the content is not acceptable" |
| A Rejected request is **not stored** and **never reaches the bank** | The brief says no payment is created. Covered by tests (bank called 0 times) |
| Bank `503`, timeout, connection error or empty reply returns **502** and nothing is stored | The bank gave no answer, so reporting Declined would be wrong. See "Not done" for the retry risk |
| Declined by the bank is `200` | The request was processed correctly. Declined is a business result, not an error |
| Full card number and CVV are **never stored, logged or returned**. Only the last four digits are kept | PCI risk. `toString()` of request records is redacted, error messages describe the rule and never quote the submitted value, and the 400 message is fixed text because Jackson's own message can quote the value |
| `cardNumber` and `cvv` are Strings, `amount` is a `long` in minor units | Leading zeros must survive, and 16 to 19 digits overflow an `int` |
| Strict JSON: `10.5` and `"100"` for `amount` return 400 | By default Jackson would silently turn `10.5` into `10` |
| Hand-written validator, no extra dependency | Easy to read, one place for every rule, and it can collect all errors and take an injected `Clock` |
| Java records for request and response models | Immutable, no boilerplate, supported by Jackson |
| Bank client behind the `BankClient` interface | The service is tested with a fake bank. The client is tested with `MockRestServiceServer` |
| `PaymentsRepository` uses a `ConcurrentHashMap` | Several requests can write at the same time |

## Assumptions

- **Currency**: any valid ISO 4217 code is accepted, using the JDK's list (upper case only). The
  brief says to validate against "no more than 3 currency codes". I read this as "must be a valid
  ISO code". If the intent is to restrict to three, it is a small allow-list change in
  `PaymentValidator`. Note that the ISO list also contains special codes such as `XXX` and `XAU`.
- **Amount** must be greater than 0 (the brief does not say, but a zero payment has no meaning).
  Amounts are passed through in minor units. No per-currency decimal handling.
- **Expiry**: a card is valid until the end of its expiry month, so the current month is accepted.
  The year must be four digits. The "in the future" check needs a valid month and year, so it is
  reported once the month is fixed.
- **No Luhn check**: the brief does not ask for it, and the simulator's test cards are chosen by the
  last digit, not by checksum.
- **Storage** is the provided in-memory repository, standing in for a database. Data is lost on
  restart.
- JSON uses camelCase, matching the sample code. The bank simulator uses its own snake_case, which is
  handled in `BankRequest` and `BankResponse`.

## Security notes

- The full card number only exists in memory during a request and in the call to the bank.
- Errors never repeat what the caller submitted.
- Not in scope here, but needed in production: merchant authentication (API key checked at the
  gateway), authorisation (a merchant can only read its own payments), TLS, rate limiting, and a
  card vault or tokenisation so the gateway does not handle raw card numbers.

## Not done on purpose (kept simple, as the brief asks)

| Missing | What I would do in production |
| --- | --- |
| **Idempotency**. A retry creates a second payment (a test documents this) | `Idempotency-Key` header, stored with a unique constraint so concurrent retries create one payment |
| **Bank timeout leaves the result unknown**. The gateway returns 502 and keeps nothing, so the merchant cannot tell whether the bank charged the card | Save the payment as `PENDING` before calling the bank, query the bank by payment id on timeout, and notify the merchant by webhook |
| In-memory storage | Database with an index on the payment id, and a unique key for idempotency |
| Authentication and per-merchant access | API key per merchant, lookups filtered by merchant id |
| Retries, circuit breaker, metrics, tracing | Retry only safe calls, circuit breaker per bank, request id in logs, alerts on bank error rate |
| Currency-specific minor units (JPY has none) | Currency table with exponent |

## Testing

75 automated tests (`./gradlew test`):

- **Validator**: every rule, boundaries (length 14 and 19, previous, current and next month),
  all errors at once, error text never contains submitted values. The clock is fixed.
- **Bank client**: Authorized, Declined, 503, 400, connection failure, empty body, request field
  names and `MM/YYYY` format.
- **Service**: Authorized and Declined are mapped and stored, an invalid request means 0 bank calls
  and nothing stored, a bank failure stores nothing, only the last four digits are kept.
- **Controller** (full Spring context, bank mocked): every status code above, strict number
  handling, no card number in any response, a POST followed by a GET.

I also ran the app against the real bank simulator and checked the three card outcomes (odd, even,
ending in 0) and the application log for leaked card data.

## How to run

```bash
docker-compose up -d     # bank simulator on :8080
./gradlew test
./gradlew bootRun        # gateway on :8090, Swagger UI at /swagger-ui/index.html
```

Requires JDK 17. The bank URL is `bank.url` in `application.properties`.
With the gateway running, `./demo.sh` walks through every outcome one step at a time (press Enter
for the next step): Authorized, Declined, Rejected, bank unavailable, malformed JSON and GET.
`./demo.sh list` shows the steps and `./demo.sh 4` runs a single one.
