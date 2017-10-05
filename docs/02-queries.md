# Queries

Most of the macros provided by `workflo/macros` involve some sort of
query. `defentity` defines an optional authorization query, `defcommand`
defines a query for additional input data, as does `defservice`, and
`defview` defines a query for data to display in the view.

All queries use the same query language. This query language is fully
specified with `clojure.spec`, via `:workflo.macros.specs.query/query`.

## The query language

The query language used by the macros is based on the
[Om Next query language](https://github.com/omcljs/om/blob/master/src/main/om/next/impl/parser.cljc#L5).
Its structure is the almost identical but its syntax is very different. What our
query language adds in terms of actual functionality is

* Aliases
* Query fragments — i.e. re-use of queries defined elsewhere
* Advanced parameterization

### Elements of a query

A query is an interleaved vector of different subqueries, some spanning
multiple vector elements. These subqueries can be:

* **Properties** — e.g. `user` or `post`, `ui.route`
* **Links** — e.g. `[user 100]` or `[ui.route _]`
* **Joins** — of the form `{SOURCE TARGET}`, where `SOURCE` can be either a property
  (e.g. `user`) or a link (e.g. `[user 100]`)
* **Prefixed properties** — e.g. `user [name email]` or `ui.route [url params]`
* **Aliased properties** — e.g. `user :as jeff` or `ui.route :as current-route`
* **Parameterizations** – of the form `(QUERY PARAMS)`, where `QUERY` is either a
  property, an aliased property, a link or a join, and `PARAMS` is a parameter
  map (see the section about query parameters below)
* **Fragments** — e.g. `...route-query` or `...user-query`

And since queries are Clojure data structures, they can include comments anywhere.

### Example queries

A query for the name and email address of a user with a specific `:workflo/id`:
```
[({user [workflo [id] user [name email]]}
  {workflo/id "some-id"})]
```
This query is identical to the following Om Next query:
```
[({:user [:workflo/id :user/name :user/email]}
  {:workflo/id "some-id"})]
```

A query for all users that have the first name `Linda`, including all posts
they have written, sorted by their last names, plus the post with the
ID with the value `"foo"`:
```
[(;; The subquery for the users
  {users [workflo [id]
          user [first-name
                last-name
                {posts [workflo [id]
                        post [title content]]}]]}

  ;; An alias for these users
  :as all-lindas

  ;; The query parameters
  {user/first-name "Linda"
   sort/attr :user/last-name})
 
 {[user "foo"]}]
```
This query is equivalent to the following Om Next query, only adding
some meta data for the alias:
```
[({:users [:workflo/id
           :user/first-name
           :user/last-name
           {:user/posts [:workflo/id
                         :post/title
                         :post/content]}]}
  {:user/first-name "Linda"
   :sort/attr :user/last-name})]
```

### Query parameters

Each query parameter is represented by a key-value pair in the parameter map
of a parameterized query. The key can take two forms:

1. a parameter name — a symbol like `foo`, `user/name` or `workflo/id`
2. a parameter path — a vector of symbols, like `[user/organization workflo/id]`
   or `[post/blog blog/author author/name]`

The second form is also called **deep parameterization**, as it allows to
match properties deep inside an entity and its relationships against the
value of a parameter.

The value of a parameter can take three forms:

1. a literal value — e.g. `"Linda"`, `10` or `:foo/bar`
2. a variable — e.g. `?first-name`, `?id` or `?user`
3. a variable path — a vector of variables e.g. `[?url ?params ?user]`

How these variables are resolved depends on the situation. Various of the
macros define functions that take in data and bind query parameters against
this data. In these cases a variable path is similar to a call to `get-in`,
allowing to extract values from deep inside a data structure.

For example, given the input data
```
{:url {:params {:user "123"}}}
```
the variable path `[?url ?params ?user]` would be resolved to `"123"` by
following the keys `:url`, `:params` and `:user`.

## API documentation / utilities

The macros come with various utilities for parsing queries into an AST,
binding query parameters against data, translating queries to Om Next
queries, creating bindings for query results in code that consumes
them and much more.

The following is a list of pointers to the API documentation for
all query utilities.

* [workflo.macros.bind](workflo.macros.bind.html)
    - Create local bindings for query results (used for binding query
      results in macro function bodies)
* [workflo.macros.query](workflo.macros.query.html)
    - Parse queries into an AST
    - Conform queries using `clojure.spec`
    - Bind query parameters
* [workflo.macros.query.bind](workflo.macros.query.bind.html)
    - Resolve variables and variable paths from query parameters:
      [workflo.macros.query.bind](workflo.macros.query.bind.html)
* [workflo.macros.query.om-next](workflo.macros.query.om-next.html)
    - Convert query ASTs to Om Next queries
    - Split queries (with potential conflicts) into minimal sequences
      of non-conflicting queries (useful for sending queries to
      backends)
* [workflo.macros.query.om-util](workflo.macros.query.om-util.html)
    - Internal utilities for working with Om Next queries
* [workflo.macros.query.util](workflo.macros.query.util.html)
    - Internal utilities for working with queries.