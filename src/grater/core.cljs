(ns grater.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET]]
            [goog.string.format]
            [goog.string :refer [format]]))
            
(enable-console-print!)

(defn appstate-validator
  "Ensures sanity remains in range of 0-10"
  [{:keys [sanity, info]}]
  (and (>= sanity 0)
       (<= sanity 10)
       (>= info 0)
       (<= info 10)))

(defn sanity-watcher
  "Ensures sanity remains between 0-10 and puts the game in a gameover state when it drops to 0"
  [_ ref {old :sanity} {curr :sanity}]
  (cond
    (and (> old 0) (<= curr 0)) (swap! ref #(-> %
                                                (assoc :sanity 0)
                                                (assoc :gamestate :game-over)))
    (> curr 10) (swap! ref assoc :sanity 10)))


(defonce appstate (atom {:deck []
                         :cards (list)
                         :money 0.00
                         :sanity 10
                         :info 0
                         :gamestate :in-game}))
(add-watch appstate nil sanity-watcher)

(defn calc-income
  "Calculates daily income based on info and chance"
  []
  (+ (* 100 (:info @appstate) .8)
     (* (- (rand) .65) 100 .2)))


(defn tweet
  "Represents a single tweet"
  [title details]
  [:div.tweet [:p title] [:p details]])

(defn get-cards!
  "Gets all cards from the server"
  []
  (GET "/json/cards.json"
       :response-format :json
       :handler (fn [data] (swap! appstate #(assoc % :deck data)))))
(get-cards!)

(defn add-card!
  [card]
  (do
    (swap! appstate #(update % :cards conj card))
    (swap! appstate #(update % :sanity + (card 2)))
    (swap! appstate #(update % :info + (card 3)))))

(defn draw-card
  "Draws a card from the deck based on weight."
  []
  (->> @appstate :deck
       (reduce #(concat %1 (repeat (get %2 4) %2)) (list))
       rand-nth))

(defn feed
  []
  [:div.feed
   (map #(into ^{:key %2} [tweet] %1) (@appstate :cards) (range))])

(defn stat
  "Represents a stat"
  [name value]
  [:p.stat [:label (str name ":")] [:span value]])

(defn stats
  []
  [:div.stats
   [stat "Money" (format "$%.2f" (:money @appstate))]
   [stat "Sanity" (str (:sanity @appstate) "/10")]
   [stat "Info" (str (:info @appstate) "/10")]])

(defn button
  [str f]
  [:input.btn.btn-primary
   {:type "button" :value str :on-click f}])


(defn read-button
  "Pulls another card from the deck"
  []
  [button "Read Tweet" #(-> (draw-card) add-card!)])

(defn work-button
  "Cash in information, reset sanity"
  []
  [button "Go To Work"
   (fn [] (swap! appstate #(-> %
                               (update :money + (calc-income))
                               (assoc :sanity 10)
                               (assoc :cards (list))
                               (assoc :info 0))))])

(defn controls
  []
  [:div.controls [read-button] [work-button]])

(defn gamescreen
  []
  [:div [feed] [stats] [controls]])

(defn gameover
  []
  [:div.gameover [:h1 "Game Over"]])

(defn view
  []
  (cond
    (= :in-game (:gamestate @appstate)) [gamescreen]
    (= :game-over (:gamestate @appstate)) [gameover]))

(reagent/render-component [view]
                          (. js/document (getElementById "app")))

