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
package org.codice.ddf.itests.common.restito;

import static com.xebialabs.restito.semantics.Action.composite;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.custom;
import static com.xebialabs.restito.semantics.Action.header;
import static java.lang.Thread.sleep;

import com.xebialabs.restito.semantics.Action;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.lang.StringUtils;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encompasses the response for a product retrieval for a Restito stub server. Allows for the simple
 * setup of specific responses, slow sending, and simulated network failure retry testing.
 */
public class ChunkedContent {
  protected static final Logger LOGGER = LoggerFactory.getLogger(ChunkedContent.class);

  private ChunkedContent() {}

  public static class ChunkedContentBuilder {
    private String message;

    private Duration delayBetweenChunks = Duration.ofMillis(0);

    private int numberOfFailures = 0;

    /**
     * Set message.
     *
     * @param message Message to be sent in the response.
     */
    public ChunkedContentBuilder(String message) {
      this.message = message;
    }

    /**
     * Set delay between sending each character of the message in milliseconds.
     *
     * @param delay Time to wait between sending each character of the message.
     * @return Builder object
     */
    public ChunkedContentBuilder delayBetweenChunks(Duration delay) {
      this.delayBetweenChunks = delay;
      return this;
    }

    /**
     * Number of times to fail (simulate network disconnect) after sending the first character. Once
     * this number is reached, the message will send successfully.
     *
     * @param numberOfFailures Number of times to fail (simulate network disconnect) after sending
     *     the first character. Once this number is reached, the message will send successfully.
     * @return Builder object
     */
    public ChunkedContentBuilder fail(int numberOfFailures) {
      this.numberOfFailures = numberOfFailures;
      return this;
    }

    /**
     * Builds Action.
     *
     * @return Action constructed from builder object.
     */
    public Action build() {
      return createChunkedContent(message, delayBetweenChunks, numberOfFailures);
    }
  }

  private static Action getChunkedResponseHeaders() {
    return composite(
        contentType("text/plain"),
        header("Transfer-Encoding", "chunked"),
        header("content-type", "text/plain"));
  }

  private static Action getRangeSupportHeaders() {
    return header("Accept-Ranges", "bytes");
  }

  /**
   * Returns a composite action that holds the headers required for a plain text response as well as
   * the response function itself. Unless the headers included are not wanted, this is the preferred
   * way to set the Restito response actions.
   *
   * <p>Note that additional headers may also be needed for specific endpoint functionality. This
   * method returns a composite action so that additional headers can be set alongside this action.
   *
   * @param responseMessage Message to be sent in response.
   * @param delayBetweenChunks Time to wait between sending each character of the message.
   * @param numberOfFailures Number of times to fail (simulate network disconnect) after sending the
   *     first character. Once this number is reached, the message will send successfully.
   * @return composite action that holds the headers required for a plain text response as well as
   *     the response function itself.
   */
  private static Action createChunkedContent(
      String responseMessage, Duration delayBetweenChunks, int numberOfFailures) {

    Action response =
        composite(
            getChunkedResponseHeaders(),
            custom(
                new ChunkedContentFunction(responseMessage, delayBetweenChunks, numberOfFailures)));

    // adds the Accept-Ranges header for range-header support
    response = composite(getRangeSupportHeaders(), response);

    return response;
  }

  /** Private inner class representing the response function */
  private static class ChunkedContentFunction implements Function<Response, Response> {
    private char[] responseMessage;

    private long messageDelayMs;

    private int numberOfFailures;

    private int numberOfRetries;

    /**
     * Implementation of the Function interface's apply method. This class can also be used as a
     * Function<Response, Response> directly in the Restito response if custom actions are needed.
     *
     * @param response Response object correlating to the incoming request. Used to write data back
     *     to the requesting client.
     * @return Response New state of the response object correlating to the incoming request.
     */
    @Override
    public Response apply(Response response) {
      return respond(response);
    }

    /**
     * Constructor for a response that has a delay and a number of planned failures. Supports range
     * headers.
     *
     * @param responseMessage Message to be sent in response.
     * @param messageDelay Time to wait between sending each character of the message.
     * @param numberOfFailures Number of times to fail (simulate network disconnect) after sending
     *     the first character. Once this number is reached, the message will send successfully.
     */
    private ChunkedContentFunction(
        String responseMessage, Duration messageDelay, int numberOfFailures) {
      this.responseMessage = responseMessage.toCharArray();
      this.messageDelayMs = messageDelay.toMillis();
      this.numberOfFailures = numberOfFailures;
    }

    private Response respond(Response response) {
      Map<String, String> requestHeaders = Collections.emptyMap();

      // if range header is present, return 206 - Partial Content status and set Content-Range
      // header if byte Offset is specified
      ByteRange byteRange;
      if (StringUtils.isNotBlank(requestHeaders.get("range"))) {
        byteRange = new ByteRange(requestHeaders.get("range"), responseMessage.length);
        response.setStatus(HttpStatus.PARTIAL_CONTENT_206);
        response.setHeader("Content-Range", byteRange.contentRangeValue());
        LOGGER.debug(
            "ChunkedContentResponse: Response range header set to [Content-Range: {}]",
            byteRange.contentRangeValue());
      } else {
        response.setStatus(HttpStatus.OK_200);
        byteRange = new ByteRange(0, responseMessage.length - 1);
        LOGGER.debug("ChunkedContentResponse: set response status to 200");
      }

      return send(response, byteRange);
    }

    /**
     * Sends message to the client one character at a time. Appropriate headers must be set on the
     * Response object before calling this method.
     *
     * @param response Response object containing an output stream to the client
     * @param byteRange Object containing the range of bytes to send to the client
     * @return
     */
    private Response send(Response response, ByteRange byteRange) {
      // send each character, respecting the range header
      for (int i = byteRange.start; i <= byteRange.end; i++) {
        try {
          LOGGER.debug("ChunkedContentResponse: Sending character [{}]", responseMessage[i]);
          response.getNIOWriter().write(responseMessage[i]);
          response.flush();
          sleep(messageDelayMs);

          // fail download by ungracefully closing the output buffer to simulate connection lost
          if (numberOfRetries < numberOfFailures) {
            response.getOutputBuffer().recycle();
            numberOfRetries++;
            return response;
          }
        } catch (IOException | InterruptedException e) {
          LOGGER.error("Error", e);
          break;
        }
      }
      response.finish();
      return response;
    }

    /**
     * Holds byte offset data specified by the request's range header and any related parsing
     * methods.
     */
    private class ByteRange {
      public final int start;

      public final int end;

      ByteRange(int start, int end) {
        this.start = start;
        this.end = end;
      }

      /**
       * Tokenizes the starting and ending bytes values from a range header. The stub server only
       * advertising supporting byte offsets specifically, so this function assumes that byte ranges
       * are provided and does not evaluate the unit of measurement
       *
       * @param rangeHeaderValue Value of the range header sent in the request
       * @param totalSizeOfProductInBytes Total size of the message to be returned. This is not the
       *     partial content size, but the FULL size of the product.
       */
      ByteRange(String rangeHeaderValue, int totalSizeOfProductInBytes) {
        // extract bytes
        String startToken = StringUtils.substringBetween(rangeHeaderValue, "=", "-");
        String endToken = StringUtils.substringAfter(rangeHeaderValue, "-");

        // range offsets can be blank to indicate "until beginning" or "until end" of data.
        try {
          if (StringUtils.isBlank(startToken)) {
            start = 0;
          } else {
            start = Integer.parseInt(startToken);
          }
          if (StringUtils.isBlank(endToken)) {
            end = totalSizeOfProductInBytes - 1;
          } else {
            end = Integer.parseInt(endToken);
          }
        } catch (NumberFormatException e) {
          LOGGER.error(
              "Incoming request's range header is improperly formatted: [range={}]",
              rangeHeaderValue,
              e);
          throw e;
        }
      }

      String contentRangeValue() {
        return "bytes " + start + "-" + end + "/" + responseMessage.length;
      }
    }
  }
}
