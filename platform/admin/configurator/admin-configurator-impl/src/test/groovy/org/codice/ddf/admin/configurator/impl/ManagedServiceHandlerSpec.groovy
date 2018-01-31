package org.codice.ddf.admin.configurator.impl

import org.codice.ddf.admin.configurator.ConfiguratorException
import org.codice.ddf.admin.core.api.jmx.AdminConsoleServiceMBean
import spock.lang.Specification

class ManagedServiceHandlerSpec extends Specification {
    private AdminConsoleServiceMBean cfgAdmMbean

    def setup() {
        cfgAdmMbean = Mock(AdminConsoleServiceMBean)
    }

    def 'test create managed service and rollback'() {
        def configs = [k1: 'v1', k2: 'v2']
        setup:
        def handler = new ManagedServiceOperation.CreateHandler('xxx', configs, cfgAdmMbean)

        when:
        def key = handler.commit()

        then:
        1 * cfgAdmMbean.createFactoryConfiguration('xxx') >> 'newPid'
        1 * cfgAdmMbean.update('newPid', configs)
        key.operationData.isPresent()
        key.operationData.get() == 'newPid'

        when:
        handler.rollback()

        then:
        1 * cfgAdmMbean.delete('newPid')
    }

    def 'test delete managed service and rollback'() {
        setup:
        def configs = [k1: 'v1', k2: 'v2']
        cfgAdmMbean.getFactoryPid('xxx') >> 'factoryPid'
        cfgAdmMbean.getProperties('xxx') >> configs
        def handler = new ManagedServiceOperation.DeleteHandler('xxx', cfgAdmMbean)

        when:
        handler.commit()

        then:
        1 * cfgAdmMbean.delete('xxx')

        when:
        def key = handler.rollback()

        then:
        1 * cfgAdmMbean.createFactoryConfiguration('factoryPid') >> 'newPid'
        1 * cfgAdmMbean.update('newPid', configs)
        key.operationData.isPresent()
        key.operationData.get() == 'newPid'
    }

    def 'test delete of pid with unknown factory pid fails'() {
        setup:
        def configs = [k1: 'v1', k2: 'v2']
        cfgAdmMbean.getFactoryPid('xxx') >> null
        cfgAdmMbean.getProperties('xxx') >> configs
        def handler = new ManagedServiceOperation.DeleteHandler('xxx', cfgAdmMbean)

        when:
        handler.commit()

        then:
        thrown ConfiguratorException
    }
}
