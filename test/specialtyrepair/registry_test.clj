(ns specialtyrepair.registry-test
  (:require [clojure.test :refer [deftest is]]
            [specialtyrepair.registry :as r]))

;; ----------------------------- parts-cost-matches-claim? -----------------------------

(deftest matches-when-claim-equals-recompute
  (is (r/parts-cost-matches-claim?
       {:parts-quantity 1 :parts-unit-price 12 :claimed-parts-cost 12.0})))

(deftest mismatches-when-claim-differs-from-recompute
  (is (not (r/parts-cost-matches-claim?
            {:parts-quantity 1 :parts-unit-price 80 :claimed-parts-cost 120.0}))))

(deftest compute-parts-cost-is-a-flat-quantity-times-unit-price
  (is (= 80.0 (r/compute-parts-cost {:parts-quantity 1 :parts-unit-price 80}))))

;; ----------------------------- register-repair-completion -----------------------------

(deftest completion-is-a-draft-not-a-real-completion
  (let [result (r/register-repair-completion "ticket-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest completion-assigns-completion-number
  (let [result (r/register-repair-completion "ticket-1" "JPN" 7)]
    (is (= (get result "completion_number") "JPN-RPR-000007"))
    (is (= (get-in result ["record" "ticket_id"]) "ticket-1"))
    (is (= (get-in result ["record" "kind"]) "repair-completion-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest completion-validation-rules
  (is (thrown? Exception (r/register-repair-completion "" "JPN" 0)))
  (is (thrown? Exception (r/register-repair-completion "ticket-1" "" 0)))
  (is (thrown? Exception (r/register-repair-completion "ticket-1" "JPN" -1))))

;; ----------------------------- register-item-return -----------------------------

(deftest return-is-a-draft-not-a-real-return
  (let [result (r/register-item-return "ticket-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest return-assigns-return-number
  (let [result (r/register-item-return "ticket-1" "JPN" 7)]
    (is (= (get result "return_number") "JPN-RTN-000007"))
    (is (= (get-in result ["record" "ticket_id"]) "ticket-1"))
    (is (= (get-in result ["record" "kind"]) "item-return-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest return-validation-rules
  (is (thrown? Exception (r/register-item-return "" "JPN" 0)))
  (is (thrown? Exception (r/register-item-return "ticket-1" "" 0)))
  (is (thrown? Exception (r/register-item-return "ticket-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-repair-completion "ticket-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-repair-completion "ticket-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-RPR-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-RPR-000001" (get-in hist2 [1 "record_id"])))))
