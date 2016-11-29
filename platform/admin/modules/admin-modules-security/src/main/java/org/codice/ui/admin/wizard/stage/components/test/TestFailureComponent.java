package org.codice.ui.admin.wizard.stage.components.test;

import org.codice.ui.admin.wizard.stage.components.Component;

public class TestFailureComponent extends Component<String> implements TestComponent {

    public TestFailureComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        // This component should be removed previous to validation
    }

}
