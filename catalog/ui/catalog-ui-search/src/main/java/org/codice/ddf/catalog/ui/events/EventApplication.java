package org.codice.ddf.catalog.ui.events;

import static spark.Spark.get;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.servlet.SparkApplication;

public class EventApplication implements SparkApplication {

  private static final List<PrintWriter> listeners = new ArrayList<>();

  private static final Logger LOGGER = LoggerFactory.getLogger(EventApplication.class);

  private static final String HEARTBEAT = ": \n\n";

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
            out.write(HEARTBEAT);
            out.flush();

            while (true) {
              out.write(HEARTBEAT);
              if (out.checkError()) {
                // Subscriber error, break out of loop
                break;
              }
              Thread.sleep(1000);
            }
            listeners.remove(out);
            return "Event Source setup successful";
          } catch (Exception e) {
            LOGGER.error("Event Source configuration error");
          }
          res.status(500);
          return "Event Source configuration error: Server error";
        });
  }

  public static void notifyListeners(String type) {
    ExecutorService es = Executors.newSingleThreadExecutor();
    es.submit(
        () -> {
          try {
            Thread.sleep(3000);
          } catch (InterruptedException e) {
            LOGGER.error("Event Source notification error");
          }
        });
    es.submit(
        () -> {
          synchronized (listeners) {
            listeners.forEach(
                (listener) -> {
                  listener.write("event: " + type + "\n");
                  listener.write("data" + HEARTBEAT);
                  listener.flush();
                });
          }
        });
  }
}
