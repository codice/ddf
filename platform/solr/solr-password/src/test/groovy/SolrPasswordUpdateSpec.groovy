/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */


import ddf.platform.solr.security.SolrAuthResource
import ddf.platform.solr.security.SolrPasswordUpdateImpl
import ddf.security.encryption.EncryptionService
import org.codice.ddf.cxf.client.ClientFactoryFactory
import org.codice.ddf.cxf.client.SecureCxfClientFactory
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.core.Response

class SolrPasswordUpdateSpec extends Specification {

    public static final String PLAINTEXT_PASSWORD = 'plaintext password'
    public static final String ENCRYPTED_PASSWORD = 'encrypted password'
    public static final String BOOTSTRAP_PASSWORD = 'autogenerated'
    public static final String SOLR_URL = 'http://solr'
    public static final String SOLR_USERNAME = 'ddf'
    private static final String WRAPPED_PASSWORD = "ENC($ENCRYPTED_PASSWORD)";

    def uuidGenerator = Mock(UuidGenerator) {
        generateUuid() >> PLAINTEXT_PASSWORD
    }

    def encryptionService = Mock(EncryptionService) {
        encrypt(PLAINTEXT_PASSWORD) >> ENCRYPTED_PASSWORD
        decryptValue(BOOTSTRAP_PASSWORD) >> BOOTSTRAP_PASSWORD
    }

    def setup() {
        System.setProperty('solr.http.url', SOLR_URL);
        System.setProperty('solr.username', SOLR_USERNAME);
        System.setProperty('solr.password', BOOTSTRAP_PASSWORD);
        System.setProperty('solr.attemptAutoPasswordChange', 'true')
    }

    @Unroll
    def 'update solr password is #outcome'() {
        given:
        System.setProperty('solr.attemptAutoPasswordChange', attemptAutoPasswordChange);


        def statusType = Mock(Response.StatusType) {
            getFamily() >> responseCode
        }
        def response = Mock(javax.ws.rs.core.Response) {
            getStatusInfo() >> statusType
        }


        def solrAuthResource = Mock(SolrAuthResource) {
            sendRequest(_) >> response
        }

        def secureClientFactory = Mock(SecureCxfClientFactory) {
            getClient() >> solrAuthResource
        }

        def clientFactoryFactory = Mock(ClientFactoryFactory) {
            getSecureCxfClientFactory(SOLR_URL, _, SOLR_USERNAME, BOOTSTRAP_PASSWORD) >> secureClientFactory
        }

        when:
        def solrPasswordUpdate = new SolrPasswordUpdateImpl(uuidGenerator, clientFactoryFactory, encryptionService);
        solrPasswordUpdate.updateSolrPassword()

        then:
        System.getProperty('solr.password').equals(password)
        solrPasswordUpdate.isSolrPasswordChangeSuccessfull().equals(success)

        where:
        outcome     || attemptAutoPasswordChange | responseCode                        | success | password
        'sucessful' || 'true'                    | Response.Status.Family.SUCCESSFUL   | true    | WRAPPED_PASSWORD
        'an error'  || 'true'                    | Response.Status.Family.SERVER_ERROR | false   | BOOTSTRAP_PASSWORD
        'disabled'  || 'false'                   | null                                | false   | BOOTSTRAP_PASSWORD
    }

    def 'password generation wrapping encrypted string'() {
        when:
        def solrPasswordUpdate = new SolrPasswordUpdateImpl(uuidGenerator, null, encryptionService);
        solrPasswordUpdate.generatePassword()

        then:
        solrPasswordUpdate.getNewPasswordPlainText().equals(PLAINTEXT_PASSWORD)
        solrPasswordUpdate.getNewPasswordEncrypted().equals(ENCRYPTED_PASSWORD)
        solrPasswordUpdate.getNewPasswordWrappedEncrypted().equals(WRAPPED_PASSWORD)
    }

    def 'password property not defined'() {
        given:
        System.clearProperty('solr.password')

        when:
        def solrPasswordUpdate = new SolrPasswordUpdateImpl(uuidGenerator, null, encryptionService);
        solrPasswordUpdate.generatePassword();

        then:
        solrPasswordUpdate.getPlaintextPasswordFromProperties() == null
    }
}
