(ns tfsa.reconciler-tests
  (:require [tfsa.reconciler :as sut]
            #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true])
            [citrus.core :as citrus]))


(t/deftest person-add
  (let [state (sut/make-init)
        initial-state (assoc @state :adding-person? true)
        expected-state (-> initial-state
                           (update :people conj "Piet")
                           (assoc :person "Piet"
                                  :adding-person? false))]
    (citrus/broadcast-sync! state :person/add "Piet")
    (t/is (= expected-state @state))))
