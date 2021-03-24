(ns cq.main
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [cq.core :as cq]
            [cq.formats :as fmt])
  (:gen-class))

(require 'cq.readers)

(def formats (set (keys fmt/formats)))

(def formats-str (str/join ", " (sort formats)))

(def cli-options
  [["-i" "--in FORMAT" (str "Input format: " formats-str)
    :default "yaml"
    :validate [formats]]
   ["-o" "--out FORMAT" (str "Output format: " formats-str)
    :default "edn"
    :validate [formats]]
   ["-p" "--[no-]pretty" "Pretty print output - default is true"
    :default true]
   ["-k" "--key-fn FN" "Function used to transform keys - currently only supported for JSON and CSV"
    :default "keyword"]
   [nil "--yaml-unsafe" "Enables unsafe mode in clj-yaml / SnakeYAML"]
   [nil "--[no]-yaml-keywords" "Turn map keys into keywords in clj-yaml - default is true"
    :default true]
   [nil "--yaml-max-aliases-for-collections" "Sets max aliases for collections in clj-yaml / SnakeYAML"]
   [nil "--yaml-allow-recursive-keys" "Allows recursive keys in clj-yaml / SnakeYAML"]
   [nil "--yaml-allow-duplicate-keys" "Allows duplicate keys in clj-yaml / SnakeYAML"]
   [nil "--yaml-flow-style STYLE" "Sets flow style in SnakeYAML"
    :default "auto"
    :validate #{"auto" "block" "flow"}]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["cq is a command-line data processor for JSON, YAML, EDN and other data formats that utilises Clojure as it's query language."
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
      (-> cfg
          (update-in [:options :key-fn] #(-> % symbol resolve))))))

(defn exit
  [status msg]
  (println msg)
  (System/exit status))

(defn- args->exprs
  [args]
  (->> args
       (str/join " ") ;; make it a single list of expressions
       (format "[%s]")
       read-string))

(defn main
  [args]
  (let [{:keys [arguments exit-message ok?]
         {:keys [in out] :as opts} :options}
        (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [expressions (args->exprs arguments)
            reader (fmt/format->reader in System/in opts)
            writer (fmt/format->writer out System/out opts)]
        (cq/run reader writer expressions)))))

(defn -main
  [& args]
  (try
    (main args)
    (catch Exception e
      (println e))))

(comment
  (cq/query {:a 1}
            (args->exprs ["#| ."])))
