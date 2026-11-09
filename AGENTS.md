# AGENTS.md

> Instructions for **OpenAI Codex** and any coding agent working in this repository.  
> **Read [`PROJECT_CONTEXT.md`](./PROJECT_CONTEXT.md) before planning, writing, or changing product code.**

## 1. Mission

Build and maintain a **reliable, understandable household finance mobile MVP** for **Media** and **Avery**. The app is a connected system of allocations, accounts, transactions, debts, and receivables. Its distinguishing requirement is **one real-world event, one underlying accounting record, multiple synchronized views**.

Optimize for correctness, simple daily workflow, auditable balance changes, and privacy. **Do not turn this into a generic finance dashboard** or add features outside the approved MVP without direction.

## 2. Product source of truth

Use these sources in order:

1. **Current explicit user instruction** (when it intentionally revises a previous decision).
2. **Locked product decisions in `PROJECT_CONTEXT.md`**, which consolidate the project conversation.
3. **Existing implementation and tests**, provided they do not contradict locked product decisions.
4. **Illustrative mockups / example values**, only for layout ideas, not accounting requirements.
5. **Agent suggestions and generic finance conventions**, only when no higher-priority requirement answers the question.

If an older mock, a code comment, or a common budgeting pattern contradicts a locked rule, **do not silently favor it**. Call out the discrepancy in the implementation plan or final summary. Do not silently change a decided workflow.

**Crucial distinction:** A source can lock *product behavior* without locking *technical implementation*. The conversation does **not** choose a mobile framework, backend, database, authentication model, OCR service, or full accounting engine. **Discover these from the repository or flag them as undecided.**

## 3. First steps on every coding task

1. Read this file, `PROJECT_CONTEXT.md`, and any deeper folder-specific `AGENTS.md` files.
2. Inspect the repository tree and existing `README`, package/build manifests, architecture documentation, data models, migrations, and tests.
3. Identify the app framework, package manager, lint/format/test commands, and prevailing naming/concurrency/error-handling conventions **from actual files**. Never fabricate commands or assume Flutter/React Native/Kotlin/Swift/etc.
4. Locate the current source of truth for account balances and financial events. Trace the complete effect of any money-affecting action before editing it.
5. State a **small implementation plan** for multi-file or stateful changes and identify which invariant(s) the task touches.
6. Make the smallest coherent change, validate it, and report what actually ran.

When starting from an empty repository, propose the smallest architecture that can support **atomic, auditable postings and shared household state**. Record major decisions instead of presenting them as preapproved.

## 4. MVP boundaries to enforce

**Included:** User context; Home/Allocations; Savings, Credit Cards, Withdrawal and Cash Accounts; Transactions; My Debts; Owed to Me; basic expense receipt scanning and person statements.

**Deferred:** Groceries, Goals, advanced charts/analytics/reports, automated financial insights, advanced reminders, rich receipt management, unrelated lifestyle modules.

**Do not accidentally reintroduce:**

- `Income` and `Transfer` as extra Add Transaction carousel types. Only **Expense** and **Debt** belong there.
- Daily spending created directly from Home or Cash Account detail.
- A global `Confirm Cash Stuffing` button, ATM withdrawal confirmation step, or `Cash on Hand` staging account.
- A single combined `Total Debt` across all `My Debts` loans.
- Multiple designated funded accounts per credit card.
- A fourth `Left` credit-card funding state.
- A mandatory `Apply Payment To` field when receiving a person's repayment.
- Unrequested assigned/unassigned bank-bucket summary or buckets hidden behind `View All`.
- Automatic loan payoff when savings happen to cover the remaining balance.
- `Goals` module merely because it appeared in an illustrative clickable Home demo.

## 5. Money handling rules (highest priority)

### Precision and postings

- Use **integer centavos** or a tested fixed-precision decimal money type. Never rely on binary floats for arithmetic, aggregation, persistence, or cross-layer serialization.
- Keep the original **effective date** separate from recorded/updated timestamps where applicable.
- Use **stable identifiers** for transactions, ledger events, transfers, loan payments, allocations, and reversals.
- Every actual money movement must be **auditable**, including its account(s), amount, action, date, and origin.
- Prefer **one atomic command / unit of work** for all records affected by a single action. If persistence architecture lacks transactions, implement appropriate guards, recovery, and consistency checks.
- Support **idempotency** for retries, rapid taps, app restarts, offline replay, and processing the same Home-linked payment from multiple screens.
- Never implement "change account balance" as an unexplained overwrite. Make an explicit **adjustment event** and preserve provenance.
- Do not hide unexpected negative balances or non-conserving transfers with silent clamping; validate and report them.

### Cardinal invariants

1. `card.outstanding == card.funded + card.others + card.unfunded` using exact money arithmetic.
2. **Reserved/Saved money is not payment**; it does not reduce the debt or mark an installment paid.
3. **Credit-card purchases create expenses; credit-card bill payments do not create a second expense.**
4. **Inter-account transfers are not expenses or income.**
5. **Collections of money owed to the household are not income.**
6. **Home-linked debt payment is one payment event** shared by the Home and My Debts projections.
7. **Internal same-bank bucket transfers preserve the parent bank total.**
8. **Cash stuffing** moves money from Withdrawal Account to one envelope Cash Account with no creation/destruction of cash.
9. For person-linked card charges, keep the genuine card transaction even if the receivable attribution is later corrected or waived.
10. Closing/settling an allocation **must not repeat** a previously recorded payment.

Every task that changes monetary state must consider these invariants explicitly.

## 6. Module-specific implementation constraints

### Home / Allocations

- Preserve lifecycle **Create → Reserve → Pay → Settle**.
- Generate recurring payday/cutoff allocation instances without rewriting prior months.
- Prefer linking a debt to a **recurring allocation template** over just one monthly occurrence.
- Paying a linked allocation updates the debt schedule and account once; subsequent settlement only closes the allocation.
- Unlinking changes **future events only**; historical links/payments remain inspectable.
- Support both Media (15th/30th) and Avery (10th/25th); previous banking/work day if weekend/holiday. Treat short months and holidays as explicit edge cases.

### Savings and cash

- A bucket deposit/withdrawal flows consistently into its parent bank balance.
- A transfer between buckets under the same bank must not change parent total.
- A Cash Account is a physical envelope mirror; daily expenses are added from **Transactions**.
- Withdrawal Account generates a **suggested** ATM plan from cutoff cash allocations. Prefer one withdrawal above ₱1,000 and other ₱400/₱900 withdrawals; do not guarantee ATM banknotes or fabricate a solution for infeasible totals.
- Cash-stuffing confirmations operate **per envelope**, and must be idempotent.

### Credit cards

- Exactly one designated funded account per card at a time.
- A new card charge increases **Outstanding** and exactly one funding-state component.
- `UNFUNDED → OTHERS`: assign expected source; **no bank change**.
- `OTHERS → FUNDED`: only once funds **actually arrive**; **Outstanding does not change** from the attribution change.
- `FUNDED` at charge creation means the money is already available; do not generate a fictitious incoming deposit.
- Implement bill payment as liability settlement, **not** a new purchase expense.
- Surface funding status per transaction plus search/filter/sort.

### Transactions

- Add flow has **exactly two choices**: Expense and Debt.
- Expense: name, amount, date, category, payment method; optional note and receipt scan.
- Cash expense decreases selected Cash Account; bank expense decreases selected bank/bucket; card expense increases card liability and funding attribution.
- Receipt scanning is **Expense only** and **prefills** suggested fields. User must review and explicitly Save. Never auto-post OCR results.
- Debt flow routes to **My Debts** or **Owed to Me**, never a generic undifferentiated debt pool.

### My Debts

- Support one-time and installment liabilities, including total installments, already paid, start date, monthly due day, total debt **including interest**, and deduction account.
- Derive installment schedule, amount paid, remaining balance, remaining installments, next due, and completion state consistently.
- An **actual installment payment** debits the account, updates the relevant installment, remaining debt, due date, and payment history exactly once.
- **Saved ≠ Paid**. A loan may be Fully Funded and still Active with all scheduled installments outstanding.
- Expose an **explicit Full Payoff** command separate from Record Current Installment.
- For a lender settlement amount differing from scheduled total, do not guess interest rebates/fees; use a confirmed payable amount and auditable adjustment/settlement model.
- Manual corrections require history. Never silently rewrite an already-recorded installment.

### Owed to Me

- Use a **person-centered running ledger**. New charges increase what that person owes; collections decrease it.
- Credit-card swipes for a person create a real card charge **once** and a linked receivable; money lent debits a chosen account and creates a receivable; manual historical entry changes the receivable only.
- Payment received credits the receiving account and decreases the person's running balance. **No mandatory per-charge allocation form**.
- When payment is genuinely received in the card's designated funding account, related `OTHERS` amounts can become `FUNDED` without reducing card outstanding.
- Distinguish a **mistaken attribution reversal** (linked card purchase stays) from a **waiver** (valid obligation forgiven with record preserved).
- Share Statement must include only data for the chosen person and exclude all internal household financial balances and unrelated obligations.

## 7. Required design for changes to financial state

When adding or changing a command that affects balances, identify all of the following **before coding**:

| Question | Expected documentation |
| --- | --- |
| What happened? | One domain command and one stable event identity. |
| Which amounts move? | Source and destination balances/deltas, or liability/receivable changes. |
| Which projections change? | Home, Accounts, Transactions, My Debts, Owed to Me as applicable. |
| What is unchanged? | Explicit invariants (e.g. card Outstanding on `OTHERS → FUNDED`). |
| What happens on retry? | Idempotent success / same event, no duplicate movement. |
| What if validation fails? | No partial committed movement; useful error. |
| What if corrected? | Reversal/adjustment with traceable original event. |
| How is it tested? | Unit test for arithmetic + integration test for cross-module effect. |

Prefer **domain use-cases/commands and pure calculation functions** rather than scattering account arithmetic in widgets/screens, controllers, or view models. UI should call one operation and render derived read models; no page should independently replicate ledger effects.

Do not assume that a strict double-entry accounting implementation is already chosen. The requirement is **traceable, correct financial movements**; if adding a double-entry engine is appropriate, justify it against the current code and scope.

## 8. Test obligations

### Run what the repository supports

Discover existing **format/lint/typecheck/unit/integration/build** commands from manifests, README, and CI configuration. Run relevant commands for the changed code. If a command or dependency is missing, say what could not run and why. **Never claim tests passed without executing them.**

### Mandatory behavioral coverage for money-touching work

Add or update targeted tests for each affected scenario:

- Centavo precision and totals; zero/negative input validation and amount rounding.
- Parent bank vs bucket balance on deposits, withdrawals, and same-bank transfers.
- Cash expense debits only the chosen envelope.
- Credit-card Outstanding = Funded + Others + Unfunded after charges, transfers between funding states, receipts, and bill settlement.
- Funding attribution changes that must **not** mutate Outstanding or bank balances.
- One linked Home debt payment reflected in both modules with **one actual debit**; confirm repeat Save/Pay/Settle/retry is idempotent.
- Debt reserve to 100% while **Active**, then monthly payment, then explicitly confirmed full payoff.
- Correct installment count, next due, remaining amount, final completion, month-end/leap-year dates, and rounding of final installment.
- Envelope-by-envelope confirmation and duplicate-click/retry behavior.
- Person charge, money-lent entry, incoming payment, waiver, and mistaken charge attribution reversal.
- Partial person repayment that doesn't require per-charge user allocation and doesn't overfund a card.
- Shared statements exclude unrelated persons and private household balances.
- Recurring rollover preserves previous month's balances, statuses, and history.
- Editing/reversal preserves an audit trail and avoids double-posting.

**Reference numeric scenarios:** See `PROJECT_CONTEXT.md` section **12. Minimum acceptance scenarios**. Prefer deterministic data fixtures and exact expected centavo values. Add integration tests for every action spanning more than one module.

## 9. UX, data exposure, and security

- Use familiar household language and reveal only fields needed for the selected workflow.
- Display exact money movement and funding effects before destructive/irreversible confirmation.
- Separate `Reserved`, `Paid`, `Settled`, `Active`, `Completed`, `Partially Funded`, and `Fully Funded` correctly. Funding states are **not** debt statuses.
- Never embed real household bank details, credentials, personal access tokens, or actual transaction exports as sample code or committed test fixtures. Use obvious fake data.
- Treat bank-account information, debts, receipts, and statements as private financial data. Avoid verbose production logs that reveal amounts, names, or account identifiers when unnecessary.
- The **shareable person statement** is an allowlisted, person-scoped view, **not** a screenshot of an internal account page.
- Require a deliberate confirmation for delete, waive, reverse, adjust balance, and full payoff actions.
- Prevent other-user/person leakage in API queries and UI projections once authentication and access controls exist.
- Receipt OCR output is **untrusted suggested input**: validate amounts/dates and require user review.

## 10. Code quality and change discipline

- Follow repository conventions for modules, naming, formatting, state management, dependency injection, and tests. Do not impose a new architecture or library because it is fashionable.
- Keep domain calculations and state transitions small, testable, and deterministic. Keep presentation/UI separate from balance computation.
- Centralize cross-module posting where practical rather than doing multiple ad-hoc UI writes.
- Avoid broad refactors or unrelated formatting churn during feature work.
- Treat migration, historic data import, reconciliation, and financial corrections as **high-risk**. Favor safe reversible migrations and test existing data.
- Never alter locked product semantics just to simplify implementation. State the trade-off and request product direction if necessary.
- If requirements are ambiguous, document the proposed assumption in the change summary or a short decision record. **Do not invent lender rates, financial totals, bank holidays, or personal financial records.**
- Keep `PROJECT_CONTEXT.md` in sync when an explicit new product decision supersedes an old one; do not quietly change it on speculation.

## 11. Task completion checklist

Before reporting completion, verify:

- [ ] The implemented user flow matches `PROJECT_CONTEXT.md` and has not reintroduced deferred features.
- [ ] All impacted account, credit-card, debt, receivable, and allocation read models agree after the action.
- [ ] Money arithmetic is exact and the action is atomic/consistent and idempotent where required.
- [ ] Reservations, funding, actual payments, and expense classification are correctly distinguished.
- [ ] Corrections and destructive actions are explicit and auditable.
- [ ] Data exposed in person statements is appropriately limited.
- [ ] Relevant unit/integration tests and available project checks have been run, or blockers are stated.
- [ ] A short summary lists **files changed, behavior implemented, checks run/results, assumptions, and unresolved questions**.

## 12. When deciding what to build next

Unless the user provides a different priority, prefer this progression:

1. Existing-repo audit and minimal architecture decisions.
2. Money types, account model, event/ledger posting and invariant tests.
3. Savings/bucket/cash accounts and account movements.
4. Transactions and credit-card funding logic.
5. Home allocations, payday schedules and recurring cycles.
6. My Debts installments, saved money, payoff, Home links.
7. Withdrawal cash stuffing and envelope confirmations.
8. Owed to Me ledgers and card/account integration.
9. Person share statements and expense receipt-scan prefill.
10. Cross-module hardening, reconciliation, migrations, and UX polish.

This ordering is advisory, not permission to broaden scope. **Accuracy of the shared money model comes before polished charts or automation.**
