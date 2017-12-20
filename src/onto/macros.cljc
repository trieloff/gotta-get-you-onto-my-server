(ns onto.macros
  (:require [openwisk.wrap :as oww])

(defmacro defnw [name args body]
  "defines a wrapped function"
  `(def ~name (oww/wrap (fn ~args ~body))))