# Business Model: Repair of other personal and household goods

## Classification

- Repository: `cloud-itonami-isic-9529`
- ISIC Rev.5: `9529`
- Activity: repair of other personal and household goods not elsewhere classified (e.g. watches, jewelry, bicycles)
- Social impact: community access, data sovereignty, transparent audit

## Customer

- independent specialty-repair shops
- cooperative repair collectives
- community right-to-repair programs

## Offer

- item intake
- diagnostic/quote proposal
- repair-completion proposal
- immutable audit ledger

## Revenue

- self-host setup: one-time implementation fee
- managed hosting: monthly subscription per shop
- support: monthly retainer with SLA
- migration: import from an incumbent repair-shop system
- per-repair fee

## Trust Controls

- no repair is performed and no item is returned without human sign-off
- a fabricated diagnostic forces a hold, not an override
- every repair path is auditable
- emergency manual override paths remain outside LLM control
- an unconfirmed precious-metal-hallmark-integrity status (jewelry repair/resizing/soldering) forces a hold, un-overridable, before completion or return

## Repair Shop Governor: decision rule

`cloud-itonami-isic-9529` deliberately shares its `:itonami.blueprint/governor`
keyword (`:repair-shop-governor`) with `repairshop`/9521 (consumer
electronics), `commrepair`/9512 (communication equipment) and
`applianceshop`/9522 (household appliances), and `furniture`/9524 (furniture
and home furnishings) — the same underlying business archetype (a specialty
repair shop, intake through completion/return) reapplied to a different
repair-item category. This is a deliberate, honest reuse, not a naming
error: every sibling brings its own genuinely differentiated HARD check
grounded in a real, distinguishing regulatory concern for its own item
category. For this vertical, that check is precious-metal-hallmark-integrity
verification — one of this blueprint's own named example activities is
jewelry repair/resizing, which in several major jurisdictions (UK, US,
Germany) is subject to a mandatory statutory hallmarking/assay-office
regime independent of general consumer-product safety law. Japan is
honestly seeded without an equivalent mandatory regime in this catalog.
