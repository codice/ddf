package ddf.catalog.plugin.hanging;

import static java.lang.Thread.sleep;

import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import java.util.Random;

@SuppressWarnings("InfiniteLoopStatement")
public class HangingIngestPlugin implements PreIngestPlugin {

  Random random = new Random();

  @Override
  public CreateRequest process(CreateRequest input) {
    while (true) {
      try {
        sleep(5000);
      } catch (InterruptedException e) {
      }
    }
  }

  @Override
  public UpdateRequest process(UpdateRequest input)
      throws PluginExecutionException, StopProcessingException {
    return null;
  }

  @Override
  public DeleteRequest process(DeleteRequest input)
      throws PluginExecutionException, StopProcessingException {
    return null;
  }
}
