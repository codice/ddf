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

import ddf.catalog.data.impl.AttributeDescriptorImpl
import ddf.catalog.data.impl.AttributeImpl
import ddf.catalog.data.impl.BasicTypes
import ddf.catalog.impl.FrameworkProperties
import spock.lang.Specification

class CreateOperationsSpec extends Specification {
    private CreateOperations createOperations

    def setup() {
        createOperations = new CreateOperations(Mock(FrameworkProperties), Mock(QueryOperations), Mock(SourceOperations), Mock(OperationsSecuritySupport), Mock(OperationsMetacardSupport), Mock(OperationsCatalogStoreSupport), Mock(OperationsStorageSupport))
    }

    def 'test single and multivalued attribute overrides'() {
        def descriptorImpl = new AttributeDescriptorImpl("foo", true, true, true, true, BasicTypes.STRING_TYPE)
        when:
        AttributeImpl single = OverrideAttributesSupport.overrideAttributeValue(descriptorImpl, (Serializable) Arrays.asList("bar"))
        then:
        single.getValues().size() == 1
        single.getValues().contains("bar")
        when:
        AttributeImpl multi = OverrideAttributesSupport.overrideAttributeValue(descriptorImpl, (Serializable) Arrays.asList("bar", "baz"))
        then:
        multi.getValues().size() == 2
        multi.getValues().contains("bar")
        multi.getValues().contains("baz")
    }
}