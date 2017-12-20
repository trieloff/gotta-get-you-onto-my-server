(ns onto.server
  (:require-macros [onto.macros :as w])
  (:require [httpurr.client :as http]
            [promesa.core :as p]
            [openwhisk.wrap :as o]
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
                                      :tags ["devbox"]
                                      :ipv6 false}}))
            (fn [resp]
              (js->clj (.parse js/JSON (:body resp)) :keywordize-keys true))))

(defn list-droplets [token]
  (p/then (http/get client "https://api.digitalocean.com/v2/droplets"
                        {:headers {"Content-Type" "application/json"
                                          "Authorization" (str "Bearer" " " token)}})
              (fn [resp]
                (:droplets (js->clj (.parse js/JSON (:body resp)) :keywordize-keys true)))))

(defn kill-droplets [token tag]
  (p/then (http/delete client "https://api.digitalocean.com/v2/droplets"
                        { :headers {"Content-Type" "application/json"
                                          "Authorization" (str "Bearer" " " token)}
                          :query-params {:tag_name tag}})
              (fn [resp]
                (:status resp))))

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
      (p/then #(println "Successfully set IP to " address)))))

(defn start-devbox [token domain]
  (-> (get-snapshots token)
      (p/then #(new-droplet token (:id (last %1))))
      (p/then #(set-a-record token domain nil (:id (:droplet %))))
      (p/then identity)))

(defn clear-devbox [token]
  (println "I'm cleaning up " token)
  (-> (kill-droplets token "devbox")
      (p/then identity)))

(defn provision [args]
  (let [token (:token (js->clj args :keywordize-keys true))
        domain (:domain (js->clj args :keywordize-keys true))]
    (start-devbox token domain)))

(defn deprovision [args]
  (let [token (:token (js->clj args :keywordize-keys true))]
    (clear-devbox token)))

(defn echo- [args]
  {:echo (assoc args :key "value")})

(w/defnw echo [args]
  {:echo (assoc args :key "value")})

;(w/defw echo echo-)


(defn main [args]
  (clj->js 
    (let [params (js->clj args :keywordize-keys true)
          domain (:domain       params)
          token  (:token        params)
          method (:__ow_method  params)
          action (:action params)]
        (case method
          "get" {:status "only post allowed"}
          "post" (case action
                    "provision" {:action "p"}
                    "deprovision" {:action "d"}
                    { :action action
                      :token  token
                      :method method
                      :domain domain})
          {:status "this should never happen"}))))

(defn old [token domain]
  (if (nil? domain)
    (clear-devbox token)
    (start-devbox token domain)))