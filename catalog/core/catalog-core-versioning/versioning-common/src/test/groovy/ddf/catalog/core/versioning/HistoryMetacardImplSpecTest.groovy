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
package ddf.catalog.core.versioning

import ddf.catalog.data.Metacard
import ddf.catalog.data.impl.BasicTypes
import ddf.catalog.data.impl.MetacardImpl
import org.apache.shiro.SecurityUtils
import org.apache.shiro.subject.Subject
import org.apache.shiro.util.ThreadContext
import spock.lang.Specification

import java.time.Instant

import static MetacardVersion.Action

class HistoryMetacardImplSpecTest extends Specification {

    void setup() {
        ThreadContext.bind(Mock(Subject))

    }

    void cleanup() {
        ThreadContext.unbindSubject()
    }

    def "History Metacard Creation"() {
        setup:
        def meta = defaultMetacard()
        Action action = Action.CREATED
        Instant start = Instant.now()

        when:
        MetacardVersion history = new MetacardVersion(
                meta.metacard,
                action,
                SecurityUtils.subject)

        then: $/All the original attributes should be there except for:
                    - Old tags should be stored in `tagsHistory
                    - new tag should be `history`
                    - Old id should be in `idHistory` /$
        !history.id.equals(meta.id)
        history.versionOfId.equals(meta.id)
        history.tags.containsAll([MetacardVersion.VERSION_TAG])
        !history.tags.any { meta.tags.contains(it) }
        history.metadata.equals(meta.metadata)
        history.title.equals(meta.title)
        history.action.equals(action)
        history.versionTags.containsAll(meta.tags)

        Instant finish = Instant.now()
        Instant versionMoment = history.versionedOn.toInstant()
        versionMoment.isAfter(start)
        versionMoment.isBefore(finish)
    }

    def "History Metacard back to Basic Metacard"() {
        setup:
        def meta = defaultMetacard()
        Action action = Action.CREATED
        MetacardVersion history = new MetacardVersion(
                meta.metacard,
                action,
                SecurityUtils.subject)

        when:
        Metacard metacard = history.toBasicMetacard()

        then:
        metacard != null
        history.versionOfId.equals(metacard.id)
        metacard.tags.containsAll(meta.tags)
        !metacard.tags.contains(MetacardVersion.VERSION_TAG)
    }

    def defaultMetacard() {
        def res = [:]
        res.id = "OriginalMetacardId"
        res.metadata = "<title>blablabla my metadata</title>"
        res.title = "Title"
        res.tags = [Metacard.DEFAULT_TAG]

        MetacardImpl metacard = new MetacardImpl(BasicTypes.BASIC_METACARD);
        metacard.id = res.id
        metacard.tags = res.tags
        metacard.metadata = res.metadata
        metacard.title = res.title
        res.metacard = metacard
        return res
    }
}