(ns tfsa.core
  (:require [rum.core :as rum]
            [citrus.core :as citrus]
            [cljs-time.core :as time]))

(def first-tfsa-year 2016)


(defn initial-date
  ([] (let [selected (time/now)]
        {:year (time/year selected)
         :month (time/month selected)
         :day (time/day selected)}))
  ([year] (initial-date year 1 1))
  ([year month] (initial-date year month 1))
  ([year month day]
   (let [selected (time/date-time year month day)]
     {:year (time/year selected)
      :month (time/month selected)
      :day (time/day selected)})))


(rum/defcs DateCapture
  "Params
     start-year :int
     end-year: int
     label: string
     change: fn taking a map as param. example {:year 2019 :month 2 :day 9}"
  < (rum/local nil ::year) (rum/local nil ::month) (rum/local nil ::day)
  {:will-mount (fn [{*day ::day *month ::month *year ::year :as state}]
                 (let [now (time/now)
                       year (time/year now)
                       [start-year end-year change init-opts] (:rum/args state)]
                   (reset! *year (get init-opts :year year))
                   (reset! *month (get init-opts :month (time/month now)))
                   (reset! *day (get init-opts :day (time/day now)))
                   (assoc state
                          ::years (range start-year (inc end-year))
                          ::months (range 1 13)
                          ::notify-change (fn [] (change {:year @*year :month @*month :day @*day})))))
   :did-update (fn [{notify-change ::notify-change :as state}]
                 (notify-change)
                 state)}
  [{*day ::day *month ::month *year ::year :as state} start-year end-year change]
  (let [last-day-of-month (time/day (time/last-day-of-the-month @*year @*month))
        days (range 1 (inc last-day-of-month))
        on-change (fn [*k]
                    (fn [e]
                      (reset! *k (js/parseInt (.. e -target -value)))
                      (let [new-last-day-of-month (time/day (time/last-day-of-the-month @*year @*month))]
                        (when (< new-last-day-of-month @*day)
                          (reset! *day 1)))))
        field (fn [range-coll *k]
                [:div.control {:key (str range-coll)}
                 [:div.select
                  [:select {:value @*k :on-change (on-change *k)}
                   (map-indexed (fn [i x] [:option {:key i} x]) range-coll)]]])]
    [(field (::years state) *year)
     (field (::months state) *month)
     (field days *day)]
    ))


(rum/defcs ContributionForm
  < (rum/local (initial-date) ::date) (rum/local "" ::amount) (rum/local "" ::note)
  [{*date ::date *amount ::amount *note ::note} deposit]
  [:div.field.is-grouped.is-grouped-centered.is-grouped-multiline
   (DateCapture first-tfsa-year (time/year (time/now)) #(reset! *date %) @*date)
   [:div.control
    [:input.input {:type "number" :placeholder "Deposit Amount" :value @*amount
                   :on-change #(reset! *amount (max 0 (js/parseFloat (.. % -target -value))))}]]
   [:div.control
    [:input.input {:type "text" :placeholder "Note" :value @*note
                   :on-change #(reset! *note (.. % -target -value))}]]
   [:div.control
    [:a.button.is-primary
     {:on-click #(deposit (assoc @*date
                                 :amount @*amount
                                 :note @*note))
      :disabled (not (number? @*amount))}
     "Deposit"]]
   ])


(rum/defc App < rum/reactive [r]
  [:div (ContributionForm prn)])


(defonce reconciler
  (citrus/reconciler
   {:state (atom {})
    :controllers {}
    :effect-handlers {}}))


(defonce init-ctrl (citrus/broadcast-sync! reconciler :init))

(rum/mount (App reconciler) (. js/document (getElementById "app")))
