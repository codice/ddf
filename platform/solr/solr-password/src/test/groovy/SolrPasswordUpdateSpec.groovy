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
import spock.util.environment.RestoreSystemProperties

import javax.ws.rs.core.Response

@RestoreSystemProperties
class SolrPasswordUpdateSpec extends Specification {

    public static final String PLAINTEXT_PASSWORD = 'plaintext password'
    public static final String BOOTSTRAP_PASSWORD = 'admin'
    public static final String SOLR_URL = 'http://solr'
    public static final String SOLR_USERNAME = 'admin'
    private static final String WRAPPED_PASSWORD = 'ENC(encrypted password)';

    def uuidGenerator = Mock(UuidGenerator) {
        generateUuid() >> PLAINTEXT_PASSWORD
    }

    def encryptionService = Mock(EncryptionService) {
        encryptValue(PLAINTEXT_PASSWORD) >> WRAPPED_PASSWORD
        decryptValue(BOOTSTRAP_PASSWORD) >> BOOTSTRAP_PASSWORD
    }

    def setup() {
        System.setProperty('solr.http.url', SOLR_URL);
        System.setProperty('solr.username', SOLR_USERNAME);
        System.setProperty('solr.password', BOOTSTRAP_PASSWORD);
        System.setProperty('solr.attemptAutoPasswordChange', 'true')
        System.setProperty('solr.useBasicAuth', 'true')
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

        def spySolrPasswordUpdate = Spy(SolrPasswordUpdateImpl, constructorArgs: [uuidGenerator, clientFactoryFactory, encryptionService]) {
            isPasswordSavedSuccessfully() >> fileUpdateSuccess
        }

        when:
        spySolrPasswordUpdate.execute()

        then:
        System.getProperty('solr.password').equals(passwordInMemory)
        spySolrPasswordUpdate.isSolrPasswordChangeSuccessfull().equals(solrUpdateSuccess)

        where:
        outcome     || attemptAutoPasswordChange | responseCode                        | solrUpdateSuccess | fileUpdateSuccess | passwordInMemory
        'sucessful' || 'true'                    | Response.Status.Family.SUCCESSFUL   | true              | true              | WRAPPED_PASSWORD
        'partial'   || 'true'                    | Response.Status.Family.SUCCESSFUL   | true              | false             | BOOTSTRAP_PASSWORD
        'an error'  || 'true'                    | Response.Status.Family.SERVER_ERROR | false             | false             | BOOTSTRAP_PASSWORD
        'disabled'  || 'false'                   | null                                | false             | false             | BOOTSTRAP_PASSWORD
    }

    def 'password generation wrapping encrypted string'() {
        when:
        def solrPasswordUpdate = new SolrPasswordUpdateImpl(uuidGenerator, null, encryptionService);
        solrPasswordUpdate.generatePassword()

        then:
        solrPasswordUpdate.newPasswordPlainText.equals(PLAINTEXT_PASSWORD)
        solrPasswordUpdate.newPasswordWrappedEncrypted.equals(WRAPPED_PASSWORD)
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

    def 'object cleanup'() {
        given:
        SolrPasswordUpdateImpl solrPasswordUpdate = new SolrPasswordUpdateImpl(null, null, null)
        solrPasswordUpdate.newPasswordPlainText = "not null"
        solrPasswordUpdate.newPasswordWrappedEncrypted = "not null"
        solrPasswordUpdate.passwordSavedSuccessfully = true
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
