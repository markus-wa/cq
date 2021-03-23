# cq (Clojure Query)

Command-line Data Processor for JSON, EDN and other data formats.

The joy of Clojure's [threading macros](https://clojure.org/guides/threading_macros), but on the command line!

## Rationale

While there are a few similar tools out there, such as jq, jet and babashka, these all have one or more reasons making them less suitable for command-line data processing.

cq aims to:

- Not require learning yet another query language - it's just ~~data~~ Clojure!
- Give the user complete power for writing queries, with as few limitations as possible
- Provide various input and output formats out of the box

## Features

- Comes with [Specter](https://github.com/redplanetlabs/specter) for transforming nested data structures
- Various [reader macros](#reader-macros) that make writing queries easier
- [Threading macro redirection](#threading-macro-redirection) reduces need for parentheses
- Supports all elements of Clojure that are supported by [SCI](https://github.com/borkdude/sci)
- No startup lag thanks to GraalVM native-images

## Usage

```
$ cq --help
cq is a Clojure command-line data processor for JSON, EDN and other data formats.

Usage: cq [options] QUERY

Options:
  -i, --in FORMAT    json  Input format: edn, json, lines, msgpack, text
  -o, --out FORMAT   edn   Output format: edn, json, lines, msgpack, text
  -p, --[no-]pretty        Pretty print output - default is true
  -k, --key-fn             Function used to transform keys
  -h, --help

See https://github.com/markus-wa/cq for more information.
```

By default `cq` uses [thread last (`->>`)](https://clojure.org/guides/threading_macros#thread-last) semantics.

```bash
$ echo '{"a": {"b": [1, 2, 3]}}' | cq ':a :b (map inc)'
(2 3 4)
```

Using `#|` you can use the current value as `.`.

```bash
$ curl -s 'https://api.github.com/repos/stedolan/jq/commits?per_page=5' | \
  cq 'first #| {:message (-> . :commit :message) :committer (-> . :commit :committer :name) :parents (->> . :parents (map :html_url))}'
{:message "Fix #2197 extended regex pattern example",
 :committer "William Langford",
 :parents
 ("https://github.com/stedolan/jq/commit/a17dd3248a666d01be75f6b16be37e80e20b0954")}
```

### Threading Macro Redirection

While things like [`->->><?as->cond->!`](https://github.com/randomcorp/thread-first-thread-last-backwards-question-mark-as-arrow-cond-arrow-bang) are pretty funny,
it can be pretty convenient to just redirect a threading macro when you're working on a simple terminal without paredit.

All threading operators will change the query after that point to their implementation until followed by any other threading operator (no need for parentheses).

Note that threading redirection is currently only supported on the top level, not in nested threading macros.

```bash
$ printf "http://example.com/some/path" | cq -i text -- '-> str/upper-case (str/split #"/") ->> (map str/reverse)'
(":PTTH" "" "MOC.ELPMAXE" "EMOS" "HTAP")
```

Currently supported threading operators for redirection:

- `->` thread first
- `->>` thread last

... much more to come!

### Reader Macros

This table explains the different reader macros provided by `cq`.
`<f>` is the form passed in after the reader macro.

| Reader Macro | Description | Interpolates to | Example |
| ------------ | ----------- | --------------- | ------- |
| `#\| <f>`  | Use the current value as `.` | `((fn [.] <f>))` | `#\| (< 5 . 10)` |
| `#map <f>` | Map elements of a seq | `(map (fn [.] <f>))` | `#map {:a (:a .) :first-child (-> . :children first)}` |

## TODO

- CSV support
- YAML support
- Transit support
- maybe [XML](https://github.com/tolitius/xml-in), [HTML](https://github.com/davidsantiago/hickory) & Parquet support

## Acknowledgements

This project takes a lot of inspiration from [`jq`](https://stedolan.github.io/jq/), [`jet`](https://github.com/borkdude/jet) and [`babashka`](https://github.com/babashka/babashka)
