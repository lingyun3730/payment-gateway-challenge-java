#!/usr/bin/env bash
# Step-by-step demo of the gateway. Start the bank simulator and the gateway first:
#   docker-compose up -d
#   ./gradlew bootRun
#
# ./demo.sh          run all steps, pause before each one (press Enter to continue)
# ./demo.sh 4        run only step 4
# ./demo.sh list     list the steps
set -u

BASE_URL="${BASE_URL:-http://localhost:8090}"
YEAR=$(( $(date +%Y) + 2 ))
LAST_BODY=""

TITLES=(
  "Authorized: card ends in an odd digit -> 200"
  "Declined: card ends in an even digit -> 200"
  "Bank unavailable: card ends in 0 -> 502, nothing stored"
  "Rejected: several invalid fields, all reported at once -> 422"
  "Rejected: required fields missing -> 422"
  "Rejected: card already expired -> 422"
  "Malformed JSON -> 400"
  "Amount is not a whole number -> 422"
  "Retrieve a payment, only the last four digits are shown -> 200"
  "Retrieve an unknown id -> 404"
  "Id that is not a UUID -> 400"
)

payment() {
  # card number, month, year, currency, amount, cvv
  echo "{\"cardNumber\":\"$1\",\"expiryMonth\":$2,\"expiryYear\":$3,\"currency\":\"$4\",\"amount\":$5,\"cvv\":\"$6\"}"
}

# Sends a request and prints it with the response. The response body is kept in LAST_BODY.
call() {
  local method="$1" path="$2" body="${3:-}" response
  echo "$method $path"
  if [ -n "$body" ]; then
    echo "request:  $body"
    response=$(curl -s -w '\n%{http_code}' -X "$method" "$BASE_URL$path" \
      -H 'Content-Type: application/json' -d "$body")
  else
    response=$(curl -s -w '\n%{http_code}' -X "$method" "$BASE_URL$path")
  fi
  LAST_BODY=$(echo "$response" | sed '$d')
  echo "response: $LAST_BODY"
  echo "status:   $(echo "$response" | tail -n 1)"
}

# Step 9 needs an existing payment, so create one quietly if step 1 was not run.
create_authorized_payment_id() {
  curl -s -X POST "$BASE_URL/payment" -H 'Content-Type: application/json' \
    -d "$(payment 2222405343248877 4 "$YEAR" GBP 100 123)" | grep -o '"id":"[^"]*"' | cut -d'"' -f4
}

run_step() {
  case "$1" in
    1) call POST /payment "$(payment 2222405343248877 4 "$YEAR" GBP 100 123)"
       PAYMENT_ID=$(echo "$LAST_BODY" | grep -o '"id":"[^"]*"' | cut -d'"' -f4) ;;
    2) call POST /payment "$(payment 2222405343248878 4 "$YEAR" USD 2500 123)" ;;
    3) call POST /payment "$(payment 2222405343248870 4 "$YEAR" EUR 100 123)" ;;
    4) call POST /payment "$(payment 12 13 2020 GBP 0 1)" ;;
    5) call POST /payment '{}' ;;
    6) call POST /payment "$(payment 2222405343248877 1 2020 GBP 100 123)" ;;
    7) call POST /payment '{oops' ;;
    8) call POST /payment "{\"cardNumber\":\"2222405343248877\",\"expiryMonth\":4,\"expiryYear\":$YEAR,\"currency\":\"GBP\",\"amount\":10.5,\"cvv\":\"123\"}" ;;
    9) PAYMENT_ID="${PAYMENT_ID:-$(create_authorized_payment_id)}"
       call GET "/payment/$PAYMENT_ID" ;;
    10) call GET /payment/00000000-0000-0000-0000-000000000000 ;;
    11) call GET /payment/not-a-uuid ;;
  esac
}

list_steps() {
  for i in "${!TITLES[@]}"; do
    echo "$((i + 1)). ${TITLES[$i]}"
  done
}

if [ "${1:-}" = "list" ]; then
  list_steps
  exit 0
fi

if ! curl -s -o /dev/null "$BASE_URL/v3/api-docs"; then
  echo "Gateway is not reachable at $BASE_URL. Start it with ./gradlew bootRun"
  exit 1
fi

PAYMENT_ID=""

if [ -n "${1:-}" ]; then
  if [ "$1" -lt 1 ] || [ "$1" -gt "${#TITLES[@]}" ] 2>/dev/null; then
    echo "Unknown step. Use ./demo.sh list"
    exit 1
  fi
  echo "=== $1. ${TITLES[$(($1 - 1))]}"
  run_step "$1"
  exit 0
fi

for i in "${!TITLES[@]}"; do
  step=$((i + 1))
  echo
  read -r -p "[Enter] to run step $step of ${#TITLES[@]}: ${TITLES[$i]} " _
  run_step "$step"
done
echo
echo "Done."
