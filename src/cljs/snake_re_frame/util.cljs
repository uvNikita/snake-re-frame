(ns snake-re-frame.util)


(defn rand-cell [{:keys [width height] :as board}]
  {:col (rand-int width)
   :row (rand-int height)})

(defn board-cells [{:keys [width height]}]
  (for [row (range height)
        col (range width)]
    {:col col :row row}))

(defn rand-apple [board objects]
  (let [empty-cells (vec (clojure.set/difference
                           (set (board-cells board))
                           (set objects)))]
    (rand-nth empty-cells)))

(defn next-cell
  [{:keys [width height] :as board}
   {:keys [col row] :as cell}
   direction]
  (case direction
    :right {:col (-> col inc (mod width)) :row row}
    :left {:col (-> col dec (mod width)) :row row}
    :down {:col col :row (-> row inc (mod height))}
    :up {:col col :row (-> row dec (mod height))}))

(def opposite-direction
  {:left :right
   :right :left
   :up :down
   :down :up})

(defn collisions? [{:keys [snake]}]
  (apply (comp not distinct?) (:body snake)))

(defn move-snake [{:keys [snake board] :as db}]
  (let [{:keys [body direction growing?]} snake
        head (last body)
        new-head (next-cell board head direction)
        new-tail (if growing? body (-> body rest vec))
        new-body (conj new-tail new-head)]
    (update db :snake assoc
      :body new-body
      :growing? false)))

(defn eating-apple? [{:keys [snake apple]}]
  (= (-> snake :body last) apple))

(defn snake-part-color [snake-length part-index]
  (let [max-lightness 0.6
        min-lightness 0.3
        lightness-range (- max-lightness min-lightness)
        part-ratio (/ part-index snake-length)
        lightness (+ min-lightness (* lightness-range part-ratio))]
    (str "hsl(38, 100%, " (int (* lightness 100)) "%)")))
