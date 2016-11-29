package org.codice.ui.admin.wizard.stage.components.test;

import org.codice.ui.admin.wizard.stage.components.Component;

public class TestSuccessComponent extends Component<String> implements TestComponent {

    public TestSuccessComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        // This component should be removed previous to validation
    }

}
