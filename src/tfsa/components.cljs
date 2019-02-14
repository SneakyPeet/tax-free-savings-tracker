(ns tfsa.components
  (:require [rum.core :as rum]
            [citrus.core :as citrus]
            [tfsa.app-state :as app-state]
            [tfsa.domain :as domain]
            [tfsa.config :as conf]
            [cljs-time.core :as time]))


;;;; DATE PICKER

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


;;;; PERSON SELECTION

(rum/defc PersonSelector < rum/static
  [people selected-person select-person add-person]
  [:div.buttons.is-centered
   (let [people-buttons
         (vec
          (map (fn [person]
                 [:button.button
                  {:key person
                   :class (when (= person selected-person) "is-primary")
                   :on-click #(select-person person)}
                  person])
               people))]
     (into people-buttons
           [[:a.button
             {:on-click #(add-person)}
             [:span.icon.is-small
              [:i.fas.fa-plus]]]]))])


(defn PersonSelectorContainer [r]
  (PersonSelector
   (rum/react (domain/all-people r))
   (rum/react (domain/selected-person r))
   #(citrus/dispatch! r :person :person/change %)
   #(citrus/dispatch! r :adding-person? :adding-person/show)))


;;;; ADD PERSON

(rum/defcs AddPerson < (rum/local "" ::person)
  [{*person ::person} f]
  [:div.field.has-addons.has-addons-centered
   [:div.control
    [:input.input {:type "text" :on-change #(reset! *person (.. % -target -value)) :value @*person}]]
   [:div.control
    [:button.button.is-primary
     {:on-click (fn []
                  (f @*person)
                  (reset! *person ""))}
     "Add Person"]]])


(defn AddPersonContainer [r]
  (AddPerson
   #(citrus/broadcast! r :person/add %)))


;;;; ADD CONTRIBUTION

(rum/defcs ContributionForm
  < (rum/local (initial-date) ::date) (rum/local "" ::amount) (rum/local "" ::note)
  [{*date ::date *amount ::amount *note ::note} deposit]
  [:div.field.is-grouped.is-grouped-centered.is-grouped-multiline
   (DateCapture conf/first-tfsa-year (time/year (time/now)) #(reset! *date %) @*date)
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


;;;; LAYOUT

(rum/defc App < rum/reactive [r]
  [:div
   (PersonSelectorContainer r)
   (when (true? (rum/react (app-state/show-adding-person? r)))
     (AddPersonContainer r))
   (ContributionForm prn)])
