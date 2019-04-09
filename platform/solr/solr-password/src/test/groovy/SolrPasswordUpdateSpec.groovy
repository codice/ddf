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
    public static final String BOOTSTRAP_PASSWORD = 'admin'
    public static final String SOLR_URL = 'http://solr'
    public static final String SOLR_USERNAME = 'admin'
    private static final String WRAPPED_PASSWORD = 'ENC(encrypted password)'

    def uuidGenerator = Mock(UuidGenerator) {
        generateUuid() >> PLAINTEXT_PASSWORD
    }

    def encryptionService = Mock(EncryptionService) {
        encryptValue(PLAINTEXT_PASSWORD) >> WRAPPED_PASSWORD
        decryptValue(BOOTSTRAP_PASSWORD) >> BOOTSTRAP_PASSWORD
    }

    def properties = null

    def setup() {
        properties = new HashMap<String, String>()
        properties.put('solr.http.url', SOLR_URL)
        properties.put('solr.username', SOLR_USERNAME)
        properties.put('solr.password', BOOTSTRAP_PASSWORD)
        properties.put('solr.attemptAutoPasswordChange', 'true')
        properties.put('solr.useBasicAuth', 'true')
    }

    @Unroll
    def 'update solr password is #outcome'() {
        given:
        properties.put('solr.attemptAutoPasswordChange', attemptAutoPasswordChange);


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

        SolrPasswordUpdateImpl solrPasswordUpdate = new SolrPasswordUpdateImpl(uuidGenerator, clientFactoryFactory, encryptionService)

        when:
        // Send the execute() message and NOT the update() message to the object.
        // The implementation of update also runs cleanup, which removes access to the mock HTTP response objects
        solrPasswordUpdate.execute(properties)
        def actualSuccess = solrPasswordUpdate.isSolrPasswordChangeSuccessful()

        then:
        properties.get('solr.password').equals expectedPassword
        solrUpdateSuccess.equals actualSuccess

        where:
        outcome      || attemptAutoPasswordChange | responseCode                        | solrUpdateSuccess | expectedPassword
        'successful' || 'true'                    | Response.Status.Family.SUCCESSFUL   | true              | WRAPPED_PASSWORD
        'an error'   || 'true'                    | Response.Status.Family.SERVER_ERROR | false             | BOOTSTRAP_PASSWORD
        'disabled'   || 'false'                   | null                                | false             | BOOTSTRAP_PASSWORD
    }

    def 'bad properties object'() {
        given:
        def spySolrPasswordUpdate = Spy(SolrPasswordUpdateImpl, constructorArgs: [uuidGenerator, null, encryptionService])

        when:
        spySolrPasswordUpdate.update(null)

        then:
        0 * spySolrPasswordUpdate.execute(_)
    }

    def 'password generation wrapping encrypted string'() {
        when:
        def solrPasswordUpdate = new SolrPasswordUpdateImpl(uuidGenerator, null, encryptionService)
        solrPasswordUpdate.properties = properties
        solrPasswordUpdate.generatePassword()

        then:
        solrPasswordUpdate.newPasswordPlainText.equals(PLAINTEXT_PASSWORD)
        solrPasswordUpdate.newPasswordWrappedEncrypted.equals(WRAPPED_PASSWORD)
    }

    def 'object cleanup'() {
        given:
        SolrPasswordUpdateImpl solrPasswordUpdate = new SolrPasswordUpdateImpl(null, null, null)
        solrPasswordUpdate.newPasswordPlainText = "not null"
        solrPasswordUpdate.newPasswordWrappedEncrypted = "not null"
        solrPasswordUpdate.solrAuthResource = Mock(SolrAuthResource)
        solrPasswordUpdate.solrResponse = Mock(javax.ws.rs.core.Response.StatusType)

        when:
        solrPasswordUpdate.cleanup()

        then:
        solrPasswordUpdate.newPasswordPlainText == null
        solrPasswordUpdate.newPasswordWrappedEncrypted == null
        solrPasswordUpdate.passwordSavedSuccessfully == false
        solrPasswordUpdate.solrAuthResource == null
        solrPasswordUpdate.solrResponse == null
    }
}