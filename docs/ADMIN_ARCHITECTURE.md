# Hello Kurukshetra Admin Architecture

## Where to find things

- `AdminApp.kt` — login entry point only.
- `ui/AdminShell.kt` — navigation and top-level admin layout.
- `ui/AdminComponents.kt` — shared cards, badges, empty/error states and JSON helpers.
- `feature/dashboard/` — operations dashboard.
- `feature/people/` — people/accounts.
- `feature/verification/` — driver + guide verification.
- `feature/rides/` — ride operations.
- `feature/payments/` — payments and refunds.
- `feature/emergency/` — emergency response.
- `feature/support/` — support tickets.
- `feature/promotions/` — promotions.
- `feature/notifications/` — notifications.
- `feature/adminusers/` — admin accounts and permissions.
- `feature/audit/` — audit history.
- `feature/settings/` — branding, home banner and ride pricing.

## Rule

When changing one feature, start in that feature's folder. Shared UI belongs in `ui/AdminComponents.kt`; API authentication belongs in `data/ApiClient.kt`.
