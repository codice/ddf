package org.codice.ddf.catalog.ui.metacard;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet("/TestServlet")
public class TestServlet extends HttpServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestServlet.class);

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    // content type must be set to text/event-stream
    response.setContentType("text/event-stream");
    // cache must be set to no-cache
    response.setHeader("Cache-Control", "no-cache");
    // encoding is set to UTF-8
    response.setCharacterEncoding("UTF-8");
    PrintWriter writer;
    try {
      writer = response.getWriter();

      for (int i = 0; i < 10; i++) {
        writer.write("data: " + i + "\r\n");
        writer.flush();
        //        try {
        //          Thread.sleep(3000);
        //        } catch (InterruptedException e) {
        //          LOGGER.error(e.getMessage());
        //        }
      }
      writer.close();
    } catch (Exception e) {
      LOGGER.error(e.getMessage());
    }
  }
}
