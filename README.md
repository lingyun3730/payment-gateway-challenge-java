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

## Request flow
```mermaid
flowchart LR
    M([Merchant]) -->|POST /payment| C[Controller]
    C --> S[PaymentGatewayService]
    S --> V{PaymentValidator}
    V -->|invalid| R422[422 Rejected<br/>all field errors<br/>bank not called]
    V -->|valid| B[BankClient]
    B -->|HTTP| SIM[(Bank simulator)]
    SIM -->|authorized / declined| ST[(PaymentsRepository<br/>last four digits only)]
    ST --> OK[200 Authorized or Declined<br/>with payment id]
    SIM -.->|503 or timeout| R502[502 Bank unavailable<br/>nothing stored]
    C -.->|unreadable JSON or wrong type| R400[400 Bad request]
    M -->|GET /payment/id| C
    C --> S
    S --> ST
```
Solid arrows are the normal path, dotted arrows are failures.

## API Documentation
For documentation openAPI is included, and it can be found under the following url: **http://localhost:8090/swagger-ui/index.html**

**Feel free to change the structure of the solution, use a different library etc.**