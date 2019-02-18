(ns tfsa.reconciler
  (:require [citrus.core :as citrus]
            [tfsa.app-state :as app-state]
            [tfsa.domain :as domain]))


(defn save-state-effect-handler [_ _ deposits] (app-state/save-state deposits))

(defn save-file-effect-handler [r _ _] (app-state/save-file (:deposits @r)))

(defn hydrate-file-effect-hander [r _ content] (app-state/hydrate-file r content))

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
      :deposit-details app-state/deposit-details
      :file app-state/file
      :help app-state/help}
     :effect-handlers {:save-state save-state-effect-handler
                       :save-file save-file-effect-handler
                       :hydrate-file hydrate-file-effect-hander}})))


(defn init [r] (citrus/broadcast-sync! r :init))

(defn make-init []
  (let [r (make)]
    (init r)
    r))
