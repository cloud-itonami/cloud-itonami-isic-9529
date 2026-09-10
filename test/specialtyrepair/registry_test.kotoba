(ns specialtyrepair.registry-test
  (:require [clojure.test :refer [deftest is testing]]
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

;; ---------------------------------------------------------------------------
;; Money is compared at money precision, not at double precision
;; ---------------------------------------------------------------------------

(deftest an-exhaustive-sweep-finds-no-correct-claim-rejected
  (testing "`(== (double claimed) (* (double qty) (double price)))` rejected
            CORRECT totals -- 14,213 of 68,568 combinations (20.7%) on this
            exact quantity x unit-price shape"
    (let [bad (for [q (range 1 25)
                    c (range 1 20000 7)
                    :let [price (/ c 100.0) truth (/ (* q c) 100.0)]
                    :when (not (r/parts-cost-matches-claim?
                                {:parts-quantity q :parts-unit-price price
                                 :claimed-parts-cost truth}))]
                [q price truth])]
      (is (empty? bad) (str "false rejections: " (count bad) " e.g. " (first bad))))))

(deftest a-genuinely-wrong-claim-is-still-caught
  (testing "rounding to money precision must not blunt the check"
    (is (not (r/parts-cost-matches-claim? {:parts-quantity 3 :parts-unit-price 29.99
                                           :claimed-parts-cost 89.96})))
    (is (not (r/parts-cost-matches-claim? {:parts-quantity 1 :parts-unit-price 10.00
                                           :claimed-parts-cost 10.01})))))

(deftest a-missing-or-non-numeric-amount-never-matches
  (testing "un-verifiable is not the same as correct, and not a crash"
    (is (not (r/parts-cost-matches-claim? {:parts-quantity 3 :parts-unit-price 29.99})))
    (is (not (r/parts-cost-matches-claim? {:parts-quantity 3 :parts-unit-price "29.99"
                                           :claimed-parts-cost 89.97})))))
