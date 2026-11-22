# Exspending requirements

Local-only personal cashflow app (Android, Kotlin, Jetpack Compose).

## Product decisions

- Single cashflow balance (no bank accounts).
- UI language: Dutch only.
- Code, comments, and KDoc: English.
- Date + time with hour and minute (no seconds), Brussels wall time (`Europe/Brussels`).
- Stored as `yyyy-MM-dd HH:mm` local strings (no timezone column).
- No ideal budget percentages in v1.
- Keep `minSdk` / `targetSdk` as configured in the project (37).
- Prefer simple Room + repository + Compose code; document in English.

## Core entities (SQL lowercase names)

| Table | Purpose |
|-------|---------|
| `categories` | Flat categories with `type`: `income`, `expense`, or `transfer` |
| `transactions` | Cash movements; `amount` always positive; direction from category type |
| `templates` | Named sets of default transactions (“sjablonen”) |
| `template_items` | Ordered template rows (no date/time) |
| `recurrence_rules` | Outlook-like schedule for a template |
| `app_meta` | Key/value app metadata |

### Initial balance (Beginsaldo)

- Stored as a `transactions` row with `is_initial_balance = 1`.
- Shown at the bottom of the newest-first list.
- Balance is always derived: initial balance ± signed amounts in `date_time` order.

### Default seed categories

- Income: Salaris, Beleggingsinkomen, Overig inkomen
- Expense: Huur, Nutsvoorzieningen, Internet en telefoon, Verzekeringen, Abonnementen, Boodschappen, Uit eten, Vervoer, Gezondheid, Shopping, Entertainment, Overig
- Expense also includes: Beleggingen
- Transfer: Sparen

## Time rules

| Action | Time assignment |
|--------|-----------------|
| Add income / expense | Now (Brussels) |
| Apply sjabloon manually | Start = now; each next item +1 minute by `sort_order` |
| Recurring generation | Start 13:00; each item +1 minute |
| Legacy Excel import (date only) | Per calendar day: first row 13:00, then +1 minute |
| Excel export | `yyyy-MM-dd HH:mm` |

## UI

- Screens: Transacties, Instellingen.
- Transaction list: scrollable, month section headers, running balance.
- Quick actions: + Inkomen, + Uitgave, + Sjabloon.
- Full edit of date, time, category, amount, note; deletes recalculate everything via Flows.
- Category settings: CRUD; deleting a used category requires remapping linked rows.
- Sjablonen: ordered items without date/time; optional recurrence (daily/weekly/monthly/yearly, interval, weekdays, month day, until date).
- Recurring occurrences generated on app start (and after rule changes) through today / until date.
- Settings: Excel import and export.

## Excel

### Export

One `.xlsx` with a sheet per table (`categories`, `transactions`, `templates`, `template_items`, `recurrence_rules`, `app_meta`). Headers = SQL column names.

### Import

1. App backup (round-trip of export sheets).
2. Legacy file (`Datum`, `Type`, `Amount`, `Category`, `Balance`, `Notitie`):
   - First balance value becomes Beginsaldo.
   - Create missing categories; map Savings/Investments → `transfer`.
   - Apply 13:00 +1 minute rule per day.
   - Ignore remaining Balance values (recomputed in app).

## Non-goals (v1)

- Multi-account
- Ideal % budgets
- Cloud sync / auth
- Seconds precision
