# Entities (defentity)

Define the types of data in your system, with support for validation using `clojure.spec`, relationships and authorization.

## Usage

### General structure

```clojure
(require '[workflo.macros.entity :refer [defentity]])

(defentity <name>
  <description> ; Optional
  <hints>       ; Optional
  <spec>        ; Required
  <auth-query>  ; Optional
  <auth>        ; Optional
  )
```

### Simple example

```clojure
(require '[clojure.spec.alpha :as s])
(require '[workflo.macros.specs.types :as t])
(require '[workflo.macros.entity :refer [defentity]])

;;;; Specs for user attributes

(s/def :user/name ::t/string)
(s/def :user/email ::t/string)
(s/def :user/friends (t/entity-ref 'user :many? true))
(s/def :user/todos (t/entity-ref 'todo :many? true))

;;;; User entity

(defentity user
  "A user in a multi-user todo app"
  (spec
    (s/keys :req [:workflo/id
                  :user/name
                  :user/email]
            :opt [:user/friends
                  :user/todos]))

;;;; Specs for todo attributes

(s/def :todo/text ::t/string)
(s/def :todo/done? ::t/boolean)
(s/def :todo/complexity (s/and ::t/enum
                               {:todo.complexity/easy
                                :todo.complexity/medium
                                :todo.complexity/hard}))

;;;; Todo entity

(defentity todo
  "A todo item in a multi-user todo app"
  (spec
    (s/keys :req [:workflo/id
                  :todo/text
                  :todo/done?
                  :todo/complexity])))
```

## Documentation

TODO