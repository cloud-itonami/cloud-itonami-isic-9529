(ns specialtyrepair.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 for `cloud-itonami-isic-9529`: this
  repo had no demo page and no generator at all. This namespace drives
  the REAL actor stack (`specialtyrepair.operation` ->
  `specialtyrepair.governor` -> `specialtyrepair.store`) through
  `langgraph.graph/run*` against this repo's OWN seed data
  (`specialtyrepair.store/demo-data`, tickets `ticket-1`..`ticket-5`)
  and renders what it observed. Nothing on the page is hand-typed
  domain content: every ticket, every hold, every rule detail string,
  every approver name and every registry draft number is read back out
  of the run or the store.

  Derived, not transcribed
  ------------------------
  The action-gate and phase-ladder tables are NOT static markup. They
  are produced at render time by calling the real
  `specialtyrepair.phase/gate` for every (phase, op) pair and the real
  advisor (`specialtyrepair.repairopsllm/infer`) for every op's stake
  and effect, so the page cannot drift away from the code it claims to
  document. The governor rule table is likewise derived: `scan-rules`
  reads this actor's OWN SOURCE off the classpath and extracts every
  `:rule :x` literal, so a rule added to `specialtyrepair.governor`
  tomorrow enters the required-coverage set automatically.

  Build-time assertions (see `assert-console!`)
  ---------------------------------------------
  `-main` throws, and writes nothing, unless ALL of the following hold:

    1. the run produced at least one `:governor-hold` ledger fact;
    2. EVERY hold rule the source declares was actually raised by the
       real governor -- a bare `>= 1 hold` check would let this page
       stay green while the governor grows rules nobody exercises;
    3. every human approval this scenario issued was actually observed
       as an `:approval-granted` fact with a named approver;
    4. the store, the ledger and the ticket directory are all non-empty
       (an evidence floor: a scenario that measured nothing must not
       render as a clean page).

  Measurement discipline
  ----------------------
  `g/run*` returns `{:state .. :events .. :status .. :frontier ..}` and
  the `:audit` channel lives UNDER `:state`, not at top level. Reading
  the top level yields nil, and an absent approver rendered as
  \"auto-committed\" reads like an observation about the domain when it
  is really a failed measurement. So every join here goes through
  `measured!`: a nil where a value was expected THROWS. This page never
  prints a conclusion it did not measure.

  Approver attribution is likewise measured, not assumed: `retention`
  walks the actual store registers for each approved op and reports
  whether an approver key survived, so the disclosure flips by itself
  if the store starts (or stops) keeping it.

  Determinism: no timestamps, no wall-clock, no map-iteration order --
  every collection is sorted before rendering, so two runs against the
  same seed are byte-identical.

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.java.io :as io]
            [kotoba.lang.text :as str]
            [langgraph.graph :as g]
            [specialtyrepair.facts :as facts]
            [specialtyrepair.governor :as governor]
            [specialtyrepair.operation :as op]
            [specialtyrepair.phase :as phase]
            [specialtyrepair.registry :as registry]
            [specialtyrepair.repairopsllm :as repairopsllm]
            [specialtyrepair.store :as store]))

;; ----------------------------- measurement guards -----------------------------

(defn- measured!
  "Returns `v`, or THROWS if it is nil/empty. Used at every join between
  the run and the page: a join that silently returns nothing must fail
  the build, never render as a domain fact."
  [label v]
  (when (or (nil? v) (and (coll? v) (empty? v)))
    (throw (ex-info (str "MEASUREMENT FAILED: " label
                         " -- the join produced nothing. Refusing to render an"
                         " absence as an observation about the domain.")
                    {:kind :unmeasurable :label label})))
  v)

;; ----------------------------- rule universe (derived from source) ------------

(def ^:private rule-sources
  "The namespaces that construct hold facts. Scanned at build time so the
  required-coverage set tracks the code rather than this file."
  ["specialtyrepair/governor.cljc" "specialtyrepair/operation.cljc"])

(def ^:private rule-literal
  #":rule\s+:([A-Za-z0-9?!*<>=_+.-]+)")

(defn- scan-one
  "Every `:rule :x` literal in one classpath source, as [{:rule :source}].
  THROWS if the file cannot be read, or if it yields zero rules -- \"I
  could not read the governor\" must not be indistinguishable from \"the
  governor declares no rules\".

  (This function is written as an explicit `mapv` rather than a `for`
  with a guard clause on purpose: a `for` binding whose expression
  returns nil produces an EMPTY result instead of running the guard, so
  the whole scan silently yields zero rules and the coverage check then
  passes vacuously. That is exactly the failure this file exists to
  make impossible; it was measured here on 2026-08-15 before the page
  was ever committed.)"
  [res]
  (let [url  (or (io/resource res)
                 (throw (ex-info (str "MEASUREMENT FAILED: " res
                                      " is not on the classpath -- refusing to report rule"
                                      " coverage from a source I could not read.")
                                 {:kind :unmeasurable :resource res})))
        hits (vec (re-seq rule-literal (slurp url)))]
    (when (empty? hits)
      (throw (ex-info (str "MEASUREMENT FAILED: scanned " res
                           " and found 0 `:rule :x` literals. Either the scan is broken or"
                           " the file moved; both are below the evidence floor.")
                      {:kind :unmeasurable :resource res})))
    (mapv (fn [[_ kw]] {:rule (keyword kw) :source res}) hits)))

(defn scan-rules
  "Reads this actor's own source off the classpath and returns
  {:order [rule ..] :by-rule {rule #{source ..}} :scanned n}."
  []
  (let [found (vec (mapcat scan-one rule-sources))
        order (vec (distinct (map :rule found)))]
    (when (< (count order) (count rule-sources))
      (throw (ex-info (str "MEASUREMENT FAILED: scanned " (count rule-sources)
                           " source file(s) and derived only " (count order)
                           " distinct hold rule(s) -- below the evidence floor of one"
                           " rule per scanned file.")
                      {:kind :unmeasurable :order order})))
    {:order   order
     :by-rule (reduce (fn [m {:keys [rule source]}]
                        (update m rule (fnil conj (sorted-set)) source))
                      {} found)
     :scanned (count rule-sources)}))

;; ----------------------------- the scenario -----------------------------------

(def operator
  "The human operator seat every run is executed under -- phase 3
  (`supervised-auto`), the most permissive phase this actor has."
  {:actor-id "op-1" :actor-role :repair-technician :phase 3})

(def scenario
  "Every step is a real request against real seeded tickets. `:why` is
  the rule this step is here to exercise; it is documentation for the
  reader of THIS file and is never rendered as a finding -- the page
  reports what the governor actually raised, which is checked against
  the source-derived rule set independently."
  [;; --- ticket-1: a full clean lifecycle, six human decisions ---------
   {:thread "t1-intake"    :kind :exec
    :request {:op :ticket/intake :subject "ticket-1"
              :patch {:id "ticket-1" :customer "Sakura Tanaka"}}
    :why :auto-commit}
   {:thread "t1-assess"    :kind :exec
    :request {:op :jurisdiction/assess :subject "ticket-1"} :why :escalate}
   {:thread "t1-assess"    :kind :approve :by "op-1"}
   {:thread "t1-safety"    :kind :exec
    :request {:op :safety/screen :subject "ticket-1"} :why :escalate}
   {:thread "t1-safety"    :kind :approve :by "op-1"}
   {:thread "t1-hallmark"  :kind :exec
    :request {:op :hallmark/screen :subject "ticket-1"} :why :escalate}
   {:thread "t1-hallmark"  :kind :approve :by "op-1"}
   {:thread "t1-complete"  :kind :exec
    :request {:op :repair/complete :subject "ticket-1"} :why :actuation}
   {:thread "t1-complete"  :kind :approve :by "op-1"}
   {:thread "t1-return"    :kind :exec
    :request {:op :item/return :subject "ticket-1"} :why :actuation}
   {:thread "t1-return"    :kind :approve :by "op-1"}

   ;; --- the HARD holds, one per governor check ------------------------
   {:thread "t2-assess"    :kind :exec
    :request {:op :jurisdiction/assess :subject "ticket-2" :no-spec? true}
    :why :no-spec-basis}
   ;; ticket-2 has NO committed jurisdiction assessment (its own
   ;; jurisdiction "ATL" is not in `specialtyrepair.facts/catalog`, so the
   ;; assessment above HARD-held and never committed). Completing its
   ;; repair therefore has no evidence checklist on file at all.
   {:thread "t2-complete"  :kind :exec
    :request {:op :repair/complete :subject "ticket-2"} :why :evidence-incomplete}
   {:thread "t3-assess"    :kind :exec
    :request {:op :jurisdiction/assess :subject "ticket-3"} :why :escalate}
   {:thread "t3-assess"    :kind :approve :by "op-1"}
   {:thread "t3-complete"  :kind :exec
    :request {:op :repair/complete :subject "ticket-3"} :why :parts-cost-mismatch}
   {:thread "t4-safety"    :kind :exec
    :request {:op :safety/screen :subject "ticket-4"} :why :safety-test-not-passed}
   {:thread "t5-hallmark"  :kind :exec
    :request {:op :hallmark/screen :subject "ticket-5"}
    :why :hallmark-integrity-unconfirmed}
   {:thread "t1-recomplete" :kind :exec
    :request {:op :repair/complete :subject "ticket-1"} :why :already-completed}
   {:thread "t1-rereturn"  :kind :exec
    :request {:op :item/return :subject "ticket-1"} :why :already-returned}

   ;; --- a human who says no --------------------------------------------
   {:thread "t4-assess"    :kind :exec
    :request {:op :jurisdiction/assess :subject "ticket-4"} :why :escalate}
   {:thread "t4-assess"    :kind :reject :by "op-2"}])

(defn- step! [actor {:keys [thread kind request by]}]
  (case kind
    :exec    (g/run* actor {:request request :context operator} {:thread-id thread})
    :approve (g/run* actor {:approval {:status :approved :by by}}
                     {:thread-id thread :resume? true})
    :reject  (g/run* actor {:approval {:status :rejected :by by}}
                     {:thread-id thread :resume? true})))

(defn run-demo!
  "Executes `scenario` against a freshly seeded store and returns
  {:db store :runs [{:i :thread :kind :request :audit :status} ..]}.

  The `:audit` channel lives under `:state`, NOT at the top level of the
  `g/run*` result -- reading the top level yields nil. `measured!`
  turns that mistake into a build failure instead of a page that quietly
  claims nobody approved anything."
  []
  (let [db    (store/seed-db)
        actor (op/build db)]
    {:db db
     :runs (vec
            (map-indexed
             (fn [i {:keys [thread kind request] :as s}]
               (let [res   (step! actor s)
                     audit (measured! (str "run " i " (" thread " " (name kind)
                                           ") :state -> :audit")
                                      (get-in res [:state :audit]))]
                 {:i i :thread thread :kind kind :request request
                  :by (:by s) :audit (vec audit) :status (:status res)}))
             scenario))}))

;; ----------------------------- derivations ------------------------------------

(defn- thread-audits
  "The final (accumulated) audit channel per thread, in first-appearance
  order. langgraph's `:audit` channel reduces with `into` and the run is
  checkpointed, so a resumed run's audit already contains the facts its
  first leg produced."
  [runs]
  (let [order (vec (distinct (map :thread runs)))
        last-of (reduce (fn [m r] (assoc m (:thread r) r)) {} runs)]
    (mapv last-of order)))

(defn- audit-facts [runs]
  (vec (mapcat :audit (thread-audits runs))))

(defn- facts-of-type [runs t]
  (vec (filter #(= t (:t %)) (audit-facts runs))))

(def ^:private hold-fact-types #{:governor-hold :approval-rejected})

(defn- hold-facts
  "Every HARD hold the STORE ledger kept (not the in-run audit channel):
  the durable record an operator would actually query later."
  [db]
  (vec (filter #(hold-fact-types (:t %)) (store/ledger db))))

(defn- observed-rules [db]
  (into (sorted-set) (mapcat :basis (hold-facts db))))

;; --- approver attribution -----------------------------------------------------

(defn- key-name [k] (if (keyword? k) (name k) (str k)))

(defn- approver-keys
  "Keys of `m` that carry an approver, found by NAME rather than by a
  hardcoded list -- so this flips by itself if the store starts keeping
  attribution under a different key."
  [m]
  (when (map? m)
    (seq (sort (filter #(str/includes? (str/lower (key-name %)) "approv")
                       (map key-name (keys m)))))))

(def ^:private effect->register
  "Which store register a committed effect lands in. Plumbing only: the
  CONCLUSION (did the approver survive?) is derived by scanning whatever
  comes back, never asserted here."
  {:ticket/upsert          (fn [db s] (store/ticket db s))
   :assessment/set         (fn [db s] (store/assessment-of db s))
   :safety-screening/set   (fn [db s] (store/safety-screening-of db s))
   :hallmark-screening/set (fn [db s] (store/hallmark-screening-of db s))
   :ticket/mark-completed  (fn [db s] (first (filter #(= s (get % "ticket_id"))
                                                     (store/completion-history db))))
   :ticket/mark-returned   (fn [db s] (first (filter #(= s (get % "ticket_id"))
                                                     (store/return-history db))))})

(defn- ledger-fact-for [db o subject]
  (last (filter #(and (= :committed (:t %)) (= o (:op %)) (= subject (:subject %)))
                (store/ledger db))))

(defn retention
  "For one approved op, MEASURES where the approver actually survived.
  Returns {:register kw :retained-in [k ..] :ledger-retained-in [k ..]
           :measured? bool}. `:measured? false` means this build could
  not look -- it is reported as such, never as \"not retained\"."
  [db o subject]
  (let [effect (:effect (repairopsllm/infer db {:op o :subject subject}))
        reader (effect->register effect)
        record (when reader (reader db subject))
        lf     (ledger-fact-for db o subject)]
    {:effect             effect
     :measured?          (boolean (and reader record))
     :retained-in        (vec (approver-keys record))
     :ledger-measured?   (boolean lf)
     :ledger-retained-in (vec (approver-keys lf))}))

;; --- derived gate tables ------------------------------------------------------

(defn- ops-in-order [] (vec (sort phase/write-ops)))

(defn- probe-subject
  "A real seeded ticket to probe an op's proposal shape against. Chosen
  from the store, not invented."
  [db] (:id (first (store/all-tickets db))))

(defn- op-posture
  "The advisor's real proposal for `o` against a real ticket, plus the
  governor's own high-stakes verdict on the stake it declared."
  [db o]
  (let [subject (probe-subject db)
        req     (cond-> {:op o :subject subject}
                  (= o :ticket/intake) (assoc :patch {:id subject}))
        p       (measured! (str "advisor proposal for " o)
                           (repairopsllm/infer db req))]
    {:op o
     :effect (:effect p)
     :stake (:stake p)
     :high-stakes? (boolean (governor/high-stakes (:stake p)))}))

(defn- gate-cell
  "The REAL `specialtyrepair.phase/gate` answer for a governor-CLEAN
  proposal of `o` at `ph`."
  [ph o]
  (phase/gate ph {:op o} :commit))

;; ----------------------------- html plumbing ----------------------------------

(defn- esc [v]
  (-> (str (if (nil? v) "" v))
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- code [v] (str "<code>" (esc v) "</code>"))

(defn- span [cls v] (str "<span class=\"" cls "\">" v "</span>"))

(defn- yes-no [b] (if b (span "ok" "yes") (span "muted" "no")))

(defn- tr [cells]
  (str "        <tr>" (str/join (map #(str "<td>" % "</td>") cells)) "</tr>"))

(defn- table [headers rows]
  (str "    <table>\n"
       "      <thead><tr>"
       (str/join (map #(str "<th>" (esc %) "</th>") headers))
       "</tr></thead>\n"
       "      <tbody>\n" (str/join "\n" rows) "\n      </tbody>\n"
       "    </table>\n"))

(defn- section [title lede body]
  (str "  <section class=\"card\">\n"
       "    <h2>" (esc title) "</h2>\n"
       "    <p class=\"muted\">" lede "</p>\n"
       body
       "  </section>\n"))

(defn- kw-list [ks]
  (if (seq ks) (str/join ", " (map #(code (str %)) (sort-by str ks)))
      (span "muted" "—")))

;; ----------------------------- sections ---------------------------------------

(defn- last-ledger-fact [db id]
  (last (filter #(= id (:subject %)) (store/ledger db))))

(defn- status-cell [db id]
  (let [f (last-ledger-fact db id)]
    (cond
      (nil? f) (span "muted" "no activity")
      (= :committed (:t f)) (span "ok" (str "committed &middot; " (esc (name (:op f)))))
      (= :governor-hold (:t f))
      (span "critical" (str "HARD hold &middot; " (esc (str/join ", " (map name (:basis f))))))
      (= :approval-rejected (:t f))
      (span "err" (str "rejected by approver &middot; " (esc (name (:op f)))))
      :else (span "muted" (esc (name (:t f)))))))

(defn- tickets-section [db]
  (let [ts (measured! "store/all-tickets" (store/all-tickets db))]
    [(count ts)
     (section
      "Repair tickets (SSoT after this run)"
      (str "Read back out of <code>specialtyrepair.store</code> after the scenario "
           "executed. Parts cost is shown as the ticket's own claim next to the "
           "value <code>specialtyrepair.registry/compute-parts-cost</code> "
           "independently recomputes from <code>:parts-quantity</code> &times; "
           "<code>:parts-unit-price</code> — the governor never trusts the claim.")
      (table ["Ticket" "Customer" "Item" "Juris." "Claimed / recomputed parts cost"
              "Precious-metal work" "Safety test" "Lifecycle" "Last decision"]
             (for [{:keys [id customer item jurisdiction claimed-parts-cost
                           safety-test-passed? involves-precious-metal-work?
                           hallmark-integrity-confirmed?
                           repair-completed? item-returned?] :as t} ts]
               (tr [(code id)
                    (esc customer)
                    (esc item)
                    (code jurisdiction)
                    (str (esc claimed-parts-cost) " / "
                         (esc (registry/compute-parts-cost t)) " "
                         (if (registry/parts-cost-matches-claim? t)
                           (span "ok" "match")
                           (span "critical" "MISMATCH")))
                    (if involves-precious-metal-work?
                      (str (span "warn" "yes") " &middot; hallmark "
                           (if hallmark-integrity-confirmed?
                             (span "ok" "confirmed") (span "critical" "unconfirmed")))
                      (span "muted" "no"))
                    (if safety-test-passed?
                      (span "ok" "passed") (span "critical" "FAILED"))
                    (cond item-returned?    (span "ok" "repaired &amp; returned")
                          repair-completed? (span "warn" "repaired, not yet returned")
                          :else             (span "muted" "in repair"))
                    (status-cell db id)]))))]))

(defn- rule-coverage-section [db rules]
  (let [holds (hold-facts db)
        by-rule (reduce (fn [m f]
                          (reduce (fn [m r] (update m r (fnil conj []) f)) m (:basis f)))
                        {} holds)]
    [(count (:order rules))
     (section
      "Governor rule coverage — every declared rule, actually raised"
      (str "The rule set is DERIVED at build time by reading this actor's own source "
           "(<code>" (str/join "</code>, <code>" rule-sources) "</code>) for "
           "<code>:rule</code> literals, so a rule added tomorrow enters this table by "
           "itself. The build <strong>throws</strong> if any declared rule stayed silent "
           "— a bare &ldquo;at least one hold&rdquo; check would let this page stay green "
           "while the governor grew rules nobody exercised. All of these are HARD: a human "
           "approver cannot override them, and the proposal never reaches the approval node.")
      (table ["#" "Rule" "Declared in" "Times raised" "Ops that raised it" "Detail as the governor wrote it"]
             (map-indexed
              (fn [i r]
                (let [fs (get by-rule r)]
                  (tr [(str (inc i))
                       (code (str r))
                       (str/join ", " (map code (get-in rules [:by-rule r])))
                       (if (seq fs) (span "ok" (str (count fs)))
                           (span "critical" "0 — build should have thrown"))
                       (kw-list (distinct (map :op fs)))
                       (esc (or (->> fs (mapcat :violations)
                                     (filter #(= r (:rule %))) first :detail)
                                (when (seq fs) "(no detail — rule raised outside the governor)")))])))
              (:order rules))))]))

(defn- holds-section [db]
  (let [holds (measured! "ledger -> hold facts" (hold-facts db))]
    [(count holds)
     (section
      "HARD holds this run produced"
      (str "Every one of these is a real <code>:governor-hold</code> (or "
           "<code>:approval-rejected</code>) fact the run appended to the append-only "
           "store ledger. A HARD hold writes the rejection and mutates nothing else — "
           "no SSoT write, and for a governor HARD hold, no human is ever asked.")
      (table ["Fact" "Op" "Ticket" "Rules" "Detail" "Advisor confidence" "Reached a human?"]
             (for [{:keys [t op subject basis violations confidence]} holds]
               (tr [(if (= :governor-hold t)
                      (span "critical" (esc (name t)))
                      (span "err" (esc (name t))))
                    (code (str op))
                    (code subject)
                    (kw-list basis)
                    (esc (str/join " / " (keep :detail violations)))
                    (esc confidence)
                    (if (= :governor-hold t)
                      (span "ok" "no — HARD, never escalated")
                      (span "warn" "yes — a human said no"))]))))]))

(defn- action-gate-section [db]
  (let [ops (ops-in-order)
        postures (map #(op-posture db %) ops)]
    [(count postures)
     (section
      "Action gate — derived from the running code"
      (str "Every cell below is computed at render time by calling the real "
           "<code>specialtyrepair.repairopsllm/infer</code> (for the stake and SSoT "
           "effect each op proposes), the real <code>specialtyrepair.governor/high-stakes</code> "
           "set, and the real <code>specialtyrepair.phase/gate</code> at phase 3 for a "
           "governor-CLEAN proposal. Nothing here is transcribed, so it cannot drift from "
           "the code it documents.")
      (table ["Op" "SSoT effect" "Declared stake" "High-stakes?" "Writable in phases"
              "Auto-eligible in phases" "Phase-3 gate when the governor is clean"]
             (for [{:keys [op effect stake high-stakes?]} postures]
               (let [{:keys [disposition reason]} (gate-cell 3 op)]
                 (tr [(code (str op))
                      (code (str effect))
                      (if stake (code (str stake)) (span "muted" "nil"))
                      (if high-stakes? (span "warn" "yes — always a human") (span "muted" "no"))
                      (kw-list (sort (keep (fn [[p {:keys [writes]}]] (when (writes op) p))
                                           phase/phases)))
                      (let [autos (sort (keep (fn [[p {:keys [auto]}]] (when (auto op) p))
                                              phase/phases))]
                        (if (seq autos) (kw-list autos)
                            (span "warn" "never, at any phase")))
                      (str (case disposition
                             :commit   (span "ok" "auto-commit")
                             :escalate (span "warn" "human approval")
                             (span "critical" "hold"))
                           (when reason (str " &middot; " (code (str reason)))))])))))]))

(defn- phase-ladder-section []
  (let [ops (ops-in-order)
        phs (sort (keys phase/phases))]
    [(count phs)
     (section
      "Phase ladder — derived by calling the real gate"
      (str "One call to <code>specialtyrepair.phase/gate</code> per (phase, op) pair, "
           "asking what happens to a proposal the governor already cleared. "
           "<code>hold</code> here means the op is not enabled in that phase at all; "
           "<code>human</code> means enabled but not auto-eligible. Note that "
           "<code>:repair/complete</code> and <code>:item/return</code> read "
           "<code>human</code> in every row including phase 3 — that is a permanent "
           "structural fact, not a rollout milestone still to come.")
      (table (into ["Phase" "Label"] (map #(str %) ops))
             (for [ph phs]
               (tr (into [(code ph) (esc (:label (phase/phases ph)))]
                         (for [o ops]
                           (let [{:keys [disposition reason]} (gate-cell ph o)]
                             (str (case disposition
                                    :commit   (span "ok" "auto")
                                    :escalate (span "warn" "human")
                                    (span "critical" "hold"))
                                  (when reason
                                    (str "<br><small class=\"muted\">"
                                         (esc (name reason)) "</small>"))))))))))]))

(defn- approvals-section [db runs]
  (let [granted (facts-of-type runs :approval-granted)
        rows (for [{:keys [op subject by]} granted]
               (let [{:keys [effect measured? retained-in ledger-measured? ledger-retained-in]}
                     (retention db op subject)]
                 (tr [(code (str op))
                      (code subject)
                      (if by (span "ok" (esc by))
                          (span "critical" "UNMEASURED"))
                      (code (str effect))
                      (cond
                        (not measured?)
                        (span "critical" "could not read the register — not a finding about the domain")
                        (seq retained-in)
                        (span "ok" (str "retained as " (str/join ", " (map code retained-in))))
                        :else
                        (span "warn" "audit only — not retained in record"))
                      (cond
                        (not ledger-measured?)
                        (span "critical" "no committed ledger fact found")
                        (seq ledger-retained-in)
                        (span "ok" (str/join ", " (map code ledger-retained-in)))
                        :else
                        (span "warn" "audit only — not retained in ledger fact"))])))]
    [(count granted)
     (section
      "Human approvals — and where the approver actually survived"
      (str "Joined from the <code>:approval-granted</code> facts the run emitted on its "
           "<code>:audit</code> channel (which lives under <code>:state</code>, not at the "
           "top level of the <code>g/run*</code> result). The last two columns are "
           "<strong>measured</strong>, not asserted: for each approved op the build reads "
           "the register the commit actually landed in and looks for a key whose name "
           "carries an approver. &ldquo;audit only&rdquo; means the approval genuinely "
           "happened and a named human granted it, but the durable record does not carry "
           "the name — which is a different statement from &ldquo;nobody approved&rdquo;, "
           "and this page will not conflate the two. If a register cannot be read at all, "
           "the cell says so rather than reporting an absence.")
      (table ["Op" "Ticket" "Approved by" "Committed effect"
              "Approver in the store register?" "Approver in the ledger fact?"]
             rows))]))

(defn- escalations-section [runs]
  (let [reqs (facts-of-type runs :approval-requested)
        outcome (reduce (fn [m f] (assoc m [(:op f) (:subject f)] f))
                        {}
                        (concat (facts-of-type runs :approval-granted)
                                (facts-of-type runs :approval-rejected)))]
    [(count reqs)
     (section
      "Escalations — every time the actor stopped and asked"
      (str "<code>interrupt-before #{:request-approval}</code> pauses the graph and hands "
           "the decision to a human. <code>:reason</code> is the real reason the run "
           "recorded: <code>:phase-approval</code> (enabled at this phase but not "
           "auto-eligible) or <code>:actuation</code> (a real-world act — the governor's "
           "high-stakes set — which no phase ever auto-commits).")
      (table ["Op" "Ticket" "Reason" "Phase" "Advisor confidence" "What the human did"]
             (for [{:keys [op subject reason phase confidence]} reqs]
               (let [f (outcome [op subject])]
                 (tr [(code (str op))
                      (code subject)
                      (code (str reason))
                      (code phase)
                      (esc confidence)
                      (cond
                        (nil? f) (span "critical" "UNMEASURED — no outcome fact found")
                        (= :approval-granted (:t f))
                        (span "ok" (str "approved by " (esc (:by f))))
                        :else (span "err" "rejected — held, nothing committed"))])))))]))

(defn- jurisdiction-section []
  (let [isos (sort (keys facts/catalog))]
    [(count isos)
     (section
      "Jurisdiction spec-basis catalog"
      (str "The G2-style citation table <code>specialtyrepair.governor</code> checks every "
           "<code>:jurisdiction/assess</code> proposal against. A jurisdiction not in this "
           "table has NO spec-basis, full stop — the advisor must not fabricate one, and "
           "the run above shows what happens when it tries. The hallmark column is "
           "deliberately empty for Japan: this R0 catalog records no mandatory statutory "
           "hallmarking/assay-office regime there, and inventing one to make coverage look "
           "bigger is the exact fabrication this discipline forbids.")
      (table ["ISO3" "Jurisdiction" "Product-safety authority" "Legal basis"
              "Required evidence" "Precious-metal hallmark regime"]
             (for [iso isos]
               (let [sb (facts/spec-basis iso)
                     hm (facts/hallmark-spec-basis iso)]
                 (tr [(code iso)
                      (esc (:name sb))
                      (esc (:owner-authority sb))
                      (esc (:legal-basis sb))
                      (str (count (:required-evidence sb)) " &middot; "
                           (esc (str/join " / " (:required-evidence sb))))
                      (if hm
                        (str (esc (:hallmark-legal-basis hm)) "<br><small class=\"muted\">"
                             (esc (:hallmark-owner-authority hm)) "</small>")
                        (span "muted" "none in this R0 catalog"))])))))]))

(defn- coverage-section []
  (let [c (measured! "facts/coverage" (facts/coverage))]
    [1
     (section
      "Coverage, reported honestly"
      (str "Straight out of <code>specialtyrepair.facts/coverage</code>. A starting "
           "catalog is not a survey of ~194 jurisdictions, and this page says so rather "
           "than implying completeness.")
      (table ["Requested" "Covered" "Covered jurisdictions" "Missing" "Note"]
             [(tr [(esc (:requested c))
                   (esc (:covered c))
                   (kw-list (:covered-jurisdictions c))
                   (if (seq (:missing-jurisdictions c))
                     (kw-list (:missing-jurisdictions c)) (span "muted" "—"))
                   (esc (:note c))])]))]))

(defn- registry-section [db]
  (let [comps (store/completion-history db)
        rets  (store/return-history db)
        rows  (concat
               (for [r comps] (tr [(span "ok" "repair completion") (code (get r "record_id"))
                                   (code (get r "ticket_id")) (code (get r "jurisdiction"))
                                   (yes-no (get r "immutable"))
                                   (span "warn" "draft-unsigned")]))
               (for [r rets] (tr [(span "ok" "item return") (code (get r "record_id"))
                                  (code (get r "ticket_id")) (code (get r "jurisdiction"))
                                  (yes-no (get r "immutable"))
                                  (span "warn" "draft-unsigned")])))]
    [(+ (count comps) (count rets))
     (section
      "Registry drafts the actuations produced"
      (str "Built by <code>specialtyrepair.registry</code> — pure functions, no call to any "
           "real repair-shop system. Every certificate this actor produces is UNSIGNED: "
           "signature is the shop's act, not the actor's.")
      (table ["Kind" "Record id" "Ticket" "Jurisdiction" "Immutable" "Certificate status"]
             (measured! "registry drafts" (vec rows))))]))

(defn- ledger-section [db]
  (let [l (measured! "store/ledger" (store/ledger db))]
    [(count l)
     (section
      "Audit ledger (append-only, as stored)"
      (str "Every decision fact this run appended, in order. This is the durable trail — "
           "note that <code>:approval-granted</code> does NOT appear here: approvals live "
           "on the run's audit channel, and the committed fact records the basis, not the "
           "approver. That is what the approvals section above measures.")
      (table ["#" "Fact" "Op" "Ticket" "Disposition" "Basis"]
             (map-indexed
              (fn [i {:keys [t op subject disposition basis]}]
                (tr [(str (inc i))
                     (case t
                       :committed (span "ok" "committed")
                       :governor-hold (span "critical" "governor-hold")
                       :approval-rejected (span "err" "approval-rejected")
                       (esc (name t)))
                     (code (str op))
                     (code subject)
                     (code (str disposition))
                     (if (every? keyword? basis)
                       (kw-list basis)
                       (esc (str/join " / " (map str basis))))]))
              l)))]))

;; ----------------------------- assertions -------------------------------------

(defn assert-console!
  "Build-time invariants. THROWS (and so writes no page) unless the run
  really exercised the governor. See ns docstring."
  [db runs rules]
  (let [ledger   (store/ledger db)
        tickets  (store/all-tickets db)
        holds    (filter #(= :governor-hold (:t %)) ledger)
        observed (observed-rules db)
        declared (set (:order rules))
        missing  (sort (remove observed declared))
        declared-approvals (filterv #(= :approve (:kind %)) scenario)
        granted  (facts-of-type runs :approval-granted)
        unnamed  (remove :by granted)]

    ;; --- evidence floor: a scenario that measured nothing is not clean ---
    (when (empty? tickets)
      (throw (ex-info "MEASUREMENT FAILED: the ticket directory is empty."
                      {:kind :unmeasurable})))
    (when (empty? ledger)
      (throw (ex-info "MEASUREMENT FAILED: the store ledger is empty -- the scenario ran but recorded nothing."
                      {:kind :unmeasurable})))

    ;; --- 1: at least one HARD governor hold ---
    (when (zero? (count holds))
      (throw (ex-info (str "GOVERNOR NEVER FIRED: the run produced 0 :governor-hold facts. "
                           "A console that shows no hold is not evidence that the governor "
                           "works; it is evidence that nothing tested it.")
                      {:kind :coverage-gap :ledger-facts (count ledger)})))

    ;; --- 2: full declared-rule coverage ---
    ;; The floor comes FIRST. An empty declared set makes the coverage check
    ;; below vacuously true, so "I could not read the source" and "every rule
    ;; fired" would return the same green page. Measured here on 2026-08-15:
    ;; a `for`-binding bug in `scan-rules` made `declared` empty and the
    ;; coverage check passed while measuring nothing at all.
    (when (empty? declared)
      (throw (ex-info (str "MEASUREMENT FAILED: the declared rule set is empty, so the "
                           "coverage check below would pass without checking anything. "
                           "Refusing to report a pass.")
                      {:kind :unmeasurable :observed (vec observed)})))
    (when (< (count declared) (count observed))
      (throw (ex-info (str "MEASUREMENT FAILED: the run raised " (count observed)
                           " rule(s) but the source scan only declared " (count declared)
                           " -- the scan is not seeing rules that demonstrably exist.")
                      {:kind :unmeasurable
                       :declared (vec (sort declared)) :observed (vec observed)})))
    (when (seq missing)
      (throw (ex-info (str "RULE COVERAGE GAP: " (count missing) " of " (count declared)
                           " hold rules declared in " (str/join ", " rule-sources)
                           " were never raised by the real governor: "
                           (str/join ", " missing)
                           ". Add a case built from real seed data, or record the rule as "
                           "unreachable -- do not weaken this check.")
                      {:kind :coverage-gap :missing (vec missing)
                       :declared (vec (sort declared)) :observed (vec observed)})))

    ;; --- 3: approver attribution evidence floor ---
    (when (not= (count declared-approvals) (count granted))
      (throw (ex-info (str "APPROVAL ATTRIBUTION LOST: the scenario issued "
                           (count declared-approvals) " human approval(s) but the page "
                           "would show " (count granted) " :approval-granted fact(s). "
                           "The :audit channel lives UNDER :state in the g/run* result; "
                           "a page that renders this absence as \"auto-committed\" is "
                           "reporting a failed measurement as a domain fact.")
                      {:kind :unmeasurable
                       :declared (mapv :thread declared-approvals)
                       :measured (mapv (juxt :op :subject) granted)})))
    (when (seq unnamed)
      (throw (ex-info (str "APPROVAL ATTRIBUTION LOST: " (count unnamed)
                           " :approval-granted fact(s) carry no approver name.")
                      {:kind :unmeasurable :facts (vec unnamed)})))

    {:ledger-facts (count ledger)
     :tickets (count tickets)
     :hard-holds (count holds)
     :rules-declared (count declared)
     :rules-observed (count observed)
     :approvals (count granted)}))

;; ----------------------------- document ---------------------------------------

(def ^:private console-css-resource "specialtyrepair/console.css")

(defn- console-css []
  (let [u (or (io/resource console-css-resource)
              (throw (ex-info (str "MEASUREMENT FAILED: vendored stylesheet "
                                   console-css-resource " is not on the classpath.")
                              {:kind :unmeasurable})))]
    (slurp u)))

(defn render
  "Renders the whole document from a store `db` and the run records
  `runs` that produced it. Returns [html {:sections n :rows n}]."
  [db runs rules]
  (let [parts [(tickets-section db)
               (rule-coverage-section db rules)
               (holds-section db)
               (action-gate-section db)
               (phase-ladder-section)
               (escalations-section runs)
               (approvals-section db runs)
               (jurisdiction-section)
               (coverage-section)
               (registry-section db)
               (ledger-section db)]
        rows (reduce + (map first parts))]
    [(str
      "<!doctype html>\n<html lang=\"ja\"><head><meta charset=\"utf-8\">\n"
      "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
      "<title>cloud-itonami-isic-9529 &middot; specialty repair &mdash; Operator Console</title>\n"
      "<style>\n" (console-css) "\n</style>\n</head><body>\n"
      "<header class=\"bar\">\n"
      "  <h1>Repair of other personal and household goods (ISIC 9529) — Operator Console</h1>\n"
      "  <span class=\"badge\">read-only sample · every row generated at build time by running the real actor · "
      "repair completion and item return are always human-approved</span>\n"
      "</header>\n"
      "<main>\n"
      "  <section class=\"card\">\n"
      "    <h2>What this page is</h2>\n"
      "    <p>Generated by <code>clojure -M:dev:render-html</code>, which executes "
      (count scenario) " real requests through <code>specialtyrepair.operation</code>'s "
      "langgraph StateGraph (<code>langgraph.graph/run*</code>) against this repo's own "
      "seed data (<code>specialtyrepair.store/demo-data</code>) and renders what it "
      "observed. Watches, jewelry and bicycles — the goods this ISIC class covers — put "
      "two independent hazards in one shop: an unsafe repair that goes back to a customer, "
      "and a precious-metal repair whose hallmark integrity was never confirmed. Both are "
      "HARD holds below.</p>\n"
      "    <p class=\"muted\">The build refuses to write this file unless every hold rule "
      "declared in the actor's own source was actually raised by the real governor, and "
      "every human approval the scenario issued was actually observed with a named "
      "approver. Nothing on this page is a placeholder; where a value could not be "
      "measured, the cell says so instead of guessing.</p>\n"
      "  </section>\n"
      (str/join (map second parts))
      "</main>\n"
      "<footer>\n"
      "  <p>Generated from <code>specialtyrepair.render-html</code> — "
      (count scenario) " scenario steps, " rows " derived rows across "
      (count parts) " sections. Deterministic: no timestamps, no wall-clock, "
      "every collection sorted before rendering, so two runs against the same seed are "
      "byte-identical. Styling is vendored DADS "
      "(<code>jp-go-digital-design-system</code>) CSS carried in this repo as "
      "<code>resources/" console-css-resource "</code>, so a cold fork needs no network.</p>\n"
      "</footer>\n"
      "</body></html>\n")
     {:sections (count parts) :rows rows}]))

(defn -main [& args]
  (let [out   (or (first args) "docs/samples/operator-console.html")
        {:keys [db runs]} (run-demo!)
        rules (scan-rules)
        stats (assert-console! db runs rules)
        [html meta*] (render db runs rules)]
    (io/make-parents out)
    (spit out html)
    (println "wrote" out
             (str "(" (count html) " chars, " (:sections meta*) " sections, "
                  (:rows meta*) " rows)"))
    (println "  scenario steps  :" (count scenario))
    (println "  ledger facts    :" (:ledger-facts stats))
    (println "  HARD holds      :" (:hard-holds stats))
    (println "  rules declared  :" (:rules-declared stats)
             (str "(" (str/join ", " (:order rules)) ")"))
    (println "  rules observed  :" (:rules-observed stats)
             (str "(" (str/join ", " (observed-rules db)) ")"))
    (println "  approvals       :" (:approvals stats))
    (println "  tickets         :" (:tickets stats))))
