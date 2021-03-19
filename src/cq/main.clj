(ns cq.main
  (:require [clojure.data.json :as json]
            [clojure.tools.cli :as cli]
            [clojure.string :as str])
  (:gen-class))

(defn query
  [x exps]
  (eval (concat `(->> ~x) exps)))

(defn run
  [in out exps]
  (-> in
      (json/read :key-fn keyword)
      (query exps)
      (json/write out))
  (println))

(def cli-options
  [["-h" "--help"]])

(defn usage [options-summary]
  (->> ["cq is a command-line data processor for JSON and other data formats."
        ""
        "Usage: cq [options] QUERY"
        ""
        "Options:"
        options-summary
        ""
        "See https://github.com/markus-wa/cq for more information."]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options errors summary] :as cfg}
        (cli/parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      :else ; failed custom validation => exit with usage summary
      cfg)))

(defn exit
  [status msg]
  (println msg)
  (System/exit status))

(defn- args->exprs
  [args]
  (->> args
       (map #(format "[%s]" %))
       (mapcat read-string)))

(defn -main
  [& args]
  (let [{:keys [arguments exit-message ok?]}
        (validate-args args)
        expressions (args->exprs arguments)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (run *in* *out* expressions))))
