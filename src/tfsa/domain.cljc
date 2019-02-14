(ns tfsa.domain
  (:require [citrus.core :as citrus]))

;;;; PERSON

(def initial-person "You")

(defmulti person (fn [event] event))

(defmethod person :init []
  {:state initial-person})

(defmethod person :person/change [_ [person]]
  {:state person})

(defmethod person :person/add [_ [person]]
  {:state person})


;;;; PEOPLE

(def initial-people #{initial-person "test"})

(defmulti people (fn [event] event))

(defmethod people :init []
  {:state initial-people})

(defmethod people :person/add [_ [person] state]
  {:state (conj state person)})


;;;; SUBSCRIPTIONS

(defn selected-person [r] (citrus/subscription r [:person]))

(defn all-people [r] (citrus/subscription r [:people]))
