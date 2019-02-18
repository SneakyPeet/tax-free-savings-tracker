(ns tfsa.app-state
  (:require [citrus.core :as citrus]
            [cljs-time.core :as time]
            [tfsa.config :as conf]
            [clojure.string :as string]))


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

(defmethod adding-person? :person/remove [_ _ state]
  {:state state})


(defn show-adding-person? [r] (citrus/subscription r [:adding-person?]))

;;;; Deposit Details

(def initial-deposit-details
  (let [now (time/now)]
    {:year (time/year now)
     :month (time/month now)
     :day (time/day now)
     :amount 0
     :note ""}))


(defmulti deposit-details (fn [evt] evt))

(defmethod deposit-details :init []
  (let [now (time/now)]
    {:state {:year (time/year now)
             :month (time/month now)
             :day (time/day now)
             :amount 0
             :note ""}}))

(defmethod deposit-details :deposit/set-field
  [_ [f v] state]
  {:state (assoc state f v)})

(defmethod deposit-details :deposit/clear
  [_ _ state]
  {:state (merge
           initial-deposit-details
           (select-keys state [:year :month :day]))})

(defmethod deposit-details :person/add [_ _ state] {:state state})
(defmethod deposit-details :person/remove [_ _ state] {:state state})

(defn deposit-form-detail [r] (citrus/subscription r [:deposit-details]))


(defn can-deposit? [r]
  (citrus/subscription
   r [:deposit-details]
   (fn [{:keys [amount year month day]}]
     (and (> amount 0)
          (time/after?
           (time/date-time year month day)
           (time/last-day-of-the-month conf/first-tfsa-year 2))))))

;;;; storage

(defn deposits->saveable [deposits]
   (->> deposits
        vals
        (map (fn [{:keys [person amount day month year note]}]
               (->> [person year month day amount note]
                    (map str)
                    (string/join ","))))
        (string/join "\r\n")))


(def storage-key "tfsa-app-state")

(defn save-state [_ _ e]
  (when-some [storage js/localStorage]
    (.setItem storage
              storage-key
              (deposits->saveable e))))
