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

import ddf.catalog.core.versioning.MetacardVersion
import ddf.catalog.data.Metacard
import ddf.catalog.data.impl.AttributeDescriptorImpl
import ddf.catalog.data.impl.AttributeImpl
import ddf.catalog.data.impl.BasicTypes
import ddf.catalog.data.impl.MetacardImpl
import ddf.catalog.data.impl.MetacardTypeImpl
import org.apache.shiro.SecurityUtils
import org.apache.shiro.subject.Subject
import org.apache.shiro.util.ThreadContext
import spock.lang.Specification

import java.time.Instant

import static ddf.catalog.core.versioning.MetacardVersion.Action

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
        MetacardVersionImpl history = new MetacardVersionImpl(
                meta.metacard,
                action,
                SecurityUtils.subject)

        then: $/All the original attributes should be there except for:
                    - Old tags should be stored in `tagsHistory
                    - new tag should be `history`
                    - Old id should be in `idHistory` /$
        history.id != meta.id
        history.versionOfId == meta.id
        history.tags.containsAll([MetacardVersion.VERSION_TAG])
        !history.tags.any { meta.tags.contains(it) }
        history.metadata == meta.metadata
        history.title == meta.title
        history.action == action
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
        MetacardVersionImpl history = new MetacardVersionImpl(
                meta.metacard as Metacard,
                action,
                SecurityUtils.subject)

        when:
        Metacard metacard = history.getMetacard([BasicTypes.BASIC_METACARD])

        then:
        metacard != null
        history.versionOfId == metacard.id
        metacard.tags.containsAll(meta.tags)
        !metacard.tags.contains(MetacardVersion.VERSION_TAG)
        metacard.title == meta.metacard.title
    }

    def "Non default metacard type back to type from existing list"() {
        setup:
        def meta = nonBasicMetacard()
        Action action = Action.CREATED

        when: "History items are created from non basic metacard types"
        MetacardVersionImpl history = new MetacardVersionImpl(
                meta.metacard as Metacard,
                action,
                SecurityUtils.subject,
                [BasicTypes.BASIC_METACARD, meta.metacardType])

        then: "The metacard type should contain non default attribute descriptors"
        meta.attributeDescriptor in history.metacardType.attributeDescriptors
        history.metacardType.attributeDescriptors.containsAll(
                meta.metacard.metacardType.attributeDescriptors)

        when:
        Metacard metacard = history.getMetacard([BasicTypes.BASIC_METACARD, meta.metacardType])

        then:
        metacard != null
        history.versionOfId == metacard.id
        metacard.tags.containsAll(meta.tags)
        !metacard.tags.contains(MetacardVersion.VERSION_TAG)
        metacard.getAttribute(meta.attributeDescriptor.name)?.value == meta.attributeValue
        meta.metacard.metacardType.attributeDescriptors.containsAll(metacard.metacardType.attributeDescriptors)


    }

    def "Non default metacard type back to type from serialized type"() {
        setup:
        def meta = nonBasicMetacard()
        Action action = Action.CREATED

        when: "History items are created from non basic metacard types"
        MetacardVersionImpl history = new MetacardVersionImpl(
                meta.metacard as Metacard,
                action,
                SecurityUtils.subject)

        then: "ensure the type-binary attribute is there"
        history.getAttribute(MetacardVersion.VERSION_TYPE_BINARY)?.value != null

        when: "reconstructing metacard with only serialized type available"
        Metacard metacard = history.getMetacard([BasicTypes.BASIC_METACARD])

        then: "ensure metacard and type are properly restored"
        metacard.metacardType == meta.metacard.metacardType
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

    def nonBasicMetacard() {
        def res = [:]
        res.id = "NonBasicMetacardTypeId"
        res.title = "NonBasic Title"
        res.tags = ["NonBasicTags"]
        res.attributeDescriptor = new AttributeDescriptorImpl(
                "New Attribute Name",
                true,
                true,
                false,
                false,
                BasicTypes.STRING_TYPE)
        res.metacardType = new MetacardTypeImpl(
                "NonBasicType",
                BasicTypes.BASIC_METACARD, [res.attributeDescriptor] as Set)
        res.metacard = new MetacardImpl(res.metacardType)
        res.attributeValue = "My New Attribute Value"
        res.metacard.with {
            id = res.id
            title = res.title
            tags = res.tags
            it.setAttribute(
                    new AttributeImpl(res.attributeDescriptor.name, res.attributeValue))
        }
        return res
    }
}