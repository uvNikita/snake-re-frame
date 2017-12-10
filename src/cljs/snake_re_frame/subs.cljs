(ns snake-re-frame.subs
  (:require [re-frame.core :as rf]
            [snake-re-frame.db :as db]
            [snake-re-frame.config :as config]))

(rf/reg-sub ::score :score)
(rf/reg-sub ::apple :apple)
(rf/reg-sub ::board :board)
(rf/reg-sub ::game-state :game-state)

(rf/reg-sub ::speed
  (fn [{:keys [game-state speed]}]
    (case game-state
      :rewinding config/rewind-speed
      speed)))

(rf/reg-sub ::snake :snake)
(rf/reg-sub ::snake-length #(-> % :snake :body count))

(rf/reg-sub ::object-on
  :<- [::snake]
  :<- [::apple]
  (fn [[snake apple] [_ cell]]
    (let [snake-part-index (.indexOf (:body snake) cell)]
      (cond
        (not= snake-part-index -1) {:type :snake-part :index snake-part-index}
        (= apple cell) {:type :apple}
        :else {:type :none}))))
