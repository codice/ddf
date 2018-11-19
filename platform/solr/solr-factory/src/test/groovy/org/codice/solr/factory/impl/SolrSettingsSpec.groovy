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
package org.codice.solr.factory.impl


import org.codice.spock.Supplemental
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

@RestoreSystemProperties
@Supplemental
class SolrSettingsSpec extends Specification {

    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder();

    def solrSettings = new SolrSettings();

    def 'solr dir is null'() {

        given:
        System.clearProperty("solr.data.dir")

        expect:
        !solrSettings.isSolrDataDirWritable()
    }


    def 'solr dir is valid'() {
        given:
        System.setProperty("solr.data.dir", tempFolder.root.getAbsolutePath())

        expect:
        tempFolder.root.canWrite()
        solrSettings.isSolrDataDirWritable()
    }


    def 'solr dir does not exist'() {
        given:
        System.setProperty("solr.data.dir", "!!!!!")

        expect:
        !solrSettings.isSolrDataDirWritable()
    }


    def 'solr dir exists, but is not writable'() {
        given:
        def readOnlyFolder = tempFolder.newFolder()
        readOnlyFolder.setReadOnly();
        System.setProperty("solr.data.dir", readOnlyFolder.getAbsolutePath());

        expect:
        readOnlyFolder.canRead();
        !readOnlyFolder.canWrite();
        !solrSettings.isSolrDataDirWritable()
    }


    def 'core directory'() {
        given:
        System.setProperty("solr.data.dir", "/")
        System.setProperty("solr.http.url", "bork")


        expect:
        solrSettings.getCoreDir("test").equals("/test")
        solrSettings.getCoreUrl("test").equals("bork/test")
        solrSettings.getCoreDataDir("test").equals("/test/data")
    }
}
