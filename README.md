# Workflo Macros

A collection of Clojure and ClojureScript macros for web and mobile
development.

```clojure
[workflo/macros "0.2.7"]
```

[CHANGELOG](CHANGELOG.md)

## `defview` - Defining Om Next components in a compact way

Om Next views are a combination of an Om Next component defined
with `om.next/defui` and a component factory created with
`om.next/factory`. The `defview` macro combines these two into
a single definition and reduces the boilerplate code needed to
define properties, idents, React keys, queries and component
functions.

[Documentation for `defview`](docs/defview.md)

## `defcommand` - Defining pure commands with queries and input data specs

The `defcommand` macro allows to define pure functions that represent
named commands in a system. Commands defined with this macro include
an optional query, e.g. to retrieve data from a database, and a
mandatory `clojure.spec` spec for the command input data. The query
results (if there are any) and the validated command data are the only
inputs passed to the command implementation at run-time.

[Documentation for `defcommand`](docs/defcommand.md)


## License

Workflo Macros is copyright (C) 2016 Workflo. Licensed under the
MIT License. For more information [see the LICENSE file](LICENSE).
