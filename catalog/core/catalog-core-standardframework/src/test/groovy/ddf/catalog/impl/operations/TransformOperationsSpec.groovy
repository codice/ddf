/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.impl.operations

import ddf.catalog.data.BinaryContent
import ddf.catalog.data.Metacard
import ddf.catalog.impl.FrameworkProperties
import ddf.catalog.operation.SourceResponse
import ddf.catalog.transform.MetacardTransformer
import ddf.catalog.transform.QueryResponseTransformer
import org.osgi.framework.BundleContext
import org.osgi.framework.InvalidSyntaxException
import org.osgi.framework.ServiceReference
import spock.lang.Specification

class TransformOperationsSpec extends Specification {
    private BundleContext bundleContext
    private FrameworkProperties frameworkProperties
    private TransformOperations transformOperations

    def setup() {
        bundleContext = Mock(BundleContext)
        frameworkProperties = Mock(FrameworkProperties)
        frameworkProperties.getBundleContext() >> bundleContext

        transformOperations = new TransformOperations(frameworkProperties)
    }

    def 'transform metacard with invalid transformer name'() {
        setup:
        def badId = 'badid'
        bundleContext.getServiceReferences(MetacardTransformer.class.name, badId) >> { String c, String s ->
            throw new InvalidSyntaxException("bad id", s)
        }

        when:
        transformOperations.transform(_ as Metacard, badId as String, _ as Map)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message ==~ /.*${badId}.*/
    }

    def 'transform metacard with null transformers found'() {
        setup:
        bundleContext.getServiceReferences(MetacardTransformer.class.name, _ as String) >> { c, s ->
            return null
        }

        when:
        transformOperations.transform(_ as Metacard, _ as String, _ as Map)

        then:
        thrown(IllegalArgumentException)
    }

    def 'transform metacard with empty transformers found'() {
        setup:
        bundleContext.getServiceReferences(MetacardTransformer.class.name, _ as String) >> { c, s ->
            []
        }

        when:
        transformOperations.transform(_ as Metacard, _ as String, _ as Map)

        then:
        thrown(IllegalArgumentException)
    }

    def 'transform null metacard'() {
        setup:
        def transformer = Mock(MetacardTransformer)
        def ref = Mock(ServiceReference)
        bundleContext.getServiceReferences(MetacardTransformer.class.name, _ as String) >> { c, s ->
            [ref]
        }
        bundleContext.getService(ref) >> { r -> transformer }

        when:
        transformOperations.transform(null as Metacard, _ as String, _ as Map)

        then:
        thrown(IllegalArgumentException)
    }

    def 'transform metacard'() {
        setup:
        def binaryContent = Mock(BinaryContent)
        def transformer = Mock(MetacardTransformer)
        def ref = Mock(ServiceReference)
        bundleContext.getServiceReferences(MetacardTransformer.class.name, _ as String) >> { c, s ->
            [ref]
        }
        bundleContext.getService(ref) >> { r -> transformer }
        transformer.transform(_ as Metacard, _ as Map) >> { met, props ->
            return binaryContent
        }

        when:
        def result = transformOperations.transform(_ as Metacard, _ as String, _ as Map)

        then:
        result == binaryContent
    }

    def 'transform response with invalid transformer name'() {
        setup:
        def badId = 'badid'
        bundleContext.getServiceReferences(QueryResponseTransformer.class.name, badId) >> { String c, String s ->
            throw new InvalidSyntaxException("bad id", s)
        }

        when:
        transformOperations.transform(_ as SourceResponse, badId as String, _ as Map)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message ==~ /.*${badId}.*/
    }

    def 'transform response with no transformers found'() {
        setup:
        bundleContext.getServiceReferences(QueryResponseTransformer.class.name, _ as String) >> { c, s ->
            return null
        }

        when:
        transformOperations.transform(_ as SourceResponse, _ as String, _ as Map)

        then:
        thrown(IllegalArgumentException)
    }

    def 'transform response with empty transformers found'() {
        setup:
        bundleContext.getServiceReferences(QueryResponseTransformer.class.name, _ as String) >> { c, s ->
            return []
        }

        when:
        transformOperations.transform(_ as SourceResponse, _ as String, _ as Map)

        then:
        thrown(IllegalArgumentException)
    }

    def 'transform null response'() {
        setup:
        def transformer = Mock(QueryResponseTransformer)
        def ref = Mock(ServiceReference)
        bundleContext.getServiceReferences(QueryResponseTransformer.class.name, _ as String) >> { c, s ->
            [ref]
        }
        bundleContext.getService(ref) >> { r -> transformer }

        when:
        transformOperations.transform(null as SourceResponse, _ as String, _ as Map)

        then:
        thrown(IllegalArgumentException)
    }

    def 'transform response'() {
        setup:
        def binaryContent = Mock(BinaryContent)
        def transformer = Mock(QueryResponseTransformer)
        def ref = Mock(ServiceReference)
        bundleContext.getServiceReferences(QueryResponseTransformer.class.name, _ as String) >> { c, s ->
            [ref]
        }
        bundleContext.getService(ref) >> { r -> transformer }
        transformer.transform(_ as SourceResponse, _ as Map) >> { met, props ->
            return binaryContent
        }

        when:
        def result = transformOperations.transform(_ as SourceResponse, _ as String, _ as Map)

        then:
        result == binaryContent
    }
}
