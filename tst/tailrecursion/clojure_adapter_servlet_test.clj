;;;-------------------------------------------------------------------------------------------------
;;; Copyright 2009-2014 Mark McGranaghan, James Reeves, jumblerg, & contributors.
;;; All rights reserved. The use and distribution terms terms for this software are covered by the
;;; Eclipse Public License 1.0 (http://www.eclipse.org/legal/epl-v10.html). By using this software
;;; in any fashion, you are agreeing to be bound by the terms of this license.  You must not remove
;;; this notice, or any other, from this software.
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.clojure-adapter-servlet-test
  (:require
    [clojure.test :refer :all] ))

;;; utilities ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- enumeration [coll]
  (let [e (atom coll)]
    (proxy [java.util.Enumeration] []
      (hasMoreElements [] (not (empty? @e)))
      (nextElement [] (let [f (first @e)] (swap! e rest) f)))))

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def config-req-fns
  {:name "Clojure Test Servlet"
   :init-params {"serve" "tailrecursion.clojure-adapter-servlet-test.clojure-servlet-req-fns/serve"} })

(def config-opt-fns
  {:name "Clojure Test Servlet"
   :init-params {"create"  "tailrecursion.clojure-adapter-servlet-test.clojure-servlet-opt-fns/create"
                 "serve"   "tailrecursion.clojure-adapter-servlet-test.clojure-servlet-req-fns/serve"
                 "destroy" "tailrecursion.clojure-adapter-servlet-test.clojure-servlet-opt-fns/destroy" }})

(def request
  (let [body (proxy [javax.servlet.ServletInputStream]   [])
        cert (proxy [java.security.cert.X509Certificate] []) ]
    {:server-port          8080
     :server-name          "foobar"
     :remote-addr          "127.0.0.1"
     :uri                  "/foo"
     :query-string         "a=b"
     :scheme               :http
     :protocol             "HTTP/1.1"
     :request-method       :get
     :headers              {"X-Client" ["Foo", "Bar"], "X-Server" ["Baz"]}
     :content-type         "text/plain"
     :content-length       10
     :character-encoding   "UTF-8"
     :servlet-context-path "/foo"
     :ssl-client-cert      cert
     :body                 body }))

(defn- servlet-config [conf]
  (proxy [javax.servlet.ServletConfig] []
    (getInitParameter      [n] ((conf :init-params) n))
    (getInitParameterNames []  (enumeration (keys (conf :init-params))))
    (getServletContext     []  nil)
    (getServletName        []  (conf :name)) ))

(defn- servlet-request [req]
  (let [attrs #(hash-map "javax.servlet.request.X509Certificate" %)]
    (proxy [javax.servlet.http.HttpServletRequest] []
      (getServerPort        []  (-> req :server-port))
      (getServerName        []  (-> req :server-name))
      (getRemoteAddr        []  (-> req :remote-addr))
      (getRequestURI        []  (-> req :uri))
      (getQueryString       []  (-> req :query-string))
      (getContextPath       []  (-> req :servlet-context-path))
      (getScheme            []  (-> req :scheme name))
      (getMethod            []  (-> req :request-method name .toUpperCase))
      (getHeaderNames       []  (-> req :headers keys enumeration))
      (getHeaders           [k] (-> req :headers (get k) enumeration))
      (getProtocol          []  (-> req :protocol))
      (getContentType       []  (-> req :cotent-type))
      (getContentLength     []  (-> req :content-length (or -1)))
      (getCharacterEncoding []  (-> req :character-encoding))
      (getAttribute         [k] (-> req :ssl-client-cert vector attrs (get k)))
      (getInputStream       []  (-> req :body)) )))

(defn- servlet-response [response]
  (proxy [javax.servlet.http.HttpServletResponse] []
    (setStatus [status]
      (swap! response assoc :status status))
    (setHeader [name value]
      (swap! response assoc-in [:headers name] value))
    (setCharacterEncoding [value])
    (setContentType [value]
      (swap! response assoc :content-type value))))

;;; tests ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest test-req-fn-config
  (let [srv (tailrecursion.ClojureAdapterServlet.)
        res (atom {}) ]
    (.init srv (servlet-config config-req-fns))
    (.service srv (servlet-request request) (servlet-response res))
    (.destroy srv)
    (is (= (@res :status) 200))
    (is (= (@res :content-type) "text/plain"))
    (is (= (get-in @res [:headers "X-Server"]) "Bar")) ))

(deftest test-opt-fn-config
  (let [srv (tailrecursion.ClojureAdapterServlet.)
        res (atom {}) ]
    (.init srv (servlet-config config-opt-fns))
    (.service srv (servlet-request request) (servlet-response res))
    (.destroy srv)
    (is (= (@res :status) 200))
    (is (= (@res :content-type) "text/plain"))
    (is (= (get-in @res [:headers "X-Server"]) "Bar")) ))
