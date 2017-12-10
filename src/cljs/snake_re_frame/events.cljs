(ns snake-re-frame.events
  (:require [cljs.spec.alpha :as s]
            [cljs.core.async :as async]
            [re-frame.core :as rf]
            [day8.re-frame.undo :as undo :refer [undoable clear-undos!]]
            [snake-re-frame.db :as db]
            [snake-re-frame.subs :as subs]
            [snake-re-frame.config :as config]
            [snake-re-frame.util :as util]))

(rf/reg-event-db
  :purge-undos
  (fn [db _]
    (clear-undos!)
    db))

(day8.re-frame.undo/undo-config!
  {:reinstate-fn (fn [db value] (reset! db (assoc value :game-state :rewinding)))})

(let [ctl-chan (async/chan)]
  (do
    (async/go-loop [running? false]
      (let [speed (rf/subscribe [::subs/speed])
            channels (if running? [ctl-chan (async/timeout (quot 1000 @speed))] [ctl-chan])
            [cmd chan] (async/alts! channels)]
        (if (identical? chan ctl-chan)
          (case cmd
            :start (recur true)
            :stop (recur false)
            :toggle (recur (not running?)))
          (do
            (rf/dispatch [::tick])
            (recur true)))))
    (rf/reg-fx
      :timer
        (fn [cmd]
          (async/put! ctl-chan cmd)))))

(defn check-and-throw-db
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db])

(def check-spec-interceptor-db
  (rf/after
    (fn [db-or-cofx]
      (let [db (get db-or-cofx :db db-or-cofx)]
        (when-not (s/valid? :snake-re-frame.db/db db)
          (throw (ex-info (str "spec check failed: "
                               (s/explain-str :snake-re-frame.db/db db)) {})))))))

(def interceptors [(when config/debug? check-spec-interceptor-db)
                   rf/trim-v])

(rf/reg-event-db
 ::game-menu
 interceptors
 (constantly db/default-db))

(rf/reg-event-db
 ::rewinding
 (conj interceptors (rf/path [:game-state]))
 (fn [_ [enable?]]
   (if enable?
     :rewinding
     :running)))

(rf/reg-event-fx
 ::game-start
 interceptors
 (fn [_ _]
   {:db (assoc db/default-db :game-state :running)
    :dispatch [:purge-undos]
    :timer :start}))

(rf/reg-event-fx
 ::game-over
 interceptors
 (fn [{:keys [db]} _]
   {:db (assoc db :game-state :game-over)}))

(rf/reg-event-fx
 ::game-won
 interceptors
 (fn [{:keys [db]} _]
   {:db (assoc db :game-state :won)
    :timer :stop}))

(rf/reg-event-fx
  ::tick-forward
  (conj interceptors (undoable))
  (fn [{:keys [db]} _]
    (let [new-db (util/move-snake db)]
      (if (util/collisions? new-db)
        {:dispatch [::game-over]}
        (merge {:db new-db}
               (when (util/eating-apple? new-db) {:dispatch [::eat-apple]}))))))

(rf/reg-event-fx
 ::tick
 interceptors
 (fn [{:keys [db]} _]
   (if (>= (:score db) config/max-score)
     {:dispatch [::game-won]}
     (case (:game-state db)
       :running {:dispatch [::tick-forward]}
       :rewinding {:dispatch [:undo]}
       :game-over {}))))

(rf/reg-event-fx
  ::toggle-pause
  interceptors
  (constantly {:timer :toggle}))

(rf/reg-event-db
 ::gen-apple
 interceptors
 (fn [{:keys [board snake] :as db}]
   (let [objects (:body snake)]
     (assoc db :apple (util/rand-apple board objects)))))

(rf/reg-event-db
  ::change-direction
  interceptors
  (fn [{:keys [board snake] :as db} [new-direction]]
    (let [snake-body (:body snake)
          snake-head (last snake-body)
          pre-snake-head (last (butlast snake-body))]
      (if (= (util/next-cell board snake-head new-direction) pre-snake-head)
        db
        (assoc-in db [:snake :direction] new-direction)))))

(rf/reg-event-fx
  ::eat-apple
  interceptors
  (constantly
    {:dispatch-n [[::grow-snake]
                  [::add-score]
                  [::gen-apple]]}))

(rf/reg-event-db
  ::add-score
  (conj interceptors [(rf/path :score)])
  inc)

(rf/reg-event-db
  ::grow-snake
  (conj interceptors [(rf/path :snake :growing?)])
  (constantly true))
