(ns ring.session.store.datomic
  (:require [taoensso.nippy :as nippy]
            [ring.middleware.session.store :refer (SessionStore)]
            [datomic.api :as d :refer (db)]))


(defrecord DatomicStore [conn opts]
  SessionStore
  (read-session [_ key]
    ((:< opts)
     (let [s (d/pull (db conn) [:*] [:session/key key])
           s-value (:session/value s)]
       (assoc s :session/value
              (and s-value (nippy/thaw s-value))))))
  (delete-session [_ key]
    @([[:db.fn/retractEntity [:session/key key]]]))
  (write-session [_ key value]
    (let [key (or key (str (java.util.UUID/randomUUID)))]
      @(d/transact
        conn
        [(-> ((:> opts) value)
             (assoc :db/id (d/tempid :ring/session))
             (assoc :session/key key)
             (update :session/value nippy/freeze))])
      key)))


(defn datomic-store [conn & {:as opts}]
  @(d/transact conn [{:db/id                 (d/tempid :db.part/db)
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
  (DatomicStore. conn opts))
