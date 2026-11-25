#!/usr/bin/env bash
# Walks through every outcome of the gateway. Start the bank simulator and the gateway first:
#   docker-compose up -d
#   ./gradlew bootRun
set -u

BASE_URL="${BASE_URL:-http://localhost:8090}"
YEAR=$(( $(date +%Y) + 2 ))

if ! curl -s -o /dev/null "$BASE_URL/v3/api-docs"; then
  echo "Gateway is not reachable at $BASE_URL. Start it with ./gradlew bootRun"
  exit 1
fi

# Sends a request, prints what was sent and what came back. The body goes to LAST_BODY.
call() {
  local title="$1" method="$2" path="$3" body="${4:-}"
  echo
  echo "=== $title"
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

payment() {
  # card number, month, year, currency, amount, cvv
  echo "{\"cardNumber\":\"$1\",\"expiryMonth\":$2,\"expiryYear\":$3,\"currency\":\"$4\",\"amount\":$5,\"cvv\":\"$6\"}"
}

call "1. Authorized (card ends in an odd digit) -> 200" POST /payment \
  "$(payment 2222405343248877 4 "$YEAR" GBP 100 123)"
AUTHORIZED_ID=$(echo "$LAST_BODY" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

call "2. Declined (card ends in an even digit) -> 200" POST /payment \
  "$(payment 2222405343248878 4 "$YEAR" USD 2500 123)"

call "3. Bank unavailable (card ends in 0) -> 502, nothing stored" POST /payment \
  "$(payment 2222405343248870 4 "$YEAR" EUR 100 123)"

call "4. Rejected: several invalid fields, all reported at once -> 422" POST /payment \
  "$(payment 12 13 2020 GBP 0 1)"

call "5. Rejected: required fields missing -> 422" POST /payment '{}'

call "6. Rejected: card already expired -> 422" POST /payment \
  "$(payment 2222405343248877 1 2020 GBP 100 123)"

call "7. Malformed JSON -> 400" POST /payment '{oops'

call "8. Amount is not an integer -> 400" POST /payment \
  "{\"cardNumber\":\"2222405343248877\",\"expiryMonth\":4,\"expiryYear\":$YEAR,\"currency\":\"GBP\",\"amount\":10.5,\"cvv\":\"123\"}"

call "9. Retrieve the authorized payment (only last four digits) -> 200" GET "/payment/$AUTHORIZED_ID"

call "10. Retrieve an unknown id -> 404" GET /payment/00000000-0000-0000-0000-000000000000

call "11. Id that is not a UUID -> 400" GET /payment/not-a-uuid
