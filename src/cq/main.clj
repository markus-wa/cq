(ns cq.main
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [cq.core :as cq]
            [cq.formats :as fmt])
  (:gen-class))

(require 'cq.readers)

(def colors #{:auto :on :off})

(def colors-str (str/join ", " (sort (map name colors))))

(def formats (set (keys fmt/formats)))

(def formats-str (str/join ", " (sort formats)))

(def yaml-flow-styles #{"auto" "block" "flow"})

(def transit-formats #{"json" "json-verbose" "msgpack"})

(def cli-options
  [["-i" "--in FORMAT" (str "Input format: " formats-str)
    :default "yaml"
    :validate [formats]]
   ["-o" "--out FORMAT" (str "Output format: " formats-str)
    :default "edn"
    :validate [formats]]
   ["-p" "--[no-]pretty" "Pretty print output - default is true"
    :default true]
   [nil "--color COLOR" (str "When pretty printing, whether to use colors: " colors-str " - default is auto")
    :default :auto
    :parse-fn keyword
    :validate [colors]]
   ["-c" nil "Same as --color=on"
    :id :color
    :assoc-fn (fn [m _ _] (assoc m :color :on))]
   ["-C" nil "Same as --color=off"
    :id :color
    :assoc-fn (fn [m _ _] (assoc m :color :off))]
   ["-k" "--key-fn FN" "Function used to transform keys - currently only supported for JSON and CSV"
    :default "keyword"]
   [nil "--yaml-unsafe" "Enables unsafe mode in clj-yaml / SnakeYAML"]
   [nil "--[no-]yaml-keywords" "Turn map keys into keywords in clj-yaml - default is true"
    :default true]
   [nil "--yaml-max-aliases-for-collections" "Sets max aliases for collections in clj-yaml / SnakeYAML"]
   [nil "--yaml-allow-recursive-keys" "Allows recursive keys in clj-yaml / SnakeYAML"]
   [nil "--yaml-allow-duplicate-keys" "Allows duplicate keys in clj-yaml / SnakeYAML"]
   [nil "--yaml-flow-style STYLE" (str "Sets flow style in SnakeYAML: " (str/join ", " (sort yaml-flow-styles)))
    :default "auto"
    :validate [yaml-flow-styles]]
   [nil "--transit-format-in FORMAT" (str "Set the reader type for transit: " (str/join ", " (sort transit-formats)))
    :default "json"
    :validate [transit-formats]]
   [nil "--transit-format-out FORMAT" (str "Set the writer type for transit: " (str/join ", " (sort transit-formats)))
    :default "json"
    :validate [transit-formats]]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["cq is a command-line data processor for JSON, YAML, EDN and other data formats that utilises Clojure as it's query language."
        ""
        "Usage: cq [options] [--] QUERY"
        ""
        "Examples"
        "  echo '{a: {b: [1, 2, 3]}}' | cq ':a :b (map inc)'"
        ""
        "  printf 'http://example.com/some/path' | cq -i text -- '-> str/upper-case (str/split #\"/\") ->> (map str/reverse)'"
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

;; we want to use System/in and out instead of *in* *out*
;; as it's easier to convert to the reader/writer formats we need in formats.clj
(def ^:dynamic *stdin* System/in)
(def ^:dynamic *stdout* System/out)

(defn- autodetect-color
  "Autodetects whether ANSI color should be enabled.

  REPLs in general support colors, but may not use a console.

  JVM gives us the `System/console`, if we have a console,
  and not a plain pipe."
  []
  (or (contains? (set (loaded-libs)) 'nrepl.core)
      (System/console)))

(defn- handle-auto-options [opts]
  (update opts :color #(case %
                         :auto (autodetect-color)
                         :on true
                         false)))

(defn main
  [args]
  (let [{:keys [arguments exit-message ok?]
         {:keys [in out] :as opts} :options}
        (validate-args args)
        opts (handle-auto-options opts)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [expressions (args->exprs arguments)
            reader (fmt/format->reader in *stdin* opts)
            writer (fmt/format->writer out *stdout* opts)]
        (cq/run reader writer expressions)))))

(defn -main
  [& args]
  (try
    (main args)
    (catch Exception e
      (binding [*out* *err*]
        (println e)
        (System/exit 1)))))

(comment
  (cq/query {:a 1}
            (args->exprs ["#| ."])))
