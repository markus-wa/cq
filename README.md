# cq (Clojure Query)

Command-line Data Processor for EDN, YAML, JSON, XML and other data formats.

The joy of Clojure's threading macros, but on the command line!

May or may not have invented the Hash-Pipe `#|` operator 🍁 (citation needed)

[![CI / CD](https://github.com/markus-wa/cq/actions/workflows/cicd.yaml/badge.svg)](https://github.com/markus-wa/cq/actions/workflows/cicd.yaml)
[![codecov](https://codecov.io/gh/markus-wa/cq/branch/main/graph/badge.svg?token=zGovO2H0bm)](https://codecov.io/gh/markus-wa/cq)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/markus-wa/cq)](https://github.com/markus-wa/cq/releases)
[![License](https://img.shields.io/badge/license-EPL--2.0-blue)](LICENSE)
[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fmarkus-wa%2Fcq.svg?type=shield)](https://app.fossa.com/projects/git%2Bgithub.com%2Fmarkus-wa%2Fcq?ref=badge_shield)

![image](https://user-images.githubusercontent.com/5138316/214540308-414fb004-0b3f-4377-b3a1-239cb8c7d3fb.png)

## Installation

### Homebrew

    brew install markus-wa/brew/cq
### Windows / Scoop
    scoop bucket add scoop-clojure
    scoop install scoop-clojure/cq
### Manual

1. Download the latest version for your OS from the [releases](https://github.com/markus-wa/cq/releases) page.
    - note: you should avoid `cq-jvm` if possible as these are not GraalVM native images and will be slow to start.
2. Rename binary to `cq`
3. `chmod +x cq`
4. Move into a location on `$PATH`

## Rationale

While there are a few similar, amazing tools out there (such as `jq`, `jet` or `babashka`), `cq` tries to resolve some of their shortcomings such as having to learn custom query languages, lacking powerful data transformation libraries or quick and _easy_ (yes I said the e word) handling of many input and output formats.

cq aims to:

- Not require learning yet another query language - it's just ~~data~~ Clojure!
- Give the user complete power for writing queries, with as few limitations as possible
- Provide various input and output formats out of the box
- Be opinionated, ship useful tools/libraries pre-bundled

## Features

- Supports all elements of Clojure that are supported by [SCI](https://github.com/borkdude/sci)
- Data Formats:
  - EDN
  - YAML
  - JSON
  - XML
  - MsgPack
  - CSV
  - Cognitec's Transit format
  - Text (raw and line-separated)
  - HTML (via Hickory)
- [Various reader macros](#reader-macros) that make writing queries easier
- [Threading macro redirection](#threading-macro-redirection) reduces need for parentheses
- Coloured output / syntax highlig for EDN output
- No startup lag thanks to GraalVM native-images
- Comes batteries-included with the following libraries for transforming nested data structures and utilities
  - [specter](https://github.com/redplanetlabs/specter) - `(s/...)`
  - [medley](https://github.com/weavejester/medley) - `(m/...)`
  - [camel-snake-kebab](https://clj-commons.org/camel-snake-kebab/) - `(csk/...)`
  - [xml-in](https://github.com/tolitius/xml-in) - `(xml-in/...)`
  - [data.json](https://github.com/clojure/data.json) - `(json/...)`
  - [data.csv](https://github.com/clojure/data.csv) - `(csv/...)`
  - [data.xml](https://github.com/clojure/data.xml) - `(xml/...)`

## Usage

```
$ cq --help
cq is a command-line data processor for JSON, YAML, EDN and other data formats that utilises Clojure as it's query language.

Usage: cq [options] [--] QUERY

Examples
  echo '{a: {b: [1, 2, 3]}}' | cq ':a :b (map inc)'

  printf 'http://example.com/some/path' | cq -i text -- '-> str/upper-case (str/split #"/") ->> (map str/reverse)'

Options:
  -i, --in FORMAT                         yaml     Input format: csv, edn, json, lines, msgpack, text, transit, yaml, html
  -o, --out FORMAT                        edn      Output format: csv, edn, json, lines, msgpack, text, transit, yaml, html
  -p, --[no-]pretty                                Pretty print output - default is true
      --color COLOR                       auto     When pretty printing, whether to use colors: auto, off, on - default is auto
  -c                                               Same as --color=on
  -C                                               Same as --color=off
  -k, --key-fn FN                         keyword  Function used to transform keys - currently only supported for JSON and CSV
      --yaml-unsafe                                Enables unsafe mode in clj-yaml / SnakeYAML
      --[no-]yaml-keywords                         Turn map keys into keywords in clj-yaml - default is true
      --yaml-max-aliases-for-collections           Sets max aliases for collections in clj-yaml / SnakeYAML
      --yaml-allow-recursive-keys                  Allows recursive keys in clj-yaml / SnakeYAML
      --yaml-allow-duplicate-keys                  Allows duplicate keys in clj-yaml / SnakeYAML
      --yaml-flow-style STYLE             auto     Sets flow style in SnakeYAML: auto, block, flow
      --transit-format-in FORMAT          json     Set the reader type for transit: json, json-verbose, msgpack
      --transit-format-out FORMAT         json     Set the writer type for transit: json, json-verbose, msgpack
  -h, --help

See https://github.com/markus-wa/cq for more information.
```

By default `cq` uses [thread last (`->>`)](https://clojure.org/guides/threading_macros#thread-last) semantics.

```bash
$ echo '{"a": {"b": [1, 2, 3]}}' | cq ':a :b (map inc)'
(2 3 4)
```

You can use `clojure.instant` (alias `inst`) to parse timestamps.

```bash
$ echo '{"a": "2023-03-11T03:01:11.000Z"}' | cq :a inst/read-instant-timestamp
#inst "2023-03-11T03:01:11.000-00:00"
```

Using `#|` you can use the current value as `.`.

```bash
$ curl -s 'https://api.github.com/repos/markus-wa/cq/commits?per_page=5' | \
cq 'second #| {:author (-> . :commit :author :name) :message (-> . :commit :message) :parents (->> . :parents (map :html_url))}'
{:author "Markus Walther",
 :message "tests: fix coloured output breaking tests",
 :parents ("https://github.com/markus-wa/cq/commit/92ff81edbd6f53f0d20aa5a18ccf6cac53bbe50e")}
```

There's also a destructuring macro `#&` to make using `(let)` easier.

```bash
$ printf "http://example.com/some/path" | \
  cq -i text -- '-> (str/split #"/") #& ([protocol _ host] {:protocol protocol :host host})'
{:protocol "http:", :host "example.com"}
```

`#f` can be used to simplify creating an anonymous function that returns a value, rather than calls a function.<br>
Also note how `m/map-kv` is provided by `medley.core`.
```bash
$ echo '{a: {b: 2, c: 3}}' | cq ':a  (m/map-kv #f [%2 %1])'
{2 :b, 3 :c}
```

### Threading Macro Redirection

While things like [`->->><?as->cond->!`](https://github.com/randomcorp/thread-first-thread-last-backwards-question-mark-as-arrow-cond-arrow-bang) are pretty funny,
it can be pretty convenient to just redirect a threading macro when you're working on a simple terminal without paredit.

All threading operators will change the query after that point to their implementation until followed by any other threading operator (no need for parentheses).

Note that threading redirection is currently only supported on the top level, not in nested threading macros.

```bash
$ printf "http://example.com/some/path" | \
  cq -i text -- '-> str/upper-case (str/split #"/") ->> (map str/reverse)'
(":PTTH" "" "MOC.ELPMAXE" "EMOS" "HTAP")
```

Currently supported threading operators for redirection:

- `->` thread first
- `->>` thread last
- `some->` thread some
- `some->>` thread some last
- `as->` thread with var name

### Included Libraries & Namespace Aliases

| Library | Namespace | Alias | Example Query |
| ------- | --------- | ----- | ------- |
| `tolitius/xml-in` | `xml-in.core` | `xml-in` | `#\| (xml-in/find-all . [:universe :system :solar :planet])` |
| `medley` | `medley.core` | `m` | `(m/mak-kv (fn [k v] [v k]))` |
| `com.rpl/specter` | `com.rpl.specter` | `s` | `(s/transform [MAP-VALS MAP-VALS] inc)` |
| `camel-snake-kebab` | `camel-snake-kebab.core` | `csk`  | `csk/->SCREAMING_SNAKE_CASE` |
| `clojure.instant` | `clojure.instant` | `inst`  | `inst/read-instant-timestamp` |
| `clojure.data.csv` | `clojure.data.csv` | `csv`  | `csv/read-csv` |
| `clojure.data.json` | `clojure.data.json` | `json`  | `json/read-str` |
| `clojure.data.xml` | `clojure.data.xml` | `xml`  | `xml/parse-str` |


### Reader Macros

This table explains the different reader macros provided by `cq`.
`<f>` is the form passed in after the reader macro.

| Reader Macro | Description | Interpolates to | Example |
| ------------ | ----------- | --------------- | ------- |
| `#\| <f>` | Hash-Pipe 🍁: Use the current value as `.` | `((fn [.] <f>))` | `#\| (< 5 . 10)` |
| `#map <f>` | Map elements of a seq | `(map (fn [.] <f>))` | `#map {:a (:a .) :first-child (-> . :children first)}` |
| `#& (<d> <f...>)` | Destructor: Destructure into vars | `((fn [.] (let [<d> .] <f>)` | `#& ({:keys [a b c]} [a b c]})` |
| `#f <f>` | Anonymous function, returns value of f, not evaluation of f | `#(do <f>)` | `(map-kv #f [%2 %1])` |

### Tips & Tricks

#### cq is slow!

Pretty printing can be pretty slow with the JSON and EDN libraries we use. one trick is to use `cq` for querying and `jq` for formatting.

E.g. this is pretty fast

    cat data.yaml | cq -o json --no-pretty | jq

## TODO

- maybe Parquet support

## Acknowledgements

This project takes a lot of inspiration from [`jq`](https://stedolan.github.io/jq/), [`jet`](https://github.com/borkdude/jet) and [`babashka`](https://github.com/babashka/babashka)


## License

This project is licensed under the [EPL-2.0 License](LICENSE).

[![FOSSA Status](https://app.fossa.com/api/projects/git%2Bgithub.com%2Fmarkus-wa%2Fcq.svg?type=large)](https://app.fossa.com/projects/git%2Bgithub.com%2Fmarkus-wa%2Fcq?ref=badge_large)
