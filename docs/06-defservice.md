# Services (defservice)

In `workflo/macros`, services are components in the system that data (events,
instructions or any other data) can be delivered to for processing. Unlike
the reducer-flavored [commands](05-defcommand.md), services are (or can be)
stateful and are expected to (but not required to) generate side-effects
as part of their processing.

Like commands, services can—as part of their processing—emit data for the
system to act on further.

In a *(Screens-)Views-Commands-Services* architecture, screens might render
views, views might trigger commands, commands might emit to services
and services might emit side-effects and updates to the app state so that the
screens/views update.

Some key features of services include:

* **Service components** — `defservice` automatically creates
  [components](https://github.com/stuartsierra/component) for each service,
  allowing services to integrate seeminglessly in most Clojure systems;
  what is more, all running instances of service components are
  stored in a global registry so that they can be looked up by the
  service name (e.g. for data deliveries)
* **Dependencies** — services can declare that they depend on other
  services; this can be used by frameworks such as
  [system](https://github.com/danielsz/system) to start service
  components in the right order and inject depenencies into dependent
  service components.
* **Service queries** — like [commands](05-defcommand.md), services support
  a query to fetch additional information before processing incoming data
* **Service registry** — services defined with `defservice` are stored
  in a global registry, from where they can be looked up at any time using
  their name
* **Multi-service dispatching** — data in the form of a collection
  of `[SERVICE_NAME DATA]` (e.g. a key/value map or vector of tuples)
  can be delivered to multiple services at once by dispatching on
  the service names in the tuples; this is a handy feature when forwarding
  data emitted from [commands](05-defcommand.md) to services
* **Delayed and debounced delivery** — services support hints that
  allow to delay deliveries by a certain time or to debounce deliveries
  (either all per service or based on the contents of the delivered data)

## Usage

### General structure

```clojure
(defservice <name>
  "description"        ; Optional
  (hints [...])        ; Optional
  (dependencies [...]) ; Optional
  (query ...)          ; Optional
  (spec ...)           ; Optional (spec for delivery data)
  (start ...)          ; Optional (executed when the service component starts)
  (stop ...)           ; Optional (executed when the service component stops)
  (process ...)        ; Optional (handles data deliveries)
  )
```

### Simple example

```clojure
(require '[clojure.spec.alpha :as s]
         '[com.stuartsierra.component :as component]
         '[workflo.macros.service :as services :refer [defservice]])


;; Specs for delivery data

(s/def ::record (s/keys :req [:db/id]))

(s/def ::create-operation (s/tuple #{:create} ::record))
(s/def ::update-operation (s/tuple #{:update} ::record))
(s/def ::delete-operation (s/tuple #{:delete} ::record))

(s/def ::operations
  (s/or :create ::create-operation
        :update ::update-operation
        :delete ::delete-operation))


;; DB service

(defservice db
  "A very simple database service."
  (spec
    (s/coll-of ::operations :kind vector?))
  (start
    (println "Starting db")
    (assoc this :store (atom {})))
  (stop
    (println "Stopping db")
    (dissoc this :store))
  (process
    (doseq [[op op-data] data]
      (let [id (get op-data :db/id)]
      (case op
        :create (swap! (get this :store) assoc id op-data)
        :update (swap! (get this :store) update id merge op-data)
        :delete (swap! (get this :store) dissoc id))))))


;; Create and start a DB service component

(-> (services/new-service-component 'db {:optional :config})
    (component/start))


;; Deliver some data

(services/deliver-to-services!
  {:db [[:create {:db/id 1 :user/name "John"}]
        [:create {:db/id 2 :user/name "Linda"}]
        [:update {:db/id 2 :user/email "linda@email.com"}]
        [:delete {:db/id 1}]]})


;; Obtain the service component and print its store

(-> (services/resolve-service-component 'db)
    (get :store)
    (deref)
    (println))

;; -> {2 {:db/id 2 :user/name "Linda" :user/email "linda@email.com"}}
```

## API documentation

The following is a list of pointers to namespaces that are related to services:

* [workflo.macros.service](workflo.macros.service.html)
    - The `defservice` macro
    - Service registry
    - Service component creation and registry
    - Service delivery
    - Service hooks
* [workflo.macros.specs.service](workflo.macros.specs.service.html)
    - Specs for `defservice` arguments
