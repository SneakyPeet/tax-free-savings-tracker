(ns tfsa.reconciler
  (:require [citrus.core :as citrus]
            [tfsa.app-state :as app-state]
            [tfsa.domain :as domain]))


(defn make []
  (citrus/reconciler
   {:state (atom {})
    :controllers
    {:person domain/person
     :people domain/people
     :adding-person? app-state/adding-person?
     :deposit-details app-state/deposit-details}
    :effect-handlers {}}))


(defn init [r] (citrus/broadcast-sync! r :init))


(defn make-init []
  (let [r (make)]
    (init r)
    r))
