/***************************************************************************************************
*** Copyright 2014 jumblerg.
*** All rights reserved. The use and distribution terms terms for this software are covered by the
*** Eclipse Public License 1.0 (http://www.eclipse.org/legal/epl-v10.html). By using this software
*** in any fashion, you are agreeing to be bound by the terms of this license.  You must not remove
*** this notice, or any other, from this software.
***************************************************************************************************/

package tailrecursion;

import clojure.lang.RT;
import clojure.lang.Symbol;
import clojure.lang.Var;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ClojureAdapterServletContextListener implements ServletContextListener {

  private static final Var REQUIRE = RT.var("clojure.core", "require");

  static { REQUIRE.invoke(Symbol.intern("tailrecursion.clojure-adapter-servlet.impl")); }

  private static final Var INITIALIZED = RT.var("tailrecursion.clojure-adapter-servlet.impl", "context-initialized");
  private static final Var DESTROYED   = RT.var("tailrecursion.clojure-adapter-servlet.impl", "context-destroyed");

  public void contextInitialized(ServletContextEvent sce) {
    INITIALIZED.invoke(sce);
  }

  public void contextDestroyed(ServletContextEvent sce) {
    DESTROYED.invoke(sce);
  }
}
