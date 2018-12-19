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

import org.apache.commons.io.FileUtils
import org.apache.solr.core.SolrConfig
import org.apache.solr.schema.IndexSchema
import org.xml.sax.SAXException
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import javax.xml.parsers.ParserConfigurationException
import java.nio.file.Path
import java.nio.file.Paths

class EmbeddedSolrFilesSpec extends Specification {
  static final String CORE = "test_core"

  static final Path CWD = Paths.get(System.getProperty("user.dir"))
  static final File HOME = CWD.resolve(File.separator + "test_home").toFile()
  static final String DATA_DIR = new File(new File(HOME, CORE), "data").toString()

  static final String CONFIG_XML = "solrconfig.xml"
  static final File CONFIG_FILE = new File(HOME, CONFIG_XML)
  static final URL CONFIG_URL = CONFIG_FILE.toURI().toURL()
  static final URL CONFIG2_URL = new File(HOME, "solrconfig2.xml").toURI().toURL()
  static final URL EXISTING_CONFIG_URL = EmbeddedSolrFilesSpec.getResource("/solrconfig.xml")
  static
  final Path EXISTING_CONFIG_INSTANCE = FileUtils.toFile(EXISTING_CONFIG_URL).parentFile.parentFile.toPath()

  static final String SCHEMA_XML = "schema.xml"
  static final File SCHEMA_FILE = new File(HOME, SCHEMA_XML)
  static final URL SCHEMA_URL = SCHEMA_FILE.toURI().toURL()
  static final URL EXISTING_SCHEMA_URL = EmbeddedSolrFilesSpec.getResource("/schema.xml")
  static final String SCHEMA2_XML = "schema2.xml"
  static final File SCHEMA2_FILE = new File(HOME, SCHEMA2_XML)
  static final URL SCHEMA2_URL = SCHEMA2_FILE.toURI().toURL()
  static final String[] SCHEMA_XMLS = [SCHEMA_XML, SCHEMA2_XML]

  @Shared
  ConfigurationFileProxy configProxy = Stub()

  IndexSchema schemaIndex = Stub()
  SolrConfig config = Stub()

  @Unroll
  def 'test constructor when #test_constructor_when'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(_, CORE) >>> [CONFIG_URL, schema_url, schema2_url]
      }

    when:
      def files = new EmbeddedSolrFiles(CORE, CONFIG_XML, SCHEMA_XMLS, configProxy)

    then:
      files.schemaFile == schema_file
      files.configFile == CONFIG_FILE
      files.configHome == HOME

    where:
      test_constructor_when        || schema_url | schema2_url || schema_file
      'schema is found right away' || SCHEMA_URL | SCHEMA2_URL || SCHEMA_FILE
      'falling back to 2nd schema' || null       | SCHEMA2_URL || SCHEMA2_FILE
  }

  @Unroll
  def 'test constructor with #test_constructor_with'() {
    when:
      new EmbeddedSolrFiles(core, config_xml, schema_xmls, config_proxy)

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains(message)

    where:
      test_constructor_with || core | config_xml | schema_xmls                     | config_proxy || message
      'null core name'      || null | CONFIG_XML | SCHEMA_XMLS                     | configProxy  || 'invalid null Solr core name'
      'null config xml'     || CORE | null       | SCHEMA_XMLS                     | configProxy  || 'invalid null Solr config file'
      'null schema xmls'    || CORE | CONFIG_XML | null                            | configProxy  || 'invalid null Solr schema files'
      'no schema xmls'      || CORE | CONFIG_XML | [] as String[]                  | configProxy  || 'missing Solr schema file'
      'null 1st schema xml' || CORE | CONFIG_XML | [null, SCHEMA2_XML] as String[] | configProxy  || 'invalid null Solr schema file'
      'null 2nd schema xml' || CORE | CONFIG_XML | [SCHEMA_XML, null] as String[]  | configProxy  || 'invalid null Solr schema file'
      'null config proxy'   || CORE | CONFIG_XML | SCHEMA_XMLS                     | null         || 'invalid null Solr config proxy'
  }

  @Unroll
  def 'test constructor when #test_constructor_when_what_not_found not found'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(_, CORE) >>> [config_url, schema_url, schema2_url]
      }

    when:
      new EmbeddedSolrFiles(CORE, CONFIG_XML, SCHEMA_XMLS, configProxy)

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains(message)

    where:
      test_constructor_when_what_not_found || config_url | schema_url | schema2_url || message
      'config is'                          || null       | SCHEMA_XML | SCHEMA2_XML || 'Unable to find Solr configuration file'
      'all schema files are'               || CONFIG_URL | null       | null        || 'Unable to find Solr schema file(s)'
  }

  def 'test creating the schema index the first time it is retrieved'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, EXISTING_SCHEMA_URL]
      }
      def files = Spy(EmbeddedSolrFiles, constructorArgs: [CORE, CONFIG_XML, SCHEMA_XMLS, configProxy])

    when:
      def createdIndex = files.schemaIndex

    then: "the retrieved schema index should be the one we created"
      createdIndex == schemaIndex

    and: "make sure a configuration was retrieved"
      1 * files.getConfig() >> config

    and: "make sure a new schema index was created"
      1 * files.newIndexSchema(config, SCHEMA_XML, _) >> schemaIndex
  }

  def 'test retrieving a previously created schema index the second time it is retrieved'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, EXISTING_SCHEMA_URL]
      }
      def files = Spy(EmbeddedSolrFiles, constructorArgs: [CORE, CONFIG_XML, SCHEMA_XMLS, configProxy])

    when: "the schema is first created"
      def createdIndex = files.schemaIndex

    then: "the retrieved schema index should be the one we created the first time"
      createdIndex == schemaIndex

    and: "make sure a configuration was retrieved only once"
      1 * files.getConfig() >> config

    and: "make sure a new schema index created only once"
      1 * files.newIndexSchema(config, SCHEMA_XML, _) >> schemaIndex

    when: "retrieved a second time"
      def retrievedIndex = files.schemaIndex

    then: "the retrieved schema index should be the one we created the first time"
      retrievedIndex == createdIndex
  }

  def 'test failing to load the schema index when attempting to retrieve it'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, SCHEMA_URL]
      }
      def files = Spy(EmbeddedSolrFiles, constructorArgs: [CORE, CONFIG_XML, SCHEMA_XMLS, configProxy]) {
        getConfig() >> config
      }

    when:
      files.schemaIndex

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains("open Solr schema file")

    and: "make sure no new schema index were created"
      0 * files.newIndexSchema(*_)
  }

  def 'test failing to instantiate the schema index when attempting to retrieve it'() {
    given:
      def error = new RuntimeException("testing")
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, EXISTING_SCHEMA_URL]
      }
      def files = Spy(EmbeddedSolrFiles, constructorArgs: [CORE, CONFIG_XML, SCHEMA_XMLS, configProxy]) {
        getConfig() >> config
      }

    when:
      files.schemaIndex

    then: "fail to create the schema index"
      1 * files.newIndexSchema(*_) >> { throw error }
      def e = thrown(IllegalArgumentException)

      e.message.contains("parse Solr schema file")
      e.cause.is(error)
  }

  def 'test failing to retrieve the config when attempting to create a schema index'() {
    given:
      def error = new IllegalArgumentException("testing")
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, SCHEMA_URL]
      }
      def files = Spy(EmbeddedSolrFiles, constructorArgs: [CORE, CONFIG_XML, SCHEMA_XMLS, configProxy])

    when:
      files.schemaIndex

    then: "fail to retrieve the config"
      1 * files.config >> { throw error }
      def e = thrown(IllegalArgumentException)

      e.is(error)

    and: "make sure no schema index were created"
      0 * files.newIndexSchema(*_)
  }

  def 'test creating the config the first time it is retrieved'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [EXISTING_CONFIG_URL, SCHEMA_URL]
      }
      def files = Spy(EmbeddedSolrFiles, constructorArgs: [CORE, CONFIG_XML, SCHEMA_XMLS, configProxy])

    when:
      def createdConfig = files.config

    then: "the retrieved config should be the one we created"
      createdConfig == config

    and: "make sure a new config was created"
      1 * files.newConfig({
        it == EXISTING_CONFIG_INSTANCE
      }, CONFIG_XML, _) >> config
  }

  def 'test retrieving a previously created config the second time it is retrieved'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [EXISTING_CONFIG_URL, SCHEMA_URL]
      }
      def files = Spy(EmbeddedSolrFiles, constructorArgs: [CORE, CONFIG_XML, SCHEMA_XMLS, configProxy])

    when: "the config is first created"
      def createdConfig = files.config

    then: "the retrieved config should be the one we created the first time"
      createdConfig == config

    and: "make sure a new config created only once"
      1 * files.newConfig({
        it == EXISTING_CONFIG_INSTANCE
      }, CONFIG_XML, _) >> config

    when: "retrieved a second time"
      def retrievedConfig = files.config

    then: "the retrieved config should be the one we created the first time"
      retrievedConfig == createdConfig
  }

  def 'test failing to load the config when attempting to retrieve it'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, SCHEMA_URL]
      }
      def files = Spy(EmbeddedSolrFiles, constructorArgs: [CORE, CONFIG_XML, SCHEMA_XMLS, configProxy])

    when:
      files.config

    then:
      def e = thrown(IllegalArgumentException)

      e.message.contains("open Solr configuration file")

    and: "make sure no new config were created"
      0 * files.newConfig(*_)
  }

  @Unroll
  def 'test failing to instantiate the config with #exception.class.simpleName when attempting to retrieve it'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [EXISTING_CONFIG_URL, SCHEMA_URL]
      }
      def files = Spy(EmbeddedSolrFiles, constructorArgs: [CORE, CONFIG_XML, SCHEMA_XMLS, configProxy])

    when:
      files.config

    then: "fail to create the config"
      1 * files.newConfig(*_) >> { throw exception.fillInStackTrace() }
      def e = thrown(IllegalArgumentException)

      e.message.contains("parse Solr configuration file")
      e.cause.is(exception)

    where:
      exception << [new ParserConfigurationException('testing'), new IOException('testing'), new SAXException('testing')]
  }

  @Unroll
  def 'test retrieving the data directory path when data directory is #data_dir_is'() {
    given:
      def configStore = Mock(ConfigurationStore) {
        isInMemory() >> false
      }
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, SCHEMA_URL]
        getDataDirectory() >> data_dir
      }
      def files = new EmbeddedSolrFiles(CORE, CONFIG_XML, SCHEMA_XMLS, configProxy)

    expect:
      files.dataDirPath == data_dir_path

    where:
      data_dir_is     || data_dir || data_dir_path
      'available'     || HOME     || DATA_DIR
      'not available' || null     || null
  }

  def 'test hash code when equal'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, SCHEMA_URL]
      }
      def files = new EmbeddedSolrFiles(CORE, CONFIG_XML, SCHEMA_XMLS, configProxy)
      def configProxy2 = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, SCHEMA_URL]
      }
      def files2 = new EmbeddedSolrFiles(CORE, CONFIG_XML, SCHEMA_XMLS, configProxy2)

    expect:
      files.hashCode() == files2.hashCode()
  }

  def 'test hash code when different'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, SCHEMA_URL]
      }
      def files = new EmbeddedSolrFiles(CORE, CONFIG_XML, SCHEMA_XMLS, configProxy)
      def configProxy2 = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, null, SCHEMA2_URL]
      }
      def files2 = new EmbeddedSolrFiles(CORE, CONFIG_XML, SCHEMA_XMLS, configProxy2)

    expect:
      files.hashCode() != files2.hashCode()
  }

  def 'test equals when equal'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, SCHEMA_URL]
      }
      def files = new EmbeddedSolrFiles(CORE, CONFIG_XML, SCHEMA_XMLS, configProxy)
      def configProxy2 = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, SCHEMA_URL]
      }
      def files2 = new EmbeddedSolrFiles(CORE, CONFIG_XML, SCHEMA_XMLS, configProxy2)

    expect:
      files.equals(files2)
  }

  def 'test equals when identical'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [CONFIG_URL, SCHEMA_URL]
      }
      def files = new EmbeddedSolrFiles(CORE, CONFIG_XML, SCHEMA_XMLS, configProxy)

    expect:
      files.equals(files)
  }

  @Unroll
  def 'test equals when #when_what_are_different are different'() {
    given:
      def configProxy = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [config_url, SCHEMA_URL]
      }
      def files = new EmbeddedSolrFiles(core, CONFIG_XML, SCHEMA_XMLS, configProxy)
      def configProxy2 = Mock(ConfigurationFileProxy) {
        getResource(*_) >>> [config_url2, schema_url2]
      }
      def files2 = new EmbeddedSolrFiles(core2, CONFIG_XML, SCHEMA_XMLS, configProxy2)

    expect:
      !files.equals(files2)

    where:
      when_what_are_different || core | core2        | config_url | config_url2 | schema_url | schema_url2
      'core names'            || CORE | 'other core' | CONFIG_URL | CONFIG_URL  | SCHEMA_URL | SCHEMA_URL
      'config files'          || CORE | CORE         | CONFIG_URL | CONFIG2_URL | SCHEMA_URL | SCHEMA_URL
      'schema files'          || CORE | CORE         | CONFIG_URL | CONFIG_URL  | SCHEMA_URL | SCHEMA2_URL
  }
}
