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

(defn selected-person [r] (citrus/subscription r [:person]))


;;;; PEOPLE

(def initial-people #{initial-person "test"})

(defmulti people (fn [event] event))

(defmethod people :init []
  {:state initial-people})

(defmethod people :person/add [_ [person] state]
  {:state (conj state person)})


(defn all-people [r] (citrus/subscription r [:people]))


;;;; DEPOSIT

(def initial-deposits {})

(defmulti deposits (fn [evt] evt))

(defmethod deposits :init []
  {:state initial-deposits})

(defmethod deposits :deposit/add
  [_ [deposit-id person deposit] state]
  {:state (assoc state deposit-id (assoc deposit :person person))})

(defmethod deposits :person/add
  [_ [person] state]
  {:state state})


(defn deposits-for-person [r person]
  (citrus/subscription
   r [:deposits]
   (fn [deposits]
     (let [people (->> deposits
                       vals
                       (group-by :person))]
       (get people person [])))))
