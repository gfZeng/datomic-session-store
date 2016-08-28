(ns ring.session.store.datomic
  (:require [taoensso.nippy :as nippy]
            [ring.middleware.session.store :refer (SessionStore)]
            [datomic.api :as d :refer (db)]))


(defrecord DatomicStore [conn opts]
  SessionStore
  (read-session [_ key]
    ((:entity->session opts :session/value)
     (let [s (d/pull (db conn) [:*] [:session/key key])
           s-value (:session/value s)]
       (assoc s :session/value
              (and s-value (nippy/thaw s-value))))))
  (delete-session [_ key]
    @([[:db.fn/retractEntity [:session/key key]]]))
  (write-session [_ key value]
    (let [key (or key
                  (when-let [session-key (:session/key opts)]
                    (session-key value))
                  (str (java.util.UUID/randomUUID)))]
      @(d/transact
        conn
        [(-> (if-let [session->entity (:session->entity opts)]
               (session->entity value)
               {:session/value value})
             (assoc :db/id (d/tempid :ring/session))
             (assoc :session/key key)
             (update :session/value nippy/freeze))])
      key)))

(def session-schemas
  [{:db/id                 (d/tempid :db.part/db)
    :db/ident              :ring/session
    :db.install/_partition :db.part/db}
   {:db/id                 (d/tempid :db.part/db)
    :db/ident              :session/key
    :db/valueType          :db.type/string
    :db/unique             :db.unique/identity
    :db/cardinality        :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/id                 (d/tempid :db.part/db)
    :db/ident              :session/user
    :db/valueType          :db.type/ref
    :db/cardinality        :db.cardinality/one
    :db.install/_attribute :db.part/db}
   {:db/id                 (d/tempid :db.part/db)
    :db/ident              :session/value
    :db/valueType          :db.type/bytes
    :db/cardinality        :db.cardinality/one
    :db/noHistory          true
    :db.install/_attribute :db.part/db}])

(defn datomic-store [conn & {:as opts}]
  @(d/transact conn (:schemas opts session-schemas))
  (DatomicStore. conn opts))


(defn noir:entity->session [s]
  (update (:session/value s)
          :noir assoc :user (:session/user s)))

(defn noir:session->entity [s]
  (cond-> {:session/value s}
    (-> s :noir :user)
    (assoc :session/user (get-in s [:noir :user :db/id]))))
