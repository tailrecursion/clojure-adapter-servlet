;;;-------------------------------------------------------------------------------------------------
;;; Copyright 2009-2014 Mark McGranaghan, James Reeves, Laurent Petit, jumblerg, & contributors.
;;; All rights reserved. The use and distribution terms terms for this software are covered by the
;;; Eclipse Public License 1.0 (http://www.eclipse.org/legal/epl-v10.html). By using this software
;;; in any fashion, you are agreeing to be bound by the terms of this license.  You must not remove
;;; this notice, or any other, from this software.
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.clojure-adapter-servlet.impl
  (:require
    [clojure.java.io :as io]
    [clojure.string  :as string] )
  (:import
    [java.io            File InputStream FileInputStream]
    [java.util          Locale]
    [javax.servlet      ServletConfig ServletContext ServletContextEvent ServletException]
    [javax.servlet.http HttpServletRequest HttpServletResponse] ))

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def serve-fn   (atom nil))
(def destroy-fn (atom nil))

(defn- get-params-map [^ServletConfig config]
  (into {} (for [param (enumeration-seq (.getInitParameterNames config))
    :let [value (.getInitParameter config param)]]
    [(keyword param) value] )))

(defn- get-config-map [^ServletConfig config]
    {:name        (.getServletName config)
     :init-params (get-params-map  config) })

(defn- get-servlet-fn [fn-name]
  (let [[n s] (map symbol ((juxt namespace name) (symbol fn-name)))]
    (require n)
    (or (ns-resolve (the-ns n) s)
      (throw (ServletException.
        (str "The function " fn-name " specified by the web.xml configuration cannot be resolved.") )))))

(defn- get-content-length [^HttpServletRequest req]
  (let [length (.getContentLength req)]
    (if (>= length 0) length) ))

(defn- get-client-cert [^HttpServletRequest req]
  (first (.getAttribute req "javax.servlet.request.X509Certificate")) )

(defn- get-headers [^HttpServletRequest req]
  (reduce
    (fn [headers, ^String name]
      (assoc headers (.toLowerCase name Locale/ENGLISH)
        (->> (.getHeaders req name)
             (enumeration-seq)
             (string/join ",") )))
    {}
    (enumeration-seq (.getHeaderNames req) )))

(defn- get-request-map [^HttpServletRequest req]
  {:server-port        (-> req .getServerPort)
   :server-name        (-> req .getServerName)
   :remote-addr        (-> req .getRemoteAddr)
   :uri                (-> req .getRequestURI)
   :query-string       (-> req .getQueryString)
   :scheme             (-> req .getScheme keyword)
   :request-method     (-> req .getMethod (.toLowerCase Locale/ENGLISH) keyword)
   :protocol           (-> req .getProtocol)
   :headers            (-> req get-headers)
   :content-type       (-> req .getContentType)
   :content-length     (-> req get-content-length)
   :character-encoding (-> req .getCharacterEncoding)
   :ssl-client-cert    (-> req get-client-cert)
   :body               (-> req .getInputStream) })

(defn- set-status [^HttpServletResponse res, status]
  (.setStatus res status) )

(defn- set-headers [^HttpServletResponse res, headers]
  (doseq [[key val-or-vals] headers]
    (if (string? val-or-vals)
      (.setHeader res key val-or-vals)
      (doseq [val val-or-vals]
        (.addHeader res key val) )))
  (when-let [content-type (get headers "Content-Type")]
    (.setContentType res content-type) ))

(defn- set-body [^HttpServletResponse res, body]
  (cond
    (string? body)
      (with-open [writer (.getWriter res)]
        (.print writer body))
    (seq? body)
      (with-open [writer (.getWriter res)]
        (doseq [chunk body]
          (.print writer (str chunk))))
    (instance? InputStream body)
      (with-open [^InputStream b body]
        (io/copy b (.getOutputStream res) ))
    (instance? File body)
      (let [^File f body]
        (with-open [stream (FileInputStream. f)]
          (set-body res stream) ))
    (nil? body)
      nil
    :else
      (throw (Exception. ^String (format "Unrecognized body: %s" body)) )))

(defn- set-response-map [^HttpServletResponse res {:keys [status headers body]}]
  (when-not res
    (throw (Exception. "Null response given.")) )
  (when status
    (set-status res status) )
  (doto res (set-headers headers) (set-body body) ))

;;; servlet implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn init [^ServletConfig config]
  (let [config   (get-config-map config)
        fn-name #(-> config :init-params %)]
    (if-let [n (fn-name :create)]
      ((get-servlet-fn n) config))
    (if-let [n  (fn-name :serve)]
      (if-let [f (get-servlet-fn n)]
        (reset! serve-fn f) )
        (throw (ServletException. "The required serve function could not be found in web.xml.")) )
    (if-let [n (fn-name :destroy)]
      (reset! destroy-fn (get-servlet-fn n))) ))

(defn service [^HttpServletRequest req ^HttpServletResponse res]
  (if-let [req (get-request-map req)]
    (set-response-map res (@serve-fn req) )))

(defn destroy []
  (if-let [destroy @destroy-fn] (destroy)) )

;;; servlet context listener implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-context-param [^ServletContextEvent sce k]
  (let [^ServletContext sc (.getServletContext sce)]
    (.getInitParameter sc k)))

(defn context-initialized [^ServletContextEvent sce]
  (when-let [fn-name (get-context-param sce "context-create")]
    ((get-servlet-fn fn-name))))

(defn context-destroyed [^ServletContextEvent sce]
  (when-let [fn-name (get-context-param sce "context-destroy")]
    ((get-servlet-fn fn-name))))
