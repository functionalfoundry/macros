(ns workflo.macros.docs.util.string
  (:require [devcards.core :refer-macros [defcard defcard-doc]]
            [om.dom :as dom]
            [workflo.macros.util.string :refer [camel->kebab]]))

(defcard-doc
  "# String utilities")

(defcard
  "## Camel case to kebab case conversion"
  (fn [state _]
    (dom/table nil
      (dom/tbody nil
        (for [string @state]
          (dom/tr #js {:key string}
            (dom/td nil string)
            (dom/td nil (camel->kebab string)))))))
  ["User" "UserProfile" "UserProfileHeader"
   "user" "user-profile" "user-profile-header"])
