package org.codice.ddf.admin.api

import org.codice.ddf.admin.api.persist.handlers.ManagedServiceHandler
import org.codice.ddf.ui.admin.api.ConfigurationAdmin
import org.codice.ddf.ui.admin.api.ConfigurationAdminMBean
import spock.lang.Specification

class ManagedServiceHandlerTest extends Specification {
    private ConfigurationAdmin configAdmin
    private ConfigurationAdminMBean cfgAdmMbean

    def setup() {
        configAdmin = Mock(ConfigurationAdmin)
        cfgAdmMbean = Mock(ConfigurationAdminMBean)
    }

    def 'test create managed service and rollback'() {
        def configs = [k1: 'v1', k2: 'v2']
        setup:
        def handler = ManagedServiceHandler.forCreate('xxx', configs, configAdmin, cfgAdmMbean)

        when:
        def key = handler.commit()

        then:
        1 * configAdmin.createFactoryConfiguration('xxx') >> 'newPid'
        1 * cfgAdmMbean.update('newPid', configs)
        key == 'newPid'

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
        def handler = ManagedServiceHandler.forDelete('xxx', configAdmin, cfgAdmMbean)

        when:
        handler.commit()

        then:
        1 * cfgAdmMbean.delete('xxx')

        when:
        def key = handler.rollback()

        then:
        1 * configAdmin.createFactoryConfiguration('factoryPid') >> 'newPid'
        1 * cfgAdmMbean.update('newPid', configs)
        key == 'newPid'
    }
}
