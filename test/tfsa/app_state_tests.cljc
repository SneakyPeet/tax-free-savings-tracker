(ns tfsa.app-state-tests
  (:require [tfsa.app-state :as sut]
            #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true])))


(t/deftest adding-person?-controller
  (t/testing ":init should return false"
    (t/is (= {:state false} (sut/adding-person? :init))))
  (t/testing ":adding-person/show should return true"
    (t/is (= {:state true} (sut/adding-person? :adding-person/show))))
  (t/testing ":adding-person/hide should return false"
    (t/is (= {:state false} (sut/adding-person? :adding-person/hide))))
  (t/testing ":person/add should return false"
    (t/is (= {:state false} (sut/adding-person? :person/add "Piet"))))
  )
