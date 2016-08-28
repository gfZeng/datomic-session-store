

(set-env!
 :source-paths #{"src"}
 :dependencies '[[adzerk/bootlaces "0.1.13" :scope "test"]

                 [com.taoensso/nippy "2.11.1"]])

(require '[adzerk.bootlaces :refer :all])

(def +version+ "0.1.2")
(bootlaces! +version+)

(task-options!
 pom {:project  'datomic-session-store
      :version  +version+
      :url      "https://github.com/gfZeng/datomic-session-store"
      :scm      {:url "https://github.com/gfZeng/datomic-session-store"}
      :license  {"MIT License (MIT)" "https://opensource.org/licenses/MIT"}})
