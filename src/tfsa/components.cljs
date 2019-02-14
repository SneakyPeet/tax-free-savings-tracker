(ns tfsa.components
  (:require [rum.core :as rum]
            [citrus.core :as citrus]
            [tfsa.app-state :as app-state]
            [tfsa.domain :as domain]
            [tfsa.config :as conf]
            [cljs-time.core :as time]))


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


;;;; DEPOSIT FORM

(def months (zipmap (range 1 13) ["Jan" "Feb" "Mrt" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]))

(rum/defcs DepositForm <
  {:will-mount (fn [state]
                 (let [now (time/now)
                       start-year conf/first-tfsa-year
                       end-year (time/year now)]
                   (assoc state
                          :years (range start-year (inc end-year))
                          :months months
                          :wrap-field (fn [c]
                                        [:div.control c]))))}
  [{:keys [years months wrap-field]}
   {:keys [year month day amount note] :as deposit-details}
   can-deposit? set-field deposit]
  (let [last-day-of-month (time/day (time/last-day-of-the-month year month))
        days (range 1 (inc last-day-of-month))]
    [:div.field.is-grouped.is-grouped-multiline.is-grouped-centered
     (wrap-field
      [:div.select
       [:select
        {:value year
         :on-change (fn [e]
                      (let [y (js/parseInt (.. e -target -value))]
                        (set-field :year y)
                        (when (< (time/day (time/last-day-of-the-month y month)) day)
                          (set-field :day 1))))}
        (->> years (map (fn [y] [:option y])))]])
     (wrap-field
      [:div.select
       [:select
        {:value month
         :on-change (fn [e]
                      (let [m (js/parseInt (.. e -target -value))]
                        (set-field :month m)
                        (when (< (time/day (time/last-day-of-the-month year m)) day)
                          (set-field :day 1))))}
        (->> months (map (fn [[i t]] [:option {:value i} t])))]])
     (wrap-field
      [:div.select
       [:select
        {:value day
         :on-change #(set-field :day (js/parseInt (.. % -target -value)))}
        (->> days (map (fn [d] [:option d])))]])
     (wrap-field
      [:input.input {:type "number"
                     :placeholder "Deposit Amount"
                     :value (str (if (zero? amount) "" amount))
                     :on-change #(set-field :amount (js/parseInt (max 0 (.. % -target -value) 0)))}])
     (wrap-field
      [:input.input {:type "text"
                     :placeholder "Note"
                     :value note
                     :on-change #(set-field :note (.. % -target -value))}])
     (wrap-field
      [:button.button.is-primary
       {:disabled (not can-deposit?)
        :on-click #(deposit deposit-details)}
       "Deposit"])]))

(defn DepositFormContainer [r]
  (DepositForm
   (rum/react (app-state/deposit-form-detail r))
   (rum/react (app-state/can-deposit? r))
   #(citrus/dispatch! r :deposit-details :deposit/set-field %1 %2)
   (fn [deposit]
     (citrus/dispatch! r :deposit-details :deposit/clear))))


;;;; LAYOUT

(rum/defc App < rum/reactive [r]
  [:div
   (PersonSelectorContainer r)
   (when (true? (rum/react (app-state/show-adding-person? r)))
     (AddPersonContainer r))
   (DepositFormContainer r)])
