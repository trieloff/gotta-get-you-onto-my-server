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

(defn get-ip [token id]
  (-> (p/then (http/get client (str "https://api.digitalocean.com/v2/droplets/" id)
                        {:headers { "Content-Type" "application/json"
                                    "Authorization" (str "Bearer" " " token)}})
               (fn [resp]
                 (println "Got an IP")
                 (println (:droplet (js->clj (.parse js/JSON (:body resp)) :keywordize-keys true)))
                 (:ip_address (first (:v4 (:networks (:droplet (js->clj (.parse js/JSON (:body resp)) :keywordize-keys true))))))))
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
                                      :size "4gb"
                                      :image snapshot
                                      :backups false
                                      :ipv6 false}}))
            (fn [resp]
              (js->clj (.parse js/JSON (:body resp)) :keywordize-keys true))))


(defn set-a-record [token domain address id]
  (println "setting A record for " domain " to " address)
  (if (nil? address)
    (-> (get-ip token id)
        (p/then #(set-a-record token domain % id)))
    (-> (get-a-record token domain)
      (p/then #(http/put client (str "https://api.digitalocean.com/v2/domains/" domain "/records/" (:id %))
                                (encode { :headers {  "Content-Type" "application/json"
                                                      "Authorization" (str "Bearer" " " token)}
                                          :body  {:data address}})))
      (p/then println))))

(defn main [args]
  (println "hello" args)
  ;(-> (get-a-record args "1.do.99productrules.com")
  ;    (p/then println))
  (-> (get-snapshots args)
      (p/then #(new-droplet args (:id (first %1))))
      (p/then #(set-a-record args "1.do.99productrules.com" nil (:id (:droplet %))))
      (p/then #(.exit js/process))))