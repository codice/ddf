package ddf.catalog.core.versioning;

import ddf.catalog.data.Metacard;

import java.util.function.Function;

/**
 * Experimental. Subject to change.
 * <br/>
 * Represents a currently soft deleted metacard.
 */
public interface DeletedMetacard extends Metacard {
    String PREFIX = "metacard.deleted";

    Function<String, String> PREFIXER = s -> String.format("%s.%s", PREFIX, s);

    String DELETED_TAG = "deleted";

    String DELETED_BY = PREFIXER.apply("deleted-by");

    String DELETION_OF_ID = PREFIXER.apply("id");

    String LAST_VERSION_ID = PREFIXER.apply("version");

}
