# Type specs

`workfloapp/macros` comes with specs—as in `clojure.spec` specs—for
various fundamental types. We've decided to focus on those supported
by [Datomic](http://www.datomic.com/), plus special types for relationships
between what we call [entities](04-defentity.md).

Please note, however, that the type specs are useful by themselves
and can be used in any application, regardless of whether you are
using Datomic or not.

## Basic types

* `:workflo.macros.specs.types/any`
* `:workflo.macros.specs.types/keyword`
* `:workflo.macros.specs.types/string`
* `:workflo.macros.specs.types/boolean`
* `:workflo.macros.specs.types/long`
* `:workflo.macros.specs.types/bigint`
* `:workflo.macros.specs.types/float`
* `:workflo.macros.specs.types/double`
* `:workflo.macros.specs.types/bigdec`
* `:workflo.macros.specs.types/instant`
* `:workflo.macros.specs.types/uuid`
* `:workflo.macros.specs.types/bytes`
* `:workflo.macros.specs.types/enum`

## Identifier types

* `:workflo.macros.specs.types/id` — a 32-character string.
* `:workflo/id` — the same as `:workflo.macros.specs.types/id` but with the
  `unique-identity` and `indexed` type hints added.

## Relationship types

* `:workflo.macros.specs.types/ref` — a map that contains at least a `:workflo/id`.
* `:workflo.macros.specs.types/ref-many` — a vector or set containing an arbitrary
  number of values of that match the `:workflo.macros.specs.types/ref` spec.

## Entity relationship types

* `(workflo.macros.specs.types/entity-ref ENTITY-SYM OPTIONS)` — a type spec
  that is equivalent to `:workflo.macros.specs.types/ref` or
  `:workflo.macros.specs.types/ref-many` (depending on `OPTIONS`) but also
  stores the name of the target entity (see [defentity](04-defentity.md)).

## Datomic-compatible type hints

* `:workflo.macros.specs.types/unique-value` — values of this type should be
  unique across all entity attributes with the same name (see
  [defentity](04-defentity.md)).
* `:workflo.macros.specs.types/unique-identity` — values of this type should
  be unique across the entire system.
* `:workflo.macros.specs.types/indexed` — values of this type should be indexed
  by databases, if possible.
* `:workflo.macros.specs.types/fulltext`
* `:workflo.macros.specs.types/component`
* `:workflo.macros.specs.types/no-history`

## Other hints

* `:workflo.macros.specs.types/non-persistent` — values of this types are not
  to be persisted (e.g. to Datomic, local storage or whatever your project
  uses for persistence).
