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
package ddf.platform.solr.security

import ddf.security.encryption.EncryptionService
import org.codice.ddf.cxf.client.ClientFactoryFactory
import org.codice.ddf.cxf.client.SecureCxfClientFactory
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator
import spock.lang.Specification
import spock.lang.Unroll

import javax.ws.rs.core.Response

class SolrPasswordUpdateSpec extends Specification {

    public static final String PLAINTEXT_PASSWORD = "plaintext password"
    public static final String ENCRYPTED_PASSWORD = "encrypted password"
    public static final String BOOTSTRAP_PASSWORD = "autogenerated"
    public static final String SOLR_URL = "http://solr"
    public static final String SOLR_USERNAME = "ddf"

    def uuidGenerator = Mock(UuidGenerator) {
        generateUuid() >> PLAINTEXT_PASSWORD
    }

    def encryptionService = Mock(EncryptionService) {
        encrypt(PLAINTEXT_PASSWORD) >> ENCRYPTED_PASSWORD
        decrypt(BOOTSTRAP_PASSWORD) >> BOOTSTRAP_PASSWORD
    }

    def setup() {
        System.setProperty("solr.http.url", SOLR_URL);
        System.setProperty("solr.username", SOLR_USERNAME);
        System.setProperty("solr.password", BOOTSTRAP_PASSWORD);
    }

    @Unroll
    def 'update solr password when #doit'() {
        given:
        System.setProperty("solr.attemptAutoPasswordChange", attemptAutoPasswordChange);


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
//        getSecureCxfClientFactory(SOLR_URL, _, SOLR_USERNAME, PLAINTEXT_PASSWORD) >> secureClientFactory
            getSecureCxfClientFactory(*_) >> secureClientFactory
        }
        def solrPasswordUpdate = new SolrPasswordUpdateImpl(uuidGenerator, clientFactoryFactory, encryptionService);
        solrPasswordUpdate.start()

        expect:
        System.getProperty("solr.password") == password
        solrPasswordUpdate.isSolrPasswordChangeSuccessfull() == success

        where:
        doit     || attemptAutoPasswordChange | responseCode                        | success | password
        'case 1' || "true"                    | Response.Status.Family.SUCCESSFUL   | true    | ENCRYPTED_PASSWORD
        'case 2' || "true"                    | Response.Status.Family.SERVER_ERROR | false   | BOOTSTRAP_PASSWORD
        'case 3' || "false"                   | null                                | false   | BOOTSTRAP_PASSWORD
    }

}
