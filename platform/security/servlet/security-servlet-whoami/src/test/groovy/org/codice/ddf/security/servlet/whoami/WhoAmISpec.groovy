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
package org.codice.ddf.security.servlet.whoami

import ddf.security.Subject
import ddf.security.SubjectUtils

class WhoAmISpec extends SubjectSpec {

    WhoAmI whoami

    def setup() {
        whoami = new WhoAmI(mockSubject())
    }

    def 'subject must not be null'() {
        when:
        new WhoAmI(null)

        then:
        thrown(IllegalArgumentException)
    }

    def 'assertion must not be null'() {
        when:
        new WhoAmI(Mock(Subject))

        then:
        thrown(IllegalArgumentException)
    }

    def 'returns subject name'() {
        when:
        def name = whoami.whoAmISubjects.get(0).getName()

        then:
        name == "Guest@127.0.0.1"
    }

    def 'returns subject display name'() {
        when:
        def displayName = whoami.whoAmISubjects.get(0).getDisplayName()

        then:
        displayName == "Guest"
    }

    def 'returns subject email'() {
        when:
        def email = whoami.whoAmISubjects.get(0).getEmail()

        then:
        email == "guest@localhost"
    }

    def 'returns assertion claims'() {
        when:
        def claims = whoami.whoAmISubjects.get(0).getClaims()

        then:
        claims.containsKey(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI)
    }

    def 'returns guest status of subject'() {
        when:
        def isGuest = whoami.whoAmISubjects.get(0).isGuest()

        then:
        isGuest
    }

    def 'returns issuer of assertion'() {
        when:
        def issuer = whoami.whoAmISubjects.get(0).getIssuer()

        then:
        issuer == 'TestIssuer'
    }

    def 'return assertion notOnOrAfter'() {
        when:
        def nooa = whoami.whoAmISubjects.get(0).getNotOnOrAfter()

        then:
        nooa.equals(notOnOrAfter)
    }

    def 'formats expiration duration of assertion'() {
        when:
        def duration = whoami.whoAmISubjects.get(0).getExpiresIn()

        then:
        duration.contains('days')
        duration.contains('hours')
        duration.contains('minutes')
        duration.contains('seconds')
    }

}