package org.codice.ddf.migration.commands;

import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationInformation;
import org.codice.ddf.migration.MigrationSuccessfulInformation;
import org.codice.ddf.migration.MigrationWarning;
import org.fusesource.jansi.Ansi;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MigrationCommandTest extends AbstractMigrationCommandTest {
    private static final String MESSAGE = "test message.";

    private MigrationCommand command;

    @Before
    public void setup() throws Exception {
        command = initCommand(Mockito.mock(MigrationCommand.class,
                Mockito.withSettings()
                        .useConstructor()
                        .defaultAnswer(Mockito.CALLS_REAL_METHODS)));
    }

    @Test
    public void testConstructor() throws Exception {
        Assert.assertThat(command.defaultExportDirectory,
                Matchers.equalTo(ddfHome.resolve(MigrationCommand.EXPORTED)));
    }

    @Test
    public void testOutputErrorMessage() throws Exception {
        command.outputErrorMessage(MESSAGE);

        verifyConsoleOutput(MESSAGE, Ansi.Color.RED);
    }

    @Test
    public void testOutputMessageWithException() throws Exception {
        command.outputMessage(new MigrationException(MESSAGE));

        verifyConsoleOutput(MESSAGE, Ansi.Color.RED);
    }

    @Test
    public void testOutputMessageWithWarning() throws Exception {
        command.outputMessage(new MigrationWarning(MESSAGE));

        verifyConsoleOutput(MESSAGE, Ansi.Color.YELLOW);
    }

    @Test
    public void testOutputMessageWithSuccess() throws Exception {
        command.outputMessage(new MigrationSuccessfulInformation(MESSAGE));

        verifyConsoleOutput(MESSAGE, Ansi.Color.GREEN);
    }

    @Test
    public void testOutputMessageWithInfo() throws Exception {
        command.outputMessage(new MigrationInformation(MESSAGE));

        verifyConsoleOutput(MESSAGE, Ansi.Color.WHITE);
    }
}
