package org.codice.ddf.platform.filter;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface HttpFilterChain {
  void doFilter(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException;
}
