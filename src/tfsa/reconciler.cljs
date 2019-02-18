(ns tfsa.reconciler
  (:require [citrus.core :as citrus]
            [tfsa.app-state :as app-state]
            [tfsa.domain :as domain]))


(defn save-state-effect-handler [_ _ deposits] (app-state/save-state deposits))

(defn make
  ([]
   (make {}))
  ([state]
   (citrus/reconciler
    {:state (atom state)
     :controllers
     {:person domain/person
      :people domain/people
      :deposits domain/deposits
      :adding-person? app-state/adding-person?
      :deposit-details app-state/deposit-details}
     :effect-handlers {:save-state save-state-effect-handler}})))


(defn init [r] (citrus/broadcast-sync! r :init))

(defn make-init []
  (let [r (make)]
    (init r)
    r))
