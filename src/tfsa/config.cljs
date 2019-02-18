(ns tfsa.config
  (:require [cljs-time.core :as time]))

(def first-tfsa-year 2015)

(def lifetime-limit 500000)

(def tax-year-limits
  (merge
   {2015 30000
    2016 30000}
   (->> (range 2017 (+ 2 (time/year (time/now))))
        (map #(hash-map % 33000))
        (into {}))))
