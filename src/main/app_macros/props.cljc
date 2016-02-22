(ns app-macros.props)

(defn pad-by
  "Add pad in between any two consecutive values in coll for which
   pred returns the same result. As an example, assume the following
   use:

       (pad-by type :same-type [:foo :bar [1 2] {3 4} {5 6}])

   The result would be

       [:foo :same-type :bar [1 2] {3 4} :same-type {5 6}].

   If called without coll, returns a transducer."
  ([pred pad]
   (fn [rf]
     (let [pv (volatile! nil)]
       (fn
         ([] (rf))
         ([result] (rf result))
         ([result input]
          (let [prior @pv]
            (vreset! pv input)
            (if (pred prior input)
              (rf (rf result pad) input)
              (rf result input))))))))
  ([pred pad coll] (sequence (pad-by pred pad) coll)))

(defn value?
  "Returns true if x is considered a basic EDN value."
  [x]
  (or (number? x) (string? x) (keyword? x) (= x '_)))

(def property-types
  "Property types supported by the properties parser."
  [{:type  :property
    :test  #(symbol? %)
    :name  #(name %)
    :query #(keyword (:name %))}
   {:type  :link
    :test  #(and (vector? %)
                 (= 2 (count %))
                 (value? (second %)))
    :name  #(first %)
    :query #(-> [(keyword (:name %)) (:target %)])}
   {:type  :join
    :test  #(and (map? %)
                 (= 1 (count %)))
    :name  #(first (keys %))
    :query #(-> {(keyword (:name %))
                 (let [target (:target %)]
                   (cond
                     (= target '...) '...
                     (number? target) target
                     :else `(~'om.next/get-query ~target)))})}])

(defn- property-resolve
  "Given a property prop, resolves the field :type or :name into
   the corresponding property type or name."
  [prop field]
  (let [match (first (filter (fn [info]
                               (or ((:test info) prop)
                                   (and (map? prop)
                                        (= (:type info)
                                           (:type prop)))))
                             property-types))]
    (if (fn? (get match field))
      ((get match field) prop)
      (get match field))))

(defn property-type
  "Returns the property type for prop."
  [prop]
  (property-resolve prop :type))

(defn property-name
  "Returns a property name for prop. If passed a parent != nil,
   the property name is namespaced according to the parent property
   name."
  [parent prop]
  (symbol (some-> parent (property-resolve :name) name)
          (some-> prop (property-resolve :name) name)))

(defn property-query
  "Returns an Om Next query expression for prop."
  [prop]
  (property-resolve prop :query))

(defn parse
  "Parses a properties specification like
   [user [name email {friends User}] [current-user _]] into
   a flat collection with the following structure:

   [{:name user/name :type :property}
    {:name user/email :type :property}
    {:name user/friends :type :join :target User}
    {:name current-user :type :link :target _}].

   From this it is trivial to generate keys for destructuring
   view props and an Om Next query."
  [spec]
  (letfn [(parse-prop [parent p]
            (let [name (property-name parent p)
                  type (property-type p)]
              (case type
                :property {:name name :type type}
                :link     {:name name :type type
                           :target (second p)}
                :join     {:name name :type type
                           :target (first (vals p))}
                :else     nil)))
          (parse-step [result [p children]]
            (concat result
                    (cond
                      (nil? children)    [(parse-prop nil p)]
                      (vector? children) (mapv #(parse-prop p %)
                                               children))))]
    (->> (pad-by #(= (property-type %1) (property-type %2)) nil spec)
         (partition-all 2 2)
         (reduce parse-step [])
         (into []))))

(defn om-query
  "Generates an Om Next query from a parsed properties specification."
  [props]
  (into [] (map property-query) props))
