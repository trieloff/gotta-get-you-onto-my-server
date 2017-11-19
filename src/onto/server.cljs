(ns onto.server
  (:require [httpurr.client :as http]
            [promesa.core :as p]
            [httpurr.client.node :refer [client]]))

(defn get-snapshots [token]
  (-> (p/then (http/get client "https://api.digitalocean.com/v2/snapshots"
                         {:headers { "Content-Type" "application/json"
                                     "Authorization" (str "Bearer" " " token)}})
               (fn [resp]
                 (:snapshots (js->clj (.parse js/JSON (:body resp)) :keywordize-keys true))))
       (p/catch (fn [err]
                  (println err)
                  (identity nil)))))

(defn main [args]
  (println "hello" args)
  (p/then (get-snapshots args) #(println %)))