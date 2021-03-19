(ns cq.main
  (:require [clojure.data.json :as json]
            [clojure.tools.cli :as cli])
  (:gen-class))

(defn query
  [x exps]
  (eval (concat `(->> ~x) exps)))

(def cli-options
  ;; An option with a required argument
  [["-e" "--expression EXPRESSION" "Clojure transformation expression"
    :id :expression
    :default "identity"]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defn -main
  [& args]
  (let [{:keys [options]}
        (cli/parse-opts args cli-options)
        exps (read-string (format "[%s]" (:expression options)))]
    (-> *in*
        (json/read :key-fn keyword)
        (query exps)
        (json/write *out*))
    (println)))
