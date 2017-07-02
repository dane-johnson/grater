(ns grater.core
  (:require [reagent.core :as reagent :refer [atom]]
            [ajax.core :refer [GET]]))

(enable-console-print!)

(defonce appstate (atom {:deck [] :cards (list)}))

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
  (swap! appstate #(update % :cards conj card)))

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
   [stat "Money" "$200.00"]
   [stat "Sanity" "8/10"]])

(defn read-button
  "Pulls another card from the deck"
  []
  [:input.btn.btn-primary
   {:type "button" :value "Read Tweet"
    :on-click #(-> (:deck @appstate) rand-nth add-card!)}])

(defn controls
  []
  [:div.controls [read-button]])


(reagent/render-component [:div [feed] [stats] [controls]]
                          (. js/document (getElementById "app")))

