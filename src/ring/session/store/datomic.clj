(ns ring.session.store.datomic
  (:require [taoensso.nippy :as nippy]
            [ring.middleware.session.store :refer (SessionStore)]
            [datomic.api :as d :refer (db)]))


(defrecord DatomicStore [conn]
  SessionStore
  (read-session [_ key]
    (-> (db conn)
        (d/entity [:session/key key])
        :session/value
        nippy/thaw))
  (delete-session [_ key]
    @([[:db.fn/retractEntity [:session/key key]]]))
  (write-session [_ key value]
    @(d/transact
      conn
      [(cond-> {:session/key key
                :session/value (nippy/freezy value)}
         (:user value) (assoc :session/user (:user value)))])))


(defn datomic-session [conn]
  @(d/transact conn [{:db/id                 (d/tempid :db.part/db)
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
  (DatomicStore. conn))
