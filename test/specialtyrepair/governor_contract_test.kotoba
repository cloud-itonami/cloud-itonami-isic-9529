(ns specialtyrepair.governor-contract-test
  "The governor contract as executable tests -- the other-personal-and-
  household-goods-repair analog of `cloud-itonami-isic-9521`'s
  `repairshop.governor-contract-test` (itself the electronics-repair
  analog of `cloud-itonami-isic-6512`'s `casualty.governor-contract-
  test`), and structurally closest to `cloud-itonami-isic-9524`'s
  `furniture.governor-contract-test`. The single invariant under test:

    RepairOps-LLM never completes a repair or returns an item the
    Repair Shop Governor would reject, `:repair/complete`/`:item/
    return` NEVER auto-commit at any phase, `:ticket/intake` (no
    direct capital risk) MAY auto-commit when clean, and every
    decision (commit OR hold) leaves exactly one ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [specialtyrepair.store :as store]
            [specialtyrepair.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :repair-technician :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- assess!
  "Walks `subject` through assess -> approve, leaving an assessment on
  file. Uses distinct thread-ids per call site by suffixing
  `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-assess") {:op :jurisdiction/assess :subject subject} operator)
  (approve! actor (str tid-prefix "-assess")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :ticket/intake :subject "ticket-1"
                   :patch {:id "ticket-1" :customer "Sakura Tanaka"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Sakura Tanaka" (:customer (store/ticket db "ticket-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest jurisdiction-assess-always-needs-approval
  (testing "assess is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :jurisdiction/assess :subject "ticket-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "ticket-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a jurisdiction/assess proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :jurisdiction/assess :subject "ticket-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "ticket-1")) "no assessment written"))))

(deftest repair-complete-without-assessment-is-held
  (testing "repair/complete before any jurisdiction assessment -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :repair/complete :subject "ticket-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest parts-cost-mismatch-is-held
  (testing "a claimed parts cost that doesn't equal quantity x unit-price -> HOLD (honest reuse of repairshop/9521's, commrepair/9512's, applianceshop/9522's and furniture/9524's own check)"
    (let [[db actor] (fresh)
          _ (assess! actor "t5pre" "ticket-3")
          res (exec-op actor "t5" {:op :repair/complete :subject "ticket-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:parts-cost-mismatch} (-> (store/ledger db) last :basis)))
      (is (empty? (store/completion-history db))))))

(deftest safety-test-not-passed-is-held-and-unoverridable
  (testing "a failed post-repair safety test on a ticket -> HOLD, and never reaches request-approval (honest reuse of repairshop/9521's, commrepair/9512's, applianceshop/9522's and furniture/9524's own check)"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :safety/screen :subject "ticket-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:safety-test-not-passed} (-> (store/ledger db) first :basis)))
      (is (nil? (store/safety-screening-of db "ticket-4")) "no clearance written"))))

(deftest hallmark-integrity-unconfirmed-is-held-and-unoverridable
  (testing "an unconfirmed hallmark-integrity status on a precious-metal ticket -> HOLD, and never reaches request-approval -- exercised via :hallmark/screen DIRECTLY, not via an actuation op against an unscreened ticket -- the genuinely NEW check this vertical adds, the 68th unconditional-evaluation-discipline grounding overall and the FIFTH conditional variant (see this actor's governor ns docstring / parksafety's ADR-2607071922 Decision 5 / eldercare's, museum's, conservation's, salon's, entertainment's, casework's, hospital's, facility's, school's, association's, leasing's, behavioral's, secondary's, card's, water's, telecom's, aerospace's, recovery's, consulting's, union's, congregation's, fab's, energy's, care's, navigator's, learning's, banking's, advertising's, polling's, research's, design's, nursing's, sports's, alliedhealth's, laundry's, holdco's, photo's, personalservice's, edsupport's, headoffice's, residential's, cultural's, reserve's, proserv's, sportsevent's, recreation's, sportsclub's, partyops's, memberorg's, commrepair's, applianceshop's, socialresearch's, bizassoc's, vocational's, training's and furniture's ADR-0001s)"
    (let [[db actor] (fresh)
          res (exec-op actor "t7" {:op :hallmark/screen :subject "ticket-5"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:hallmark-integrity-unconfirmed} (-> (store/ledger db) first :basis)))
      (is (nil? (store/hallmark-screening-of db "ticket-5")) "no clearance written"))))

(deftest hallmark-screen-is-a-noop-when-no-precious-metal-work
  (testing "the hallmark-integrity check is CONDITIONAL: a repair that does not involve precious-metal work has no hallmark-integrity requirement at all"
    (let [[_db actor] (fresh)
          res (exec-op actor "t7b" {:op :hallmark/screen :subject "ticket-1"} operator)]
      (is (= :interrupted (:status res)) "clean screening still escalates for human sign-off, but is NOT a HARD hold"))))

(deftest repair-complete-always-escalates-then-human-decides
  (testing "a clean, fully-assessed, matching-parts-cost ticket still ALWAYS interrupts for human approval -- actuation/complete-repair is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t8pre" "ticket-1")
          r1 (exec-op actor "t8" {:op :repair/complete :subject "ticket-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, completion record drafted"
        (let [r2 (approve! actor "t8")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:repair-completed? (store/ticket db "ticket-1"))))
          (is (= 1 (count (store/completion-history db))) "one draft completion record"))))))

(deftest item-return-always-escalates-then-human-decides
  (testing "a clean, fully-assessed, passed-safety-test ticket still ALWAYS interrupts for human approval -- actuation/return-item is never auto"
    (let [[db actor] (fresh)
          _ (assess! actor "t9pre" "ticket-1")
          r1 (exec-op actor "t9" {:op :item/return :subject "ticket-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, return record drafted"
        (let [r2 (approve! actor "t9")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:item-returned? (store/ticket db "ticket-1"))))
          (is (= 1 (count (store/return-history db))) "one draft return record"))))))

(deftest repair-complete-double-completion-is-held
  (testing "completing the same ticket's repair twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t10pre" "ticket-1")
          _ (exec-op actor "t10a" {:op :repair/complete :subject "ticket-1"} operator)
          _ (approve! actor "t10a")
          res (exec-op actor "t10" {:op :repair/complete :subject "ticket-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-completed} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/completion-history db))) "still only the one earlier completion"))))

(deftest item-return-double-return-is-held
  (testing "returning the same ticket's item twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t11pre" "ticket-1")
          _ (exec-op actor "t11a" {:op :item/return :subject "ticket-1"} operator)
          _ (approve! actor "t11a")
          res (exec-op actor "t11" {:op :item/return :subject "ticket-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-returned} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/return-history db))) "still only the one earlier return"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :ticket/intake :subject "ticket-1"
                          :patch {:id "ticket-1" :customer "Sakura Tanaka"}} operator)
      (exec-op actor "b" {:op :jurisdiction/assess :subject "ticket-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
