(ns tfsa.core
  (:require [rum.core :as rum]
            [citrus.core :as citrus]
            [tfsa.reconciler :refer [reconciler]]
            [tfsa.components :refer [App]]))


(defonce init-ctrl (citrus/broadcast-sync! reconciler :init))

(rum/mount (App reconciler) (. js/document (getElementById "app")))
