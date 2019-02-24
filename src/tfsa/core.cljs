(ns tfsa.core
  (:require [rum.core :as rum]
            [citrus.core :as citrus]
            [tfsa.app-state :as app-state]
            [tfsa.reconciler :as reconciler]
            [tfsa.components :refer [App]]
            [devcards.core :as dc]))


(defn startup [element]
  (defonce r (reconciler/make (app-state/load-state)))
  (defonce init-ctrl (reconciler/init r))
  (rum/mount (App r) element))


(if-let [element (. js/document (getElementById "app"))]
  (startup element)
  (devcards.core/start-devcard-ui!))
