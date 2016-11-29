package org.codice.ui.admin.wizard.stage.components.test;

import org.codice.ui.admin.wizard.stage.components.Component;

public class TestInfoComponent extends Component<String> implements TestComponent {

    public TestInfoComponent(String id) {
        super(id);
    }

    @Override
    public void validate() {
        // This component should be removed previous to validation
    }

}
