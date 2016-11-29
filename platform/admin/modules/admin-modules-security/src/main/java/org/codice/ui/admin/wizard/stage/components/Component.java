package org.codice.ui.admin.wizard.stage.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codice.ui.admin.wizard.stage.components.test.TestComponent;
import org.codice.ui.admin.wizard.stage.components.test.TestFailureComponent;
import org.codice.ui.admin.wizard.stage.components.test.TestInfoComponent;
import org.codice.ui.admin.wizard.stage.components.test.TestSuccessComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Component<T> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(Component.class);

    private String id;

    private String label;

    // TODO: tbatie - 11/1/16 - This could probably be turned back into a list as long as add(index, element) works the same
    private LinkedList<Component> children;

    private String error;

    private T value;

    private Boolean disabled;

    private String description;

    protected Boolean required = true;

    private List<T> defaults;

    public Component(String id) {
        this.id = id;
        children = new LinkedList<>();
        defaults = new ArrayList<>();
    }

    public abstract void validate();

    public void validateAll() {
        validate();

        for (Component component : children) {
            component.validate();
        }
    }

    public void addError(String error) {
        this.error = error;
    }

    /**
     * Clears all errors from this component and all sub components
     */
    public void clearAllErrors() {

        children = children.stream()
                .filter(component -> !(component instanceof ErrorInfoComponent || component instanceof TestComponent))
                .collect(Collectors.toCollection(LinkedList::new));

        error = null;
        if (children != null) {
            for (Component subComponent : children) {
                subComponent.clearAllErrors();
            }
        }
    }

    /**
     * Checks this component and all sub components for errors
     *
     * @return
     */
    public boolean containsErrors() {
        if (error != null) {
            return true;
        }
        // TODO: tbatie - 11/4/16 - Rename this method since it relies on TestInfoCOmponent which technically is not an error
        return children != null && children.stream()
                .anyMatch(p -> p instanceof ErrorInfoComponent || p instanceof TestFailureComponent || p instanceof TestInfoComponent || p.containsErrors() );
    }

    public Component getComponent(String componentId) {
        if (id != null && componentId.equals(id)) {
            return this;
        }

        if (children != null) {
            for (Component subComponent : children) {
                if (subComponent.getId() != null && componentId.equals(id)) {
                    return subComponent;
                }

                Component evenMoreSubComponent = subComponent.getComponent(componentId);
                if (evenMoreSubComponent != null) {
                    return evenMoreSubComponent;
                }
            }
        }
        return null;
    }

    // TODO: tbatie - 10/31/16 - It's late, I don't want to do index checking, fix me
    public void addChildBeforeComponent(String prevChildId, Component newChild) {
        // TODO: tbatie - 11/1/16 - Iterate over the list instead, then you can use the index i, else you're doing 2*n when you call indexOf
        Optional<Component> foundChild = children.stream()
                .filter(child -> prevChildId.equals(child.getId()))
                .findFirst();

        if(foundChild.isPresent()) {
            children.add(children.indexOf(foundChild.get()), newChild);
        } else {
            for(Component child : children) {
                child.addChildBeforeComponent(prevChildId, newChild);
            }
        }
    }

    // TODO: tbatie - 10/31/16 - It's late, I don't want to do index checking, fix me
    public void addChildAfterComponent(String prevChildId, Component newChild) {
        Optional<Component> foundChild = children.stream()
                .filter(child -> prevChildId.equals(child.getId()))
                .findFirst();

        if(foundChild.isPresent()) {
            children.add(children.indexOf(foundChild.get()) + 1, newChild);
        } else {
            for(Component child : children) {
                child.addChildBeforeComponent(prevChildId, newChild);
            }
        }
    }

    public LinkedList<Component> getChildren() {
        return children;
    }

    public void clearChildren() {
        children = new LinkedList<>();
    }

    public String getId() {
        return id;
    }

    public T getValue() {
        return value;
    }

    public Map<String, Object> getComponentValues() {
        return getComponentValues(new HashMap<>());
    }

    protected Map<String, Object> getComponentValues(Map<String, Object> componentMap) {

        if (this.getId() != null && getValue() != null) {
            componentMap.put(getId(), getValue());
        }

        for (Component component : getChildren()) {
            component.getComponentValues(componentMap);
        }

        return componentMap;
    }

    //
    //  Builder Methods
    //
    public static <T, S extends Component<T>> S builder(Class<S> component, String id) {
        S newInstance = null;
        try {
            // TODO: tbatie - 10/31/16 - Probably shouldnt be use reflection for this >.>
            newInstance = component.getDeclaredConstructor(String.class).newInstance(id);
        } catch (Exception e) {
            LOGGER.error("Unable to make component of type {} and id of {}", component, id);
        }
        return newInstance;
    }

    public static <T, S extends Component<T>> S builder(Class<S> component) {
        return builder(component, null);
    }

    public Component<T> defaults(T... defaults) {
        this.defaults.addAll(Arrays.asList(defaults));
        if(value == null && !this.defaults.isEmpty()) {
            value(this.defaults.get(0));
        }
        return this;
    }

    public Component<T> defaults(Collection<T> defaults) {
        this.defaults.addAll(defaults);
        if(value == null && !this.defaults.isEmpty()) {
            value(this.defaults.get(0));
        }

        return this;
    }

    public Component<T> label(String label) {
        this.label = label;
        return this;
    }

    public Component<T> subComponents(Component... subComponents) {
        this.children.addAll(Arrays.asList(subComponents));
        return this;
    }

    public Component<T> subComponents(List<Component> subComponents) {
        this.children.addAll(subComponents);
        return this;
    }

    public Component<T> isDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    public Component<T> disabled() {
        this.disabled = true;
        return this;
    }

    public Component<T> required(boolean required) {
        this.required = required;
        return this;
    }

    public Component<T> value(T value) {
        this.value = value;
        return this;
    }

    public Component description(String description){
        this.description = description;
        return this;
    }
}
