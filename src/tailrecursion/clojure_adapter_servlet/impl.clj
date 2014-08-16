;;;-------------------------------------------------------------------------------------------------
;;; Copyright 2009-2014 Mark McGranaghan, James Reeves, Laurent Petit, jumblerg, & contributors.
;;; All rights reserved. The use and distribution terms terms for this software are covered by the
;;; Eclipse Public License 1.0 (http://www.eclipse.org/legal/epl-v10.html). By using this software
;;; in any fashion, you are agreeing to be bound by the terms of this license.  You must not remove
;;; this notice, or any other, from this software.
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.clojure-adapter-servlet.impl
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import [java.io            File InputStream FileInputStream]
           [javax.servlet      ServletConfig]
           [javax.servlet.http HttpServletRequest HttpServletResponse] ))

;;; private ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def serve-fn   (atom nil))
(def destroy-fn (atom nil))

(defn- get-params-map [^ServletConfig config]
  (into {} (for [param (enumeration-seq (.getInitParameterNames config))
    :let [value (.getInitParameter config param)]]
    [(keyword param) value] )))

(defn- get-config-map [^ServletConfig config]
    {:name        (.getServletName config)
     :init-params (get-params-map  config) })

(defn- get-servlet-fn [conf key]
  (if-let [value (-> conf :init-params key)]
    (let [[n s] (map symbol ((juxt namespace name) (symbol value)))]
      (require n) (ns-resolve (the-ns n) s) )))

(defn- get-content-length
  [^HttpServletRequest request]
  (let [length (.getContentLength request)]
    (if (>= length 0) length)))

(defn- get-client-cert
  [^HttpServletRequest request]
  (first (.getAttribute request "javax.servlet.request.X509Certificate")))

(defn- get-headers
  [^HttpServletRequest request]
  (reduce
    (fn [headers, ^String name]
      (assoc headers
        (.toLowerCase name)
        (->> (.getHeaders request name)
             (enumeration-seq)
             (string/join ",") )))
    {}
    (enumeration-seq (.getHeaderNames request) )))

(defn- get-request-map
  [^HttpServletRequest request]
  {:server-port        (.getServerPort request)
   :server-name        (.getServerName request)
   :remote-addr        (.getRemoteAddr request)
   :uri                (.getRequestURI request)
   :query-string       (.getQueryString request)
   :scheme             (keyword (.getScheme request))
   :request-method     (keyword (.toLowerCase (.getMethod request)))
   :headers            (get-headers request)
   :content-type       (.getContentType request)
   :content-length     (get-content-length request)
   :character-encoding (.getCharacterEncoding request)
   :ssl-client-cert    (get-client-cert request)
   :body               (.getInputStream request) })

(defn- set-status
  [^HttpServletResponse response, status]
  (.setStatus response status))

(defn- set-headers
  [^HttpServletResponse response, headers]
  (doseq [[key val-or-vals] headers]
    (if (string? val-or-vals)
      (.setHeader response key val-or-vals)
      (doseq [val val-or-vals]
        (.addHeader response key val) )))
  (when-let [content-type (get headers "Content-Type")]
    (.setContentType response content-type) ))

(defn- set-body
  [^HttpServletResponse response, body]
  (cond
    (string? body)
      (with-open [writer (.getWriter response)]
        (.print writer body))
    (seq? body)
      (with-open [writer (.getWriter response)]
        (doseq [chunk body]
          (.print writer (str chunk))))
    (instance? InputStream body)
      (with-open [^InputStream b body]
        (io/copy b (.getOutputStream response) ))
    (instance? File body)
      (let [^File f body]
        (with-open [stream (FileInputStream. f)]
          (set-body response stream) ))
    (nil? body)
      nil
    :else
      (throw (Exception. ^String (format "Unrecognized body: %s" body)) )))

(defn- set-response-map
  {:arglists '([response response-map])}
  [^HttpServletResponse response, {:keys [status headers body]}]
  (when-not response
    (throw (Exception. "Null response given.")) )
  (when status
    (set-status response status) )
  (doto response
    (set-headers headers)
    (set-body body) ))

;;; implementation ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn init [^ServletConfig config]
  (let [config (get-config-map config)
        get-fn (partial get-servlet-fn config) ]
    (if-let [create (get-fn :create)] (create config))
    (reset! serve-fn   (get-fn :serve))
    (reset! destroy-fn (get-fn :destroy)) ))

(defn service [^HttpServletRequest request ^HttpServletResponse response]
  (if-let [req (get-request-map request)]
    (set-response-map response (@serve-fn req) )))

(defn destroy []
  (if-let [destroy @destroy-fn] (destroy)) )
