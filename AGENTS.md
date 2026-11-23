# AGENTS.md

Prisoner Pay API: calculates and records prisoner pay (special pay statuses and, in future, activity pay) via an overnight batch. Integration with Prisoner Finance is planned.

Business rules live in Confluence (space PPR, "Business Analysis"). Key pages: Pay Calculation Business Requirements, Multi-Pay Statuses Business Rules, the LTS/STS/Retirement status pages, Prisoner Pay Business Questions.

## Current schema

Source of truth: Flyway migrations in `src/main/resources/db/migrations/common/`.

**Schema snapshot through:** `V1_7__seed_short_term_sick_pay_rates.sql` (the latest migration; it seeds data rather than changing the schema). When adding migrations, update this section and the snapshot marker. All primary keys are `uuid default uuidv7()`.

### pay_status_period
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| prisoner_number | varchar(7) | not null |
| prison_code | varchar(5) | not null |
| type | varchar(20) | not null, e.g. LONG_TERM_SICK |
| start_date | date | not null |
| end_date | date | nullable, inclusive |
| created_date_time | timestamp | not null |
| created_by | varchar(80) | not null |

Indexes: prisoner_number, start_date, end_date, prison_code.

### pay_rate
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| prison_code | varchar(5) | not null |
| type | varchar(20) | not null, e.g. LONG_TERM_SICK, SHORT_TERM_SICK |
| start_date | date | not null |
| rate | integer | not null, pence per session |
| created_date_time | timestamp | not null |
| created_by | varchar(80) | not null |
| updated_date_time | timestamp | nullable |
| updated_by | varchar(80) | nullable |

Unique: (prison_code, type, start_date). Indexes: prison_code, start_date. Seed rows exist for RSI and BCI (LTS and STS).

### payment
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| prison_code | varchar(5) | not null |
| prisoner_number | varchar(7) | not null |
| event_date | date | not null, the date the status period or attendance relates to |
| time_slot | varchar(2) | nullable, AM or PM |
| payment_type | varchar(20) | not null |
| payment_date_time | timestamp | not null |
| payment_amount | integer | not null, pence |
| reference | varchar(12) | not null, unique |

Indexes: prisoner_number, prison_code, event_date, payment_date_time, unique reference.

## Changes (proposed, not yet implemented)

These are design proposals, not descriptions of the current implementation. See [ADR007](https://dsdmoj.atlassian.net/wiki/spaces/PPR/pages/6378523940) and [ADR008](https://dsdmoj.atlassian.net/wiki/spaces/PPR/pages/6379340294), both draft.

### Rules driving the design
- Pay is only ever positive. Never write negative pay lines or claw back; recovery is a separate Prisoner Finance deduction.
- Attendance can be marked or amended for 7 calendar days after the session. Paid cannot be changed to unpaid.
- Special pay applies to AM and PM, not ED. Compare applicable special pay and activity pay per session; activity pay may also apply in ED. Some allowances are independent.
- Waiting for attendance before paying was abandoned. Special pay is paid first and uplifted if later-marked activity pay is higher. Any docs that still say to wait are out of date.
- Corrections are not a record type. A top-up is an ordinary pay record whose detail supersedes an earlier detail.

### Reconciliation gate (per prisoner, session date, session)
1. Compute the entitlement for each applicable component, applying the relevant precedence and restriction rules.
2. Reconcile against relevant existing details and their issuance states, keeping independent payable components separate.
3. If payable activity attendance is recorded within the 7-day window after special pay was issued, pay only any positive difference. For equal, lower or unpaid activity, make no further payment and do not claw back.
4. Confirm the window boundary and how to process attendance recorded in time but processed later. Do not silently drop missed initial payments or retryable failures because a lookback window has elapsed.

### New tables
Proposed replacement of the flat `payment` table with a pay record and pay detail model; migration and Finance transaction grouping remain to be designed.

**pay_record** (one transaction to Prisoner Finance)
- id uuid PK
- prisoner_number, session_date, session (including ED for applicable activity pay)
- sequence integer, so a session can have more than one record
- total_amount integer (pence), equal to the sum of its details
- finance_transaction_id (external reference); the Finance idempotency contract remains to be confirmed
- processed timestamp (optional, for ordering and reconciliation)
- Unique: (prisoner_number, session_date, session, sequence)
- Unique: (id, prisoner_number, session_date, session), to support the composite FK from detail

**pay_detail** (why money was paid; append-only)
- id uuid PK
- prisoner_number, session_date, session
- pay_type (e.g. SPECIAL type, ACTIVITY)
- amount_paid integer (pence paid in this transaction)
- rate_applied integer (full session rate of this component)
- pay_record_id uuid, nullable while details are not linked to an instruction
- supersedes_pay_detail_id uuid, nullable, self-reference
- Composite FK (pay_record_id, prisoner_number, session_date, session) to pay_record, so a detail always matches its parent (not enforced while pay_record_id is null; the issue step must assert it)
- Composite FK (supersedes_pay_detail_id, prisoner_number, session_date, session) to pay_detail, with a unique constraint on (id, prisoner_number, session_date, session)
- Unique on supersedes_pay_detail_id where not null, so chains stay linear
- No incentive level column; see the mapping table below

**pay_detail_attendance** (activity details only)
- pay_detail_id uuid PK and FK to pay_detail
- attendance_id (activities API attendance ID; soft reference, no FK as attendances can be deleted)

### Behaviour
- Details can be written before issuing. Design durable issuance states and retry/concurrency handling with the actual Finance contract; linking a detail to a record alone does not prove payment succeeded.
- For the simple special-pay-to-activity uplift, the amounts paid along the chain sum to the winning rate. Do not apply this invariant blindly to independent payable components or pending/failed payments.
- Example: AM session, special pay 120p paid first, activity 250p marked later. Detail 1 is SPECIAL, 120 paid, 120 rate. Detail 2 is ACTIVITY, 130 paid, 250 rate, supersedes detail 1.
- Reporting must distinguish prepared, pending, failed and successfully issued payments.
- Special pay rates come from `pay_rate` in this service. Activity amounts come from the activities API attendance `payAmount`; this service never recomputes activity rates.

### Changes needed in hmpps-activities-management-api
- Store the prisoner's incentive level code on the attendance row alongside `payAmount`, in the 04:00 job and the mid-day creation path (`createAnyAttendancesForToday`). Do not default a missing level to a zero rate silently; leave it null and alert.
- The proposed rule treats same-day incentive changes as effective for pay from the following day. Same-day creation currently uses the then-current level, so a consistent approach needs design and confirmation.

### Open questions
- Business Questions Q5 (delay pay), Q6 (condensing transactions across sessions), Q7 (weekends and bank holidays), Q8 (transfers), Q10 (status added and removed same day). If Q6 is answered "condense", the session fields move off the pay record.
- How to treat attendances created before the incentive level column exists.
- How to retain or recover the activity pay basis if attendance records are deleted or changed.
- Confirm with the BAs that provisional special pay followed by an uplift is the rule, not waiting for attendance (the Retirement page still describes waiting).
