(ns leiningen.test-daemon
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [bond.james :as bond]
            [leiningen.core.main :refer (abort)]
            [leiningen.core.project :as project]
            [leiningen.daemon :as daemon]
            [leiningen.daemon-starter :as starter]
            [leiningen.daemon.common :as common]))

(defn cleanup-pids [f]
  (common/sh! "bash" "-c" "rm -rf *.pid")
  (f))

(defn standard-lein-name [f]
  (with-redefs [daemon/get-lein-script (constantly "lein")]
    (f)))

(defmacro with-no-spawn [& body]
  `(bond/with-stub [common/sh! daemon/wait-for-running]
     ~@body))

(use-fixtures :each cleanup-pids standard-lein-name)

(deftest daemon-args-are-passed-to-do-start
  (with-no-spawn
    (let [project {:daemon {"foo" {:pidfile "foo.pid"
                                   :args ["bar" "baz"]}}}]
      (daemon/daemon project "start" "foo")
      (let [bash-cmd (str/join " " (-> common/sh! bond/calls first :args))]
        (is (re-find #"lein daemon-starter foo bar baz" bash-cmd))))))

(deftest cmd-line-args-are-passed-to-do-start
  (with-no-spawn
    (let [project {:daemon {"foo" {:pidfile "foo.pid"}}}]
      (daemon/daemon project "start" "foo" "bar")
      (let [bash-cmd (str/join " " (-> common/sh! bond/calls first :args))]
        (is (re-find #"lein daemon-starter foo bar" bash-cmd))))))

(deftest daemon-cmd-line-args-are-combined
  (with-no-spawn
    (let [project {:daemon {"foo" {:pidfile "foo.pid"
                                   :args ["bar"]}}}]
      (daemon/daemon project "start" "foo" "baz")
      (let [bash-cmd (str/join " " (-> common/sh! bond/calls first :args))]
        (is (re-find #"lein daemon-starter foo bar baz" bash-cmd))))))

(deftest passing-string-foo-on-cmd-line-finds-keyword-foo
  (with-no-spawn
    (let [project {:daemon {:foo {:ns "foo.bar"}}}]
      (daemon/daemon project "start" "foo")
      (let [bash-cmd (str/join " " (-> common/sh! bond/calls first :args))]
        (is (re-find #"lein daemon-starter foo" bash-cmd))))))

(deftest passing-string-foo-on-cmd-line-finds-string-foo
  (with-no-spawn
    (let [project {:daemon {"foo" {:ns "foo.bar"}}}]
      (daemon/daemon project "start" "foo")
      (let [bash-cmd (str/join " " (-> common/sh! bond/calls first :args))]
        (is (re-find #"lein daemon-starter foo" bash-cmd))))))

(def dummy-project (project/make {:eval-in :subprocess
                                  :dependencies ['[org.clojure/clojure "1.4.0"]]}))

(deftest pid-path-handles-keywords
  (let [project (merge dummy-project {:daemon {:foo {:ns "bogus.main"}}})]
    (is (= "foo.pid" (common/get-pid-path project :foo)))))

#_(deftest daemon-starter-finds-string-info
  (starter/daemon-starter (merge dummy-project {:daemon {"foo" {:ns "bogus.main"}}}) "foo"))

#_(deftest daemon-starter-finds-keyword-daemon
  (starter/daemon-starter (merge dummy-project {:daemon {:foo {:ns "bogus.main"}}}) "foo"))

(deftest log-files-dont-include-colon
  (with-no-spawn
    (with-no-spawn
      (let [project {:daemon {:foo {:ns "foo.bar"}}}]
        (daemon/daemon project "start" "foo")
        (let [bash-cmd (str/join " " (-> common/sh! bond/calls first :args))]
          (is (re-find #"> foo.log" bash-cmd)))))))