(ns specialtyrepair.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean ticket through
  intake -> jurisdiction assessment -> post-repair safety screening ->
  hallmark-integrity screening -> repair completion (escalate/approve/
  commit) -> item return (escalate/approve/commit), then shows HARD-
  hold scenarios: a jurisdiction with no spec-basis, a parts-cost
  mismatch (verified first), a failed post-repair safety test
  (screened directly via `:safety/screen`), an unconfirmed hallmark-
  integrity status on a precious-metal ticket (screened directly via
  `:hallmark/screen` -- never via an actuation op against an
  unscreened ticket -- see this actor's own governor ns docstring /
  the lesson `parksafety`'s ADR-2607071922 Decision 5, `eldercare`'s,
  `museum`'s, `conservation`'s, `salon`'s, `entertainment`'s,
  `casework`'s, `hospital`'s, `facility`'s, `school`'s, `association`'s,
  `leasing`'s, `behavioral`'s, `secondary`'s, `card`'s, `water`'s,
  `telecom`'s, `aerospace`'s, `recovery`'s, `consulting`'s, `union`'s,
  `congregation`'s, `fab`'s, `energy`'s, `care`'s, `navigator`'s,
  `learning`'s, `banking`'s, `advertising`'s, `polling`'s, `research`'s,
  `design`'s, `nursing`'s, `sports`'s, `alliedhealth`'s, `laundry`'s,
  `holdco`'s, `photo`'s, `personalservice`'s, `edsupport`'s,
  `headoffice`'s, `residential`'s, `cultural`'s, `reserve`'s,
  `proserv`'s, `sportsevent`'s, `recreation`'s, `sportsclub`'s,
  `partyops`'s, `memberorg`'s, `commrepair`'s, `applianceshop`'s,
  `socialresearch`'s, `bizassoc`'s, `vocational`'s, `training`'s and
  `furniture`'s ADR-0001s already recorded), a double completion, and a
  double return."
  (:require [langgraph.graph :as g]
            [specialtyrepair.store :as store]
            [specialtyrepair.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :repair-technician :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== ticket/intake ticket-1 (JPN, clean; no precious-metal work) ==")
    (println (exec-op actor "t1" {:op :ticket/intake :subject "ticket-1"
                                  :patch {:id "ticket-1" :customer "Sakura Tanaka"}} operator))

    (println "== jurisdiction/assess ticket-1 (escalates -- human approves) ==")
    (println (exec-op actor "t2" {:op :jurisdiction/assess :subject "ticket-1"} operator))
    (println (approve! actor "t2"))

    (println "== safety/screen ticket-1 (clean; escalates -- human approves) ==")
    (println (exec-op actor "t3" {:op :safety/screen :subject "ticket-1"} operator))
    (println (approve! actor "t3"))

    (println "== hallmark/screen ticket-1 (no precious-metal work; escalates -- human approves) ==")
    (println (exec-op actor "t3b" {:op :hallmark/screen :subject "ticket-1"} operator))
    (println (approve! actor "t3b"))

    (println "== repair/complete ticket-1 (always escalates -- actuation/complete-repair) ==")
    (let [r (exec-op actor "t4" {:op :repair/complete :subject "ticket-1"} operator)]
      (println r)
      (println "-- human repair technician approves --")
      (println (approve! actor "t4")))

    (println "== item/return ticket-1 (always escalates -- actuation/return-item) ==")
    (let [r (exec-op actor "t5" {:op :item/return :subject "ticket-1"} operator)]
      (println r)
      (println "-- human repair technician approves --")
      (println (approve! actor "t5")))

    (println "== jurisdiction/assess ticket-2 (no spec-basis -> HARD hold) ==")
    (println (exec-op actor "t6" {:op :jurisdiction/assess :subject "ticket-2" :no-spec? true} operator))

    (println "== jurisdiction/assess ticket-3 (escalates -- human approves; sets up the parts-cost-mismatch test) ==")
    (println (exec-op actor "t7" {:op :jurisdiction/assess :subject "ticket-3"} operator))
    (println (approve! actor "t7"))

    (println "== repair/complete ticket-3 (claimed 120.0 vs recompute 80.0 -> HARD hold) ==")
    (println (exec-op actor "t8" {:op :repair/complete :subject "ticket-3"} operator))

    (println "== safety/screen ticket-4 (failed -> HARD hold, never reaches a human) ==")
    (println (exec-op actor "t9" {:op :safety/screen :subject "ticket-4"} operator))

    (println "== hallmark/screen ticket-5 (precious-metal work, integrity unconfirmed -> HARD hold, never reaches a human) ==")
    (println (exec-op actor "t10" {:op :hallmark/screen :subject "ticket-5"} operator))

    (println "== repair/complete ticket-1 AGAIN (double-completion -> HARD hold) ==")
    (println (exec-op actor "t11" {:op :repair/complete :subject "ticket-1"} operator))

    (println "== item/return ticket-1 AGAIN (double-return -> HARD hold) ==")
    (println (exec-op actor "t12" {:op :item/return :subject "ticket-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft repair-completion records ==")
    (doseq [r (store/completion-history db)] (println r))

    (println "== draft item-return records ==")
    (doseq [r (store/return-history db)] (println r))))
