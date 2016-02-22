# App Macros

A collection of Clojure and ClojureScript macros for web and mobile
development.

```clojure
[workflo/app-macros "0.1.0"]
```

## `defview` - Define Om Next components in a compact way

Om Next views are a combination of an Om Next component defined
with `om.next/defui` and a component factory created with
`om.next/factory`. The `defview` macro combines these two into
a single definition and reduces the boilerplate code needed to
define properties, queries and component functions.

### Defining views

Views are defined using `defview`, accepting the following
information:

* A destructuring form for properties (optional)
* A destructuring form for computed properties (optional)
* A `key` or `keyfn` function (optional)
* A `validate` or `validator` function (optional)
* An arbitrary number of regular Om Next, React or JS
  functions (e.g. `query`, `ident`, `componentWillMount`
  or `render`).

A `(defview UserProfile ...)` expression defines both a
`UserProfile` component and a `user-profile` component
factory.

### Properties destructuring

All properties declared via the destructuring forms are
made available in component functions via an implicit
`let` statement wrapping the original function body in
the view definition.

As an example, the properties destructuring form

```clojure
[user [name email {friends ...}] [current-user _]]
```

would destructure the properties as follows:

```clojure
:user/name    -> name
:user/email   -> email
:user/friends -> friends
:current-user -> current-user
```

### Automatic Om Next query generation

The `defview` macro predefines `(query ...)` for any view based
on the properties destructuring form. Auto-generation currently
supports joins and links but not unions and parameterization.

As an example, the properties destructuring form

```clojure
[user [name email {friends User}] [current-user _]]
```

would generate the following `query` function:

```clojure
static om.next/IQuery
(query [this]
  [:user/name
   :user/email
   {:user/friends (om/get-query User)}
   [:current-user _]])
```

This can be overriden simply by implementing your own `query`:

```clojure
(defview User
  [...]
  (query
    [:name :email]))
```

In the future, we will likely add a simple way to transform
auto-generated queries. On idea is to implicitly bind the
auto-generated query when overriding `query` and providing
convenient methods to parameterize sub-queries, e.g.

```clojure
(query
  (-> auto-query
      (set-param :user/friends :param :value)
      (set-param :current-user :id [:user 15])))
```

### Implicit binding of `this` and `props` in functions

The names `this` and `props` are available inside
function bodies depending on their signature (e.g. `render`
only makes `this` available, whereas `ident` pre-binds `this`
and `props`).

### Example

```clojure
(ns foo.bar
  (:require [app-macros.view :refer-macros [defview]]))

(defview User
  [user [name email address {friends ...}]]
  [ui [selected?] select-fn]
  (key name)
  (validate (string? name))
  (ident [:user/by-name name])
  (render
    (dom/div #js {:className (when selected? "selected")
                  :onClick (select-fn (om/get-ident this))}
      (dom/h1 nil "User: " name)
      (dom/p nil "Address: " street " " house))))
```

Example usage in Om Next:

```clojure
(user (om/computed {:user/name "Jeff"
                    :user/email "jeff@jeff.org"
                    :user/address {:street "Elmstreet"
                                   :house 13}}
                   {:ui/selected? true
                    :select-fn #(js/alert "Selected!")))
```

### `defview` internals

Most of the internals of the `defview` macro are available as
separate functions for easy reuse:

```clojure
(require '[app-macros.props :as p]
         '[app-macros.view :as v])

;; Utilities

(p/pad-by = nil [0 1 1 1 2 3])    -> [0 1 nil 1 nil 1 2 3]
(p/property-type 'foo)            -> :property
(p/property-type '{foo Bar})      -> :join
(p/property-type '[foo 13])       -> :link
(p/property-name 'foo '{bar Baz}) -> foo/bar

;; Parser for properties destructuring forms

(p/parse '[user [name email {friend ...}] [current-user _]])
-> [{:name user/name :type :property}
    {:name user/email :type :property}
    {:name user/friend :type :join :target ...}
    {:name current-user :type :link :target _}]

;; Property queries based on parsed property forms

(p/property-query '{:name foo :type :property})
-> :foo

(p/property-query '{:name foo/bar :type :join :target Foo})
-> {:foo/bar (om/get-query Foo)}

(p/property-query '{:name bar :type :join :target ...})
-> {:bar ...}

(p/property-query '{:name bar :type :join :target 5})
-> {:bar 5}

(p/property-query '[current-user _])
-> [:current-user _]

;; Om Next query generation

(p/om-query '[{:name user/name :type :property}
              {:name user/email :type :property}
              {:name user/friend :type :join :target ...}
              {:name current-user :type :link :target _}])
-> [:user/name
    :user/email
    {:user/friend ...}
    [:current-user _]]
```

## License

App Macros is copyright (C) 2016 Workflo. Licensed under the
MIT License. For more information [see the LICENSE file](LICENSE).
