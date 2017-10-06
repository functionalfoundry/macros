# Permissions (defpermission)

The `defpermission` macro essentially allows to give different permissions
a name and some human-readable information for documentation purposes.

Other parts of the system can then use the names of these permissions
to restrict access to e.g. entities, commands or user-facing features.

## Usage

### General structure

```clojure
(require '[workflo.macros.permission :refer [defpermission]])

(defpermission <name>
  (title <string>)       ; Required
  (description <string>) ; Optional
  )
```

### Simple example

```clojure
(require '[workflo.macros.permission :refer [defpermission]])

(defpermission administer-organization
  (title "Administer organization")
  (description
    "* Can view, add, remove and change roles in the organization
     * Can add and remove roles to or from users (cannot remove
       predefined roles)
     * Can view and change the billing info of the organization
       - No other permission can access this information
     * Can view and change the subscription of the organization"))

(defpermission manage-organization
  (title "Manage organization")
  (description
    "* Can view users of the organization and invite new users to
       the organization
     * Can add and remove roles to or from users (except for the
       Admin role)
     * Can reassign permissions inside the organization
     * Can view but not change the subscription of the organization"))

(defpermission view-organization
  (title "View organization")
  (description
    "* Can view non-admin information of the organization
     * Can view users of the organization
     * Can view roles and permission assignments of the organization"))
```

## API documentation

* [workflo.macros.permission](workflo.macros.permission.html)
    - The `defpermission` macro
    - Permission registry
* [workflo.macros.specs.permission](workflo.macros.specs.permission.html)
    - Specs for `defpermission` arguments