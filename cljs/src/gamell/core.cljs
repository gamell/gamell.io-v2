(ns gamell.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]
            [ajax.core :refer [GET]]))

(def content-url "https://s3.amazonaws.com/gamell-io/data.json")

  ;; -- Domino 1 - Event Dispatch -----------------------------------------------

  ; (defn dispatch-timer-event
  ;   []
  ;   (let [now (js/Date.)]
  ;     (rf/dispatch [:timer now])))  ;; <-- dispatch used

  ;; Call the dispatching function every second.
  ;; `defonce` is like `def` but it ensures only one instance is ever
  ;; created in the face of figwheel hot-reloading of this file.
  ; (defonce do-timer (js/setInterval dispatch-timer-event 1000))

;  (def initial-content
;    {:pictures    ["first photo" "second photo"]
;     :repos     ["repo1" "repo2"]
;     :articles  ["article1" "article2"]})

  ;; -- Domino 2 - Event Handlers -----------------------------------------------

(rf/reg-event-db             ;; sets up initial application state
 :initialize                 ;; usage:  (dispatch [:initialize])
 (fn [_ _]                   ;; the two parameters are not important here, so use _
   {:content {}
    :markdowns {}}))           ;; What it returns becomes the new application state    
                              ;; so the application state will initially be a map with two keys



  ; (rf/reg-event-db                ;; usage:  (dispatch [:time-color-change 34562])
  ;   :time-color-change            ;; dispatched when the user enters a new colour into the UI text field
  ;   (fn [db [_ new-color-value]]  ;; -db event handlers given 2 parameters:  current application state and event (a vector)
  ;     (assoc db :time-color new-color-value)))   ;; compute and return the new application state


(defn load-content
  []
  (GET content-url
    {:response-format :json
     :keywords? true
     :handler (fn [content]
                (js/console.log "*** CONTENT LOADED ***")
                (rf/dispatch [:content-loaded content])
                (rf/dispatch [:markdowns-loaded (:markdowns content)]))}))

(rf/reg-event-db                  ;; usage:  (dispatch [:time-color-change 34562])
 :content-loaded
 (fn [db [_ new-content]]
   (assoc db :content (:data new-content))))

(rf/reg-event-db                  ;; usage:  (dispatch [:time-color-change 34562])
 :markdowns-loaded
 (fn [db [_ new-markdowns]]
   (assoc db :markdowns new-markdowns)))

  ; (rf/reg-event-db                 ;; usage:  (dispatch [:timer a-js-Date])
  ;   :timer                         ;; every second an event of this kind will be dispatched
  ;   (fn [db [_ new-time]]          ;; note how the 2nd parameter is destructured to obtain the data value
  ;     (assoc db :time new-time)))  ;; compute and return the new application state


  ;; -- Domino 4 - Query  -------------------------------------------------------

  ; (rf/reg-sub
  ;   :time
  ;   (fn [db _]     ;; db is current app state. 2nd unused param is query vector
  ;     (:time db))) ;; return a query computation over the application state
  ;
  ; (rf/reg-sub
  ;   :time-color
  ;   (fn [db _]
  ;     (:time-color db)))

(rf/reg-sub
 :content
 (fn [db _]
   (:content db)))

(rf/reg-sub
 :markdowns
 (fn [db _]
   (:markdowns db)))

  ;; -- Domino 5 - View Functions ----------------------------------------------

  ; (defn clock
  ;   []
  ;   [:div.example-clock
  ;    {:style {:color @(rf/subscribe [:time-color])}}
  ;    (-> @(rf/subscribe [:time])
  ;        .toTimeString
  ;        (str/split " ")
  ;        first)])

  ; (defn color-input
  ;   []
  ;   [:div.color-input
  ;    "Time color: "
  ;    [:input {:type "text"
  ;             :value @(rf/subscribe [:time-color])
  ;             :on-change #(rf/dispatch [:time-color-change (-> % .-target .-value)])}]])  ;; <---

  ; (def card-types {
  ;   :pictures {
  ;             :title :name
  ;             :body :imageUrl}
  ;   :repos {
  ;           :title :repo
  ;           :body :description}
  ;   :articles {
  ;             :title :title
  ;             :body :content}})


(defn picture-card
  [card-info]
  [:a {:href (:url card-info)}
   [:img {:src (:imageUrl card-info) :title (:caption card-info)}]])

(defn repo-card
  [card-info]
  [:div {:class "wrapper"}
   [:h4 [:a {:href (:link card-info)} (:name card-info)]]
   [:p {:class "description"} (:description card-info)]
   [:ul {:class "meta"}
    [:li {:class "stars"} (:stars card-info)]
    [:li {:class "forks"} (:forks card-info)]]])

(defn article-card
  [card-info]
  (def meta-info (:hybridGraph (:embedInfo card-info)))
  [:div {:class "wrapper"}
   [:div {:class "top"}
    [:img {:src (:image meta-info) :class "thumbnail"}]
    [:div {:class "article-text"}
     [:h4 [:a {:href (:link card-info)} (:title meta-info)]]
     [:div {:class "description"} (:description meta-info)]]]
   [:div {:class "meta"}
    "Published on"
    [:img {:class "favicon" :src (:favicon meta-info)}]
    "on"
    [:span {:class "date"} (:date card-info)]]])

(def type-map {:pictures {:card picture-card :class "photography" :title "Recent Pictures"}
               :repos {:card repo-card :class "projects" :title "Favorite Projects"}
               :articles {:card article-card :class "articles" :title "Recent Writings"}})

(defn get-class
  [type]
  (:class (get type-map type)))

(defn get-card
  [type]
  (:card (get type-map type)))

(defn card
  [type card-info id]
  ^{:key (str "update-card-" (name type) "-" id)}
  [:li.card ((get-card type) card-info)])

(defn contact-section
  [data id]
  [:section
   [:a {:name "contact"}]
   [:h3 "Contact information"]])

(def articles-footer
  [:div {:class "footer"}
   "You will find more of my writing in "
   [:a {:href "https://substack.graymatters.com"}
    "Gray Matters 🧠"]
   "and my "
   [:a {:href "https://medium.com/@gamell"}
    "Medium"]
   "."])

(defn content-section
  [type data id]
  ^{:key (str "update-section-" (name type) "-" id)}
  (if (= type :personalInformation)
    (contact-section data id)
    (let [class (get-class type)
          title (:title (get type-map type))]
      [:section {:class class}
       [:a {:name class}]
       [:h3 title]
       [:ul.section {:class class}
        (map #(card type %1 %2) data (iterate inc 0))]])))

;    (if (= type :articles)
; [res (articles-footer)]
; res)

(defn sections
  []
  [:div.sections
   (let [content @(rf/subscribe [:content])]
     (map
      (fn [[type data] id] (content-section type data id))
      content
      (iterate inc 0)))])

(defn intro
  []
  [:div.intro
   (let [markdowns @(rf/subscribe [:markdowns])]
    ; (m/md->hiccup (:intro markdowns))
     {:dangerouslySetInnerHTML {:__html (:intro markdowns)}})])

(defn app
  []
  [:div#app
   [intro]
   [sections]])


  ;; -- Entry Point -------------------------------------------------------------


(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])       ;; puts a value into application state
  (load-content)
  (reagent/render
   [app]                   ;; mount the application's ui into '<div id="content" />'
   (js/document.getElementById "mount-point")))