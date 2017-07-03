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
       (<= sanity 10)))

(defonce appstate (atom {:deck []
                         :cards (list)
                         :money 0.00
                         :sanity 10
                         :info 0}
                        :validator appstate-validator))

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
  [button "Read Tweet" #(-> (:deck @appstate) rand-nth add-card!)])

(defn work-button
  "Cash in information, reset sanity"
  []
  [button "Go To Work"
   (fn [] (swap! appstate #(-> %
                               (update :money + (calc-income))
                               (assoc :sanity 10)
                               (assoc :cards [])
                               (assoc :info 0))))])

(defn controls
  []
  [:div.controls [read-button] [work-button]])


(reagent/render-component [:div [feed] [stats] [controls]]
                          (. js/document (getElementById "app")))

