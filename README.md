# cq (Clojure Query)

Command-line Data Processor for JSON, EDN and other data formats.

Like Clojure's [threading macros](https://clojure.org/guides/threading_macros), but on the command line!

## Features

- Comes with [Specter](https://github.com/redplanetlabs/specter) for transforming nested data structures
- Various reader macros that make writing queries easier
- Supports all elements of Clojure supported by [SCI](https://github.com/borkdude/sci)
- Fast startup thanks to GraalVM native-images

## Usage

```
$ cq --help
cq is a Clojure command-line data processor for JSON, EDN and other data formats.

Usage: cq [options] QUERY

Options:
  -i, --in FORMAT    json  Input format: edn, json, msgpack
  -o, --out FORMAT   edn   Output format: edn, json, msgpack
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

... much more to come!

## TODO

- complete [specter](https://github.com/redplanetlabs/specter) support

## Acknowledgements

This project takes a lot of inspiration from [`jq`](https://stedolan.github.io/jq/), [`jet`](https://github.com/borkdude/jet) and [`babashka`](https://github.com/babashka/babashka)
