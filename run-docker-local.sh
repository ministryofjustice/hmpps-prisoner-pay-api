docker run \
  -e DB_SERVER=localhost:5432 \
  -e DB_USER=prisoner-pay-api \
  -e DB_PASS=prisoner-pay-api \
  -e DB_SERVER=prisoner-pay-api-db:5432 \
  -e API_BASE_URL_HMPPS_AUTH="https://sign-in-dev.hmpps.service.justice.gov.uk/auth" \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://prisoner-pay-api-db:5432/prisoner-pay-api-db?sslmode=prefer" \
  -e HMPPS_SQS_LOCALSTACK_URL="http://prisoner-pay-api-localstack:4566" \
  -e SPRING_PROFILES_ACTIVE="local" \
  --network hmpps-prisoner-pay-api_hmpps \
  -p 8080:8080 \
  prisoner-pay-api