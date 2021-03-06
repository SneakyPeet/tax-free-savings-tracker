(ns tfsa.app-state
  (:require [citrus.core :as citrus]
            [cljs-time.core :as time]
            [cljs-time.format :as time.format]
            [tfsa.config :as conf]
            [clojure.string :as string]
            [tfsa.domain :as domain]
            [cljsjs.filesaverjs]))


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
   (fn [{:keys [amount year month day] :as deposit}]
     (let [tax-year (domain/calculate-tax-year deposit)
           current-tax-year (:year (domain/current-tax-year-end-details))]
       (and (> amount 0)
            (time/after?
             (time/date-time year month day)
             (time/last-day-of-the-month conf/first-tfsa-year 2))
            (<= tax-year current-tax-year))))))


;;;; file

(defmulti file (fn [evt] evt))

(defmethod file :default [_] {:state nil})

(defmethod file :save [_]
  {:state nil
   :save-file nil})

(defmethod file :load [_ [content]]
  {:state nil
   :hydrate-file content})


;;;; help
(defmulti help (fn [evt] evt))

(defmethod help :default [_ _ state] {:state state})

(defmethod help :init [_]
  {:state {:show? false
           :options {:store-local? true}}})

(defmethod help :show [_ _ state]
  {:state (assoc state :show? true)})

(defmethod help :hide [_ _ state]
  {:state (assoc state :show? false)})


(defn show-help? [r] (citrus/subscription r [:help :show?]))

;;;; SIDE EFFECTS
;;;; storage

(defn reset-state
  ([r] (reset-state r {:person domain/initial-person
                       :people domain/initial-people
                       :deposits domain/initial-deposits}))
  ([r state]
   (let [{:keys [person people deposits]} state]
     (citrus/dispatch! r :deposits :init deposits)
     (citrus/dispatch! r :people :init people)
     (citrus/dispatch! r :person :init person))))

(defn deposits->saveable [deposits]
   (->> deposits
        vals
        (map (fn [{:keys [person amount day month year note]}]
               (->> [person year month day amount note]
                    (map str)
                    (string/join ","))))
        (string/join "\r\n")))


(defn saveable->deposits [s]
  (->> (string/split-lines s)
       (map #(string/split % ","))
       (map (fn [[person year month day amount note]]
              (domain/calculate-deposit-data
               {:person person
                :year (js/parseInt year)
                :month (js/parseInt month)
                :day (js/parseInt day)
                :amount (js/parseFloat amount)
                :note (or note "")
                :deposit-id (random-uuid)})))))

(defn saveable->state [s]
  (let [deposits (saveable->deposits s)
        people (set (map :person deposits))
        person (first people)]
    {:person person
     :people people
     :deposits (->> (map (juxt :deposit-id identity) deposits)
                    (into {}))}))


(def storage-key "tfsa-app-state")

(defn save-state [deposits]
  (when-some [storage js/localStorage]
    (.setItem storage
              storage-key
              (deposits->saveable deposits))))


(defn load-state []
  (if-some [storage js/localStorage]
    (let [item (or (.getItem storage storage-key) "")]
      (if-not (string/blank?  item)
        (saveable->state item)
        {}))
    {}))


;; file storage

(defn save-file [deposits]
  (let [file-name (str "TFSA-Tracker-" (time.format/unparse (time.format/formatter "yyyyMMdd") (time/now)) ".csv")
        content (deposits->saveable deposits)
        name (clj->js [content])
        type (clj->js {:type "text/plain;charset=utf-8"})
        blob (js/Blob. name type)]
    (js/saveAs blob file-name)))


(defn hydrate-file [r content]
  (reset-state r (saveable->state content)))
