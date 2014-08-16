(defproject tailrecursion/clojure-adapter-servlet "0.1.0-SNAPSHOT"
  :description       "A shim to create a clojure-friendly servlet container interface."
  :url               "https://github.com/tailrecursion/clojure-adapter-servlet"
  :license           {:name "Eclipse Public License"
                      :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies      [[org.clojure/clojure             "1.6.0"]
                      [javax.servlet/javax.servlet-api "3.1.0" :scope "provided"] ]
  :java-source-paths ["src"]
  :test-paths        ["tst"]
  :target-path       "tgt/%s/"
  :scm               {:name "git"
                      :url  "https://github.com/tailrecursion/clojure-adapter-servlet" })
