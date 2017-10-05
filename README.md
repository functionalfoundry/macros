# workfloapp/macros

[![Clojars Project](https://img.shields.io/clojars/v/workflo/macros.svg)](https://clojars.org/workflo/macros)
[![Build Status](https://travis-ci.org/workfloapp/macros.svg?branch=master)](https://travis-ci.org/workfloapp/macros)

[Examples](https://github.com/workfloapp/macros/tree/master/examples) |
[API documentation](https://workfloapp.github.io/macros/) |
[Changes](CHANGELOG.md)

A collection of Clojure and ClojureScript macros (and related utilities)
for web and mobile development. The main goal of these macros is to
provide all main, high-level building blocks of an application:

* Data (`defentity`)
* Permissions (`defpermission`)
* Services (`defservice`)
* Commands / Actions (`defcommand`)
* Views (`defview`)
* Screens (`defscreen`)

How the building blocks created using these macros are combined into
working applications is left open. All macros provide hooks to make
this as easy as possible.

## License

`workfloapp/macros` is copyright (C) 2016-2017 Workflo. Licensed under
the MIT License. For more information [see the LICENSE file](LICENSE).
