package org.codice.ddf.catalog.ui.events;

import com.google.gson.*;
import java.lang.reflect.Type;

public class EventTypeDeserializer implements JsonDeserializer<EventType> {

  @Override
  public EventType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    return new EventType(json.getAsString());
  }
}
