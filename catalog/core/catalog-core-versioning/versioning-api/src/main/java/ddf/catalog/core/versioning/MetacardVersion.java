package ddf.catalog.core.versioning;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import ddf.catalog.data.Metacard;

/**
 * Experimental. Subject to change.
 * <br/>
 * Represents a version at a particular instant. Also included are the {@link Action} that was
 * performed, who it was edited by, and what time it was edited on.
 */
public interface MetacardVersion extends Metacard {
    String PREFIX = "metacard.version";

    Function<String, String> PREFIXER = s -> String.format("%s.%s", PREFIX, s);

    ////////////////////////////////////////////////////////////////////////////////////
    // OPERATION PROPERTIES
    ////////////////////////////////////////////////////////////////////////////////////

    String SKIP_VERSIONING = "skip-versioning";

    String HISTORY_METACARDS_PROPERTY = "history-metacards";

    ////////////////////////////////////////////////////////////////////////////////////
    // ATTRIBUTE VALUES
    ////////////////////////////////////////////////////////////////////////////////////

    /**
     * {@link ddf.catalog.data.Attribute} value for {@link ddf.catalog.data.Metacard#TAGS} when
     * a metacard is a History Metacard.
     */
    String VERSION_TAG = "revision";

    ////////////////////////////////////////////////////////////////////////////////////
    // ATTRIBUTE NAMES
    ////////////////////////////////////////////////////////////////////////////////////

    /**
     * {@link ddf.catalog.data.Attribute} name for action of the current {@link MetacardVersion}.
     * Can be one of <code>Created</code>, <code>Updated</code>, or <code>Deleted</code>.
     *
     * @since DDF-2.9.0
     */
    String ACTION = PREFIXER.apply("action");

    /**
     * {@link ddf.catalog.data.Attribute} name for the editor of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    String EDITED_BY = PREFIXER.apply("edited-by");

    /**
     * {@link ddf.catalog.data.Attribute} name for version date of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    String VERSIONED_ON = PREFIXER.apply("versioned-on");

    /**
     * {@link ddf.catalog.data.Attribute} name for metacard ID on a history item of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    String VERSION_OF_ID = PREFIXER.apply("id");

    /**
     * {@link ddf.catalog.data.Attribute} name for original tags of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    String VERSION_TAGS = PREFIXER.apply("tags");

    /**
     * {@link ddf.catalog.data.Attribute} name for original metacard type of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    String VERSION_TYPE = PREFIXER.apply("type");

    /**
     * {@link ddf.catalog.data.Attribute} name for original serialized metacard type  of this {@link Metacard} revision.
     *
     * @since DDF-2.9.0
     */
    String VERSION_TYPE_BINARY = PREFIXER.apply("type-binary");

    /**
     * {@link ddf.catalog.data.Attribute} name for original resource URI of this {@link Metacard} revision.
     */
    String VERSIONED_RESOURCE_URI = PREFIXER.apply("resource-uri");

    enum Action {
        // @formatter:off
        DELETED("Deleted"),
        DELETED_CONTENT("Deleted-Content"),
        VERSIONED("Versioned"),
        VERSIONED_CONTENT("Versioned-Content");
        // @formatter:on

        private static Map<String, Action> keyMap = new HashMap<>();

        static {
            for (Action action : Action.values()) {
                keyMap.put(action.getKey(), action);
            }
        }

        private String key;

        Action(String key) {
            this.key = key;
        }

        public String getKey() {
            return this.key;
        }

        public static Action fromKey(String key) {
            return keyMap.get(key);
        }

        public static Action ofMetacard(Metacard metacard) {
            if (isNotVersion(metacard)) {
                throw new IllegalArgumentException(
                        "Cannot get action of a non version metacard [" + metacard.getId() + "]");
            }
            Serializable svalue = Optional.ofNullable(metacard.getAttribute(ACTION))
                    .map(ddf.catalog.data.Attribute::getValue)
                    .orElse(null);
            if (!(svalue instanceof String)) {
                throw new IllegalArgumentException("The action attribute must be a string");
            }
            String value = (String) svalue;
            return keyMap.get(value);
        }

        public static boolean isNotVersion(Metacard metacard) {
            return !isVersion(metacard);
        }

        public static boolean isVersion(Metacard metacard) {
            return metacard instanceof MetacardVersion || PREFIX.equals(metacard.getMetacardType()
                    .getName());
        }

    }

}