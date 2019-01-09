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

import ddf.catalog.operation.Operation
import ddf.security.SecurityConstants
import ddf.security.Subject
import org.apache.shiro.util.ThreadContext
import spock.lang.Specification

class OperationsSecuritySupportSpec extends Specification {
    private OperationsSecuritySupport opsSecurity

    def setup() {
        opsSecurity = new OperationsSecuritySupport()
    }

    def 'test building with no policies'() {
        setup:
        Map<String, Set<String>> policyMap = [:]

        when:
        opsSecurity.buildPolicyMap(policyMap, null)

        then:
        policyMap.isEmpty()
    }

    def 'test building with policies'() {
        setup:
        HashMap<String, Set<String>> policyMap = [a1: ['a', 'b', 'c'] as Set, a2: ['1', '2'] as Set,
                                                  b1: ['b1'] as Set, b2: ['b2', 'b3', 'b4'] as Set] as HashMap
        def policy1 = Mock(Map.Entry)
        policy1.getKey() >> { 'a1' }
        policy1.getValue() >> { ['c', 'd', 'e'] as Set }
        def policy2 = Mock(Map.Entry)
        policy2.getKey() >> { 'b1' }
        policy2.getValue() >> { ['b2'] as Set }
        def policy3 = Mock(Map.Entry)
        policy3.getKey() >> { 'xx' }
        policy3.getValue() >> { ['yy'] as Set }
        def policies = [policy1, policy2, policy3] as Set

        when:
        opsSecurity.buildPolicyMap(policyMap, policies)

        then:
        policyMap.size() == 5
        policyMap.a1 == ['a', 'b', 'c', 'd', 'e'] as Set
        policyMap.a2 == ['1', '2'] as Set
        policyMap.b1 == ['b1', 'b2'] as Set
        policyMap.b2 == ['b2', 'b3', 'b4'] as Set
        policyMap.xx == ['yy'] as Set
    }

    def 'test get subject from operation'() {
        setup:
        Subject subject
        Subject mockSubject = Mock(Subject)
        Operation operation = Mock(Operation)
        operation.getPropertyValue(SecurityConstants.SECURITY_SUBJECT) >> mockSubject


        when:
        subject = opsSecurity.getSubject(operation)

        then:
        subject == mockSubject
    }

    def 'test get subject on operation without a subject'() {
        Subject subject
        Subject mockSubject = Mock(Subject)
        Operation operation = Mock(Operation)
        operation.getPropertyValue(SecurityConstants.SECURITY_SUBJECT) >> null
        ThreadContext.bind(mockSubject)

        when:
        subject = opsSecurity.getSubject(operation)

        then:
        subject == mockSubject
        ThreadContext.unbindSubject()
    }

    def 'test get subject with no subject'() {
        setup:
        Subject subject
        Operation operation = Mock(Operation)
        operation.getPropertyValue(SecurityConstants.SECURITY_SUBJECT) >> null

        when:
        subject = opsSecurity.getSubject(operation)

        then:
        subject == null
    }
}
