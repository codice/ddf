package org.codice.ddf.catalog.ui.events;

import static spark.Spark.get;

import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.servlet.SparkApplication;

public class EventApplication implements SparkApplication {

  private static final Map<String, PrintWriter> listeners = new ConcurrentHashMap<>();

  private static final Logger LOGGER = LoggerFactory.getLogger(EventApplication.class);

  // HEARTBEAT is used to write silent messages to keep the client connection. It is also used to
  // trigger events
  // by sending it with the data: tag. For more information, see
  // https://developer.mozilla.org/en-US/docs/Web/API/Server-sent_events/Using_server-sent_events#Event_stream_format
  private static final String HEARTBEAT = ": \n\n";

  @Override
  public void init() {
    get(
        "/events/:id",
        (req, res) -> {
          try {
            String sourceId = req.params(":id");
            if (listeners.size() >= 5) {
              notifyListener(sourceId, "close");
              res.status(429);
              return "Too many HTTP connections - cannot create new Event Source";
            }
            res.type("text/event-stream; charset=UTF-8");
            res.header("Connection", "keep-alive");
            res.header("Cache-Control", "no-cache");

            PrintWriter out = res.raw().getWriter();

            res.status(200);
            listeners.put(sourceId, out);
            while (!out.checkError()) {
              out.write(HEARTBEAT);
              out.flush();
              Thread.sleep(15000);
            }
            notifyListener(sourceId, "close");
            listeners.remove(sourceId);
            return "Event Source connection closed";
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
            listeners
                .values()
                .forEach(
                    (listener) -> {
                      listener.write("event: " + type + "\n");
                      listener.write("data" + HEARTBEAT);
                      listener.flush();
                    });
          }
        });
  }

  public static void notifyListener(String id, String type) {
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
          listeners.get(id).write("event: " + type + "\n");
          listeners.get(id).write("data" + HEARTBEAT);
          listeners.get(id).flush();
        });
  }
}
