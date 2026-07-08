# ADR-0001: RepairOps-LLM ⊣ Repair Shop Governor architecture

## Status

Accepted. `cloud-itonami-isic-9529` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-9529` publishes an OSS business blueprint for
repair of other personal and household goods (e.g. watches, jewelry,
bicycles). Like every prior actor in this fleet, the blueprint alone
is not an implementation: this ADR records the governed-actor
architecture that promotes it to real, tested code, following the
same langgraph StateGraph + independent Governor + Phase 0→3 rollout
pattern established by `cloud-itonami-isic-6511` (life insurance) and
applied across eighty-two prior siblings, most recently `cloud-
itonami-isic-9524` (furniture and home furnishings repair).

This blueprint's own `:itonami.blueprint/governor` keyword,
`:repair-shop-governor`, is IDENTICAL to `repairshop`/9521's (consumer
electronics), `commrepair`/9512's (communication equipment),
`applianceshop`/9522's (household appliances) and `furniture`/9524's
(furniture and home furnishings). Per the fleet-wide governor-name-
reuse precedent `commrepair`/9512's own ADR-0001 established --
confirmed six times since across three other governor-name families --
sharing a governor name is acceptable when the underlying business
archetype is genuinely the same, provided the reuse is documented and
the new build brings its own genuinely differentiated, well-grounded
check. This build is the EIGHTH confirmation overall, and the SECOND
time the precedent applies within the ORIGINAL `:repair-shop-governor`
family -- now for a FOURTH sibling.

## Decision

### Decision 1: governor-name reuse -- eighth confirmation, fourth sibling in the original family

`repairshop`/9521, `commrepair`/9512, `applianceshop`/9522,
`furniture`/9524 and `specialtyrepair`/9529 all share the IDENTICAL
`:repair-shop-governor` keyword -- a deliberate, honest reuse of the
same repair-shop business archetype (diagnose, quote/assess, repair,
return) applied to a different repair-item category each time
(consumer electronics, communication equipment, household appliances,
furniture and home furnishings, and now watches/jewelry/bicycles and
similar personal/household goods). This is the same reasoning
`commrepair`/9512's, `applianceshop`/9522's and `furniture`/9524's own
ADR-0001s established, now confirmed a FOURTH time within this
specific family.

### Decision 2: dual-actuation shape

This blueprint's own README, business-model.md and operator-guide.md
consistently name two real-world acts: "performing a repair or
returning an item to the customer." Matching `repairshop`/9521's,
`commrepair`/9512's, `applianceshop`/9522's and `furniture`/9524's own
dual-actuation shape, `high-stakes` here is a two-member set,
`#{:actuation/complete-repair :actuation/return-item}`.

### Decision 3: `parts-cost-matches-claim?` and `safety-test-not-passed?` -- honest, literal reuses

`specialtyrepair.registry/parts-cost-matches-claim?` and
`specialtyrepair.governor/safety-test-not-passed-violations` are
HONEST, LITERAL reuses of `repairshop.registry`'s/`commrepair.
registry`'s/`applianceshop.registry`'s/`furniture.registry`'s own
EXACT-MATCH independent-recompute checks -- NOT claimed as new. A
watch/jewelry/bicycle-repair ticket's own claimed parts cost (movement
parts, precious-metal solder/findings, brake/gear components) against
quantity-times-unit-price, and a post-repair safety test (e.g. a
repaired bicycle brake must actually stop the wheel), are the SAME
real-world concerns as `furniture`/9524's own checks, reapplied to a
different repair-item category.

### Decision 4: entity and op shape

The primary entity is a `ticket`. Six ops: `:ticket/intake` (directory
upsert, no capital risk), `:jurisdiction/assess` (per-jurisdiction
consumer-product-safety/hallmark-integrity evidence checklist, never
auto), `:safety/screen` (post-repair safety screening, honest reuse,
never auto), `:hallmark/screen` (precious-metal-hallmark-integrity
screening, GENUINELY NEW, never auto), `:repair/complete` (POSITIVE,
high-stakes), and `:item/return` (POSITIVE, high-stakes).

### Decision 5: `hallmark-integrity-unconfirmed?` -- the 68th unconditional-evaluation grounding, a genuinely new MATERIAL-INTEGRITY concept, the FIFTH conditional variant

Before writing this check, every prior sibling's governor namespace
across the entire fleet was grepped for any check function named
`hallmark`, `precious-metal`, `assay` or `fineness` -- zero hits,
confirming this is a genuinely new concept.
`hallmark-integrity-unconfirmed-violations` reuses the unconditional-
evaluation-screening DISCIPLINE (`casualty.governor/sanctions-
violations`'s original fix) for the 68th distinct application overall
(most recently `furniture.governor/flammability-compliance-
unconfirmed-violations` at 67th). This is the FIFTH conditional
variant (after `socialresearch`/7220's, `bizassoc`/9411's,
`training`/8549's and `furniture`/9524's own, at 63rd, 64th, 66th and
67th) -- CONDITIONAL on the ticket's own `:involves-precious-metal-
work? true` ground truth: a bicycle-brake or watch-battery repair has
no hallmark-integrity concern at all.

Unlike every prior check in this discipline, which verifies either a
fact about the SUBJECT being acted upon (a study, a position, a
ticket) or (as `training`/8549's `instructor-license-unconfirmed?`
first established) a fact about the ASSESSOR/professional performing
an act, this check verifies a fact about the MATERIAL/COMPOSITION
integrity of the item itself -- a third, structurally distinct
category this discipline has not exercised before. Grounded in real
precious-metal-hallmark-integrity law: the UK's Hallmarking Act 1973
(British Hallmarking Council / UK Assay Offices in London, Birmingham,
Sheffield and Edinburgh -- one of the oldest, most well-documented
consumer-protection-through-metal-integrity regimes in the world), the
US National Stamping Act (15 U.S.C. §297) / FTC Guides for the
Jewelry, Precious Metals, and Pewter Industries (16 C.F.R. Part 23),
Germany's Feingehaltsgesetz -- honestly ABSENT for Japan, which has no
mandatory statutory hallmarking/assay-office regime in this R0
catalog (precious-metal marking in Japan is largely voluntary/JIS-
standard-based). Gates `:hallmark/screen` and `:repair/complete`/
`:item/return`.

### Decision 6: dedicated double-actuation-guard booleans

`:repair-completed?`/`:item-returned?` are dedicated booleans on the
`ticket` record, never a single `:status` value -- an honest, literal
reuse of `furniture.governor`'s own guards, informed by `cloud-
itonami-isic-6492`'s real status-lifecycle bug (ADR-2607071320).

### Decision 7: Store protocol, MemStore + DatomicStore parity

`specialtyrepair.store/Store` is implemented by both `MemStore`
(atom-backed, default for dev/tests/demo) and `DatomicStore`
(`langchain.db`-backed), proven to satisfy the same contract in
`test/specialtyrepair/store_contract_test.clj` -- the same seam every
sibling actor uses so swapping the SSoT backend is a configuration
change, not a rewrite.

### Decision 8: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:ticket/intake` (no
capital risk). `:jurisdiction/assess`, `:safety/screen` and
`:hallmark/screen` are never auto-eligible at any phase (matching
every sibling's screening-op posture), and `:repair/complete`/`:item/
return` are permanently excluded from every phase's `:auto` set -- a
structural fact, not a rollout milestone, enforced by BOTH
`specialtyrepair.phase` and `specialtyrepair.governor`'s `high-stakes`
set independently.

### Decision 9: no bespoke domain capability lib, and no `blueprint.edn` field-sync fixes needed

This blueprint's own `:itonami.blueprint/required-technologies` names
no domain-specific capability beyond the generic robotics/identity/
forms/dmn/bpmn/audit-ledger stack -- there was no capability-lib
decision to make at all. This repo's `blueprint.edn` already had the
correct `isic-` prefixed `:id` and correctly populated `:required-
technologies`/`:optional-technologies` matching the `kotoba-lang/
industry` registry's own entry for `"9529"` exactly -- only the
`:maturity` field itself needed adding.

### Decision 10: mock + LLM advisor pair

`specialtyrepair.repairopsllm` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-
completing a repair or auto-returning an item).

## Alternatives considered

- **An unconditional hallmark-integrity check** (applying to every
  ticket regardless of whether the repair actually touches precious-
  metal materials). Rejected: a bicycle-brake or watch-battery repair
  has no hallmark-integrity concern at all -- forcing the check onto
  every ticket would fabricate a requirement.
- **Declining the build and leaving the repair-shop cluster
  exhausted at three siblings.** Rejected: `commrepair`/9512's,
  `applianceshop`/9522's and `furniture`/9524's own ADR-0001s already
  established that the governor-name-collision constraint was a self-
  imposed convention, not a structural requirement -- extending it to
  a fourth sibling confirms the precedent generalizes rather than
  being limited to three instances.
- **Framing the new check as another assessor-credential check**,
  reusing `training`/8549's own structural category. Rejected: the
  hallmark-integrity concern is about the ITEM's own material
  composition/marking, not about any professional's license or
  credential -- a genuinely distinct third structural category within
  the discipline, not a repeat of the second.

## Consequences

- Eighty-fourth actor in this fleet (83 implemented before this
  build).
- Confirms the fleet-wide governor-name-reuse precedent an eighth
  time, and confirms it generalizes to a FOURTH sibling within the
  original `:repair-shop-governor` family.
- Establishes a genuinely NEW conditional unconditional-evaluation-
  screening concept (hallmark-integrity-unconfirmed?), the first
  MATERIAL-INTEGRITY check in the discipline (distinct from both
  subject-fact and assessor-credential checks), grep-verified absent
  from every prior sibling before the claim was finalized.
- `MemStore` ‖ `DatomicStore` parity is proven by
  `test/specialtyrepair/store_contract_test.clj`, the same `:db-api`-
  driven swap pattern every sibling actor uses.
- 41 tests / 191 assertions pass; lint is clean; the demo
  (`clojure -M:dev:run`) walks one clean dual-actuation lifecycle plus
  five HARD-hold scenarios end-to-end.
- `blueprint.edn` required no field-sync fixes this time (already
  correct) -- only the `:maturity` flip itself.
- One repair-shop candidate remains open for a future build: 9523
  (repair of footwear and leather goods), still requiring its own
  genuinely differentiated check.
