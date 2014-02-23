(ns dactic-server.controllers.charge
  (:require
   [liberator.core :refer [defresource]]
   [clj-stripe.common :as common]
   [clj-stripe.charges :as charges]
   ;;validation
   [noir.validation :as validation :refer [rule errors? has-value? get-errors on-error is-email?]]
   ;;utils
   [dactic-server.controllers.util :as utils]
   ;;db
   [dactic-server.models.course :as course-db]
   [dactic-server.models.util :as db-util]

   ;;string slugification
   [slugger.core :refer [->slug]]
   ;;session
   [noir.session :as session]))
(defresource charge-resource
  [{:keys [stripeCharge stripeEmail] :as args}]
  :available-media-types ["application/json"]
  :allowed-methods [:post]
  :malformed? false
  :allowed? (fn [ctx]
              ;;first and foremost, he must be logged in to do any
              ;;course stuff
              (if-let [user-id (session/get :user-id false)]


                [false {:errors {:auth ["not logged in"]}
                        :representation {:media-type "application/json"}}]))
  :handle-forbidden utils/errors-in-ctx
  :post! (fn [_] (let [chrg (common/with-token ""
                              (common/execute (charges/create-charge
                                               (common/money-quantity 1000 "gbp")
                                               (common/card stripeCharge)
                                               ;;(common/description "This an extra charge for some stuff")
                                               )))]
                  (.println System/out (str "charged " chrg))))
  :handle-ok ::entry
  :handle-created ::entry
  :handle-malformed utils/errors-in-ctx)


