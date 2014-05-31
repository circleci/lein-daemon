(defproject circleci/lein-daemon "0.5.6"
  :description "A lein plugin that daemonizes a clojure process"
  :url "https://github.com/circleci/lein-daemon"
  :license {:name "Eclipse Public License"}
  :eval-in-leiningen true
  :profiles {:dev
             {:dependencies
              [[bond "0.2.6" :exclusions [org.clojure/clojure]]]}})
