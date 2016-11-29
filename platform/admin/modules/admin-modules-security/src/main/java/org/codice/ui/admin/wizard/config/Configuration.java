package org.codice.ui.admin.wizard.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.map.HashedMap;

public abstract class Configuration<T extends Enum> {

    private Map<T, Object> values;

    public Configuration(Configuration config) {
        this.values = new HashedMap(config.getValues());
    }

    public Configuration(){
        values = new HashMap<>();
    }

    public void setValues(Map<T, Object> values) {
        this.values = values;
    }

    public void addValue(T key, Object value) {
        values.put(key, value);
    }

    public Object getValue(T key) {
        return values.get(key);
    }

    public abstract Configuration copy();

    public Map<T, Object> getValues() { return values; }
}
