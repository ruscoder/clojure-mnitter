(defproject service "0.1.0-SNAPSHOT"
  :description "Mini twitter"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [digest "1.4.4"]
                 [ring "1.2.1"]
                 [compojure "1.1.6"]
                 [clj-time "0.7.0"]
                 [sonian/carica "1.1.0" :exclusions [[cheshire]]]
                 [org.clojure/java.jdbc "0.3.2"]
                 [postgresql "9.1-901.jdbc4"]
                 [korma "0.3.1"]
                 [mysql/mysql-connector-java "5.1.30"]
                 [fogus/ring-edn "0.2.0"]
                 [http-kit "2.1.18"]
                 [com.taoensso/carmine "2.6.0"]]
  :plugins [[lein-ring "0.6.3"]]
  :main service.core
  :min-lein-version "2.3.4"
  :ring {:handler service.core/app})
