(ns tfsa.app-state
  (:require [citrus.core :as citrus]))


;;;; ADDING PERSON

(defmulti adding-person? (fn [event] event))

(defmethod adding-person? :init []
  {:state false})

(defmethod adding-person? :adding-person/show []
  {:state true})

(defmethod adding-person? :adding-person/hide []
  {:state false})

(defmethod adding-person? :person/add []
  {:state false})


;;;; SUBSCRIPTIONS

(defn show-adding-person? [r] (citrus/subscription r [:adding-person?]))
