docker run \
  -e SPRING_DATASOURCE_URL="jdbc:postgresql://prisoner-pay-api-db:5432/prisoner-pay-api-db?sslmode=prefer" \
  -e HMPPS_SQS_LOCALSTACK_URL="http://prisoner-pay-api-localstack:4566" \
  -e SPRING_PROFILES_ACTIVE="local" \
  --network hmpps-prisoner-pay-api_hmpps \
  -p 8080:8080 \
  prisoner-pay-api