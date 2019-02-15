(ns tfsa.domain
  (:require [citrus.core :as citrus]
            [cljs-time.core :as time]))


;;;; TAX YEAR

(defn calculate-tax-year
  [{:keys [year month day]}]
  (if (time/after? (time/date-time year month day) (time/last-day-of-the-month year 2))
    year
    (dec year)))

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
  (let [deposit (assoc deposit
                       :person person
                       :tax-year (calculate-tax-year deposit))]
    {:state (assoc state deposit-id deposit)}))

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


(defn lifetime-contributions [deposits]
  (->> deposits
       (map :amount)
       (reduce + 0)))
