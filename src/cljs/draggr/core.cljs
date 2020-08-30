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
(defn style-thing [item-cursor]
  {:position  "absolute"
   :left      (str (:x @item-cursor) "px")
   :top       (str (:y @item-cursor) "px")})

(defn drag-init [item-cursor]
  {:style (style-thing item-cursor)
   :on-mouse-down (mouse-down-builder item-cursor)})

(defn make-draggable
  [start-coord [tags attributes & body]]
    (let [item-cursor (reagent/atom start-coord)]
      (fn []
        [tags (merge (drag-init item-cursor) attributes) body])))

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
