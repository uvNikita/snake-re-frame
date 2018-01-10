(ns snake-re-frame.views
  (:require [re-frame.core :as rf]
            [snake-re-frame.subs :as subs]
            [snake-re-frame.events :as events]
            [snake-re-frame.util :as util]
            [snake-re-frame.config :as config]))

(defn cell [pos]
  (let [object @(rf/subscribe [::subs/object-on pos])
        snake-length @(rf/subscribe [::subs/snake-length])
        snake-part-color (partial util/snake-part-color snake-length)]
    [:div.cell
     (case (:type object)
       :snake-part {:style {:background (snake-part-color (:index object))}}
       :apple {:class :apple}
       :none  {:class :empty})]))


(defn score []
  (let [score (rf/subscribe [::subs/score])]
    [:div.score (str "Score: " @score)]))

(defn board []
  (let [board (rf/subscribe [::subs/board])
        height (:height @board)
        width (:width @board)
        gt (str "repeat(" height ", 20px) / repeat(" width ", 20px)")]
    [:div.board
     {:style {:grid-template gt}}
     (for [pos (util/board-cells @board)]
       ^{:key pos} [cell pos])]))

(defn start-button [text]
  [:div {:class :start-button-wrapper}
   [:input {:type :button :value text :class :start-button
            :on-click #(rf/dispatch [::events/game-start])}]])

(defn game-won []
  [:div
   [score]
   [:div.splash "You won!"]
   [start-button "Start again"]])


(defn game-over []
  [:div
   [score]
   [:div.splash "Game Over"]
   [start-button "Start again"]])

(defn instruction-row [keys description]
  [[:div.keys keys]
   [:div.colon ":"]
   [:div description]])

(defn game-menu []
  [:div
   [:div.splash "Snake Game"]
   [:div.instructions
    (concat
      (instruction-row "⇦⇨⇧⇩" "change direction")
      (instruction-row "r" "rewind time")
      (instruction-row "space" "pause the game"))]
   [start-button "Start"]])

(defn game []
  [:div [score] [board]])

(defn main []
  (let [game-state (rf/subscribe [::subs/game-state])]
    (case @game-state
      :in-menu [game-menu]
      :running [game]
      :rewinding [game]
      :won [game-won]
      :game-over [game-over])))
