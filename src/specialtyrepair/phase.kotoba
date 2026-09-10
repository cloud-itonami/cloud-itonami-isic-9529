(ns specialtyrepair.phase
  "Phase 0->3 staged rollout -- the other-personal-and-household-
  goods-repair analog of `cloud-itonami-isic-9524`'s `furniture.
  phase`.

    Phase 0  read-only        -- no writes, still governor-gated.
    Phase 1  assisted-intake  -- ticket intake allowed, every write
                                 needs human approval.
    Phase 2  assisted-assess  -- adds jurisdiction assessment + safety
                                 screening + hallmark-integrity
                                 screening writes, still approval.
    Phase 3  supervised auto  -- governor-clean, high-confidence
                                 `:ticket/intake` (no capital risk yet)
                                 may auto-commit. `:repair/complete`/
                                 `:item/return` NEVER auto-commit, at
                                 any phase.

  `:repair/complete`/`:item/return` are deliberately ABSENT from every
  phase's `:auto` set, including phase 3 -- a permanent structural
  fact, not a rollout milestone still to come. Completing a real
  repair and returning a real item are the two real-world legal acts
  this actor performs; both are always a human repair technician/shop
  owner's call. `specialtyrepair.governor`'s `:actuation/complete-
  repair`/`:actuation/return-item` high-stakes gate enforces the same
  invariant independently -- two layers, not one, agree on this.
  `:safety/screen`/`:hallmark/screen` are likewise never auto-eligible,
  at any phase -- the same posture every sibling's screening op has.
  Like `repairshop.phase`'s/`commrepair.phase`'s/`applianceshop.
  phase`'s/`furniture.phase`'s (and every prior sibling's) phase 3
  `:auto` set, this domain has only ONE member (`:ticket/intake`) --
  no separate no-capital-risk 'file' lifecycle distinct from the
  ticket itself.")

(def read-ops  #{})
(def write-ops #{:ticket/intake :jurisdiction/assess :safety/screen
                 :hallmark/screen :repair/complete :item/return})

;; NOTE the invariant: `:repair/complete`/`:item/return` are members of
;; `write-ops` (governor-gated like any write) but are NEVER members
;; of any phase's `:auto` set below. Do not add them there.
(def phases
  "phase -> {:label .. :writes <ops allowed to write> :auto <ops allowed to
  auto-commit when governor-clean>}."
  {0 {:label "read-only"       :writes #{}                                                                                :auto #{}}
   1 {:label "assisted-intake" :writes #{:ticket/intake}                                                                  :auto #{}}
   2 {:label "assisted-assess" :writes #{:ticket/intake :jurisdiction/assess :safety/screen :hallmark/screen}             :auto #{}}
   3 {:label "supervised-auto" :writes write-ops
      :auto #{:ticket/intake}}})

(def default-phase 3)

(defn gate
  "Adjust a governor disposition for the rollout phase. Returns
  {:disposition kw :reason kw|nil}.

  - a governor HOLD always stays HOLD (compliance wins).
  - a write op not yet enabled in this phase -> HOLD (:phase-disabled).
  - a write op enabled but not auto-eligible -> ESCALATE (:phase-approval),
    even if the governor was clean.
  - `:repair/complete`/`:item/return` are never auto-eligible at any
    phase, so they always escalate once the governor clears them (or
    hold if the governor doesn't)."
  [phase {:keys [op]} governor-disposition]
  (let [{:keys [writes auto]} (get phases phase (get phases default-phase))]
    (cond
      (= :hold governor-disposition)       {:disposition :hold :reason nil}
      (contains? read-ops op)              {:disposition governor-disposition :reason nil}
      (not (contains? writes op))          {:disposition :hold :reason :phase-disabled}
      (and (= :commit governor-disposition)
           (not (contains? auto op)))      {:disposition :escalate :reason :phase-approval}
      :else                                {:disposition governor-disposition :reason nil})))

(defn verdict->disposition
  "Map a Repair Shop Governor verdict to a base disposition before the
  phase gate."
  [verdict]
  (cond (:hard? verdict) :hold
        (:escalate? verdict) :escalate
        :else :commit))
