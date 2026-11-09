# PROJECT_CONTEXT.md

> **Project:** Household Finance App MVP  
> **Audience:** Codex, engineers, designers, and product reviewers  
> **Last consolidated:** 2026-11-09  
> **Status:** Product flows and the Android MVP delivery approach are approved; implementation has not started.

## 1. Purpose and product philosophy

Build a **mobile household-finance operating system** for **Media** and **Avery** that replaces or improves the household's spreadsheet-based process. It must make it easy to plan salary cutoffs, reserve money, track actual account balances, pay debts, manage credit-card funding, stuff physical cash envelopes, and collect money owed by others.

**Core product promise:** Record a real-world financial event **once**, and every related screen reflects its impact. The app optimizes for **clarity, accurate money movement, traceability, and low-friction daily use** rather than sophisticated graphs.

This is a household-specific MVP, **not** a generic personal-finance app. Currency is **Philippine pesos (PHP/₱)** and amounts must retain centavo precision. The default household calendar and date presentation should be appropriate for the Philippines; storage/time-zone implementation is a technical decision, not a reason to change the agreed payday rules.

### Intended users and payday cadence

Both users access the **same household financial data**. At entry, select the current context: **Media** or **Avery**. Selection can affect ownership, payday context, or presentation; it does **not** create separate household ledgers.

| User | Nominal paydays |
| --- | --- |
| Avery | 10th and 25th |
| Media | 15th and 30th |

If a nominal payday is a weekend or holiday, expect payment on the **previous banking/work day**. There are typically four salary events per month. The approved short-month and Philippine banking-holiday policies are recorded in Section 13 and the delivery plan in Section 16.

## 2. Scope boundaries

### In the MVP

1. **User context**: Media/Avery selector and shared household state.
2. **Home / Allocations**: payday-focused income and budget planning; reserve, pay, settle.
3. **Accounts**: savings banks and buckets, credit cards, withdrawal account, cash-envelope accounts.
4. **Transactions**: actual expense and debt activity; receipt scanning for expenses.
5. **My Debts**: one-time and installment liabilities, saved/reserved funds, payment history, payoff, Home links.
6. **Owed to Me**: person-based receivables, charges and payments, credit-card links, waivers, shareable statements.

### Explicitly deferred

Groceries module; Goals/savings-goals module; advanced analytics, charts, reports, financial insights, complex automation, advanced reminders, rich receipt management beyond basic MVP scanning, and additional lifestyle-finance features.

**Important:** An earlier clickable **Home UI sample** included a `Goals` navigation item and illustrative dates, figures, and controls. It was a **demo**, not a mandate to include Goals in the MVP or treat its amounts as real data. Use its high-level Home structure as design reference only. Follow the requirements in this document when the demo and locked scope diverge.

## 3. Financial terms and invariants

| Concept | Meaning |
| --- | --- |
| **Allocation** | An intention/plan for a particular payday or recurring expense, bill, debt, or savings purpose. |
| **Reserve / Saved** | Money set aside or earmarked. This is **not a payment** and does not automatically reduce a loan or settle a credit card. |
| **Pay** | A confirmed real-world transfer or obligation settlement with an actual money impact. |
| **Settle** | Finalize/close an allocation for the period, using an existing payment where already paid. |
| **Account / bucket** | An actually tracked balance or sub-balance; moving money between buckets in one bank does not change the bank's total. |
| **Funded / Others / Unfunded** | The three mutually exclusive parts of a credit-card outstanding amount, based on where payment money is or will come from. |
| **My Debts** | Household liabilities payable to creditors. |
| **Owed to Me** | Household receivables from other people; not salary/income. |
| **Financial event** | One traceable recorded action, potentially reflected in multiple modules. |

**Non-negotiable invariants**

1. For each credit card: **Outstanding = Funded + Others + Unfunded**. These represent outstanding obligations and their funding attribution, **not** three separate balances to add to physical cash.
2. **Saved ≠ Paid**. Savings/reservations alone never advance installments or reduce `Total Left`.
3. Repayments of **Owed to Me** receivables are **not Income**.
4. **Paying a credit-card bill is not a second expense.** The card purchase generated the expense; bill settlement discharges the liability and affects payment-account funds.
5. **Transfers between household accounts/buckets are not Expenses.** Internal movements must not inflate expense totals.
6. **A Home-linked debt payment is one payment event**, not a Home payment plus a second My Debts payment.
7. Every balance-changing action is **traceable**. Correct through explicit adjustment, reversal, correction, or waiver events rather than silently overwriting history.
8. **Never use binary floating point for money.** Store integer centavos, e.g. `205664` means `₱2,056.64`, or use a proven fixed-precision decimal money type.
9. A credit card has **exactly one designated funded account at a time**.
10. **Do not show one combined Total Debt** for all `My Debts`. Show the individual loans and their obligations.

For operations that touch several ledgers, update the underlying records **atomically** or apply equivalent consistency controls. An event should have a stable identifier and references to its source, destination, linked payment/allocation, and associated corrections. The desired outcome is correctness and idempotence, not a mandated database design.

## 4. Home / Allocations

Home is for **planning by payday/cutoff**, not entering daily purchases. A useful Home summary includes the selected payday, expected/actual salary, extra income, total income, allocated/completed/unallocated amounts, allocations, and amounts due before the next payday. These labels come from the agreed Home demo; its numeric values are illustrative.

### Locked allocation lifecycle

**Create → Reserve → Pay → Settle**

- **Create:** Define an allocation for a bill, debt, expense, or purpose, associated with a cutoff or recurring template.
- **Reserve:** Assign or earmark funds. No obligation has been paid yet.
- **Pay:** Record the real payment, using the appropriate source account and any linked debt.
- **Settle:** Finalize the period allocation. If already paid, do **not** generate a second debit.

A debt may be linked to a recurring Home allocation/template, so newly created monthly instances inherit the relationship. When a linked allocation is marked **Paid**, `My Debts` must reflect that same payment:

- decrement the chosen payment account;
- decrement reserved funds **only if those funds were actually used**;
- update debt remaining balance and installment progress;
- advance next due date as appropriate;
- append one debt-payment history entry, marked `Paid via Home`;
- protect against double payment if the allocation is subsequently `Settled`, retried, or reopened.

Users can change or unlink the allocation. Unlinking stops future automatic debt updates but **must not erase previous payment events or history**. A debt can alternatively use **Manual** tracking.

Recurring allocations and payday cycles should roll forward without destroying earlier periods.

## 5. Accounts

### 5.1 Savings accounts and buckets

A physical bank may have multiple buckets, e.g. **CIMB** with Bills, House Fund, Emergency Fund, and others. Savings-account detail shows **bank name, tracked bank balance, and every bucket immediately**. Do not hide buckets behind `View All`. Do not add an assigned/unassigned summary panel.

Each bucket has a detail view with **bucket balance, Transfer Money, Adjust Balance, and recent activity/history**. Add/edit/delete/reorder (long-press drag) accounts where appropriate.

Rules:

- Adding money to a bucket increases that bucket and the physical bank total.
- Moving money between buckets of the **same bank** changes the two bucket balances by opposite amounts and leaves the bank total unchanged.
- Bucket-level expenses debit the selected bucket and update the bank total consistently.
- Adjustments create audit/history entries.

Whether an account can contain a balance not assigned to a bucket is an unresolved data-model policy. **Do not introduce an unwanted assigned/unassigned UI just to solve it.**

### 5.2 Credit cards

Credit cards expose **Outstanding, Funded, Others, Unfunded, Credit Limit, Available Limit, Due Date, and Funded Account**. Do not duplicate funding summaries.

**Funding states**

- **FUNDED:** Payment money is **already physically in** the card's one designated funded account.
- **OTHERS:** The future payment source is named, but the money has **not arrived**; examples include Arjay, Jang, a future budget, or reimbursement.
- **UNFUNDED:** No payment source has been assigned.

Each card transaction visibly displays one of these labels. Card transaction list supports search; filters `All / Funded / Others / Unfunded`; sorting `Newest / Oldest / Highest amount / Lowest amount`.

**State transitions**

- `UNFUNDED → OTHERS`: add/edit the expected payment source. **Outstanding and bank balances do not change**.
- `OTHERS → FUNDED`: upon actual receipt of money, increase the designated funded account as applicable. **Outstanding does not change** just because funding became available.
- The payment flow that settles an actual credit-card bill is distinct from funding attribution; avoid recording the original purchase as a second expense.
- Do **not** create an extra `Left` funding status.

Funding must not imply a phantom cash transfer. A transaction marked `FUNDED` when created uses already-available money in the designated account; do not increase that account a second time on save.

### 5.3 Withdrawal Account and cash stuffing

Cash stuffing is **derived from cash allocations for the selected cutoff**. It is **not manually created in** the Withdrawal Account.

Show selected cutoff, tracked withdrawal-account balance, total cash needed, suggested ATM withdrawal count and pattern, and cash envelopes for that cutoff. Typical denominations: **₱100, ₱500, ₱1,000**.

**Preferred ATM strategy**

- Exactly **one** withdrawal above ₱1,000.
- Use additional **₱400 and ₱900** withdrawals to encourage more ₱100 banknotes.
- Minimize transactions while keeping counts of ₱400 and ₱900 reasonably balanced.
- Show suggestions as **estimates**, not guaranteed note output from the ATM.
- Avoid `Large` / `Mix` labels.

Example: **₱5,200 = ₱1,700 × 1 + ₱900 × 3 + ₱400 × 2**, totaling **6 withdrawals**. Handle totals that cannot satisfy the preferred pattern as an explicit, user-visible edge case; do not falsely promise a viable pattern.

**Envelope-by-envelope confirmation**

A separate **Confirm** action for each cash envelope:

1. Reduce Withdrawal Account by the envelope amount.
2. Increase that envelope's corresponding Cash Account by the same amount.
3. Mark that envelope confirmed and update `Confirmed count`, `Stuffed total`, and `Remaining`.

No global `Confirm Cash Stuffing`, no ATM-withdrawal confirmation step, and no intermediate `Cash on Hand` staging account. Reconfirmation or refresh must never duplicate a transfer.

Examples of cash envelopes: Eating Out, Snacks, Office Allowance, Buffer, Mineral Water, Gym, Transportation.

### 5.4 Cash accounts

Each Cash Account mirrors a **physical envelope** and shows Current Balance, Transfer Cash, Adjust Balance, and Transactions. Filters: `All / Money In / Money Out`; sorting: `Newest / Oldest / Highest / Lowest / Money in first / Money out first`.

**Daily cash spending is entered through Transactions, not the Cash Account detail screen.** For example, an Expense paid using `Cash → Eating Out` immediately reduces that Cash Account.

## 6. Transactions

The primary Transactions page supports **Search, Filter, Sort, Add Transaction, and a Receipt Scan shortcut**. Primary filters: `All / Expenses / Debt`. Sort by `Newest / Oldest / Highest / Lowest`. Rows should expose the originating payment account/card and relevant funding state, not merely a vague transaction name.

### Locked Add Transaction flow

The Add screen has a **two-type carousel only**:

1. **Expense**
2. **Debt**

**Income and Transfer are NOT third/fourth Add Transaction options.** Those financial operations may exist in their own appropriate flows.

### 6.1 Expense

Required fields: **Transaction Name, Amount, Date, Category, Paid Using**. Optional: **Receipt Scan** and **Note**.

Receipt Scan applies **only to Expense**. It may prefill merchant/name, amount, date, and suggested category; the user must **review and confirm** before Save. Never automatically save an OCR result or pretend its extracted fields are certain.

**Expense payment branches**

| Paid Using | User selects | Save effect |
| --- | --- | --- |
| **Cash** | Specific Cash Account, e.g. Eating Out | Decrease that Cash Account. |
| **Bank / Debit** | Bank and bucket (when applicable) | Decrease that bank/bucket balance. |
| **Credit Card** | Specific card and `Funded / Others / Unfunded` | Increase card outstanding, record its funding state, and create the card charge once; do not debit a bank account merely for making a card purchase. |

For card `FUNDED`, use the card's designated funded account as the available funding reference. For `OTHERS`, require an **editable Payment Source** (e.g. Arjay, reimbursement, November budget). For `UNFUNDED`, no source is set.

### 6.2 Debt transaction

Debt entries are explicitly routed to either **My Debts** or **Owed to Me**. These represent opposite directions of obligation and must not be conflated.

## 7. My Debts: household liabilities

Represent **each debt separately**, with `Total Left`, payment amount, installment progress, next due date, and status. Examples include credit-card loans, Pag-IBIG loans, housing loans, personal loans, installment financing.

### Debt types and fields

| Type | Required input |
| --- | --- |
| **One-time** | Debt Name, Creditor/Lender, Debt Amount, Due Date, Deduct From Account. |
| **Installment** | Debt Name, Creditor/Lender, **Debt Amount including interest**, Total Number of Installments, Number of Installments Already Paid, Start Date, Monthly Due Day, Deduct From Account. |

For installment debts, generate a schedule and derive **Installment Amount, Remaining Installments, Amount Paid, Remaining Debt, Next Due**. The schedule and balances must update after every recorded payment. Avoid fixed month-specific values or stale cached totals.

### Reserved money for a debt

Show a distinct `Money Saved for This Loan` section with:

- Saved/Reserved amount;
- Coverage %;
- Remaining Debt;
- Still Need to Save;
- Funding Status: **Partially Funded** or **Fully Funded**.

For example, when `Remaining Debt = ₱20,000` and `Saved = ₱20,000`, show **Fully Funded** while debt status remains **Active**. The household may intentionally keep those funds earning interest and continue paying monthly. **Do not reduce Total Left, complete the loan, or pay future installments merely because it is fully funded.** Reservations should be reconciled with the actual cash/bucket holding the money without creating duplicate physical balances.

### Record an installment payment

For an actual installment:

1. Debit the selected payment account **once**.
2. Consume reserved money **only to the extent actually used**.
3. Record a DebtPayment against the applicable installment.
4. Increment completed installments when the relevant installment is actually paid.
5. Decrease Remaining Debt/Total Left by the appropriate amount.
6. Advance Next Due according to the schedule.
7. Append a visible payment-history event.
8. Set debt to **Completed** after the final installment is satisfied.

Do not treat an incomplete or partial installment as a fully paid installment without an explicit payment policy.

### Full payoff is a separate action

Users choose between **Current Installment Payment** and **Full Payoff / Paid Overall**. An intentional full payoff settles the actual remaining payable amount, marks the remaining schedule settled as appropriate, and completes the debt. **Full funding is never an automatic trigger for payoff.** If the lender quotes a special settlement amount, rebate, or fee, confirm the actual payable amount; do not invent lender calculations.

### Home-linked vs manual tracking

A debt has configurable mode:

- **Linked to Home:** Choose a recurring allocation/template; recorded Home payments drive debt updates. Preserve the single source payment event and mark history `Paid via Home`.
- **Manual:** Record current installment, full payoff, saved money updates, corrections to installments paid/balance, or next due.

Changing or unlinking a Home allocation affects **future** behavior only. Corrections must remain auditable.

## 8. Owed to Me: receivables from people

This module is grouped **per person**, rather than grouping only by loan product. Example person: **Arjay**. A person screen contains `Total Left` (a running balance), ledger entries/charges/installments, incoming payments, and **Share Statement**.

- A charge **increases** the person's receivable.
- A payment **decreases** the person's receivable and **increases** the chosen receiving account.
- Do **not** require matching each repayment to a particular charge.
- Repayments are **not Income**.

### Add receivable (Add Transaction → Debt → Owed to Me)

Capture `Track Under (person)`, `Debt Name`, `Amount`, `Date`, and `How This Debt Happened`:

| Source | Effect |
| --- | --- |
| **Credit Card Swipe** | Create/retain the underlying card charge **once**; increase person's receivable; increase card outstanding once; can label card attribution `OTHERS`, source = person. |
| **Money Lent** | Decrease selected household payment account; increase person's receivable by the same amount. |
| **Manual Entry** | Increase receivable only; **no physical account or card movement**, useful for history/imports. |

An individual's receivable can be **one-time** or **installment**. Display the current installment obligation and progress, e.g. `18/24`, while preserving the individual ledger's running total.

### Record payment received

Fields: `Amount Received`, `Date Received`, `Received In Account`, optional `Note/Reference`. **No `Apply Payment To` field.** Record one negative person-ledger entry plus one positive receiving-account movement.

Example: Arjay owes **₱9,402.31** and pays **₱5,000** into UnionBank. New receivable: **₱4,402.31**; UnionBank increases **₱5,000**. Do not create salary/income.

If the money is actually received into the relevant card's **designated funded account**, Arjay-related credit-card attribution can move **OTHERS → FUNDED** without changing card outstanding. The product does **not** require users to allocate that incoming payment to individual charges. Any behind-the-scenes allocation must preserve aggregate conservation and avoid overfunding; exact partial-allocation policy remains an implementation question.

### Correct or waive receivables

- **Remove mistake:** Remove/reverse the incorrectly assigned **receivable**, but **do not automatically delete the real card transaction** linked to it. Example: the card charge was real but not Arjay's responsibility.
- **Waive balance:** Record a historical waiver event that reduces what the person owes but preserves original debt history and the waiver explanation.

These are separate workflows with different accounting meanings.

### Share Statement

Generate a clean, screenshot/share-ready **person-specific** statement with charges, installment descriptions/progress, payments, and current Balance Due.

**Never expose** household bank or savings balances, bucket details, other household debts, credit-card funded-account balances, or unrelated allocations. Statement contents are an intentionally narrow projection of the selected person's ledger.

## 9. Cross-module event matrix

| User action | Ledger effects / projections | Must NOT happen |
| --- | --- | --- |
| Save Cash Expense | Cash Account ↓; Expense History +1 | Re-enter spending through Cash detail. |
| Save Bank Expense | Chosen bank/bucket ↓; Expense History +1 | Debit a second account. |
| Save Card Expense | Card Outstanding ↑; corresponding funding attribution ↑; Expense +1 | Debit funded bank or count card payment as another expense. |
| Identify source for unfunded card transaction | `UNFUNDED ↓, OTHERS ↑` | Change Outstanding or account balances. |
| Receive earmarked card money | Receiving funded account ↑; `OTHERS ↓, FUNDED ↑` | Change Outstanding without settling bill. |
| Pay linked Home debt allocation | Payment account ↓; debt schedule/history update; Home shows paid | Duplicate debt debit or debt-payment history. |
| Confirm one cash envelope | Withdrawal Account ↓; Cash Account ↑; envelope confirmed | Repeat transfer on re-click/retry. |
| Lend money to someone | Selected account ↓; person's receivable ↑ | Record loan principal as daily spending or income. |
| Record repayment from someone | Receiving account ↑; person's receivable ↓ | Record repayment as income or force per-charge allocation. |
| Reserve enough money for a loan | Saved coverage ↑, funding = Fully Funded | Reduce Total Left or complete loan. |
| Monthly installment paid | Account ↓; debt remaining ↓; schedule progresses | Mark future installments paid. |
| Deliberate full payoff | Account ↓; loan settled; remaining schedule resolved | Trigger without user action. |
| Move between buckets at same bank | Source bucket ↓; destination bucket ↑ | Change bank total or show an Expense. |
| Correct erroneous receivable | Receivable reversed/corrected | Silently delete linked genuine card charge. |

**Consistency rule:** A single recorded payment/charge can appear in several screens, but all screens should resolve to **one underlying financial event**, not separate independently editable copies.

## 10. Conceptual data model (not a mandated schema)

The previous product conversation suggested these entities. Adapt them to the repository and chosen persistence architecture while preserving their relationships:

```text
Household
  ├─ User (Media, Avery)
  ├─ Account
  │    ├─ AccountBucket
  │    ├─ CashAccount
  │    └─ designated funded account references from CreditCard
  ├─ CreditCard
  │    └─ CreditCardTransaction / funding attribution
  ├─ AllocationTemplate? -> Allocation (payday/cutoff instance)
  │    └─ AllocationPayment (points to actual FinancialEvent)
  ├─ Transaction (Expense or Debt; payment method + references)
  │    └─ LedgerEntry / FinancialEvent
  ├─ Debt (optional recurring linkedAllocationId)
  │    ├─ DebtInstallment
  │    ├─ DebtPayment (points to actual FinancialEvent)
  │    └─ DebtReservation
  └─ ReceivablePerson
       ├─ ReceivableEntry (optional linkedTransactionId / linkedCreditCardId)
       └─ ReceivablePayment
```

Prefer stable IDs, explicit household ownership, account references, clear posting timestamps and effective dates, and relationships that allow the UI to project history and balances. Consider keeping a durable event/movement record separate from user-visible category labels; the source explicitly calls for a ledger but does **not** prescribe accounting library, database vendor, backend, or state-management framework.

**Suggested fields / constraints for a future schema review** (proposal, not approved schema):

- `amountCentavos` integer/fixed decimal; `currency = PHP`.
- An immutable event identifier plus `createdAt`, effective `date`, optional `reversalOf`/`correctsEventId`, source workflow, and actor.
- Each transfer has paired account deltas with zero net household-cash change.
- Debt installment records distinguish `due`, `partially paid`, `paid`, and `settled by payoff` when needed.
- Link `DebtPayment` and `AllocationPayment` to **the same payment event** when generated from Home.
- Keep reservation records tied to real stored funds without adding them again to aggregate wealth.
- Receivable entry may link a credit-card charge but does not own that charge's lifecycle.

## 11. UX, monthly rollover, and audit expectations

- Favor **plain household language**, minimal finance jargon, contextual forms, and clear balance effects before confirm.
- Keep the **two-type Add Transaction carousel** simple; dynamically reveal fields according to the chosen payment method or debt source.
- Keep details and histories easy to inspect, including provenance such as `Paid via Home`, `Cash • Eating Out`, or `BPI Amore • OTHERS • Source: Arjay`.
- Every destructive action needs explicit confirmation. Corrections, reversals, and waivers must be retrievable in history.
- Automatically generate upcoming recurring allocation instances, installments, next-due dates, and cutoff cycles without deleting older records or paying anything automatically.
- Ensure partial saves, retries, duplicate taps, offline replays, and linked-screen navigation do not double post money.
- Basic receipt scan is a **prefill assistant**, never autonomous bookkeeping.

## 12. Minimum acceptance scenarios

The scenarios below express expected behaviors. They are candidates for unit, integration, and end-to-end tests.

1. **Savings bucket transfer:** Move ₱1,000 from CIMB Bills to CIMB Emergency Fund. Bills −₱1,000, Emergency Fund +₱1,000, CIMB bank total unchanged. One auditable transfer.
2. **Cash expense:** Enter Yabu ₱850 from Cash → Eating Out. Eating Out balance −₱850, one expense in Transactions, no duplicate Cash detail entry.
3. **CC source assignment:** A ₱2,056.64 card transaction is UNFUNDED. Set source to Arjay. `UNFUNDED −₱2,056.64`, `OTHERS +₱2,056.64`; Outstanding and bank cash unchanged.
4. **CC funding receipt:** Receive ₱2,056.64 from Arjay in the card's designated funded account. Receiving balance +₱2,056.64, receivable −₱2,056.64 when linked, card `OTHERS → FUNDED`, Outstanding unchanged until bill payment.
5. **Envelope confirmation:** Confirm Snacks envelope ₱900. Withdrawal Account −₱900, Snacks Cash +₱900, confirmation persisted. A duplicate confirmation creates **no** second movement.
6. **Fully funded yet active:** Loan Total Left ₱20,000, Saved ₱20,000. Funding Status Fully Funded, Debt Status Active; no installment count or due date changes.
7. **Home-linked installment:** Pay linked La Mirada Pag-IBIG ₱13,000 from Home. Payment account −₱13,000, debt schedule progresses once, same payment event is linked in both modules. Marking Home Settled afterward does not re-debit.
8. **Full payoff:** A user explicitly selects Full Payoff and confirms actual settlement amount. Loan becomes Completed and future installments resolve appropriately; simply reserving the amount never causes payoff.
9. **Money lent:** Send Arjay ₱5,000 from Maya. Maya −₱5,000; Arjay's receivable +₱5,000; no salary/income and no extra duplicate expense.
10. **Person repayment:** Arjay's ₱9,402.31 becomes ₱4,402.31 after a ₱5,000 receipt in UnionBank, which rises ₱5,000. There is no Apply Payment To step.
11. **Misattributed receivable:** Reverse a mistaken receivable linked to a legitimate card purchase. Person balance adjusts, the card purchase remains.
12. **Statement privacy:** A generated Arjay statement includes only his charges, payments, and balance due, never household bank balances or other people/debts.
13. **Rollover:** Next month's cutoff and installment schedule are created without overwriting prior history, duplicating linked debt payments, or automatically settling reserved items.
14. **Money precision:** ₱2,056.64 and ₱0.01 flows round-trip exactly without float drift.

## 13. Approved MVP decisions and deferred work

The following decisions are approved for the Android MVP and supersede earlier open questions:

1. **Client and shared code:** Deliver Android first using the existing Jetpack Compose client and Kotlin Multiplatform shared domain. iOS UI parity is post-MVP, while shared boundaries remain iOS-compatible.
2. **Backend and persistence:** Use Supabase/Postgres as the authoritative store, with transactional server operations, row-level security and stable idempotency keys. Use SQLDelight for the Android read cache and offline command outbox.
3. **Users and authentication:** Media and Avery use individual email magic links and belong to one pre-created, allowlisted household. Public registration and in-app household creation are excluded.
4. **Synchronization:** Support cached offline reads and queued financial commands. Commands display a Pending state, replay safely on reconnect and reconcile with the server. Financial commands are append-only deltas; editable metadata uses optimistic version checks instead of silent last-write-wins updates.
5. **Balance composition:** Every savings-bank peso belongs to a visible bucket, including a normal `Main` bucket when needed. The parent bank total is derived from all buckets; no assigned/unassigned panel is introduced.
6. **Reservations:** Reserve creates an auditable earmark against a chosen bucket without physically transferring money. Reservations cannot exceed unreserved funds and cannot be counted twice.
7. **Debt schedule:** MVP installment debts use equal centavo installments, with any remainder assigned to the final installment. Users can record the exact current installment or an explicit full payoff. Partial, late and extra-principal payment policies are deferred.
8. **Credit-card settlement:** Card bills can be paid only from the card's designated funded account, up to both `Funded` and `Outstanding`. The account, Outstanding and Funded amounts decrease together; the bill payment is not another expense.
9. **Partial receivable funding:** When a person's repayment reaches an eligible card's designated funded account, reclassify that person's oldest eligible `OTHERS` charges first, capped by the receipt. Do not add an allocation field to the payment form or overfund a card.
10. **Corrections:** Posted financial events are immutable. Correct them through a linked reversal and optional replacement. A mistaken receivable attribution reversal must retain a legitimate linked card purchase.
11. **Receipt scanning:** Use on-device Android OCR for Expense prefill. OCR output is untrusted, never auto-posted and always falls back to manual entry.
12. **Release:** Deliver a signed private Android APK for Media and Avery with privacy-scrubbed crash reporting. Play Store release work is not part of this MVP.
13. **Home posting:** Recording actual salary or extra income on Home credits a selected account through a traceable event. For an ordinary non-debt allocation, Home `Pay` changes allocation status only and creates no account or Transactions movement. A linked debt payment remains the explicit exception and posts one real payment event.
14. **Negative balances:** A normal account, bucket or cash debit that would make the balance negative is permitted only after a clear warning and explicit confirmation recorded with actor and time. Reservations still cannot exceed unreserved funds.
15. **Dates and holidays:** Use Asia/Manila local dates. Media's nominal 30th payday uses the month's final calendar day when needed, then moves backward for weekends or configured Philippine banking holidays. A versioned server table is seeded from official published holiday dates. Debt due days missing from a month clamp to month-end and do not shift for holidays.
16. **Cash-withdrawal fallback:** If the preferred ATM pattern is infeasible, show a manual-withdrawal message rather than fabricating a valid denomination plan.
17. **Full payoff display:** Preserve the distinction between scheduled installments actually paid and remaining installments settled through an explicit payoff.

Explicitly deferred: iOS UI release timing, CSV/spreadsheet import, public Play Store release, flexible installment payments, credit-card refunds and negative card balances, public household signup, advanced analytics, Goals and the other post-MVP features in Section 2.

## 14. Approved implementation sequence

The earlier advisory sequence is superseded by the approved 12-sprint Android MVP roadmap in Section 16. Work must follow its dependencies and exit gates unless a later explicit decision revises the plan.

## 15. Source and authority notes

This specification consolidates decisions from the **Create Finance App MVP** project conversation and the attached `household_finance_mvp_project_context.md`. The additional `Create Finance App MVP.txt` contains an **illustrative clickable Home UI**, not an alternate complete requirements specification.

For coding-agent operating instructions, read [`AGENTS.md`](./AGENTS.md). Treat explicitly **locked** behaviors here as product requirements. Do not silently override them with generic budgeting-app patterns or incidental demo elements.

## 16. Approved Android MVP delivery plan

### 16.1 Delivery baseline

- **Capacity:** One primary developer using AI assistance.
- **Cadence:** Twelve two-week sprints, approximately 24 working weeks.
- **Target:** Android-first private household beta for Media and Avery.
- **Architecture:** Jetpack Compose Android UI, Kotlin Multiplatform shared finance domain, Supabase/Postgres authoritative backend, SQLDelight local cache/outbox and email magic-link authentication.
- **Milestones:** Core Money Alpha after Sprint 5; Household Workflow Beta after Sprint 9; Private MVP after Sprint 12.

The current repository is a healthy KMP scaffold with native Android and iOS presentation layers, but its media-dashboard vertical slice is placeholder functionality. Replace that product surface rather than treating it as finance-domain implementation.

### 16.2 Sprint roadmap

#### Sprint 1 - Product shell and authentication

**Goal and deliverables:** Replace the media dashboard with Home, Accounts, Transactions, My Debts and Owed to Me navigation. Add the Media/Avery context selector. Configure Supabase environments, email magic-link deep links, household membership and row-level security.

**Dependencies:** Existing KMP and Android scaffold; Supabase development project and the two allowlisted user emails.

**Exit gate:** Media and Avery can sign in and access the same empty household. An unrelated account cannot read it. No Goals, Income or Transfer carousel option exists.

#### Sprint 2 - Ledger and server foundation

**Goal and deliverables:** Introduce exact centavo `Money`, stable command/event IDs, effective dates, actor/source metadata, immutable financial events, paired account movements, linked reversals and typed transactional Postgres operations. Opening balances are explicit events.

**Dependencies:** Sprint 1 authentication, household ownership and server environment.

**Exit gate:** Atomic posting, duplicate-command replay, reversal, centavo precision and household-isolation tests pass against the server.

#### Sprint 3 - Offline cache and synchronization

**Goal and deliverables:** Add SQLDelight read projections and command outbox. Queue offline financial commands, show them as Pending, replay through Android WorkManager and reconcile authoritative server results. Protect editable metadata with row versions.

**Dependencies:** Sprint 2 command contracts and ledger event model.

**Exit gate:** An offline command reaches the server exactly once after reconnection, two devices converge and rejected optimistic commands roll back with a recoverable message.

#### Sprint 4 - Accounts and buckets

**Goal and deliverables:** Implement savings banks, required visible buckets, Cash Accounts and Withdrawal Account. Add opening balances, adjustments, archive/delete safeguards, long-press reordering, activity history and same-bank bucket transfers. Derive the bank total from its buckets.

**Dependencies:** Sprints 2-3 posting, local projection and synchronization infrastructure.

**Exit gate:** Moving ₱1,000 between CIMB buckets leaves the bank total unchanged and creates one auditable transfer. Used accounts retain their history through archival.

#### Sprint 5 - Transactions and credit cards

**Goal and deliverables:** Add the Expense/Debt carousel shell and complete Expense entry for Cash, Bank/Debit and Credit Card. Add cards, one designated funded account, funding attribution, payment-source assignment, funded-account bill settlement, search, filters, sorting and reversal-based corrections.

**Dependencies:** Sprint 4 account and bucket sources; Sprint 2 financial commands.

**Exit gate:** Cash and bank expenses debit once; card purchases increase Outstanding once; `Outstanding = Funded + Others + Unfunded`; bill payment creates no second expense. **Core Money Alpha achieved.**

#### Sprint 6 - Home and payday allocations

**Goal and deliverables:** Implement salary/cutoff cycles, expected and actual salary, extra income, recurring templates, allocation instances, Reserve/Pay/Settle status and cutoff summaries. Actual salary or extra income credits a selected account. Ordinary non-debt Home Pay is status-only.

**Dependencies:** Sprint 4 accounts, Sprint 2 income receipt event and approved calendar policy.

**Exit gate:** Media and Avery payday generation works in Asia/Manila across short months, weekends and configured holidays. Reserve does not pay anything; ordinary Pay and Settle do not move money or duplicate an action.

#### Sprint 7 - My Debts

**Goal and deliverables:** Implement one-time and installment debts, deterministic centavo schedules, next due date, bucket-backed reservations, exact current-installment payment, history, explicit Full Payoff and reversal-based correction.

**Dependencies:** Sprint 4 accounts/reservations and Sprint 2 posting primitives.

**Exit gate:** Fully funded debts remain Active; exact installments advance once; payoff requires explicit confirmation; unsupported partial and extra-principal payments are rejected without mutation.

#### Sprint 8 - Home-linked debts and rollover

**Goal and deliverables:** Link recurring allocation templates to debts. A linked allocation payment produces one account debit and one shared payment event projected in Home and My Debts. Add future-only unlinking, monthly generation and historical rollover protection.

**Dependencies:** Sprints 6-7 allocations and debt schedules.

**Exit gate:** The reference ₱13,000 linked-debt payment appears in both modules with one debit. Retry, reopen and Settle do not duplicate it; old cycles remain unchanged.

#### Sprint 9 - Withdrawal and cash stuffing

**Goal and deliverables:** Derive cutoff envelopes from cash allocations, calculate the preferred ATM pattern and confirm each envelope independently. Confirmation transfers Withdrawal Account money into the matching Cash Account.

**Dependencies:** Sprint 6 cutoff allocations and Sprint 4 Withdrawal/Cash Accounts.

**Exit gate:** A ₱900 envelope confirmation is zero-sum and idempotent. Infeasible ATM totals show a manual-withdrawal message. **Household Workflow Beta achieved.**

#### Sprint 10 - Owed to Me core

**Goal and deliverables:** Add people, running receivable ledgers, one-time/installment entries, Money Lent, Manual Entry, repayment receipt, waiver and mistaken-attribution reversal.

**Dependencies:** Sprint 4 account movements and Sprint 2 reversal primitives.

**Exit gate:** Money Lent debits the selected account and increases the receivable once. Repayment credits an account and reduces the person's balance without creating Income or requiring `Apply Payment To`.

#### Sprint 11 - Receivable/card integration and statements

**Goal and deliverables:** Add Credit Card Swipe as one linked card-charge/receivable action. Reclassify eligible `OTHERS` amounts using the approved oldest-first rule. Generate an allowlisted person-only PDF and invoke Android native sharing.

**Dependencies:** Sprint 5 card charges and Sprint 10 person ledgers.

**Exit gate:** Partial receipts never overfund a card; reversing a receivable attribution retains the card purchase; statement privacy tests exclude household balances and unrelated people/debts.

#### Sprint 12 - Receipt scan and private-beta hardening

**Goal and deliverables:** Add on-device Android receipt OCR for Expense prefill with mandatory review. Complete synchronization, security, negative-balance warning, recovery, accessibility, performance and privacy regression. Add privacy-scrubbed crash reporting and produce a signed private APK with rollback notes.

**Dependencies:** All prior feature sprints and release credentials.

**Exit gate:** All Section 12 acceptance scenarios, two-device convergence and duplicate replay pass. No sensitive financial data enters crash logs. **Private Android MVP released.**

### 16.3 Architecture and public interfaces

- Represent money as immutable `Money(amountCentavos: Long, currency = PHP)` and never use floating-point accounting arithmetic.
- Store an Asia/Manila `effectiveDate` separately from the UTC `recordedAt` timestamp.
- Give every financial write a stable `commandId`. A transactional Postgres operation validates and records all related movements and returns the existing result for a duplicate command.
- Group persistence around household/member identity, financial events and movements, accounts/buckets/reservations, cards/card charges, allocation templates/instances, debts/installments/payments and receivable people/entries/receipts.
- Expose typed domain operations such as `transferBuckets`, `recordExpense`, `reserveAllocation`, `payLinkedDebtAllocation`, `confirmCashEnvelope` and `receivePersonPayment`. Presentation code must never write balances directly.
- Apply optimistic local projections with a visible Pending state. On server rejection, roll back the optimistic projection and preserve a useful failure reason.
- Require explicit `allowNegative` confirmation when a normal debit would create a negative balance; record the confirming actor and time.

### 16.4 Testing and Definition of Done

- Reserve sprint capacity for shared-domain tests, Postgres/RLS integration tests, Android ViewModel tests, Compose UI tests and migration/replay coverage.
- Run formatting, shared tests, Android unit tests, lint and assembly on every merge.
- Test transactional operations against a local Supabase stack, including atomicity, household isolation, reversal and idempotency.
- Maintain end-to-end coverage for all Section 12 scenarios plus salary receipt, Home status-only behavior, negative-balance confirmation, two-device convergence and offline replay.
- A sprint is complete only when its vertical slice works, checks have executed, migrations are additive/reversible, decision documentation is current and the feature can be demonstrated with fake household data.

### 16.5 Delivery boundaries

- The editable source of truth is this `PROJECT_CONTEXT.md`; generated summaries or PDFs must not become competing specifications.
- Initial spreadsheet information is entered manually through explicit opening-balance/import events; no CSV importer is included.
- Public signup, iOS screens, public Play distribution, card refunds/negative card balances, flexible installment payments and post-MVP modules remain deferred.
- Generate shareable statements and roadmap documents from allowlisted data only; never include credentials, live account identifiers or private financial exports in committed fixtures or diagnostics.
