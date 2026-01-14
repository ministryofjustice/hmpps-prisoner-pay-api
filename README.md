# hmpps-activities-management-api

[![repo standards badge](https://img.shields.io/badge/endpoint.svg?&style=flat&logo=github&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-prisoner-pay-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-report/hmpps-prisoner-pay-api "Link to report")
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-prisoner-pay-api)
[![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://prisoner-pay-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html?configUrl=/v3/api-docs/swagger-config)

This service allows for the management of payments for prisoners.

## Building the project

Tools required:

* JDK v25
* Kotlin (Intellij) v2.2
* PostgresSQL v18
* [Docker](https://www.docker.com/)
* [Docker Compose](https://docs.docker.com/compose/)

Useful tools that can be installed, using [Homebrew](https://brew.sh/), but are not essential:

* [kubectl](https://kubernetes.io/docs/reference/kubectl/) - not essential for building the project but will be needed for other tasks.
* [k9s](https://k9scli.io/) - a terminal-based UI to interact with your Kubernetes clusters.
* [jq](https://jqlang.github.io/jq/) - a lightweight and flexible command-line JSON processor.
* [AWS CLI](https://aws.amazon.com/cli/) - useful if running [LocalStack](https://www.localstack.cloud/), interrogating queues, etc.

## Install gradle and build the project

```bash
./gradlew
```

```bash
./gradlew clean build
```

## Running the service locally

Add a local `.env` file to the root of the project:

#### Set up the local environment variables
```
DB_NAME=prisoner-pay-api-db
DB_USER=prisoner-pay-api
DB_PASS=prisoner-pay-api
DB_SERVER=localhost:15432
API_BASE_URL_HMPPS_AUTH=https://sign-in-dev.hmpps.service.justice.gov.uk/auth
```

- You **must** escape any '\$' characters with '\\$'
- `DB_SERVER` should include the port of the local Postgres DB Docker container.

#### Run LocalStack and Postgres Docker containers
```bash
docker-compose up --remove-orphans
```

## Common gradle tasks

To list project dependencies, run:

```bash
./gradlew dependencies
``` 

To check for dependency updates, run:
```bash
./gradlew dependencyUpdates --warning-mode all
```

To run an OWASP dependency check, run:
```bash
./gradlew clean dependencyCheckAnalyze --info
```

#### KtLint

To run Ktlint check:
```bash
./gradlew ktlintCheck
```

To run Ktlint format:
```bash
./gradlew ktlintFormat
```

### Building and running the docker image locally

The `Dockerfile` relies on the application being built first. Steps to build the docker image:
1. Build the jar files
```bash
./gradlew clean assemble
```
2. Copy the jar files to the base directory so that the docker build can find them
```bash
cp build/libs/*.jar .
```
3. Build the docker image with required arguments
```bash
docker build -t prisoner-pay-api .
```
4. Run the docker image
```bash
./run-docker-local.sh
```