(ns onto.server
  (:require [httpurr.client :as http]
            [httpurr.client.node :refer [client]]))
 
(defn main [args]
  (println "hello")
  (+ 1 1))