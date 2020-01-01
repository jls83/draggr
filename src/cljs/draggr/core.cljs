(ns draggr.core
  (:require
   [reagent.core :as reagent]
   [reagent.session :as session]
   [reitit.frontend :as reitit]
   [clerk.core :as clerk]
   [goog.events :as events]
   [accountant.core :as accountant])
  (:import [goog.events EventType]))


(enable-console-print!)

;; -------------------------
;; Routes

(def router
  (reitit/router
   [["/" :index]
    ["/drag" :drag]]))

(defn path-for [route & [params]]
  (if params
    (:path (reitit/match-by-name router route params))
    (:path (reitit/match-by-name router route))))

(path-for :about)
;; -------------------------
;; Page components

(defn home-page []
  (fn []
    [:span.main
     [:h1 "Welcome to draggr"]
     [:ul [:li [:a {:href (path-for :drag)} "Dragz"]]]]))

;; Drag items

;; Drag State
(def state-map {:foo  {:x 100 :y 100}
                :bar  {:x 150 :y 150}
                :baz  {:x 200 :y 200}
                :quux {:x 250 :y 250}})

(def drag-state (reagent/atom state-map))

(def drag-foo-cursor (reagent/cursor drag-state [:foo]))
(def drag-bar-cursor (reagent/cursor drag-state [:bar]))
(def drag-baz-cursor (reagent/cursor drag-state [:baz]))
(def drag-quux-cursor (reagent/cursor drag-state [:quux]))

(defn get-client-rect [e]
  (let [r (.getBoundingClientRect (.-target e))]
    {:left (.-left r)
     :top  (.-top r)}))

;; Event Handlers
(defn mouse-move-builder [offset cursor]
  (fn [e]
    (let [x (- (.-clientX e) (:x offset))
          y (- (.-clientY e) (:y offset))]
      (reset! cursor {:x x :y y}))))

(defn mouse-up-builder [on-move]
  (fn [e]
    (events/unlisten js/window EventType.MOUSEMOVE on-move)))

(defn mouse-down-builder [cursor]
  (fn [e]
    (.preventDefault e)
    (let [{:keys [left top]} (get-client-rect e)
          offset {:x (- (.-clientX e) left)
                  :y (- (.-clientY e) top)}
          on-move (mouse-move-builder offset cursor)]
      (events/listen js/window EventType.MOUSEMOVE
                     on-move)
      (events/listen js/window EventType.MOUSEUP
                     (mouse-up-builder on-move)))))

;; Draggable components
(defn draggable-button [button-text item-cursor]
  [:div
   [:button.btn.btn-default
    {:style {:position  "absolute"
             :left      (str (:x @item-cursor) "px")
             :top       (str (:y @item-cursor) "px")}
     :on-mouse-down (mouse-down-builder item-cursor)}
    button-text]])

(defn draggable-image [img-src img-alt-text item-cursor]
  [:img
   {:style {:position  "absolute"
            :left      (str (:x @item-cursor) "px")
            :top       (str (:y @item-cursor) "px")}
    :alt img-alt-text
    :src img-src
    :on-mouse-down (mouse-down-builder item-cursor)}])

(defn drag-page []
  (fn [] [:span.main
          [:h1 "Drag things"]
          [draggable-button "Drag This" drag-foo-cursor]
          [draggable-button "Drag Other" drag-bar-cursor]
          [draggable-image "images/joe.jpeg" "Joe" drag-baz-cursor]
          [draggable-image "images/joyce.jpeg" "Joyce" drag-quux-cursor]
          ]))

;; -------------------------
;; Translate routes -> page components

(defn page-for [route]
  (case route
    :index #'home-page
    :drag #'drag-page))

;; -------------------------
;; Page mounting component

(defn current-page []
  (fn []
    (let [page (:current-page (session/get :route))]
      [:div
       [:header
        [:p [:a {:href (path-for :index)} "Home"]]]
       [page]
       [:footer
        [:p "draggr was generated by the "
         [:a {:href "https://github.com/reagent-project/reagent-template"} "Reagent Template"] "."]]])))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (clerk/initialize!)
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (let [match (reitit/match-by-path router path)
            current-page (:name (:data  match))
            route-params (:path-params match)]
        (reagent/after-render clerk/after-render!)
        (session/put! :route {:current-page (page-for current-page)
                              :route-params route-params})
        (clerk/navigate-page! path)
        ))
    :path-exists?
    (fn [path]
      (boolean (reitit/match-by-path router path)))})
  (accountant/dispatch-current!)
  (mount-root))