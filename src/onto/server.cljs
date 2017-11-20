(ns onto.server
  (:require [httpurr.client :as http]
            [promesa.core :as p]
            [httpurr.client.node :refer [client]]))

(defn get-domain-records [token domain]
  (-> (p/then (http/get client (str "https://api.digitalocean.com/v2/domains/" domain "/records")
                        {:headers { "Content-Type" "application/json"
                                    "Authorization" (str "Bearer" " " token)}})
               (fn [resp]
                 (:domain_records (js->clj (.parse js/JSON (:body resp)) :keywordize-keys true))))
       (p/catch (fn [err]
                  (println err)
                  (identity nil)))))

(defn filter-a-record [domainrecords]
  (filter #(= "A" (:type %)) domainrecords))

(defn get-a-record [token domain]
  (-> (get-domain-records token domain)
      (p/then filter-a-record)
      (p/then first)))

(defn get-snapshots [token]
  (-> (p/then (http/get client "https://api.digitalocean.com/v2/snapshots"
                        {:headers { "Content-Type" "application/json"
                                    "Authorization" (str "Bearer" " " token)}})
               (fn [resp]
                 (:snapshots (js->clj (.parse js/JSON (:body resp)) :keywordize-keys true))))
       (p/catch (fn [err]
                  (println err)
                  (identity nil)))))
(defn encode
  [request]
  (update request :body #(js/JSON.stringify (clj->js %))))

(defn new-droplet [token snapshot]
  (p/then (http/post client "https://api.digitalocean.com/v2/droplets"
                     (encode {:headers {"Content-Type" "application/json"
                                        "Authorization" (str "Bearer" " " token)}
                              :body  {:name "1.do.99productrules.com",
                                      :region "fra1"
                                      :size "512mb"
                                      :image snapshot
                                      :backups false
                                      :ipv6 false}}))
            (fn [resp]
              (js->clj (.parse js/JSON (:body resp)) :keywordize-keys true))))

(defn main [args]
  (println "hello" args)
  (-> (get-a-record args "1.do.99productrules.com")
      (p/then println))
  (-> (get-snapshots args)
      ;(p/then #(new-droplet args (:id (first %1))))
      (p/then println)))