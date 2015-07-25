# clojure adapter servlet [![Build Status][1]][2]
a shim to create a clojure-friendly servlet container interface.

## rationale
the java servlet api is ill-suited for use with the clojure programming language; first, it requires a reference to a named class, which imposes class generation constructs and a compilation step on a project; second, it passes java objects instead of the [ring request and response maps][1] that have become the de facto standard in the clojure community; and third, it mandates the use of an impure function to mutate response instances.

this library decouples the cohesive functionality needed to adapt java servlet containers for use with clojure (which can be incorporated into a build task) from project-specific functions for serving requests (such as ring wrappers and handlers that belong in an application).  this separation of concerns makes it unique from other libraries with similar functionality.

## installation
this library is available on clojars.

[![latest version][4]][5]

## application
this library was designed to make it easy to create simple clojure servlets free of the extra dependencies and compilation steps that often complicate clojure web applications.  to use it, one need only:

* add this adapter to your application's dependencies.

* configure your application's [web.xml][6] file as show below (note that the create and serve init-params are optional).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://java.sun.com/xml/ns/javaee" metadata-complete="true" version="3.0" xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd">
  <display-name>My Web App</display-name>
  <description>My web application that does things.</description>
  <servlet>
    <servlet-name>my-web-app</servlet-name>
    <servlet-class>tailrecursion.ClojureAdapterServlet</servlet-class>
    <init-param>
      <param-name>create</param-name>
      <param-value>my.web.app/create</param-value>
    </init-param>
    <init-param>
      <param-name>serve</param-name>
      <param-value>my.web.app/serve</param-value>
    </init-param>
    <init-param>
      <param-name>destroy</param-name>
      <param-value>my.web.app/destroy</param-value>
    </init-param>
  </servlet>
  <servlet-mapping>
    <servlet-name>my-web-app</servlet-name>
    <url-pattern>/*</url-pattern>
  </servlet-mapping>
</web-app>
```
* create an entry point to your application as specified by the init-params in the aformentioned web.xml file.

* if this library is used outside the context of a servlet container, it will also be necessary to add the javax servlet library to the project dependecies:

```edn
[javax.servlet/javax.servlet-api "3.1.0" :scope "provided"]
```

```clojure
(defn create [config]
  "called by the container when the servet is placed into service (optional)."
  nil )

(defn serve [req]
  "called by the container when there is a request."
  {:status 200 :body "hello world"} )

(defn destroy []
  "called by the container when the servlet is taken out of service (optional)."
  nil )
```

## considerations
this project would not have been possible without the work done on preexisting solutions for the servlet problem, but it differers from them in the following ways:

* [__ring__][7].  unlike ring's servlet, which invokes proxy to extend [HttpServlet][8] and imposes a compilation step on the consumer at project-build-time, this libary is compiled at library-build-time to include the class file expected by the java servlet container with its distribution.  clojure-adapter-servlet also exposes optional `(create)` and `(destroy)` function calls from the container with a configuration map.

* [__ring-java-servlet__][9]. the idea behind [RingHttpServlet][10] is similar, but clojure-adapter-servlet compiles to a single class file using javac instead of doing aot compilation with genclass; more importantly, it includes ring request, ring response, and config map conversion facilities as part of the distribution.

* [__pedestal__][11].  unlike [ClojureVarServlet][12], this library only uses the java class for a shim to proxy calls to the clojure implementation; it also contains the logic to generate ring request, response, and configuration maps.  it is designed to work with ring handlers instead of interceptors.


the `(create)` and `(serve)` functions are named differently from `init()` and `service()` because their semantics differ: create accepts a configuration map instead of a [ServletConfig][13] object as its argument, while serve is a pure function that accepts a ring request map and returns a ring response map instead of [HttpServletRequest][14] and [HttpServletResponse][15].  the behavior of `(destroy)` remain unchanged, and so does the name.

this library uses code from ring-servlet to ensure the request and response maps remain perfectly compatible with ring handlers.  it omits, however, the '`(merge-servlet-keys)` [function][16] that sets the legacy `:servlet`, `:servlet-request`, `:servlet-response`, `:servlet-context`, and `:servlet-context-path` functions on the request map in order to keep the interface small and clean.  this function may be added back in if it is determined that its removal breaks too many things in practice.

much like the spec for the ring request and response maps, the servlet's configuration and context maps also need to be standardized (and described here in more detail once finalized).

this project would also benefit from some robust tests, which should probably be borrowed from ring.

## resources
[ring's][7] [servlet namespace][8], [ring-java-servlet's][9] [RingHttpServlet class][10], and [pedestal's][11] [ClojureVarServlet class][12] were studied (and pilfered from in some cases) to make this library.

rich hickey explains the java shim technique used by this library in a [december 2012 lightning talk on clojure's runtime package][17].

## license
distributed with clojure under version 1.0 of the eclipse public license.  copyright © jumblerg and contributors.  all rights reserved.

[1]: https://travis-ci.org/tailrecursion/clojure-adapter-servlet.png?branch=master
[2]: https://travis-ci.org/tailrecursion/clojure-adapter-servlet
[3]: https://github.com/mmcgrana/ring/blob/master/SPEC
[4]: https://clojars.org/tailrecursion/clojure-adapter-servlet/latest-version.svg?bustcache=0.2.1
[5]: https://clojars.org/tailrecursion/clojure-adapter-servlet
[6]: http://docs.oracle.com/cd/E13222_01/wls/docs92/webapp/configureservlet.html
[7]: https://github.com/ring-clojure/ring/
[8]: https://github.com/ring-clojure/ring/blob/master/ring-servlet/src/ring/util/servlet.clj
[9]: https://github.com/laurentpetit/ring-java-servlet
[10]: https://github.com/laurentpetit/ring-java-servlet/blob/master/src/org/lpetit/ring/servlet/RingHttpServlet.clj
[11]: https://github.com/pedestal/pedestal/
[12]: https://github.com/pedestal/pedestal/blob/master/service/java/io/pedestal/servlet/ClojureVarServlet.java
[13]: http://docs.oracle.com/javaee/7/api/javax/servlet/ServletConfig.html
[14]: http://docs.oracle.com/javaee/7/api/javax/servlet/http/HttpServletRequest.html
[15]: http://docs.oracle.com/javaee/7/api/javax/servlet/http/HttpServletResponse.html
[16]: https://github.com/ring-clojure/ring/blob/master/ring-servlet/src/ring/util/servlet.clj#L51
[17]: https://skillsmatter.com/skillscasts/3864-impromptu-rich-hickey-lightning-talk
