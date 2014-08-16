;;;-------------------------------------------------------------------------------------------------
;;; Copyright 2014 jumblerg.
;;; All rights reserved. The use and distribution terms terms for this software are covered by the
;;; Eclipse Public License 1.0 (http://www.eclipse.org/legal/epl-v10.html). By using this software
;;; in any fashion, you are agreeing to be bound by the terms of this license.  You must not remove
;;; this notice, or any other, from this software.
;;;-------------------------------------------------------------------------------------------------

(ns tailrecursion.clojure-adapter-servlet-test.clojure-servlet-req-fns)

(defn serve [req]
  "called by the container when there is a request."
  {:status  200
   :headers {"Content-Type" "text/plain", "X-Server" "Bar"}
   :body    nil} )
