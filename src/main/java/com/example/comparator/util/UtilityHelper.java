CreditApply / PrequalApply 
    │
    ├──► Existing Flow: Save data to `tempass` DB
    │                    Publish to existing RabbitMQ (consumer1)
    │
    └──► New Flow:
         └─ Publish to new RabbitMQ queue: `apply.app.data.queue`
                     │
                     ▼
         New Application: [Name TBD]
                     ├──► Save to DB table: `APPLICATION_DATA` (excluding accountNumber, creditLimit)
                     └──► Publish to [New Queue Name TBD] for email triggering
                                       │
                                       ▼
                         Consumed by `email-consumer`
                                       │
                         ├── Validate `decision-code` for allowed values
                         ├── Check EMAIL_DATA to avoid re-sending
                         ├── If PSCC client, call RAPID service
                         ├── Match Email Template
                         ├── Call Zeta Email Service
                         └── Update EMAIL_DATA with final status

    | Topic                                                            | Notes                                                                         | Confirmation |
| ---------------------------------------------------------------- | ----------------------------------------------------------------------------- | ------------ |
| ✅ **No changes to existing logic in CreditApply / PrequalApply** | Only new RabbitMQ publish logic added                                         | \[ ]         |
| ✅ **New Queue Name**                                             | `apply.app.data.queue` for incoming message                                   | \[ ]         |
| ✅ **New Application Name**                                       | TBD – Proposed: `application-data-processor`                                  | \[ ]         |
| ✅ **Second Queue Name (for email trigger)**                      | Proposed: `application.email.trigger.queue`                                   | \[ ]         |
| ✅ **Template resolution logic based on partial match**           | Use best-effort with available fields                                         | \[ ]         |
| ✅ **Handling of missing template scenario**                      | Log and exit cleanly                                                          | \[ ]         |
| ✅ **503 response retry logic**                                   | Retry 2 times max only on 503                                                 | \[ ]         |
| ✅ **Fields passed to email-consumer**                            | Full payload including accountNumber, creditLimit, response\_code, reasonCode | \[ ]         |
| ✅ **EMAIL\_DATA duplication check logic**                        | By `applicationId` only                                                       | \[ ]         |
