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

import java.io.IOException;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ClojureAdapterServlet extends GenericServlet {

  private static final Var REQUIRE = RT.var("clojure.core", "require");

  static { REQUIRE.invoke(Symbol.intern("tailrecursion.clojure-adapter-servlet.impl")); }

  private static final Var INIT    = RT.var("tailrecursion.clojure-adapter-servlet.impl", "init");
  private static final Var SERVICE = RT.var("tailrecursion.clojure-adapter-servlet.impl", "service");
  private static final Var DESTROY = RT.var("tailrecursion.clojure-adapter-servlet.impl", "destroy");

  @Override
  public void init(ServletConfig conf) throws NullPointerException, ServletException {
    INIT.invoke(conf);
  }

  @Override
  public void service(ServletRequest req, ServletResponse res) throws IOException, ServletException {
    SERVICE.invoke(req, res);
  }

  @Override
  public void destroy() {
    DESTROY.invoke();
  }

}
