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
package org.codice.solr.settings


import org.codice.spock.Supplemental
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

@RestoreSystemProperties
@Supplemental
class SolrSettingsSpec extends Specification {

    @Rule
    TemporaryFolder tempFolder = new TemporaryFolder();

    def 'solr dir is null'() {

        given:
        System.clearProperty("solr.data.dir")
        SolrSettings.loadSystemProperties()

        expect:
        !SolrSettings.isSolrDataDirWritable()
    }


    def 'solr dir is valid'() {
        given:
        setTestProperty("solr.data.dir", tempFolder.root.getAbsolutePath())

        expect:
        tempFolder.root.canWrite()
        SolrSettings.isSolrDataDirWritable()
    }


    def 'solr dir does not exist'() {
        given:
        setTestProperty("solr.data.dir", "!!!!!")

        expect:
        !SolrSettings.isSolrDataDirWritable()
    }


    def 'solr dir exists, but is not writable'() {
        given:
        def readOnlyFolder = tempFolder.newFolder()
        readOnlyFolder.setReadOnly();
        setTestProperty("solr.data.dir", readOnlyFolder.getAbsolutePath());

        expect:
        readOnlyFolder.canRead();
        !readOnlyFolder.canWrite();
        !SolrSettings.isSolrDataDirWritable()
    }


    def 'core directory'() {
        given:
        setTestProperty("solr.data.dir", "/")
        setTestProperty("solr.http.url", "bork")


        expect:
        SolrSettings.getCoreDir("test").equals("/test")
        SolrSettings.getCoreUrl("test").equals("bork/test")
        SolrSettings.getCoreDataDir("test").equals("/test/data")
    }

    def setTestProperty(propertyName, propertyValue) {
        System.setProperty(propertyName, propertyValue);
        SolrSettings.loadSystemProperties();
    }


    //TODO: add this test
//    @Test
//    public void nearestNeighborLimitIsAlwaysPositive() {
//        SolrSettings.setNearestNeighborDistanceLimit(-1.0);
//
//        assertThat(SolrSettings.getNearestNeighborDistanceLimit(), closeTo(1.0, 0.00001));
//    }


}
