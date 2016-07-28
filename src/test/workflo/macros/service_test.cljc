(ns workflo.macros.service-test
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.test :refer-macros [deftest is]]
               :clj  [clojure.test :refer [deftest is]])
            [com.stuartsierra.component :as component]
            #?(:cljs [workflo.macros.service
                      :as service
                      :refer-macros [defservice]]
               :clj  [workflo.macros.service
                      :as service
                      :refer [defservice]])))

(deftest a-simple-service
  ;; Specs for email data
  (s/def ::to string?)
  (s/def ::subject string?)
  (s/def ::body string?)
  (s/def ::email (s/keys :req-un [::to ::subject ::body]))

  (def email-service-info (atom {}))
  (def sent-emails (atom []))

  ;; Email service definition
  (defservice email
    "Email service"
    (data-spec ::email)
    (start
      (swap! email-service-info assoc :started? true)
      this)
    (stop
      (swap! email-service-info dissoc :started?)
      this)
    (process (swap! sent-emails conj data)))

  (and
   ;; Verify that the email service is defined as a record
   (is (record? email))

   ;; Verify that it has all the expected fields
   (is (= "Email service" (:description email)))
   (is (= ::email (:data-spec email)))
   (is (fn? (:start email)))
   (is (fn? (:stop email)))
   (is (fn? (:process email)))

   ;; Verify that the email service is registered
   (is (= email (service/resolve-service 'email)))

   ;; Verify that sending an email doesn't work yet, since the
   ;; service hasn't been instantiated as a component yet
   (do
     (service/deliver-to-services!
      {:email {:to "Recipient"
               :subject "Subject"
               :body "Body"}})
     (is (empty? @sent-emails)))

   ;; Verify that the start function of the email service
   ;; hasn't been called yet and that there is no registered
   ;; email service component
   (and
    (is (nil? (:started? @email-service-info)))
    (is (nil? (get (service/registered-service-components) 'email))))

   (and
    ;; Start the email service
    (do
      (let [c (service/new-service-component 'email)]
        (component/start c))

      ;; Verify that the start function has been called and
      ;; that there is a registered email service component now
      (and
       (is (true? (:started? @email-service-info)))
       (is (not (nil? (get (service/registered-service-components)
                           'email))))))
    (do
      ;; Verify that sending emails works
      (service/deliver-to-services!
       {:email {:to "Recipient 1"
                :subject "Subject 1"
                :body "Body 1"}})
      (= [{:to "Recipient 1" :subject "Subject 1" :body "Body 1"}]
         @sent-emails))
    (do
      ;; Verify that sending another email works
      (service/deliver-to-services!
       {:email {:to "Recipient 2"
                :subject "Subject 2"
                :body "Body 2"}})
      (= [{:to "Recipient 1" :subject "Subject 1" :body "Body 1"}
          {:to "Recipient 2" :subject "Subject 2" :body "Body 2"}]
         @sent-emails))
    (do
      ;; Stop the email service
      (let [c (get (service/registered-service-components) 'email)]
        (component/stop c))

      ;; Verify that the stop function has been called and
      ;; there no longer is a registered email service component
      (and
       (is (nil? (:started? @email-service-info)))
       (is (nil? (get (service/registered-service-components)
                      'email)))))
    (do
      ;; Verify that sending emails no longer works
      (service/deliver-to-services!
       {:email {:to "Recipient"
                :subject "Subject"
                :body "Body"}})
      (is (= [{:to "Recipient 1" :subject "Subject 1" :body "Body 1"}
              {:to "Recipient 2" :subject "Subject 2" :body "Body 2"}]
             @sent-emails))))))
