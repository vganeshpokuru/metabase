(ns metabase.util
  "Common utility functions useful throughout the codebase."
  (:require [medley.core :refer :all]
            [clj-time.format :as time]
            [clj-time.coerce :as coerce]))


(defn contains-many? [m & ks]
  (every? true? (map #(contains? m %) ks)))

(defn select-non-nil-keys
  "Like `select-keys` but filters out key-value pairs whose value is nil."
  [m & keys]
  (->> (select-keys m keys)
       (filter-vals identity)))

(defmacro fn->
  "Returns a function that threads arguments to it through FORMS via `->`."
  [& forms]
  `(fn [x#]
     (-> x#
         ~@forms)))

(defmacro fn->>
  "Returns a function that threads arguments to it through FORMS via `->>`."
  [& forms]
  `(fn [x#]
     (->> x#
          ~@forms)))

(defn regex?
  "Is ARG a regular expression?"
  [arg]
  (= (type arg)
     java.util.regex.Pattern))

(defn regex=
  "Returns `true` if the literal string representations of REGEXES are exactly equal.

    (= #\"[0-9]+\" #\"[0-9]+\")           -> false
    (regex= #\"[0-9]+\" #\"[0-9]+\")      -> true
    (regex= #\"[0-9]+\" #\"[0-9][0-9]*\") -> false (although it's theoretically true)"
  [& regexes]
  (->> regexes
       (map #(.toString ^java.util.regex.Pattern %))
       (apply =)))

(defn self-mapping
  "Given a function F that takes a single arg, return a function that will call `(f arg)` when
  passed a non-sequential ARG, or `(map f arg)` when passed a sequential ARG.

    (def f (self-mapping (fn [x] (+ 1 x))))
    (f 2)       -> 3
    (f [1 2 3]) -> (2 3 4)"
  [f & args]
  (fn [arg]
    (if (sequential? arg) (map f arg)
        (f arg))))

;; looking for `apply-kwargs`?
;; turns out `medley.core/mapply` does the same thingx


(declare -assoc*)
(defmacro assoc*
  "Like `assoc`, but associations happen sequentially; i.e. each successive binding can build
   upon the result of the previous one using `<>`.

    (assoc* {}
            :a 100
            :b (+ 100 (:a <>)) ; -> {:a 100 :b 200}"
  [object & kvs]
  `((fn [~'<>]          ; wrap in a `fn` so this can be used in `->`/`->>` forms
      (-assoc* ~@kvs))
    ~object))

(defmacro -assoc* [k v & rest]
  `(let [~'<> (assoc ~'<> ~k ~v)]
        ~(if (empty? rest) `~'<>
             `(-assoc* ~@rest))))

(defn new-sql-date
  "`java.sql.Date` doesn't have an empty constructor so this is a convenience that lets you make one with the current date.
   (Some DBs like Postgres will get snippy if you don't use a `java.sql.Date`)."
  []
  (-> (java.util.Date.)
      .getTime
      (java.sql.Date.)))

(defn parse-iso8601
  "parse a string value expected in the iso8601 format into a `java.sql.Date`."
  [datetime]
  (when datetime
    (->> datetime
      (time/parse (time/formatters :date-time-no-ms))
      (coerce/to-long)
      (java.sql.Date.))))

(defn jdbc-clob-to-str
  "Convert a `JdbcClob` to a `String` so it can be serialized to JSON."
  [^org.h2.jdbc.JdbcClob clob]
  (.getSubString clob 1 (.length clob)))
