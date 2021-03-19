# cq (Clojure Query)

Command-line Data Processor for JSON (and soon more data formats).

Like Clojure's [threading macros](https://clojure.org/guides/threading_macros), but on the command line!

## Usage

By default `cq` uses [thread last (`->>`)](https://clojure.org/guides/threading_macros#thread-last) semantics.

```
$ echo '{"a": {"b": [1, 2, 3]}}' | cq ':a :b (map inc)'
[2,3,4]
```

... much more to come!
