package ddf.common.test.restito;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds byte offset data specified by the request's range header and any related parsing
 * methods.
 */
class ByteRange {
    private static final Logger LOGGER = LoggerFactory.getLogger(ByteRange.class);

    private final char[] responseMessage;

    final int start;

    final int end;

    ByteRange(int start, int end, char[] responseMessage) {
        this.responseMessage = responseMessage;
        this.start = start;
        this.end = end;
    }

    /**
     * Tokenizes the starting and ending bytes values from a range header. The stub server only
     * advertising supporting byte offsets specifically, so this function assumes that byte
     * ranges are provided and does not evaluate the unit of measurement
     *
     * @param rangeHeaderValue          Value of the range header sent in the request
     * @param totalSizeOfProductInBytes Total size of the message to be returned. This is
     *                                  not the partial content size, but the FULL size of
     *                                  the product.
     */
    ByteRange(String rangeHeaderValue, int totalSizeOfProductInBytes, char[] responseMessage) {
        this.responseMessage = responseMessage;
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
            LOGGER.error("Incoming request's range header is improperly formatted: [range={}]",
                    rangeHeaderValue,
                    e);
            throw e;
        }
    }

    String contentRangeValue() {
        return "bytes " + start + "-" + end + "/" + responseMessage.length;
    }
}
