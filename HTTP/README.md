# HTTP Client

## Setup

Create a file called `http-client.private.env.json` like this:

```json
{
  "local": {
    "orchestrator-client-id": "<<orchestrator-client-id>>",
    "orchestrator-client-secret": "<<orchestrator-client-secret>>",
    "ui-client-id": "<<ui-client-id>>",
    "ui-client-secret": "<<ui-client-secret>>"
  },
  "dev": {
    "orchestrator-client-id": "<<orchestrator-client-id>>",
    "orchestrator-client-secret": "<<orchestrator-client-secret>>",
    "ui-client-id": "<<ui-client-id>>",
    "ui-client-secret": "<<ui-client-secret>>"
  }
}
```

where `<<orchestrator-client-id>>`, `<<orchestrator-client-secret>>`, `<<ui-client-id>>` and `<<ui-client-secret>>` can be obtained from Kubernetes secrets.

See IntelliJ HTTP client docs [here](https://www.jetbrains.com/help/idea/http-client-variables.html#example-working-with-environment-files).