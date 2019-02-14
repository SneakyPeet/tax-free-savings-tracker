(ns tfsa.core
  (:require [rum.core :as rum]
            [citrus.core :as citrus]
            [tfsa.reconciler :as reconciler]
            [tfsa.components :refer [App]]))

(defonce r (reconciler/make))
(defonce init-ctrl (reconciler/init r))
(rum/mount (App r) (. js/document (getElementById "app")))
