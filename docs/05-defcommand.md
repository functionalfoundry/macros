# Commands (defcommand)

Commands represent actions that can be performed in a system, either
triggered by the user or by any other part of the system.

A command takes in input data (the payload) and optionally asks the
system to run a query to fetch additional data required for it to run.
It then executes code and eventually emits data, which typically would
be delivered to registered services (see
[Services](06-defservice.html) for more information on those.

Commands are defined and registered with the `defcommand` macro. Some
of the key features of commands are:

* **Interface similar to reducers** — commands are designed to encourage
  concise, side-effect-free, self-contained code; see the section about
  the reducing nature of commands below
* **Queries, parameterized using command input data** — every command
  has an optional [query](02-queries.html) to fetch additional data;
  values of query parameters are resolved using the data passed to
  the command
* **Local bindings for query results** — query results are bound to
  names in the command execution scope automatically
* **Authorization** — like with [entities](04-defentity.html), support for
  authorization comes built-in through an auth query and an auth function
* **Command registry** — all commands are stored in a registry from
  which they can be looked up again by their names at any time

## Commands are like reducers

Commands can be thought of as being similar to reducers of the
form

```elm
Reducer :: (State, Data) -> State
```

with their query and output data added into the mix as follows:

```elm
BindQuery      :: UnboundQuery -> Data -> BoundQuery

-- In reality, how queries are executed is entirely up to you
RunQuery       :: State -> BoundQuery -> QueryResults

-- Command implementations are, ideally, free of side-effects
Implementation :: QueryResults -> Data -> EmitData

-- Rough outline of how commands are executed
RunCommand     :: State -> UnboundQuery -> Data -> Implementation -> EmitData
RunCommand state, unboundQuery, data, implementation =
  implementation (RunQuery state (BindQuery unboundQuery data)) data
```

## Usage

### General structure

```clojure
(require '[workflo.macros.command :refer [defcommand]])

(defcommand <name>
  "description"     ; Optional
  (hints [...])     ; Optional
  (spec ...)        ; Optional (clojure.spec for the input data)
  (query ...)       ; Optional
  (auth-query ...)  ; Optional
  (auth ...)        ; Optional
  ...               ; Optional (arbitrary (foo ...) forms)
  (emit ...)        ; Required (the implementation of the code)
  )
```

### Simple example

```clojure
(require '[clojure.spec.alpha :as s]
         '[workflo.macros.command :as commands :refer [defcommand]]
         '[workflo.macros.entity :refer [defentity]]
         '[workflo.macros.specs.types :as types])

        
;; User entity (this establishes specs for a user's attributes)

(s/def :user/name ::types/string)
(s/def :user/email ::types/string)

(defentity user
  (spec
    (s/keys :req [:workflo/id
                  :user/name
                  :user/email])))


;; Specs for the command input data

(s/def :create-user/user (get user :spec)) ; Reuse the user spec here
(s/def :create-user/timestamp ::types/instant)


;; Implementation of a command to create new users

(defcommand create-user
  "Creates a user and sends them a welcome email."
  (spec
    ;; The command expects a user and a timestamp
    (s/keys :req-un [:create-user/user
                     :create-user/timestamp]))
  (query
    ;; Query all existing users with their emails
    [{users [user [email]]} :as existing-users])
  (emit
    (let [;; Extract the new user from the input data
          new-user          (get data :user)
          ;; Define a predicate for checking whether the user already exists
          matches-new-user? (fn [existing-user]
                              (= (get existing-user :user/email) 
                                 (get new-user :user/email)))]
      (if (some matches-new-user? existing-user)
        ;; Emit nothing if the user exists
        {}
        ;; Otherwise, create the user in the database and send an email
        ;; What you emit here is entirely up to you
        {:db [[:create new-user]]
         :email [{:from "Great App <info@great-app.com>"
                  :to (get new-user :user/email)
                  :subject (str "Welcome to Great App, "
                                (get new-user :user/email)}]})))))


;; Run the command

(commands/run-command! 'create-user
                       {:user {:workflo/id "..."
                               :user/name "John"
                               :user/email "john@doe.org"}
                        :timestamp (java.util.Date.now.)})

;; -> {:db [[:create {:workflo/id "..."
;;                    :user/name "John"
;;                    :user/email "john@doe.org"}]]
;;     :email [{:from "Great App <info@great-app.com>"
;;              :to "john@doe.org"
;;              :subject (str "Welcome to Great App, John")}]}


;; Run the command again

(commands/run-command! 'create-user
                       {:user {:workflo/id "..."
                               :user/name "John"
                               :user/email "john@doe.org"}
                        :timestamp (java.util.Date.now.)})

;; -> {}
```

## API documentation

The following is a list of pointers to namespaces related to commands:

* [workflo.macros.command](workflo.macros.command.html)
    - The `defcommand` macro
    - Command execution
    - Command registry
    - Command hooks
* [workflo.macros.command.util](workflo.macros.command.util.html)
    - Internal utilities for `defcommand`
* [workflo.macros.specs.command](workflo.macros.specs.command.html)
    - Specs for `defcommand` arguments