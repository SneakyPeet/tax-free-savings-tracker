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

(defmethod person :person/remove [_ [person] state]
  {:state (when (not= person state) state)})

(defn selected-person [r] (citrus/subscription r [:person]))


;;;; PEOPLE

(def initial-people #{initial-person})

(defmulti people (fn [event] event))

(defmethod people :init []
  {:state initial-people})

(defmethod people :person/add [_ [person] state]
  {:state (conj state person)})

(defmethod people :person/remove [_ [person] state]
  {:state (disj state person)})


(defn all-people [r] (citrus/subscription r [:people]))


;;;; DEPOSIT

(def initial-deposits {})

(defmulti deposits (fn [evt] evt))

(defmethod deposits :init []
  {:state initial-deposits})

(defmethod deposits :deposit/add
  [_ [deposit-id person {:keys [year month day] :as deposit}] state]
  (let [deposit (assoc deposit
                       :deposit-id deposit-id
                       :person person
                       :tax-year (calculate-tax-year deposit)
                       :timestamp (.getTime (time/date-time year month day)))]
    {:state (assoc state deposit-id deposit)}))

(defmethod deposits :deposit/remove
  [_ [deposit-id] state]
  {:state (dissoc state deposit-id)})

(defmethod deposits :person/add
  [_ [person] state]
  {:state state})

(defmethod deposits :person/remove
  [_ [person] state]
  {:state (->> state
               (remove (fn [[k v]] (= (:person v) person)))
               (into {}))})


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

(defn current-tax-year-end-details []
  (let [now (time/now)
        year (time/year now)
        month (time/month now)
        year (if (> month 2) (inc year) year)
        last-day (time/last-day-of-the-month year month)
        interval (time/interval now last-day)]
    {:ends-in-days (time/in-days interval)
     :end-date last-day}))
