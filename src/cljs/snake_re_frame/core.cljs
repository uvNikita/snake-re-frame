(ns snake-re-frame.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [re-frame.db :refer [app-db]]
            [snake-re-frame.events :as events]
            [snake-re-frame.subs :as subs]
            [snake-re-frame.views :as views]
            [snake-re-frame.db :as db]
            [snake-re-frame.config :as config]))
(def keycode->action
  {37 :left
   38 :up
   40 :down
   39 :right
   82 :rewind
   32 :toggle-pause})

(defn on-keydown [event]
  (let [game-state @(rf/subscribe [::subs/game-state])]
    (when-let [action (keycode->action (.-keyCode event))]
      (case action
        :rewind
          (when (#{:running :game-over} game-state)
            (rf/dispatch [::events/rewinding true]))
        :toggle-pause
          (rf/dispatch [::events/toggle-pause])
        (rf/dispatch [::events/change-direction action])))))

(defn on-keyup [event]
  (let [game-state @(rf/subscribe [::subs/game-state])]
    (when-let [action (keycode->action (.-keyCode event))]
      (when (and (= action :rewind) (= game-state :rewinding))
        (rf/dispatch [::events/rewinding false])))))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (r/render [views/main]
            (.getElementById js/document "app")))

(defn ^:export init []
  (rf/dispatch-sync [::events/game-menu])
  (dev-setup)
  (mount-root)
  (js/addEventListener "keydown" (partial on-keydown))
  (js/addEventListener "keyup" (partial on-keyup)))
