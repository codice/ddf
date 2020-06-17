package org.codice.ddf.catalog.ui.events;

import static spark.Spark.get;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import spark.servlet.SparkApplication;

public class EventApplication implements SparkApplication {

  private static final List<PrintWriter> listeners = new ArrayList<>();

  @Override
  public void init() {
    get(
        "/events",
        (req, res) -> {
          try {
            res.type("text/event-stream; charset=UTF-8");
            res.header("Connection", "keep-alive");
            res.header("Cache-Control", "no-cache");
            res.status(200);
            PrintWriter out = res.raw().getWriter();
            synchronized (listeners) {
              listeners.add(out);
            }
            out.write("retry: 300000\n");
            out.write("data: " + System.currentTimeMillis() + "\n\n");
            out.flush();

            // Testing code
            while (true) {
              // Sending SSE heartbeat
              out.write(": \n\n");
              if (out.checkError()) {
                // Subscriber error, break out of loop
                break;
              }
              Thread.sleep(1000);
            }
            listeners.remove(out);
            return "";
          } catch (Exception e) {
            e.printStackTrace();
          }
          return "";
        });
  }

  public static void notifyAllListeners() {
    ExecutorService es = Executors.newSingleThreadExecutor();
    es.submit(
        () -> {
          try {
            // currently 3 sec. will adjust to ~1 sec. later
            Thread.sleep(3000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        });
    es.submit(
        () -> {
          synchronized (listeners) {
            listeners.forEach(
                (listener) -> {
                  listener.write("data: " + "id=1234" + "\n\n");
                  listener.flush();
                });
          }
        });
  }
}
