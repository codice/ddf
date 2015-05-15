package org.codice.ddf.platform.util;

/**
 * This utility class defines methods useful to perform on throwable or exception objects.
 */
public final class Exceptions {

    private Exceptions() {
        // as a utility this should never be constructed, hence it's private
    }

    /**
     * Given a throwable, this traces back through the causes to construct a full message.
     *
     * @param th
     * @return
     */
    public static String getFullMessage(Throwable th) {
        StringBuilder message = new StringBuilder(th.getMessage());
        th = th.getCause();
        while (th != null) {
            message.insert(0, th.getMessage() + "\n");
            th = th.getCause();
        }
        return message.toString();
    }
}
