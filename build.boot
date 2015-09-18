(set-env!
  :resource-paths #{"src"}
  :source-paths   #{"tst"}
  :target-path      "tgt"
  :dependencies  '[[org.clojure/clojure             "1.7.0"  :scope "provided"]
                   [javax.servlet/javax.servlet-api "3.1.0"  :scope "provided"]
                   [adzerk/bootlaces                "0.1.12" :scope "test"]
                   [adzerk/boot-test                "1.0.4"  :scope "test"] ]
 :repositories   [["clojars"       "https://clojars.org/repo/"]
                  ["maven-central" "https://repo1.maven.org/maven2/"] ])
(require
  '[adzerk.bootlaces :refer :all]
  '[adzerk.boot-test :refer [test]] )

(def +version+ "0.2.2")

(bootlaces! +version+)

(replace-task!
  [b build-jar] (fn [& xs] (comp (javac) (apply b xs)))
  [t test]      (fn [& xs] (comp (javac) (apply t xs))) )

(deftask build []
  (comp (test) (build-jar)) )

(deftask develop []
  (comp (wait) (speak) (javac) (test)) )

(task-options!
  pom  {:project     'tailrecursion/clojure-adapter-servlet
        :version     +version+
        :description "A shim to create a clojure-friendly servlet container interface."
        :url         "https://github.com/tailrecursion/clojure-adapter-servlet"
        :scm         {:url "https://github.com/tailrecursion/clojure-adapter-servlet"}
        :license     {"EPL" "http://www.eclipse.org/legal/epl-v10.html"} }
  test  {:namespaces #{'tailrecursion.clojure-adapter-servlet-test}} )
