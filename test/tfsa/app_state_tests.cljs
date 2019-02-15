(ns tfsa.app-state-tests
  (:require [tfsa.app-state :as sut]
            [cljs.test :as t :include-macros true]
            [tfsa.reconciler :as reconciler]
            [tfsa.config :as conf]
            [citrus.core :as citrus]))


(t/deftest adding-person?-controller
  (t/testing ":init should return false"
    (t/is (= {:state false} (sut/adding-person? :init))))
  (t/testing ":adding-person/show should return true"
    (t/is (= {:state true} (sut/adding-person? :adding-person/show))))
  (t/testing ":adding-person/hide should return false"
    (t/is (= {:state false} (sut/adding-person? :adding-person/hide))))
  (t/testing ":person/add should return false"
    (t/is (= {:state false} (sut/adding-person? :person/add ["Piet"]))))
  (t/testing ":person/remove should do nothing"
    (t/is (= {:state false} (sut/adding-person? :person/remove ["Piet"] false))))
  )


(t/deftest show-adding-person?
  (let [state (reconciler/make-init)]
    (t/is (= false @(sut/show-adding-person? state)))))


;;;; Deposit Details

(t/deftest deposit-details-controller
  (t/testing ":init should return initial details"
    (t/is (= {:state sut/initial-deposit-details} (sut/deposit-details :init))))
  (t/testing ":deposit/set-field should set field"
    (t/is (= {:state (assoc sut/initial-deposit-details :amount 10)}
             (sut/deposit-details :deposit/set-field [:amount 10] sut/initial-deposit-details))))
  (t/testing ":deposit/clear clears only amount and note"
    (t/is (= {:state (assoc sut/initial-deposit-details
                            :year 2000
                            :month 1
                            :day 1)}
             (sut/deposit-details :deposit/clear nil (assoc sut/initial-deposit-details
                                                            :year 2000
                                                            :month 1
                                                            :day 1
                                                            :amount 10
                                                            :note ""))))))


(t/deftest deposit-form-detail
  (let [state (reconciler/make-init)]
    (t/is (= sut/initial-deposit-details @(sut/deposit-form-detail state)))))

(t/deftest can-deposit?
  (let [state (reconciler/make-init)]
    (t/testing "false amount 0"
      (t/is (false? @(sut/can-deposit? state))))
    (t/testing "true amount > 0"
      (citrus/dispatch-sync! state :deposit-details :deposit/set-field :amount 10)
      (t/is (true? @(sut/can-deposit? state))))
    (t/testing "false if date is before tfsa started"
      (let [state (reconciler/make-init)]
        (citrus/dispatch-sync! state :deposit-details :deposit/set-field :year conf/first-tfsa-year)
        (citrus/dispatch-sync! state :deposit-details :deposit/set-field :month 2)
        (citrus/dispatch-sync! state :deposit-details :deposit/set-field :day 28)
        (citrus/dispatch-sync! state :deposit-details :deposit/set-field :amount 10)
        (t/is (false? @(sut/can-deposit? state)))))))
