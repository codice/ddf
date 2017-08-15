package org.codice.ddf.configuration.migration;

import java.util.Optional;
import java.util.stream.Stream;

import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationWarning;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class AbstractMigrationReportTest extends AbstractMigrationTest {
    protected final static String[] MESSAGE_STRINGS =
            new String[] {"warning1", "info2", "error3", "info4", "info5", "warning6", "error7",
                    "error8", "warning9", "warning10", "info11"};

    protected final static String[] POTENTIAL_WARNING_MESSAGE_STRINGS = Stream.of(MESSAGE_STRINGS)
            .filter(m -> !m.startsWith("info"))
            .toArray(String[]::new);

    protected final static String[] ERRORS = Stream.of(MESSAGE_STRINGS)
            .filter(m -> m.startsWith("error"))
            .toArray(String[]::new);

    protected final static String[] WARNINGS = Stream.of(MESSAGE_STRINGS)
            .filter(m -> m.startsWith("warning"))
            .toArray(String[]::new);

    protected final static String[] INFOS = Stream.of(MESSAGE_STRINGS)
            .filter(m -> m.startsWith("info"))
            .toArray(String[]::new);

    protected final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.EXPORT,
            Optional.empty());

    protected final MigrationException[] EXCEPTIONS = new MigrationException[ERRORS.length];

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void baseReportBefore() {
        int i = 0;

        for (final String msg : MESSAGE_STRINGS) {
            if (msg.startsWith("info")) {
                REPORT.record(msg);
            } else if (msg.startsWith("warning")) {
                REPORT.record(new MigrationWarning(msg));
            } else {
                final MigrationException e = new MigrationException(msg);

                EXCEPTIONS[i++] = e;
                REPORT.record(e);
            }
        }
    }
}
