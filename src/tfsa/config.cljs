(ns tfsa.config
  (:require [cljs-time.core :as time]))

(def first-tfsa-year 2015)

(def lifetime-limit 500000)

(def tax-year-limits
  (merge
   {2015 30000
    2016 30000
    2017 33000
    2018 33000
    2019 33000}
   (->> (range 2021 (+ 2 (time/year (time/now))))
        (map #(hash-map % 36000))
        (into {}))))
