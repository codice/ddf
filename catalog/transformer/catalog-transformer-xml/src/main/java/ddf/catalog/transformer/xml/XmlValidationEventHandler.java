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
package ddf.catalog.transformer.xml;

import java.net.URL;
import java.text.MessageFormat;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * This class extends the default Oracle handler in a effort to avoid printing to the console and
 * instead log exceptions and errors. The class should exhibit the same behavior and perform the
 * same as the DefaultValidationEventHandler otherwise.
 */
public class XmlValidationEventHandler extends DefaultValidationEventHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlInputTransformer.class);

  private static final String SEVERITY_MESSAGE = "XmlValidationEventHandler.SeverityMessage";

  private static final String UNRECOGNIZED_SEVERITY =
      "XmlValidationEventHandler.UnrecognizedSeverity";

  private static final String WARNING = "XmlValidationEventHandler.Warning";

  private static final String ERROR = "XmlValidationEventHandler.Error";

  private static final String FATAL_ERROR = "XmlValidationEventHandler.FatalError";

  private static final String LOCATION_UNAVAILABLE =
      "XmlValidationEventHandler.LocationUnavailable";

  @Override
  public boolean handleEvent(ValidationEvent event) {

    if (event == null) {
      LOGGER.debug("XmlValidationEventHandler handleEvent was called with a null ValidationEvent.");
      throw new IllegalArgumentException();
    }

    // calculate the severity prefix and return value
    String severity = null;
    boolean retVal = false;
    switch (event.getSeverity()) {
      case ValidationEvent.WARNING:
        severity = format(WARNING);
        retVal = true; // continue after warnings
        break;
      case ValidationEvent.ERROR:
        severity = format(ERROR);
        retVal = false; // terminate after errors
        break;
      case ValidationEvent.FATAL_ERROR:
        severity = format(FATAL_ERROR);
        retVal = false; // terminate after fatal errors
        break;
      default:
        assert false : format(UNRECOGNIZED_SEVERITY, event.getSeverity());
    }

    // calculate the location message
    String message = format(SEVERITY_MESSAGE, severity, event.getMessage(), getLoc(event));

    LOGGER.debug(message);

    // fail on the first error or fatal error
    return retVal;
  }

  /** Calculate a location message for the event */
  private String getLoc(ValidationEvent event) {
    StringBuilder msg = new StringBuilder();

    ValidationEventLocator locator = event.getLocator();

    if (locator != null) {

      URL url = locator.getURL();
      Object obj = locator.getObject();
      Node node = locator.getNode();
      int line = locator.getLineNumber();

      if (url != null || line != -1) {
        msg.append("line ").append(line);
        if (url != null) msg.append(" of ").append(url);
      } else if (obj != null) {
        msg.append(" obj: ").append(obj.toString());
      } else if (node != null) {
        msg.append(" node: ").append(node.toString());
      }
    } else {
      msg.append(format(LOCATION_UNAVAILABLE));
    }

    return msg.toString();
  }

  /** Loads a string resource and formats it with specified arguments. */
  private String format(String property, Object... args) {
    return MessageFormat.format(property, args);
  }
}
