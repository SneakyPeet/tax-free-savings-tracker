(ns tfsa.domain-tests
  (:require [tfsa.domain :as sut]
            [cljs.test :as t :include-macros true]
            [tfsa.reconciler :as reconciler]
            [citrus.core :as citrus]))


;;;; PERSON

(t/deftest person-controller
  (t/testing ":init should return initial-person"
    (t/is (= {:state sut/initial-person} (sut/person :init))))
  (t/testing ":person/change should return new person"
    (t/is (= {:state "Piet"} (sut/person :person/change ["Piet"] "Foo"))))
  (t/testing ":person/add should return new person"
    (t/is (= {:state "Piet"} (sut/person :person/add ["Piet"] "Foo")))))


(t/deftest selected-person
  (let [state (reconciler/make-init)]
    (t/is (= sut/initial-person @(sut/selected-person state)))))


;;;; PEOPLE

(t/deftest people-controller
  (t/testing ":init should return initial-people"
    (t/is (= {:state sut/initial-people} (sut/people :init))))
  (t/testing ":person/add should add person to people"
    (t/is (= {:state (conj sut/initial-people "Piet")} (sut/people :person/add ["Piet"] sut/initial-people)))))


(t/deftest all-people
  (let [state (reconciler/make-init)]
    (t/is (= sut/initial-people @(sut/all-people state)))))


;;;; DEPOSIT

(t/deftest deposits-controller
  (t/testing ":init should return empty map"
    (t/is (= {:state sut/initial-deposits} (sut/deposits :init))))
  (t/testing ":person/add should do nothing"
    (t/is (= {:state sut/initial-deposits} (sut/deposits :person/add ["Piet"] sut/initial-deposits))))
  (t/testing ":deposit/add should add the deposit"
    (let [deposit-id (random-uuid)
          person "Piet"
          deposit {}
          expected {deposit-id (assoc deposit :person person)}]
      (t/is (= {:state expected} (sut/deposits :deposit/add [deposit-id person deposit] sut/initial-deposits))))))


(t/deftest deposits-for-person
  (let [deposit-id (random-uuid)
        person "Piet"
        deposit {:foo "bar"}
        expected [(assoc deposit :person person)]
        state (reconciler/make-init)]
    (citrus/dispatch-sync! state :deposits :deposit/add deposit-id person deposit)
    (t/testing "no deposits should return []"
      (t/is (= [] @(sut/deposits-for-person state "Foo"))))
    (t/testing "all deposits for person should return"
      (t/is (= expected @(sut/deposits-for-person state person))))))


(t/deftest lifetime-contributions
  (let [deposits [{:amount 10} {:amount 15} {:amount 25}]]
    (t/is (= 50 (sut/lifetime-contributions deposits)))))
