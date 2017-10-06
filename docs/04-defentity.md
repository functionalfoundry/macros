# Entities (defentity)

Entities in `workflo/macros` define the types of data in your system (e.g.
users, accounts, articles). The following list sums up the key features of
entities:

* **Specs** — entities are fully specified using `clojure.spec`
* **Relationships** — relationships between entities can easily be defined.
  This includes one-to-one, one-to-many and many-to-many relations
* **Entity registry** — entities defined with `defentity` are stored
  in a global registry, from where they can be looked up at any time
* **Schema generation** — schemas for popular Clojure(Script) databases
  such as Datomic and DataScript can be derived from entities easily
* **Authorization** — `defentity` comes with built-in support for
  authorization to control access to entities in your system
* **Hints** — entities can be tagged with arbitrary hints to allow
  special-casing entity data in the system (e.g. prevent certain entities
  from being persisted)

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

## API documentation

The following is a list of pointers to the namespaces that are related to
entities:

* [workflo.macros.entity](workflo.macros.entity.html)
    - The `defentity` macro
    - Entity registry
    - Entity references lookup
    - Entity validation
    - Entity authorization
* [workflo.macros.entity.datascript](workflo.macros.entity.datascript.html)
    - DataScript schema generation from registered entities
* [workflo.macros.entity.datomic](workflo.macros.entity.datomic.html)
    - Datomic schema generation from registered entities
* [workflo.macros.entity.refs](workflo.macros.entity.refs.html)
    - Internal registry of references between entities
* [workflo.macros.entity.schema](workflo.macros.entity.schema.html)
    - Intermediate schema representation for entities (from which
      schemas for any database can be derived)
* [workflo.macros.specs.entity](workflo.macros.specs.entity.html)
    - Specs for `defentity` arguments