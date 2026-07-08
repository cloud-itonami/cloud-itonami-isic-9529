(ns specialtyrepair.registry
  "Pure-function repair-completion + item-return record construction --
  an append-only specialty-repair-shop book-of-record draft, closely
  modeled on `cloud-itonami-isic-9524`'s `furniture.registry`.

  Like every sibling actor's registry, there is no single international
  check-digit standard for a repair-completion or item-return reference
  number -- every shop/jurisdiction assigns its own reference format.
  This namespace does NOT invent one; it builds a jurisdiction-scoped
  sequence number and validates the record's required fields, the same
  honest, non-fabricating discipline `specialtyrepair.facts` uses.

  `parts-cost-matches-claim?` is an HONEST, LITERAL reuse of
  `repairshop.registry`'s/`commrepair.registry`'s/`applianceshop.
  registry`'s/`furniture.registry`'s own EXACT-MATCH independent-
  recompute shape, the SAME real-world concern (a claimed parts cost
  must equal parts-quantity x parts-unit-price) reapplied to watch/
  jewelry/bicycle-repair tickets (movement parts, precious-metal
  solder/findings, brake/gear components) rather than consumer-
  electronics, communication-equipment, household-appliance or
  furniture repair tickets -- not claimed as new.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real repair-shop-management system. It builds the RECORD
  a shop would keep, not the act of completing a repair or returning
  an item itself (that is `specialtyrepair.operation`'s `:repair/
  complete`/`:item/return`, always human-gated -- see README
  `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  repair shop's act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn compute-parts-cost
  "The ground-truth parts cost owed for `ticket`'s own `:parts-
  quantity` and `:parts-unit-price` -- a single flat quantity x unit-
  price calculation, not a full parts-catalog/labor/tax invoice
  engine."
  [{:keys [parts-quantity parts-unit-price]}]
  (* (double parts-quantity) (double parts-unit-price)))

(defn parts-cost-matches-claim?
  "Does `ticket`'s own `:claimed-parts-cost` equal the independently
  recomputed `compute-parts-cost`? A pure ground-truth check against
  the ticket's own permanent fields -- see ns docstring for why this
  is an honest, literal reuse of `furniture.registry`'s own check, not
  a new concept."
  [{:keys [claimed-parts-cost] :as ticket}]
  (== (double claimed-parts-cost) (compute-parts-cost ticket)))

(defn register-repair-completion
  "Validate + construct the REPAIR-COMPLETION registration DRAFT -- the
  shop's own legal act of completing a real repair. Pure function --
  does not touch any real repair-shop-management system; it builds
  the RECORD a shop would keep. `specialtyrepair.governor`
  independently re-verifies the ticket's own parts-cost arithmetic,
  and blocks a double-completion of the same ticket, before this is
  ever allowed to commit."
  [ticket-id jurisdiction sequence]
  (when-not (and ticket-id (not= ticket-id ""))
    (throw (ex-info "repair-completion: ticket_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "repair-completion: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "repair-completion: sequence must be >= 0" {})))
  (let [completion-number (str (str/upper-case jurisdiction) "-RPR-" (zero-pad sequence 6))
        record {"record_id" completion-number
                "kind" "repair-completion-draft"
                "ticket_id" ticket-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "completion_number" completion-number
     "certificate" (unsigned-certificate "RepairCompletion" completion-number completion-number)}))

(defn register-item-return
  "Validate + construct the ITEM-RETURN registration DRAFT -- the
  shop's own legal act of returning a real item to the customer. Pure
  function -- does not touch any real repair-shop-management system;
  it builds the RECORD a shop would keep. `specialtyrepair.governor`
  independently re-verifies the ticket's own post-repair safety-test
  status and hallmark-integrity status (when applicable), and blocks a
  double-return of the same ticket, before this is ever allowed to
  commit."
  [ticket-id jurisdiction sequence]
  (when-not (and ticket-id (not= ticket-id ""))
    (throw (ex-info "item-return: ticket_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "item-return: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "item-return: sequence must be >= 0" {})))
  (let [return-number (str (str/upper-case jurisdiction) "-RTN-" (zero-pad sequence 6))
        record {"record_id" return-number
                "kind" "item-return-draft"
                "ticket_id" ticket-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "return_number" return-number
     "certificate" (unsigned-certificate "ItemReturn" return-number return-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
