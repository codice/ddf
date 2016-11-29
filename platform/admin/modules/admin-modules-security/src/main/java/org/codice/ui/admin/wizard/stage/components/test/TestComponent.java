package org.codice.ui.admin.wizard.stage.components.test;

import static org.codice.ui.admin.wizard.api.ConfigurationTestMessage.MessageType.FAILURE;
import static org.codice.ui.admin.wizard.api.ConfigurationTestMessage.MessageType.SUCCESS;

import org.codice.ui.admin.wizard.api.ConfigurationTestMessage;
import org.codice.ui.admin.wizard.stage.components.Component;

// TODO: tbatie - 11/4/16 - Used for determining whether a message should be removed upon submission
public interface TestComponent {

    static Component createTestComponent(ConfigurationTestMessage testMessage) {
        switch (testMessage.getType()) {
        case FAILURE:
            return new TestFailureComponent(null).label(testMessage.getMessage());
        case SUCCESS:
            return new TestSuccessComponent(null).label(testMessage.getMessage());
        case INFO:
            return new TestInfoComponent(null).label(testMessage.getMessage());
        }

        return null;
    }

}
