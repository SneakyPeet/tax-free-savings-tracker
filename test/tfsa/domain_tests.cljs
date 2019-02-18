(ns tfsa.domain-tests
  (:require [tfsa.domain :as sut]
            [cljs.test :as t :include-macros true]
            [tfsa.reconciler :as reconciler]
            [citrus.core :as citrus]
            [cljs-time.core :as time]))


;;;; PERSON

(t/deftest person-controller
  (t/testing ":init should return initial-person"
    (t/is (= {:state sut/initial-person} (sut/person :init))))
  (t/testing ":person/change should return new person"
    (t/is (= {:state "Piet"} (sut/person :person/change ["Piet"] "Foo"))))
  (t/testing ":person/add should return new person"
    (t/is (= {:state "Piet"} (sut/person :person/add ["Piet"] "Foo"))))
  (t/testing ":person/remove should do nothing if different person"
    (t/is (= {:state "Foo"} (sut/person :person/remove ["Piet"] "Foo"))))
  (t/testing ":person/remove should set person to nil if same person"
    (t/is (= {:state nil} (sut/person :person/remove ["Piet"] "Piet")))))


(t/deftest selected-person
  (let [state (reconciler/make-init)]
    (t/is (= sut/initial-person @(sut/selected-person state)))))


;;;; PEOPLE

(t/deftest people-controller
  (t/testing ":init should return initial-people"
    (t/is (= {:state sut/initial-people} (sut/people :init))))
  (t/testing ":person/add should add person to people"
    (t/is (= {:state (conj sut/initial-people "Piet")} (sut/people :person/add ["Piet"] sut/initial-people))))
  (t/testing ":person/remove should remove person from people"
    (t/is (= {:state #{"Daan"}} (sut/people :person/remove ["Piet"] #{"Piet" "Daan"})))))


(t/deftest all-people
  (let [state (reconciler/make-init)]
    (t/is (= sut/initial-people @(sut/all-people state)))))


;;;; DEPOSIT

(t/deftest deposits-controller
  (t/testing ":init should return empty map"
    (t/is (= {:state sut/initial-deposits} (sut/deposits :init))))
  (t/testing ":person/add should do nothing"
    (t/is (= {:state sut/initial-deposits} (sut/deposits :person/add ["Piet"] sut/initial-deposits))))
  (t/testing ":person/remove should remove all deposits"
    (let [deposits {1 {:person "Piet"}
                    2 {:person "Piet"}
                    3 {:person "Daan"}}
          expected {3 {:person "Daan"}}]
      (t/is (= {:state expected :save-state expected} (sut/deposits :person/remove ["Piet"] deposits)))))
  (t/testing ":deposit/add should add the deposit and calculate the tax year and add the date"
    (let [deposit-id (random-uuid)
          person "Piet"
          deposit {:year 2018 :month 1 :day 11}
          expected {deposit-id (assoc deposit
                                      :deposit-id deposit-id
                                      :person person
                                      :tax-year 2017
                                      :timestamp (.getTime (time/date-time 2018 1 11)))}]
      (t/is (= {:state expected :save-state expected}
               (sut/deposits :deposit/add [deposit-id person deposit] sut/initial-deposits)))))
  (t/testing ":deposit/remove should remove the deposit"
    (let [deposits {"1" {} "2" {} "3" {}}
          expected {"2" {} "3" {}}]
      (t/is (= {:state expected :save-state expected}
               (sut/deposits :deposit/remove ["1"] deposits))))))


(t/deftest deposits-for-person
  (let [deposit-id (random-uuid)
        person "Piet"
        deposit {:year 2019 :month 8 :day 8}
        expected [(assoc deposit :person person :deposit-id deposit-id
                         :tax-year 2019 :timestamp (.getTime (time/date-time 2019 8 8)))]
        state (reconciler/make-init)]
    (citrus/dispatch-sync! state :deposits :deposit/add deposit-id person deposit)
    (t/testing "no deposits should return []"
      (t/is (= [] @(sut/deposits-for-person state "Foo"))))
    (t/testing "all deposits for person should return"
      (t/is (= expected @(sut/deposits-for-person state person))))))


(t/deftest lifetime-contributions
  (let [deposits [{:amount 10} {:amount 15} {:amount 25}]]
    (t/is (= 50 (sut/lifetime-contributions deposits)))))


;;;; Tax Year

(t/deftest calculate-tax-year
  (let [d (fn [y m d] {:year y :month m :day d})]
    (t/is (= 2017 (sut/calculate-tax-year (d 2018 1 1))))
    (t/is (= 2017 (sut/calculate-tax-year (d 2018 2 28))))
    (t/is (= 2015 (sut/calculate-tax-year (d 2016 2 29))))
    (t/is (= 2018 (sut/calculate-tax-year (d 2018 3 1))))
    (t/is (= 2018 (sut/calculate-tax-year (d 2018 12 31))))))
