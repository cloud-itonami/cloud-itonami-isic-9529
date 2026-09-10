(ns specialtyrepair.governor
  "Repair Shop Governor -- the independent compliance layer that earns
  the RepairOps-LLM the right to commit. The LLM has no notion of
  jurisdictional consumer-product-safety/precious-metal-hallmark-
  integrity law, whether a claimed parts cost actually equals the
  ticket's own parts-quantity times parts-unit-price, whether an item
  has actually passed its post-repair safety test, whether a repaired
  precious-metal item's hallmark/quality-mark integrity is actually
  confirmed, or when an act stops being a draft and becomes a real-
  world repair completion or item return, so this MUST be a separate
  system able to *reject* a proposal and fall back to HOLD -- the
  other-personal-and-household-goods-repair analog of `cloud-itonami-
  isic-9521`'s `repairshop.governor` (itself the electronics-repair
  analog of `cloud-itonami-isic-6512`'s `casualty.governor`), and
  structurally closest to `cloud-itonami-isic-9524`'s `furniture.
  governor` (furniture and home-furnishings repair).

  This is the EIGHTH confirmation of the fleet-wide governor-name-
  reuse precedent `commrepair`/9512's own ADR-0001 established (1st:
  commrepair/9512; 2nd: applianceshop/9522; 3rd: socialresearch/7220;
  4th: bizassoc/9411; 5th: vocational/8522; 6th: training/8549; 7th:
  furniture/9524), and the SECOND time it applies within the ORIGINAL
  `:repair-shop-governor` family for a FOURTH sibling: `repairshop`/
  9521, `commrepair`/9512, `applianceshop`/9522, `furniture`/9524 and
  now `specialtyrepair`/9529 all share the IDENTICAL published
  `:itonami.blueprint/governor` keyword -- see this repo's own
  docs/adr/0001-architecture.md Decision 1 for why this is a
  deliberate, honest reuse of the SAME business archetype for a
  different repair-item category, following the precedent
  `commrepair`/9512's own ADR-0001 established. This blueprint's own
  named example activities (watches, jewelry, bicycles and similar
  personal/household goods) routinely include jewelry repair/resizing
  work, a genuinely distinct real-world concern none of `repairshop`/
  9521's, `commrepair`/9512's, `applianceshop`/9522's nor `furniture`/
  9524's own catalogs model -- see Decision 5 below.

  Seven checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them (you don't get to approve your way
  past a fabricated jurisdiction spec-basis, incomplete repair
  evidence, a parts-cost claim that doesn't match quantity times unit-
  price, an item returned without a passed safety test, an unconfirmed
  hallmark-integrity status for precious-metal work, or a double
  completion/return). The confidence/actuation gate is SOFT: it asks a
  human to look (low confidence / actuation), and the human may
  approve -- but see `specialtyrepair.phase`: for `:stake :actuation/
  complete-repair`/`:actuation/return-item` (a real repair completion
  or item return) NO phase ever allows auto-commit either. Two
  independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the jurisdiction proposal cite
                                       an OFFICIAL source
                                       (`specialtyrepair.facts`), or
                                       invent one?
    2. Evidence incomplete         -- for `:repair/complete`/`:item/
                                       return`, has the jurisdiction
                                       actually been assessed with a
                                       full repair-evidence checklist
                                       on file?
    3. Parts cost mismatch         -- for `:repair/complete`,
                                       INDEPENDENTLY recompute whether
                                       the ticket's own `:claimed-
                                       parts-cost` equals `parts-
                                       quantity x parts-unit-price`
                                       (`specialtyrepair.registry/
                                       parts-cost-matches-claim?`) --
                                       an HONEST, literal reuse of
                                       `repairshop.registry`'s/
                                       `commrepair.registry`'s/
                                       `applianceshop.registry`'s/
                                       `furniture.registry`'s own
                                       EXACT-MATCH independent-
                                       recompute check for the SAME
                                       real-world concern, not claimed
                                       as new.
    4. Safety test not passed      -- for `:item/return`, reported by
                                       THIS proposal itself (a
                                       `:safety/screen` that just found
                                       a failed test), or already on
                                       file for the ticket (`:safety/
                                       screen`/`:item/return`).
                                       Evaluated UNCONDITIONALLY (not
                                       scoped to a specific op) -- an
                                       HONEST, literal reuse of
                                       `repairshop.governor`'s/
                                       `commrepair.governor`'s/
                                       `applianceshop.governor`'s/
                                       `furniture.governor`'s own check
                                       for the SAME real-world concern
                                       (post-repair safety testing --
                                       e.g. a repaired bicycle brake
                                       must actually stop the wheel),
                                       not claimed as new.
    5. Hallmark integrity
       unconfirmed                    -- for a ticket whose own record
                                       declares `:involves-precious-
                                       metal-work? true` (i.e. this
                                       repair actually involved
                                       soldering/resizing/adding
                                       material to a precious-metal
                                       item -- not every specialty
                                       repair does), INDEPENDENTLY
                                       check whether `:hallmark-
                                       integrity-confirmed?` is true. A
                                       GENUINELY NEW concept (grep-
                                       verified absent fleet-wide --
                                       zero hits for 'hallmark'/
                                       'precious-metal'/'assay'/
                                       'fineness' as a governor CHECK
                                       function name), the 68th
                                       distinct application of the
                                       unconditional-evaluation
                                       discipline overall (most
                                       recently `furniture.governor/
                                       flammability-compliance-
                                       unconfirmed-violations` at
                                       67th), the FIFTH CONDITIONAL
                                       variant (after `socialresearch`/
                                       7220's, `bizassoc`/9411's,
                                       `training`/8549's and
                                       `furniture`/9524's own, at 63rd,
                                       64th, 66th and 67th).
                                       CONDITIONAL on the ticket's own
                                       `:involves-precious-metal-work?`
                                       ground truth -- a bicycle-brake
                                       or watch-battery repair has no
                                       hallmark-integrity concern at
                                       all. Grounded in real precious-
                                       metal-hallmark-integrity law:
                                       UK's Hallmarking Act 1973
                                       (British Hallmarking Council /
                                       UK Assay Offices -- one of the
                                       oldest consumer-protection-
                                       through-metal-integrity regimes
                                       in the world), the US National
                                       Stamping Act (15 U.S.C. §297) /
                                       FTC Jewelry Guides (16 C.F.R.
                                       Part 23), Germany's
                                       Feingehaltsgesetz -- honestly
                                       ABSENT for Japan, which has no
                                       mandatory statutory hallmarking/
                                       assay-office regime in this R0
                                       catalog.
    6. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:repair/complete`/
                                       `:item/return` (REAL acts) ->
                                       escalate.

  Two more guards, double-completion/double-return prevention, are
  enforced but NOT listed as numbered HARD checks above because they
  need no upstream comparison at all -- `already-completed-violations`/
  `already-returned-violations` refuse to complete/return the SAME
  ticket twice, off dedicated `:repair-completed?`/`:item-returned?`
  facts (never a `:status` value) -- the SAME 'check a dedicated
  boolean, not status' discipline every prior governor's guards
  establish, informed by `cloud-itonami-isic-6492`'s status-lifecycle
  bug (ADR-2607071320)."
  (:require [specialtyrepair.facts :as facts]
            [specialtyrepair.registry :as registry]
            [specialtyrepair.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Completing a real repair and returning a real item are the two
  real-world actuation events this actor performs -- a two-member set,
  matching `repairshop`/9521's, `commrepair`/9512's, `applianceshop`/
  9522's and `furniture`/9524's own dual-actuation shape."
  #{:actuation/complete-repair :actuation/return-item})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:jurisdiction/assess` (or `:repair/complete`/`:item/return`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's consumer-product-safety/hallmark-integrity
  requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:jurisdiction/assess :repair/complete :item/return} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:repair/complete`/`:item/return`, the jurisdiction's required
  diagnostic/parts-used/safety-test/hallmark-integrity evidence must
  actually be satisfied -- do not trust the advisor's self-reported
  confidence alone."
  [{:keys [op subject]} st]
  (when (contains? #{:repair/complete :item/return} op)
    (let [t (store/ticket st subject)
          assessment (store/assessment-of st subject)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      (:jurisdiction t) (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(故障診断書/使用部品記録/安全試験記録/純度証明記録等)が充足していない状態での提案"}]))))

(defn- parts-cost-mismatch-violations
  "For `:repair/complete`, INDEPENDENTLY recompute whether the
  ticket's own claimed parts cost equals parts-quantity x parts-unit-
  price via `specialtyrepair.registry/parts-cost-matches-claim?` --
  needs no proposal inspection or stored-verdict lookup at all, an
  honest reuse of `repairshop.registry`'s/`commrepair.registry`'s/
  `applianceshop.registry`'s/`furniture.registry`'s own check."
  [{:keys [op subject]} st]
  (when (= op :repair/complete)
    (let [t (store/ticket st subject)]
      (when-not (registry/parts-cost-matches-claim? t)
        [{:rule :parts-cost-mismatch
          :detail (str subject " の申告部品代金(" (:claimed-parts-cost t)
                      ")が独立再計算値(" (registry/compute-parts-cost t) ")と一致しない")}]))))

(defn- safety-test-not-passed-violations
  "A not-passed post-repair safety test -- reported by THIS proposal
  (e.g. a `:safety/screen` that itself just found a failure), or
  already on file for the ticket (`:safety/screen`/`:item/return`) --
  is a HARD, un-overridable hold. Evaluated UNCONDITIONALLY (not
  scoped to a specific op) so the screening op itself can HARD-hold on
  its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :failed (get-in proposal [:value :verdict]))
        ticket-id (when (contains? #{:safety/screen :item/return} op) subject)
        hit-on-file? (and ticket-id (= :failed (:verdict (store/safety-screening-of st ticket-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :safety-test-not-passed
        :detail "修理後安全試験に合格していない品目を返却する提案は進められない"}])))

(defn- hallmark-integrity-unconfirmed-violations
  "For a ticket whose own record declares `:involves-precious-metal-
  work? true`, INDEPENDENTLY check whether `:hallmark-integrity-
  confirmed?` is true -- a genuinely new concept (see ns docstring),
  CONDITIONAL on the ticket's own `:involves-precious-metal-work?`
  ground truth (a repair that does not touch precious-metal materials
  has no hallmark-integrity requirement at all). Scoped to `:hallmark/
  screen` and `:repair/complete`/`:item/return`, so the screening op
  itself can HARD-hold on its own finding, matching every prior
  unconditional-evaluation check's scoping shape."
  [{:keys [op subject]} st]
  (when (contains? #{:hallmark/screen :repair/complete :item/return} op)
    (let [t (store/ticket st subject)]
      (when (and (true? (:involves-precious-metal-work? t))
                 (not (true? (:hallmark-integrity-confirmed? t))))
        [{:rule :hallmark-integrity-unconfirmed
          :detail (str subject " は貴金属加工を伴うが純度証明(ハルマーク)が未確認 -- 修理完了/返却提案は進められない")}]))))

(defn- already-completed-violations
  "For `:repair/complete`, refuses to complete the SAME ticket's
  repair twice, off a dedicated `:repair-completed?` fact (never a
  `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :repair/complete)
    (when (store/ticket-already-completed? st subject)
      [{:rule :already-completed
        :detail (str subject " は既に修理完了済み")}])))

(defn- already-returned-violations
  "For `:item/return`, refuses to return the SAME ticket's item
  twice, off a dedicated `:item-returned?` fact (never a `:status`
  value)."
  [{:keys [op subject]} st]
  (when (= op :item/return)
    (when (store/ticket-already-returned? st subject)
      [{:rule :already-returned
        :detail (str subject " は既に返却済み")}])))

(defn check
  "Censors a RepairOps-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (parts-cost-mismatch-violations request st)
                           (safety-test-not-passed-violations request proposal st)
                           (hallmark-integrity-unconfirmed-violations request st)
                           (already-completed-violations request st)
                           (already-returned-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
