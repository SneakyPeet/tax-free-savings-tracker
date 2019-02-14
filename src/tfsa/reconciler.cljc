(ns tfsa.reconciler
  (:require [citrus.core :as citrus]
            [tfsa.app-state :as app-state]
            [tfsa.domain :as domain]))

(defonce reconciler
  (citrus/reconciler
   {:state (atom {})
    :controllers
    {:person domain/person
     :people domain/people
     :adding-person? app-state/adding-person?}
    :effect-handlers {}}))
