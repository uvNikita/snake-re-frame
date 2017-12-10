(ns snake-re-frame.db
  (:require [cljs.spec.alpha :as s]
            [snake-re-frame.config :as config]
            [snake-re-frame.util :as util]))

(s/def :board/width pos?)
(s/def :board/height pos?)
(s/def ::board (s/keys :req-un [:board/width :board/height]))

(s/def ::col (s/and int? #(>= % 0)))
(s/def ::row (s/and int? #(>= % 0)))
(s/def ::cell (s/keys :req-un [::row ::col]))

(s/def ::score (s/and int? #(>= % 0)))
(s/def ::speed (s/and int? #(>= % 0)))
(s/def ::apple ::cell)

(def initial-snake-length 4)
(s/def :snake/direction #{:right :left :up :down})
(s/def :snake/body (s/coll-of ::cell
                     :kind vector?
                     :distinct true
                     :min-count initial-snake-length
                     :into []))
(s/def :snake/growing? boolean?)
(s/def ::snake (s/keys :req-un [:snake/body :snake/direction :snake/growing?]))
(defn snake-gen [board length direction start-pos]
  (let [opposite-direction (util/opposite-direction direction)
        add-segment (fn [[head & _ :as body]]
                      (conj body
                            (util/next-cell board head opposite-direction)))]
    {:direction direction
     :growing? false
     :body (vec (reduce (fn [body _] (add-segment body))
                        (list start-pos)
                        (range (dec length))))}))

(s/def ::game-state #{:in-menu :running :rewinding :won :game-over})

(defn apple-pred [{:keys [snake apple]}]
  (not-any? #(= apple (butlast snake)) (:body snake)))

(defn score-pred [{:keys [snake score]}]
  (>= score (- (count (:body snake)) initial-snake-length)))

(defn cells-pred [{:keys [board snake apple]}]
  (let [{:keys [width height]} board
        cells (conj (-> snake :body) apple)
        cell-pred (fn [{:keys [row col]}] (and (< row height) (< col width)))]
    (every? cell-pred cells)))

(s/def ::db (s/and (s/keys :req-un [::board ::speed ::game-state
                                    ::snake ::score ::apple])
                   cells-pred
                   apple-pred
                   score-pred))


(def default-db
  (let [default-board {:width 17 :height 17}
        default-head {:col (quot (:width default-board) 2)
                      :row (quot (:height default-board) 2)}
        default-snake (snake-gen default-board
                                 initial-snake-length
                                 :right
                                 default-head)
        objects (:body default-snake)]
    {:snake default-snake
     :game-state :in-menu
     :score 0
     :apple (util/rand-apple default-board objects)
     :board default-board
     :speed 11}))
