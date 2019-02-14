(ns tfsa.domain-tests
  (:require [tfsa.domain :as sut]
            #?(:clj [clojure.test :as t]
               :cljs [cljs.test :as t :include-macros true])))


;;;; PERSON

(t/deftest person-controller
  (t/testing ":init should return initial-person"
    (t/is (= {:state sut/initial-person} (sut/person :init))))
  (t/testing ":person/change should return new person"
    (t/is (= {:state "Piet"} (sut/person :person/change ["Piet"] "Foo"))))
  (t/testing ":person/add should return new person"
    (t/is (= {:state "Piet"} (sut/person :person/add ["Piet"] "Foo")))))


;;;; PEOPLE

(t/deftest people-controller
  (t/testing ":init should return initial-people"
    (t/is (= {:state sut/initial-people} (sut/people :init))))
  (t/testing ":person/add should add person to people"
    (t/is (= {:state (conj sut/initial-people "Piet")} (sut/people :person/add ["Piet"] sut/initial-people)))))
