(ns specialtyrepair.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:repair/complete`/`:item/return` must NEVER be a
  member of any phase's `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [specialtyrepair.phase :as phase]))

(deftest repair-complete-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real repair completion"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :repair/complete))
          (str "phase " n " must not auto-commit :repair/complete")))))

(deftest item-return-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real item return"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :item/return))
          (str "phase " n " must not auto-commit :item/return")))))

(deftest safety-and-hallmark-screen-never-auto-at-any-phase
  (testing "screening carries no direct capital risk, but is still never auto-eligible, matching every sibling screening op in this fleet"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :safety/screen))
          (str "phase " n " must not auto-commit :safety/screen"))
      (is (not (contains? auto :hallmark/screen))
          (str "phase " n " must not auto-commit :hallmark/screen")))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-capital-risk-ops
  (testing ":ticket/intake carries no direct capital risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:ticket/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :ticket/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :repair/complete} :commit))))
  (is (= :escalate (:disposition (phase/gate 3 {:op :item/return} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :ticket/intake} :commit)))))
