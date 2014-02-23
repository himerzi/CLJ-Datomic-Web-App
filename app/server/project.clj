(defproject dactic-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.3.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"local" "file:maven_repo"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.novemberain/monger "1.5.0"]
                 [liberator "0.10.0"]
                 [ring "1.2.1"]
                 ;;for slugifying strings
                 [slugger "1.0.1"]
                 [compojure "1.1.6"]
                 [com.datomic/datomic-pro "0.9.4384"]
                 [lib-noir "0.7.9"]
                 [ring/ring-json "0.2.0"]
                 [digest "1.4.3"]
                 [abengoa/clj-stripe "1.0.3"]]
  :main dactic-server.core)
