# App Macros

```
[workflo/app-macros "0.1.0-SNAPSHOT"]
```

A collection of Clojure and ClojureScript macros for web and mobile
development.

## `defview` - Define Om Next components in a compact way

Views are defined using `defview`, accepting the following information:

* A destructuring form for properties (optional)
* A destructuring form for computed properties (optional)
* A `keyfn` function (optional)
* A `validator` function (optional)
* An arbitrary number of regular Om Next, React or JS
  functions (e.g. `query`, `ident`, `componentWillMount`
  or `render`)

Example:

```
(defview User
  [user [name email address]]
  [ui [selected?] select-fn]
  (keyfn [_] name)
  (validator [_] (string? name))
  (ident [this]
    [:user/by-name name])
  (render [this]
    (dom/div #js {:className (when selected? "selected")
                  :onClick (select-fn (om/get-ident this))}
      (dom/h1 nil "User: " name)
      (dom/p nil "Address: " street " " house))))
```

Example usage in Om Next:

```
(user (om/computed {:user/name "Jeff"
                    :user/email "jeff@jeff.org"
                    :user/address {:street "Elmstreet"
                                   :house 13}}
                   {:ui/selected? true
                    :select-fn #(js/alert "Selected!")))
```

## License

App Macros is copyright (C) 2016 Workflo. Licensed under the
MIT License. For more information [see the LICENSE file](LICENSE).
