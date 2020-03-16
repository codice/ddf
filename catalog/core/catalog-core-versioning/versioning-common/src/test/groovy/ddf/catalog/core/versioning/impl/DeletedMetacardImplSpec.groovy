/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.core.versioning.impl

import ddf.catalog.core.versioning.DeletedMetacard
import ddf.catalog.data.Metacard
import ddf.catalog.data.impl.AttributeImpl
import ddf.catalog.data.impl.MetacardImpl
import org.apache.shiro.subject.Subject
import org.apache.shiro.util.ThreadContext
import spock.lang.Specification


class DeletedMetacardImplSpec extends Specification {

    void setup() {
        ThreadContext.bind(Mock(Subject))

    }

    void cleanup() {
        ThreadContext.unbindSubject()
    }

    def "Tags of metacard being deleted are stored in the deleted metacard"() {
        setup:
        def id = "OriginalMetacardId"
        def title = "Title"
        def tags = ['a-special-tag', 'a-second-special-tag'] as Set
        Metacard metacard = new MetacardImpl().with {
            setAttribute(ID, id)
            setAttribute(TITLE, title)
            setAttribute(new AttributeImpl(TAGS, new ArrayList<>(tags)))
            return it
        }

        when: "deleted metacards are created from a metacard with specific tags"
        def deletedMetacard = new DeletedMetacardImpl(
                "DeletedMetacardId",
                id,
                "UserSubjectName",
                "VersionMetacardId",
                metacard)

        then: "deleted metacards have those specific tags for searchability"
        deletedMetacard.
                getAttribute(DeletedMetacard.DELETED_METACARD_TAGS)?.
                values?.
                containsAll(tags)

        when: "deleted metacard has no tags (which would be odd)"
        Metacard metacardNoTags = new MetacardImpl().with {
            setAttribute(ID, id)
            setAttribute(TITLE, title)
            return it
        }

        def deletedMetacardNoTags = new DeletedMetacardImpl(
                "DeletedMetacardId",
                id,
                "UserSubjectName",
                "VersionMetacardId",
                metacardNoTags)
        then: "nothing bad happens and there is no deleted_metacard_tags"
        noExceptionThrown()
        // Metacard interface does not dictate whether an empty set is "non existant" and will
        // be treated as a delete aka not in the map and returns null or will be treated as a
        // value and set as an empty attribute value
        deletedMetacardNoTags.getAttribute(DeletedMetacard.DELETED_METACARD_TAGS) == null ||
                deletedMetacardNoTags.
                        getAttribute(DeletedMetacard.DELETED_METACARD_TAGS).
                        values.
                        isEmpty()
    }


}