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

(defn get-offset [e]
  (let [client-rect (get-client-rect e)]
    {:x (- (.-clientX e) (:left client-rect))
     :y (- (.-clientY e) (:top client-rect))}))

;; Event Handler Builders
(defn mouse-move-builder [offset cursor]
  (fn [e]
    (let [x (- (.-clientX e) (:x offset))
          y (- (.-clientY e) (:y offset))
          new-cursor {:x x :y y}]
      (reset! cursor new-cursor))))

(defn mouse-up-builder [on-mouse-move]
  (fn [e]
    (events/unlisten js/window EventType.MOUSEMOVE on-mouse-move)))

(defn mouse-down-builder [cursor]
  (fn [e]
    (.preventDefault e)
    (let [on-mouse-move (mouse-move-builder (get-offset e) cursor)
          on-mouse-up (mouse-up-builder on-mouse-move)]
      (events/listen js/window EventType.MOUSEMOVE on-mouse-move)
      (events/listen js/window EventType.MOUSEUP on-mouse-up))))

;; Draggable components
(defn drag-init [item-cursor]
  {:style {:position  "absolute"
           :left      (str (:x @item-cursor) "px")
           :top       (str (:y @item-cursor) "px")}
   :on-mouse-down (mouse-down-builder item-cursor)})

(defn make-draggable
  [start-coord [tags attributes & body]]
    (let [item-cursor (reagent/atom start-coord)]
      (fn []
        ; TODO: I'm not sure why we can't have the result of `drag-init` saved in a `let`
        ; binding, but...yeah
        [tags (merge attributes (drag-init item-cursor)) body])))

(defn drag-page []
  (fn [] [:span.main
          [:h1 "Drag things"]
          [make-draggable {:x 100 :y 100} [:button.btn.btn-default {} "Drag This"]]
          [make-draggable {:x 150 :y 150} [:button.btn.btn-default {} "Drag Other"]]
          [make-draggable {:x 200 :y 200} [:img {:src "images/joe.jpeg" :alt "Joe"}]]
          [make-draggable {:x 250 :y 250} [:img {:src "images/joyce.jpeg" :alt "Joyce"}]]
          ]))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [drag-page] (.getElementById js/document "app")))

(defn init! [] (mount-root))
