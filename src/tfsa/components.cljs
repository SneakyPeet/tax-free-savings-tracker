(ns tfsa.components
  (:require [rum.core :as rum]
            [citrus.core :as citrus]
            [tfsa.app-state :as app-state]
            [tfsa.domain :as domain]
            [tfsa.config :as conf]
            [cljs-time.core :as time]
            [goog.string :as gstring])
  (:require-macros
   [devcards.core :refer [defcard]]))


(defn currency [n] (str "R" (.toLocaleString n)))

(defn bold-currency [n] [:strong.has-text-white (currency n)])

;;;; PERSON SELECTION

(rum/defc PersonSelector < rum/static
  [people selected-person select-person add-person]
  [:nav.tabs.is-boxed.is-centered.is-fullwidth
   [:div.container
    [:ul
     (map (fn [person]
            [:li {:key person
                  :class (when (= person selected-person) "is-active")}
             [:a
              {:on-click #(select-person person)}
              person]])
          people)
     [:li
      [:a
       {:on-click #(add-person)}
       [:span.icon.is-small
        [:i.fas.fa-plus {:aria-hidden true}]]
       ]]]]])


(defn PersonSelectorContainer [r]
  (PersonSelector
   (rum/react (domain/all-people r))
   (rum/react (domain/selected-person r))
   #(citrus/dispatch! r :person :person/change %)
   #(citrus/dispatch! r :adding-person? :adding-person/show)))


;;;; ADD PERSON

(rum/defcs AddPerson < rum/static (rum/local "" ::person)
  {:did-mount (fn [s]
                (.focus (js/document.getElementById "add-person"))
                s)}
  [{*person ::person} f close]
  [:div.modal.is-active
   [:div.modal-background]
   [:div.modal-content
    [:form {:on-submit (fn [e]
                      (.preventDefault e)
                      (f @*person)
                      (reset! *person ""))}
     [:div.field.has-addons.has-addons-centered
      [:div.control
       [:input.input {:id "add-person"
                      :type "text" :on-change #(reset! *person (.. % -target -value))
                      :placeholder "Name" :value @*person}]]
      [:div.control
       [:button.button.is-primary
        {:type "submit"}
        "Add Person"]]]]]
   [:button.modal-close.has-text-dark {:aria-label "close" :on-click close}]])


(defn AddPersonContainer [r]
  (AddPerson
   #(citrus/broadcast! r :person/add %)
   #(citrus/dispatch! r :adding-person? :adding-person/hide)))


(rum/defc RemovePerson < rum/static
  [selected-person f]
 [:div.has-text-centered
  [:button.button.is-danger.is-small.is-outlined
   {:on-click #(let [delete? (js/confirm (str "Are you sure you want to delete " selected-person))]
                 (when delete? (f)))}
   [:span "Delete This Person"]
   [:span.icon.is-small
    [:i.fas.fa-times]]]])

(defn RemovePersonContainer [r]
  (let [selected-person (rum/react (domain/selected-person r))
        people (rum/react (domain/all-people r))
        next-person (-> people
                        (disj selected-person)
                        first)]
    (RemovePerson
     selected-person
     (fn []
       (citrus/dispatch! r :person :person/change next-person)
       (citrus/broadcast! r :person/remove selected-person)))))


;;;; DEPOSIT FORM

(def months (zipmap (range 1 13) ["Jan" "Feb" "Mrt" "Apr" "May" "Jun" "Jul" "Aug" "Sep" "Oct" "Nov" "Dec"]))

(rum/defcs DepositForm < rum/static
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
    [:div {:style {:padding-top "20px" :padding-bottom "20px"}}
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
        "Deposit"])]]))

(defn DepositFormContainer [r]
  (DepositForm
   (rum/react (app-state/deposit-form-detail r))
   (rum/react (app-state/can-deposit? r))
   #(citrus/dispatch! r :deposit-details :deposit/set-field %1 %2)
   (fn [deposit]
     (citrus/dispatch! r :deposit-details :deposit/clear)
     (citrus/dispatch! r :deposits :deposit/add (random-uuid) @(domain/selected-person r) deposit))))


;;;; INFO

(defn date-string [year month day]
  (str day " " (get months month) " " year))

(rum/defc TaxYearTable < rum/static
  [deposits-by-tax-year current-tax-year]
  [:table.table.is-striped.is-narrow.is-hoverable.is-fullwidth
   [:thead
    [:tr [:th.has-text-centered.is-size-3 {:col-span 4} "Contributions By Tax Year"]]
    [:tr [:th "Tax Year"] [:th "Allowed"] [:th "Actual"] [:th "Remaining"]]]
   [:tbody
    (->> deposits-by-tax-year
         (sort-by first)
         reverse
         (map
          (fn [[year deposits]]
            (let [current? (= year current-tax-year)
                  limit (get conf/tax-year-limits year)
                  amount (->> deposits
                              (map :amount)
                              (reduce + 0))
                  remainder (- limit amount)
                  under-limit? (>= remainder 0)
                  contributed-all? (zero? remainder)]
              [:tr {:key year
                    :class (cond (not under-limit?) "has-background-danger has-text-white"
                                 contributed-all? "has-background-primary has-text-white"
                                 current? "has-background-info has-text-white")}
               [:td year]
               [:td (currency limit)]
               [:td (currency amount)]
               (cond
                 (not under-limit?)
                 [:td "You went over the allowed limit by " (bold-currency (- remainder)) " and will get taxed on that amount"]
                 contributed-all?
                 [:td "Well done you have maxed out your tax free for " year]
                 current?
                 [:td "You can still contribute " (bold-currency remainder) " for this year"]
                 :else
                 [:td ""])]))))]])

(rum/defc DepositTable < rum/static
  [deposits f]
  [:table.table.is-striped.is-narrow.is-hoverable.is-fullwidth
   [:thead
    [:tr [:th.has-text-centered.is-size-3 {:col-span 5} "All Contributions"]]
    [:tr [:th "Date"] [:th "Tax Year"] [:th "Amount"] [:th {:col-span 2}"Note"]]]
   [:tbody
    (->> deposits
         (sort-by :timestamp)
         reverse
         (map-indexed
          (fn [i {:keys [timestamp tax-year amount note year month day deposit-id]}]
            [:tr {:key i}
             [:td (date-string year month day)]
             [:td tax-year]
             [:td (currency amount)]
             [:td note]
             [:td.has-text-right
              [:button.button.is-danger.is-small.is-outlined
               {:on-click
                #(let [delete? (js/confirm (str "Are you sure you want to delete this?"))]
                   (when delete? (f deposit-id)))}
               [:span.icon.is-small
                [:i.fas.fa-times]]]]])))]])


(defn DepositTableContainer [r deposits]
  (DepositTable
   deposits
   #(citrus/dispatch! r :deposits :deposit/remove %)))


(rum/defc AllowedContributions < rum/static
  [deposits deposits-by-tax-year current-tax-year]
  (let [lifetime (domain/lifetime-contributions deposits)
        {:keys [ends-in-days end-date]} (domain/current-tax-year-end-details)
        this-year (->> (get deposits-by-tax-year current-tax-year)
                       (map :amount)
                       (reduce + 0))
        year-limit (get conf/tax-year-limits current-tax-year)
        remainder (max 0 (- year-limit this-year))]
    (let [item (fn [t v s & [text-style]]
                 [:div.level-item.has-text-centered {:class text-style}
                  [:div
                   [:p.heading t]
                   [:p.title {:class text-style} v]
                   [:p.subtitle {:style {:margin-top "0"} :class text-style} s]]])]
      [:nav.level
       (item "You Can Still Contribute" (currency remainder) (str " for " current-tax-year) "has-text-info")
       (item "Tax Year Ends In" (str ends-in-days " Days")
             (str "on " (date-string
                           (time/year end-date)
                           (time/month end-date)
                           (time/day end-date))))
       (item "This Year You Saved" (currency this-year) (str "of " (currency year-limit)))
       (item "Your Lifetime Savings" (currency lifetime) (str  "of " (currency conf/lifetime-limit)))

       ])))

;;;; LAYOUT

(rum/defc welcome-message < rum/static
  [people selected-person deposits]
  (let [new-user? (and (= 1 (count people)) (empty? deposits))
        new-person? (empty? deposits)
        single-deposit? (= 1 (count deposits))]
    (cond
      new-user?
      [:div.notification.is-primary.has-text-centered
       [:h1.title "Welcome To TFSA Tracker"]
       [:h2.subtitle "To get started, add YOUR tax free savings account contributions"]]
      new-person?
      [:div.notification.is-primary.has-text-centered
       [:h2.subtitle "Add some contributions to the the tax free savings that are in " selected-person "'s name"]]
      single-deposit?
      [:div.notification.is-info.has-text-centered
       [:h1.title "Good Job"]
       [:h2.subtitle "Remember to add all the deposits you have made to all your tax free saving accounts. You can add contributions for your family members by clicking the "  [:i.fas.fa-plus] ". Click the " [:i.fas.fa-question-circle] " for more info."]]
      :else nil)))

(defcard "# Welcome Message
The welcome message should changed based on the amount of people and deposits")

(defcard "Welcome a new person"
  (welcome-message #{"Jannie"} "Jannie" {}))

(defcard "Guide a new person after making a deposit"
  (welcome-message #{"Jannie"} "Jannie" {1 {}}))

(defcard "Hidden after multiple deposits"
  (welcome-message #{"Jannie"} "Jannie" {1 {} 2 {}}))

(defcard "Guide a second person"
  (welcome-message #{"Piet" "Jan"} "Piet" {}))


(defn read-file [r e]
  (let [file (aget (.. e -target -files) 0)
        reader (js/FileReader.)]
    (set! (.-onload reader)
          (fn [e]
            (citrus/dispatch! r :file :load (.. e -target -result))
            (.reset (.getElementById js/document "upload-form"))))
    (.readAsText reader file)))


(rum/defc Menu
  [r]
  [:nav.navbar
   [:div.container
    [:div.navbar-end.has-text-right
     [:a.navbar-item {:on-click #(citrus/dispatch! r :file :save)
                      :style {:display "inline-block"}}
      [:span.icon [:i.fas.fa-save]]]
     [:a.navbar-item {:style {:display "inline-block"}}
      [:form {:id "upload-form"}
       [:input.file-input {:type "file" :style {:cursor "pointer"} :on-change #(read-file r %)}]]
      [:span.icon [:i.fas.fa-upload]]]
     [:a.navbar-item {:on-click #(citrus/dispatch! r :help :show) :style {:display "inline-block"}}
      [:span.icon [:i.fas.fa-question-circle]]]
     [:a.navbar-item {:style {:display "inline-block"}
                      :target "_blank" :href "https://github.com/SneakyPeet/tax-free-savings-tracker"}
      [:span.icon [:i.fab.fa-github]]]]]])


(rum/defc Help < rum/reactive
  [r]
  (if (rum/react (app-state/show-help? r))
    [:div.modal.is-active
     [:div.modal-background]
     [:div.modal-card
      [:header.modal-card-head
       [:p.modal-card-title "Welcome to TFSA Tracker"]
       [:button.delete {:aria-label "close" :on-click #(citrus/dispatch! r :help :hide)}]]
      [:section.modal-card-body
       [:p "TFSA Tracker exists to help track contributions to your South African Tax Free Savings Accounts, so you can maximize your savings while staying within the TFSA contribution limits."]
       [:br]
       [:p "TFSA Tracker " [:strong "DOES NOT"] " store any of your data, nor does it send it over the internet. Your data is only stored locally in the browser you are using. You can always clear the data kept in your browser by clicking the " [:em "'Clear My Data'"] " button below."]
       [:br]
       [:p "It is a good idea to make regular backups of your data using the "  [:i.fas.fa-save] " button. You can always reload a backup by using the " [:i.fas.fa-upload] " button."]
       [:br]
       [:p "TFSA Tracker is in essence just a fancy calculator. Thus, the creator of TFSA calculator cannot be held responsible for any losses due to incorrect data entry."]
       [:br]
       [:p "TFSA Tracker is built and maintained by " [:a {:href "https://sneakycode.net"} "Pieter Koornhof"] ". Source code is available on " [:a {:href "https://github.com/SneakyPeet/tax-free-savings-tracker"} "github"] "."]
       ]
      [:footer.modal-card-foot
       [:button.button.is-danger
        {:on-click #(let [clear? (js/confirm "Clear all data permanently")]
                      (when clear?
                        (app-state/reset-state r)))}
        "Clear My Data"]]]]
    [:span]))




(rum/defc App < rum/reactive [r]
  (let [selected-person (rum/react (domain/selected-person r))
        people (rum/react (domain/all-people r))
        deposits (rum/react (domain/deposits-for-person r selected-person))
        deposits-by-tax-year (group-by :tax-year deposits)
        now (time/now)
        current-tax-year (domain/calculate-tax-year {:year (time/year now) :month (time/month now) :day (time/day now)})
        deposits? (> (count deposits) 0)]
    [:div
     [:section.hero.is-primary
      [:div.hero-head
       (Menu r)]
      [:div.hero-body
       [:div.container
        [:h1.title "Tax Free Savings Tracker"]]]
      [:div.hero-foot
       (PersonSelectorContainer r)]]
     (when (true? (rum/react (app-state/show-adding-person? r)))
       (AddPersonContainer r))
     [:div.section
      [:div.container
       (welcome-message people selected-person deposits)
       (when deposits? (AllowedContributions deposits deposits-by-tax-year current-tax-year))
       (DepositFormContainer r)
       (when deposits? (TaxYearTable deposits-by-tax-year current-tax-year))
       (when deposits? (DepositTableContainer r deposits))
       (when (> (count people) 1) (RemovePersonContainer r))
       (Help r)]]
     ]))
