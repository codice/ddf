/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.session;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.filter.session.SessionFilter;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.xml.XmlConfiguration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JettySessionManagementTest {

  private static Server server;
  private static int port;

  @BeforeClass
  public static void setupClass() throws Exception {
    // To get the AccessRequestLog to log in the target folder
    System.setProperty("ddf.data", "target");
    server = new Server();
    HandlerList handlers = new HandlerList();
    server.setHandler(handlers);
    // Configure server according to the jetty.xml file
    XmlConfiguration configuration =
        new XmlConfiguration(JettySessionManagementTest.class.getResourceAsStream("/jetty.xml"));
    configuration.configure(server);
    // Have server bind to first available port
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(0);
    server.addConnector(connector);
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.addServlet(new ServletHolder(new TestServlet()), "/");
    handlers.addHandler(context);

    server.start();

    port = connector.getLocalPort();
  }

  @AfterClass
  public static void stopJetty() {
    try {
      server.stop();
    } catch (Exception e) {
    }
  }

  @Test
  public void sessionsCanBeObtained() throws Exception {
    String sessionId =
        RestAssured.given()
            .get(String.format("http://localhost:%s/newSession", port))
            .then()
            .statusCode(is(SC_OK))
            .extract()
            .sessionId();

    assertThat(sessionId, is(not(nullValue())));
  }

  @Test
  public void sessionsAreReusable() {
    SessionFilter sessionFilter = new SessionFilter();
    String sessionId =
        RestAssured.given()
            .filter(sessionFilter)
            .get(String.format("http://localhost:%s/newSession", port))
            .then()
            .statusCode(is(SC_OK))
            .extract()
            .sessionId();
    assertThat(sessionId, is(not(nullValue())));

    RestAssured.given()
        .filter(sessionFilter)
        .get(String.format("http://localhost:%s/existingSession", port))
        .then()
        .statusCode(is(SC_OK));
  }

  @Test
  public void sessionAreNotUsableWhenInvalidated() {
    SessionFilter sessionFilter = new SessionFilter();
    String sessionId =
        RestAssured.given()
            .filter(sessionFilter)
            .get(String.format("http://localhost:%s/newSession", port))
            .then()
            .statusCode(is(SC_OK))
            .extract()
            .sessionId();
    assertThat(sessionId, is(not(nullValue())));

    RestAssured.given()
        .filter(sessionFilter)
        .get(String.format("http://localhost:%s/existingSession", port))
        .then()
        .statusCode(is(SC_OK));

    RestAssured.given()
        .filter(sessionFilter)
        .get(String.format("http://localhost:%s/invalidateSession", port))
        .then()
        .statusCode(is(SC_OK));

    RestAssured.given()
        .filter(sessionFilter)
        .get(String.format("http://localhost:%s/existingSession", port))
        .then()
        .statusCode(is(SC_BAD_REQUEST));
  }

  @Test
  public void sessionsMaintainAttributesBetweenContexts() {
    SessionFilter sessionFilter = new SessionFilter();
    String sessionId =
        RestAssured.given()
            .filter(sessionFilter)
            .get(String.format("http://localhost:%s/newSession", port))
            .then()
            .statusCode(is(SC_OK))
            .extract()
            .sessionId();

    assertThat(sessionId, is(not(nullValue())));
    RestAssured.given()
        .filter(sessionFilter)
        .get(String.format("http://localhost:%s/addSessionAttribute", port))
        .then()
        .statusCode(is(SC_OK));

    RestAssured.given()
        .filter(sessionFilter)
        .get(String.format("http://localhost:%s/checkSessionAttribute", port))
        .then()
        .statusCode(is(SC_OK));
  }

  private static class TestServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      switch (request.getServletPath()) {
        case "/newSession":
          request.getSession();
          break;
        case "/existingSession":
          checkForValidSession(request, response);
          break;
        case "/invalidateSession":
          HttpSession session = checkForValidSession(request, response);
          session.invalidate();
          break;
        case "/addSessionAttribute":
          HttpSession sessionWithoutAttribute = checkForValidSession(request, response);
          sessionWithoutAttribute.setAttribute("testAttribute", "testValue");
          break;
        case "/checkSessionAttribute":
          HttpSession sessionWithAttribute = checkForValidSession(request, response);
          if (!sessionWithAttribute.getAttribute("testAttribute").equals("testValue")) {
            response.sendError(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Session did not have the attribute");
          }
          break;
        default:
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid servlet path");
      }
    }

    private HttpSession checkForValidSession(
        HttpServletRequest request, HttpServletResponse response) throws IOException {
      HttpSession session = request.getSession(false);
      if (session == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No valid session found");
      }

      return session;
    }
  }
}
