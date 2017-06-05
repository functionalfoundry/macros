# CHANGELOG workflo/macros

## 0.2.56

### Added

* Add a `:debounced-by-data` hint for debouncing service deliveries
  with the same data/payload.

## 0.2.55

### Changed

* Replace `layout` form in `defscreen` macro with a form called
  `sections'.

## 0.2.54

### Added

* Add `:before` and `:after` hooks to `defcommand`.

### Changed

* Pass more information (such as the command name, context etc.) to
  command hooks.

## 0.2.53

### Added

* Add a `(hints [<kw> ...])` form to `defservice`. Just like the hints
  forms for `defentity` and `defcommand`, this can be used to tag services
  with arbitrary hints.
* Add support for a built-in `:async` service hint. Data delivered to
  async services is delayed by a configurable `:async-delay` time (defaults
  to 500ms).

## 0.2.52

### Fixed

* Also fix the ClojureScript implementation of `entity-for-data`.
* Make sure to exclude `:db/id` in `entity-for-data`, to avoid guessing
  entities based on common attributes.

## 0.2.51

### Fixed

* Fix `entity-for-data` to return an entity definition instead of
  an attribute. ALso make `entity-for-attr` public.

## 0.2.50

### Added

* Add an `entity-for-data` function to guess an entity definition
  from any map (typically an entity instance), based on the keys in
  the map. Use memoization to make this function fast.

### Changed

* Allow `:deliver` hooks to alter the data delivered to services.

### Fixed

* Update the command running tests to the new command hooks.
* Pass the right query (the auth query) to `:auth-query` command hooks.

## 0.2.49

### Added

* Add `defhooks` macro and use it for services and commands. This allows
  an arbitrary number of hooks of every kind to be registered. A reduce
  is then applied to all of them to allow all hooks to shape the data
  further.

## 0.2.48

### Added

* Add `(hints [<kw> ...])` form to `defentity` and `defcommand`. This allows
  to filter entities and commands by hints, e.g. to log or forward them to
  additional services.

## 0.2.47

### Added

* Add support for Om Next / React lifecycle function signatures in `defview`.

## 0.2.46

### Changed

* Add another parameter to the `:before-emit` hook: the command context.

## 0.2.45

### Changed

* Bumped all dependencies to their latest versions, including Clojure
  and ClojureScript.
* Change syntax to reference query fragments from `:foo` to `...foo`. This
  avoids parsing conflicts and ambiguity. It's also consistent with the
  spreading syntax in JavaScript.

### Fixed

* Fix tests, particularly those involving query and function specs.
* Avoid `clojure.string/starts-with?` in ClojureScript; it is broken in
  PhantomJS. Use `subs` and `re-matches` instead.

## 0.2.44

### Added

* Add a `:before-emit` hook to `defcommand`.

### Changed

* Log a more useful error when failing to resolve service components.

### Fixed

* Fix a typo in the `entity-backrefs` documentation.

## 0.2.43

### Fixed

* Fix quoting of `...` in Om Next query generation. We only want to
  double-quote the `...` if we are generating code for CLJS from
  CLJ (e.g. from within a macro). Otherwise, single-quoting is
  sufficient.

## 0.2.42

### Fixed

* Fix the function spec of `workflo.macros.query.om-next/property-query`.

## 0.2.41

### Added

* Add a query fragment registry in `workflo.macros.query`. This can be
  used to register reusable query fragments.
* Add the support for query fragments in queries. Query fragments can
  be added as keywords (other than `:as`), e.g. `[foo :example bar]`.
  During parsing they are then resolved into the actual queries using
  the fragment registry. The result is then spliced into the parent
  query, resulting in e.g. `[foo baz ruux bar]` if `:example` resolves
  to `[baz ruux]`.
* Add tests for the above.

### Changed

* Revert the `resolve-<registry name>` change made in 0.2.40.

### Fixed

* Add missing `defregistry` require expression for `defpermission`.
* Fix only including the `(auth ...)` form in entities when generating
  for Clojure.

## 0.2.40

### Changed

* Changed the `auth-query` forms of `defentity` and `defcommand`
  so that they require valid, regular queries as their body,
  instead of allowing arbitrary, query-generating code. The latter
  breaks query bindings in other forms of `defentity` and
  `defcommand`.
* Make the `resolve-<registry-name>` function (e.g. `resolve-entity`)
  accept symbols and keywords alike, rather than always assuming
  symbols.

## 0.2.39

### Added

* Extend the query language to allow parameter paths (e.g.
  `[:user/account :db/id]`) to be used in addition to simple
  keywords. This allows to express things such as "query only
  the users that belong to the account with the given ID".

### Changed

* Log the full result of `clojure.spec.test/check` on failure. This
  way we get to see what actually went wrong.
* Require at least one variable in variable paths (e.g. `[?foo]`)
  in query parameter maps.

### Fixed

* Fix random testing of `defcommand*` and `defentity*` by always
  generating valid queries for the `auth-query` form.
* Properly detect when an `auth-query` body is a valid query in
  `defcommand` and `defentity`.

## 0.2.38

### Added

* Add `defpermission` macro to define permission with names, titles
  and optional descriptions.

## 0.2.37

BROKEN RELEASE.

## 0.2.36

### Fixed

* Fix preparing data for binding `defcommand` queries and auth queries.
  The previous approach introduced in 0.2.35 threw an error when the
  command data was not sequential. The new approach puts the command
  data into a map under the `:data` key if it isn't already a map,
  so that queries can bind to it via `?data`.

## 0.2.35

### Added

* Add `auth` and `auth-query` forms to the `defcommand` macro. These
  forms are omitted when generating for ClojureScript; this follows the
  assumption that authorization will be performed server-side and that
  there is no interest in leaking authorization logic into client code.
* Add an `:auth-query` hook to the `defcommand` configuration.
* Add `auth-query` form to the `defentity` macro. This form is omitted
  when generating for ClojureScript.
* Add an `authorized?` function for entities. This function can check
  whether, given an entity definition, env, entity ID and viewer ID,
  the viewer is authorized to access the entity, according to the
  implementation of the `auth` form.

### Changed

* Include authorization logic in `run-command!`k.
* Change the signature and behavior of the `auth` form of `defentity`
  to be aligned with the same form in `defcommand`.
* Change the signature of the `:auth-query` hook for `defentity`.
* Change the order of forms in `defcommand` and `defentity` - first
  `spec`, then `auth-query`, then `auth`.

### Removed

* Remove the `authenticate` function for entities. This is replaced by
  the more aptly named and freshly implemented `authorized?` function.

## 0.2.34

### Added

* Add `:workflo.macros.specs.types/non-persistent` hint for entity
  attributes. This hint can be added to the specs of entity attributes
  in order to indicate that these attributes are not to be persisted.
* Add `non-persistent-key?`  and `non-persistent-keys` functions to
  check whether a keyword corresponds to a non-persistent entity
  attribute and to obtain all non-persistent keys for an entity.

## 0.2.33

### Fixed

* Add missing require statement for `workflo.macros.bind`.

## 0.2.32

### Changed

* Allow commands to return data for services as a sequence or
  vector of tuples instead of a map, to allow them to control the
  order in which the data items are delivered to services.

## 0.2.31

### Added

* Add `:default-params` to the Bidi-based screen router.

## 0.2.30

### Added

* Add `backref-attr?` and `singular-backref-attr?` helpers to
  identify backrefs in queries.
* Add support for binding the results of backref joins in queries
  to the backref namespace symbol.

### Fixed

* Fix derivation of cardinality information in the new global
  entity refmap.

## 0.2.29

### Added

* Add an optional callback to `defregistry` to allow reacting to
  registration and unregistration events.
* Add a global entity refmap to store references (refs) and
  reverse references (backrefs) between registered entitiesk.
  The refmap is automatically updated as entities are registered
  and unregistered.
* Add the functions `entity-refs` and `entity-backrefs` to access
  the refs and backrefs of entities by entity name.

### Changed

* BREAKING: Make `matching-entity-schema` take a map of registered
  entities rather than calling `registered-entities` inside the
  function. This makes it more pure and allows to use the
  `workflo.macros.entity.schema` namespace to be used in
  `workflo.macros.entity`.

## 0.2.28

### Changed

* Allow for fully qualified commands to be bound in views.
  Commands like `ui/foo` or `foo/baz` will be bound to `foo`
  and `baz`, respectively.

## 0.2.27

### Changed

* Allow fully qualified names to be used for entities, commands,
  services etc. Examples include: `foo.bar`, `foo.bar/baz`.
* Simplify binding names like `foo.bar` to `bar` in the binding
  utilities that we use to bind query results.

## 0.2.26

### Added

* Add a `:process-output` hook to `defservice` to allow
  further processing of the data returned by services from
  their `process` implementations.

## 0.2.25

### Added

* Add a `(replay? <bool>)` form to `defservice`. This can be
  used in command/event sourced systems to mark services to be
  skipped when replaying commands/events.

## 0.2.24

### Added

* Add an optional `context` parameter to `run-commands!`,
  `deliver-to-services!` and `deliver-to-service-component!`
  that is then passed on to the `:query` and `:process-emit`
  hooks as well as the `process` implementation of services.

### Fixed

* Fix not querying for the `:db/id` in a view in the
  screen-based example app.

## 0.2.23

### Added

* Add a `merge-schemas` function to merge DataScript schemas
  and detect conflicting attribute schemas.

## 0.2.22

### Changed

* Update all dependencies to their latest versions.

### Fixed

* Always refer to devcard macros with `:refer-macros`.

## 0.2.21

### Added

* Add support for generating Datomic schemas from entities
  with attributes that have mixed namespaces.
* Add a `merge-schemas` function to merge Datomic schemas.

## 0.2.20

### Fixed

* Fix availability of our macros in ClojureScript by requiring
  them with :require-macros.

### Changed

* Use `:refer` instead of `:refer-macros` in ClojureScript
  wherever we can.

## 0.2.19

### Fixed

* Only build snapshot/release JARs once when deploying them
  to Clojars.

## 0.2.18

### Fixed

* Fix alias for `camelize-keys` in `workflo.macros.jscomponents`.

## 0.2.17

### Added

* Add a `camelize-keys` function to recursively convert keys in
  a map to camel-case.

### Changed

* Change `defjscomponents`, so the keys in props are converted
  to camel-case using `camelize-keys` before they are passed to
  the JS components.

## 0.2.16

### Added

* Add an `entity-refs` spec that can be used to define entities
  or entity keys that refer to one or many instances of another
  entity.
* Add `entity-refs` function to extract references of an entity
  to other entities.
* Add `keys` and `optional-keys` functions to obtain all keys
  and only the optional keys of an entity.

### Changed

* Move `val-after` function into `workflo.macros.util.misc`.
* Rename `:cmd-params` to `:cmd-data` in Om Next mutation
  payload generated by the default `:run-command` hook for
  `defview`.

## 0.2.15

### Added

* Add a `with-query-bindings` macro to automatically and deeply
  destructure query results with the help of the query they
  correspond to. Use this to wrap all function forms in `defview`,
  `defcommand` and `defservice` in order to bind as many parts
  of the query results to short names from the query itself as
  possible.
* Add documentation generation using Codox `via `boot docs`.
* Add a `boot production` task to be able to test examples with
  CLJS advanced optimizations.
* Add support for CLJS REPLs in Cider.

### Changed

* Rewrite the query language and its specs to add support for
  aliases (`:as`).
* Rewrite query parsing from scratch to support the updated
  query langauge and aliases in particular.
* Bump Clojure and ClojureScript to 1.9.0-alpha11 and 1.9.229,
  respectively; update all specs and tests accordingly and simplify
  (:require ...) in ns expressions.

### Removed

* Remove now unused `with-destructured-query` macro.

### Fixed

* Fix the way `defjscomponents` resolves JS components to make it
  work with advanced optimizations.
* Don't return nil from `workflo.macros.util.string/camel->kebab`
  when processing non-camel-case strings.

## 0.2.14

### Added

* Add support for typed URL segments in screens, with the help of Bidi.
* Add `(spec ...)` form to `defentity`.
* Add specs for various fundamental types for entities and entity
  keys in `workflo.macros.specs.types`.
* Add specs for type options (mostly for Datomic) in
  `workflo.macros.specs.types`.
* Add functions to extract schemas from entities in
  `workflo.macros.entity.schema`.
* Add Datomic schema generation from entities in
  `workflo.macros.entity.datomic`.
* Add DataScript schema generation from entities in
  `workflo.macros.entity.datascript`.

### Changed

* Only include examples when developing, not when running the
  tests. This ensures specs from examples and tests don't clash.
* Generate the initial :screen-mounted call before updating the
  root component query.

### Removed

* Remove `(validation ...)` form in `defentity`.
* Remove `(schema ...)` form in `defentity`.

### Fixed

* Fix invalid namespace forms in `workflo.macros.config` and
  `workflo.macros.registry`.
* Fix generator of `:workflo.macros.specs.view/view-form-args`
  spec to always generate valid view form arguments.

## 0.2.13

### Changed

* Allow arbitrary parameters to be passed to view commands,
  not just maps.

## 0.2.12

### Fixed

* Fixed Om Next query generation, particularly in
  ClojureScript.

## 0.2.11

### Changed

* `defview` now takes an optional `(commands [cmd ...])` form
  to make arbitrary external commands available in each view
  function; it comes with a `:run-command` hook that allows
  to handle these commands; this can then be used to transact
  Om Next mutations or call arbitrary functions.
* The syntax and names of forms in the macros was changed
  in various incompatible ways, e.g. the query and data spec
  vector in `(defcommand ... [<query> <spec>] ...)` were changed
  to `(defcommand ... (query ...) (spec ...))` forms.
* The `:process-result` hook of `defcommand` has been renamed
  to `:process-emit` and the implementation form of `defcommand`
  has been moved into `(emit <implementation>)`.
* The Om Next query generation for ClojureScript was improved
  and fixed, especially parameterized queries.

### Added

* `defentity` macro for bundle system entities with
  schemas/validation specs and authorization.
* `defscreen` macro for defining screens of ClojureScript
  apps with URL patterns, navigation information and views
  for different layout segments.
* A screen-based router and application layouer that works
  with Om Next, including a demo app (`screen-app.html`).
* `defservice` macro to define services that can be used to
  consume and process data emitted from commands written with
  `defcommand`. `defservice` generates a com.stuartsierra.component
  component for each service to allow straight forward integration
  of services written with `defservice` into systems built using
  `org.danielsz/system`.

## 0.2.10

### Changed

* Queries now support joins in combination with links, that is,
  links that are joined with models, properties or recursion, e.g.:
  `{[current-user _] [db [id] user [name email]]}`.
* The spec for parsed joins has changed to include an additional
  `:join-source` key that holds either a regular parsed property
  or a parsed link property. One example:
  `{:name current-user :type :join
    :join-source {:name current-user :type :link :link-id _}
    :join-target [{:name db/id :type :property}
                  {:name user/name :type :property}]}`.

### Fixed

* Generate '_ for global links in Om Next queries, not ''_.
* The CLJS generator for the `::map-destructuring-keys` spec now works.

## 0.2.9

### Added

* New functions to resolve variables and variable paths using
  potentially deeply nested parameter/value maps:
    - `workflo.macros.query.bind/var?`
    - `workflo.macros.query.bind/path?`
    - `workflo.macros.query.bind/denamespace`
    - `workflo.macros.query.bind/denamespace-keys`
    - `workflo.macros.query.bind/resolve-var`
    - `workflo.macros.query.bind/resolve-path`
    - `workflo.macros.query.bind/resolve`

### Fixed

* When binding query parameters, only recurse into join targets
  if they are real subqueries.

## 0.2.8

### Added

* Function to bind query parameters using a parameter/value
  map: `workflo.macros.query/bind-query-parameters`.
* A new syntax for binding query parameters to deeply
  nested values in the parameter map: `{param/name [?foo ?bar]}`
  in addition to `{param/name ?var}`.
* Bind defcommand query parameters using the command data
  as a parameter/value map.

## 0.2.7

### Changed

* Change notation for defcommand implementation forms by no
  longe requiring a sequence/list expression.

## 0.2.6

### Added

* A new defcommand macro for defining commands (e.g. handlers for
  Om Next mutations or other CQRS-style commands) with a store
  query, a command data spec and an implementation function.
* Data format specs:
    - `:workflo.macros.command.util/unqualified-symbol` - spec
      for a symbol without a namespace component.
    - `:workflo.macros.specs.query/query` - the data format for
      queries as used in the `defview` macro.
    - `:workflo.macros.specs.conforming-query/query` - the data
      format resulting from parsing queries with
      `clojure.spec/conform`.
    - `:workflo.macros.specs.parsed-query/query` - the data format
      generated by parsing queries with `workflo.macros.query/parse`.
    - `:workflo.macros.specs.om-query/query` - the data format for
      Om Next queries, as generated by `workflo.macros.query/om-query`.
* Function specs for the following functions:
    - `workflo.macros.query.util/combine-properties-and-groups`
    - `workflo.macros.query.util/capitalized-name`
    - `workflo.macros.query.util/capitalized-symbol?`
    - `workflo.macros.query.util/one-item?`
    - `workflo.macros.query/conform`
    - `workflo.macros.query/conform-and-parse`
    - `workflo.macros.query/parse-subquery`
    - `workflo.macros.query/parse`
    - `workflo.macros.query/map-destructuring-keys`
    - `workflo.macros.query.om-next/property-query`
    - `workflo.macros.query.om-next/query`
    - `workflo.macros.view/defview*`
    - `workflo.macros.command.util/bind-query-keys`
    - `workflo.macros.command.util/unqualify`
    - `workflo.macros.command.util/prefix-form-name`
    - `workflo.macros.command/defcommand*`
    - `workflo.macros.command.util/form->defn`
* Helper functions for query parsing:
    - `workflo.macros.query.util/combine-properties-and-groups`
    - `workflo.macros.query.util/capitalized-name`
    - `workflo.macros.query.util/capitalized-symbol?`
    - `workflo.macros.query.util/print-spec-gen`
    - `workflo.macros.query/parse-prop`
* Helper functions for defcommand:
    - `workflo.macros.command.util/bind-query-keys`
    - `workflo.macros.command.util/unqualify`
    - `workflo.macros.command.util/prefix-form-name`
    - `workflo.macros.command/defcommand*`
    - `workflo.macros.command.util/form->defn`
* New `workflo.macros.query/map-destructuring-keys` function for
  generating keys for destructuring properties from queries,
  replacing `map-keys`.
* New `workflo.macros.query.om-next/property-query` function to
  replace `workflo.macros.query/om-property-query`.
* New boot task `test-once` for running the test suites once.

### Changed

* Only define `defview` configuration atom once at load time.
* Properties specifications are now called queries everywhere.
* `workflo.macros.props` namespace renamed to `workflo.macros.query`
* Improved test suite capable of exercising most things in both,
  Clojure and ClojureScript.
* Random function tests based on clojure.spec generators.

### Removed

* `workflo.macros.props/map-keys` (replaced with
  `workflo.macros.query/map-destructuring-keys`)
* `workflo.macros.props/pad-by`
* `workflo.macros.props/property-name`
* `workflo.macros.props/property-type`
* `workflo.macros.props/property-query` (replaced with
  `workflo.macros.query.om-next/property-query`)
* `workflo.macros.props/value?`

## Notes

This changelog loosely follows the guidelines discussed on
[Keep a CHANGELOG](http://keepachangelog.com/).
