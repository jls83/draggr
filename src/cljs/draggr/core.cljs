(ns draggr.core
  (:require
   [reagent.core :as reagent]
   [reagent.session :as session]
   [goog.events :as events])
  (:import [goog.events EventType]))

(enable-console-print!)

;; Drag items
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
(defn draggable-button [button-text start-coord]
  (let [item-cursor (reagent/atom start-coord)]
    (fn []
      [:div
       [:button.btn.btn-default
        {:style {:position  "absolute"
                 :left      (str (:x @item-cursor) "px")
                 :top       (str (:y @item-cursor) "px")}
         :on-mouse-down (mouse-down-builder item-cursor)}
        button-text]])))

(defn draggable-image [img-src img-alt-text start-coord]
  (let [item-cursor (reagent/atom start-coord)]
    (fn []
      [:img
       {:style {:position  "absolute"
                :left      (str (:x @item-cursor) "px")
                :top       (str (:y @item-cursor) "px")}
        :src img-src
        :alt img-alt-text
        :on-mouse-down (mouse-down-builder item-cursor)}])))

(defn drag-page []
  (fn [] [:span.main
          [:h1 "Drag things"]
          [draggable-button "Drag This" {:x 100 :y 100}]
          [draggable-button "Drag Other" {:x 150 :y 150}]
          [draggable-image "images/joe.jpeg" "Joe" {:x 200 :y 200}]
          [draggable-image "images/joyce.jpeg" "Joyce" {:x 250 :y 250}]
          ]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [drag-page] (.getElementById js/document "app")))

(defn init! [] (mount-root))
