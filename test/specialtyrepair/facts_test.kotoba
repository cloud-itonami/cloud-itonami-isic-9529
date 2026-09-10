(ns specialtyrepair.facts-test
  (:require [clojure.test :refer [deftest is]]
            [specialtyrepair.facts :as facts]))

(deftest jpn-has-a-spec-basis
  (is (some? (facts/spec-basis "JPN")))
  (is (string? (:provenance (facts/spec-basis "JPN")))))

(deftest jpn-has-no-hallmark-spec-basis
  (is (nil? (facts/hallmark-spec-basis "JPN"))
      "Japan has no formal statutory precious-metal-hallmark/assay-office regime in this R0 catalog -- must not be fabricated"))

(deftest usa-gbr-deu-each-have-a-hallmark-spec-basis
  (doseq [iso3 ["USA" "GBR" "DEU"]]
    (is (some? (facts/hallmark-spec-basis iso3)) (str iso3 " hallmark-spec-basis"))
    (is (string? (:hallmark-provenance (facts/hallmark-spec-basis iso3))) (str iso3 " hallmark-provenance"))))

(deftest unknown-jurisdiction-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "ATL"))))

(deftest unknown-jurisdiction-has-no-hallmark-spec-basis
  (is (nil? (facts/hallmark-spec-basis "ATL"))))

(deftest coverage-never-reports-a-missing-jurisdiction-as-covered
  (let [report (facts/coverage ["JPN" "ATL" "GBR"])]
    (is (= 2 (:covered report)))
    (is (= ["ATL"] (:missing-jurisdictions report)))
    (is (= ["GBR" "JPN"] (:covered-jurisdictions report)))))

(deftest required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "JPN")]
    (is (facts/required-evidence-satisfied? "JPN" all))
    (is (not (facts/required-evidence-satisfied? "JPN" (rest all))))
    (is (not (facts/required-evidence-satisfied? "ATL" all)) "no spec-basis -> never satisfied")))
