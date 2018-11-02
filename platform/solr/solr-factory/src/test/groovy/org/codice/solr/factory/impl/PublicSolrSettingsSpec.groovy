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
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

@RestoreSystemProperties
@Supplemental
class PublicSolrSettingsSpec extends Specification {

    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder();

    def 'solr dir is null'() {

        given:
        System.clearProperty("solr.data.dir")
        PublicSolrSettings.loadSystemProperties()

        expect:
        !PublicSolrSettings.isSolrDataDirWritable()
    }


    def 'solr dir is valid'() {
        given:
        setTestProperty("solr.data.dir", tempFolder.root.getAbsolutePath())

        expect:
        tempFolder.root.canWrite()
        PublicSolrSettings.isSolrDataDirWritable()
    }


    def 'solr dir does not exist'() {
        given:
        setTestProperty("solr.data.dir", "!!!!!")

        expect:
        !PublicSolrSettings.isSolrDataDirWritable()
    }


    def 'solr dir exists, but is not writable'() {
        given:
        def readOnlyFolder = tempFolder.newFolder()
        readOnlyFolder.setReadOnly();
        setTestProperty("solr.data.dir", readOnlyFolder.getAbsolutePath());

        expect:
        readOnlyFolder.canRead();
        !readOnlyFolder.canWrite();
        !PublicSolrSettings.isSolrDataDirWritable()
    }


    def 'core directory'() {
        given:
        setTestProperty("solr.data.dir", "/")
        setTestProperty("solr.http.url", "bork")


        expect:
        PublicSolrSettings.getCoreDir("test").equals("/test")
        PublicSolrSettings.getCoreUrl("test").equals("bork/test")
        PublicSolrSettings.getCoreDataDir("test").equals("/test/data")
    }

    def setTestProperty(propertyName, propertyValue) {
        System.setProperty(propertyName, propertyValue);
        PublicSolrSettings.loadSystemProperties();
    }


    //TODO: add this test
//    @Test
//    public void nearestNeighborLimitIsAlwaysPositive() {
//        PublicSolrSettings.setNearestNeighborDistanceLimit(-1.0);
//
//        assertThat(PublicSolrSettings.getNearestNeighborDistanceLimit(), closeTo(1.0, 0.00001));
//    }


}
