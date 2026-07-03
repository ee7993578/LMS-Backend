# Razorpay integration — what changed & how to set it up

## ⚠️ Rotate your test key first
Your screenshot shared a live Test Key ID + Secret on screen. Treat that secret as
burned — regenerate a new one from the Razorpay Dashboard → Settings → API Keys
before using this anywhere, even in test mode shared with others.

## Configuration
Set these as environment variables (don't hardcode real keys in `application.properties`):

```
RAZORPAY_KEY_ID=rzp_test_xxxxxxxx
RAZORPAY_KEY_SECRET=xxxxxxxxxxxxxxxx
RAZORPAY_WEBHOOK_SECRET=xxxxxxxxxxxxxxxx
```

`RAZORPAY_WEBHOOK_SECRET` is set separately in Razorpay Dashboard → Settings → Webhooks
when you add the webhook URL (see below) — it is NOT the same as your key secret.

## What was added

### 1. Library signup with a paid plan
`POST /api/public/create` is unchanged — still creates a library immediately (used when no
plan or the free/order-1 plan is chosen).

New paid flow:
- `POST /api/public/create/initiate` — body is the same `LibraryDTO` the register form already
  sends. If the chosen plan is free (or none chosen), it creates the library immediately and
  returns `{ requiresPayment: false, result: <LibraryDTO> }`. If the plan is paid, **nothing is
  created yet** — it stages the signup and returns Razorpay order details:
  `{ requiresPayment: true, razorpayOrderId, keyId, amountPaise, currency, paymentRecordId, planName }`.
- `POST /api/public/create/verify` — body `{ paymentRecordId, razorpayOrderId, razorpayPaymentId, razorpaySignature }`
  (exactly what Razorpay Checkout's success handler gives you). Verifies the signature and only
  then actually creates the library/admin/subscription.
- `POST /api/public/create/cancel` — body `{ paymentRecordId }`. Call this if the user closes
  the Razorpay popup without paying, so the staged signup doesn't linger.

### 2. Library admin's paid plan upgrade
The old manual flow (`POST /api/libraryadmin/plan-requests` → SuperAdmin approves) still exists
and is used automatically for **free** plan switches (SuperAdmin still signs off — no money
changed hands).

New paid flow:
- `POST /api/libraryadmin/plan-requests/initiate` — body `{ requestedPlanId, note }`. Creates a
  `PENDING` `PlanUpgradeRequest` row (for audit/history) and, if the plan is paid, a Razorpay
  order. Response shape is the same `RazorpayOrderResponseDTO` as above.
- `POST /api/libraryadmin/plan-requests/verify` — same body shape as signup's verify. On a valid
  signature, the plan switch is applied **immediately** — no SuperAdmin click needed. The
  `PlanUpgradeRequest` is marked `APPROVED` with an auto-generated resolution note, so it still
  shows correctly in the SuperAdmin "Plan Requests" history page (just with no action buttons,
  since it's already resolved).
- `POST /api/libraryadmin/plan-requests/cancel` — body `{ paymentRecordId }`. Cancels the pending
  request if the admin closes the checkout without paying.

### 3. Webhook (safety net)
`POST /api/public/webhooks/razorpay` — configure this URL in Razorpay Dashboard → Settings →
Webhooks, subscribed to at least `payment.captured`, `payment.failed`, `order.paid`. This covers
the case where the browser dies right after paying but before the `/verify` call completes —
Razorpay calls this independently. Both paths are idempotent and converge on the same logic.

### New table
`razorpay_payment` (auto-created by Hibernate `ddl-auto=update`) tracks every order: purpose
(signup vs upgrade), status (CREATED/PAID/FAILED/CANCELLED), amount, and links back to either
the staged signup payload or the `PlanUpgradeRequest` id.

## Frontend
- `src/utils/razorpay.js` — lazy-loads `checkout.js` and wraps Checkout in a promise.
- `src/pages/auth/RegisterLibrary.jsx` — calls `initiateLibrarySignup` → if payment required,
  opens Checkout → `verifyLibrarySignup` on success, `cancelLibrarySignup` on close/failure.
- `src/pages/libraryadmin/LibraryAdminSettings.jsx` — same pattern via `initiatePlanUpgrade` /
  `verifyPlanUpgrade` / `cancelPlanUpgrade`.

No new npm package was added — Razorpay Checkout is loaded via their standard `<script>` tag,
which is how Razorpay itself recommends embedding it.
