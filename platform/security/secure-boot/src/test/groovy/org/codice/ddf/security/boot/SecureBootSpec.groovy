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
package org.codice.ddf.security.boot

import java.nio.file.Paths
import org.apache.karaf.system.SystemService
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import java.nio.file.Files

@RestoreSystemProperties
class SecureBootSpec  extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    @Rule
    TemporaryFolder ddfHome

    @Rule
    TemporaryFolder userHome

    def 'System exits normally when security manager enabled and ddf installed inside of user home dir'() {
        setup:
        def exitedDueToSystemServiceFailureToExit = false
        def userHomeSysProp = userHome.getRoot().toString()
        def ddfHomeSysProp = userHome.newFolder("ddf").toString()
        def systemService = Mock(SystemService)
        def securityManager = Mock(SecurityManager)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp

        when:
        def secureBoot = new SecureBoot(systemService) {
            boolean securityManagerEnabled() {
                return securityManager;
            }

            void systemExit(Exception e) {
                exitedDueToSystemServiceFailureToExit = true
            }
        }
        secureBoot.init()

        then:
        1 * systemService.halt("0")
        exitedDueToSystemServiceFailureToExit == false
    }

    def 'System boots when security manager enabled and ddf installed outside of user home dir'() {
        setup:
        def exitedDueToSystemServiceFailureToExit = false
        def userHomeSysProp = userHome.newFolder("tomPenny").toString()
        def ddfHomeSysProp = ddfHome.newFolder("ddf").toString()
        def systemService = Mock(SystemService)
        def securityManager = Mock(SecurityManager)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp

        when:
        def secureBoot = new SecureBoot(systemService) {
            boolean securityManagerEnabled() {
                return securityManager;
            }

            void systemExit(Exception e) {
                exitedDueToSystemServiceFailureToExit = true
            }
        }
        secureBoot.init()

        then:
        0 * systemService.halt("0")
        exitedDueToSystemServiceFailureToExit == false
    }

    def 'System boots when security manager disabled and ddf installed inside of user home dir'() {
        setup:
        def exitedDueToSystemServiceFailureToExit = false
        def userHomeSysProp = userHome.getRoot().toString()
        def ddfHomeSysProp = userHome.newFolder("ddf").toString()
        def systemService = Mock(SystemService)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp

        when:
        def secureBoot = new SecureBoot(systemService) {
            boolean securityManagerEnabled() {
                return null;
            }
            void systemExit(Exception e) {
                exitedDueToSystemServiceFailureToExit = true
            }
        }
        secureBoot.init()

        then:
        0 * systemService.halt("0")
        exitedDueToSystemServiceFailureToExit == false
    }

    def 'System boots when security manager disabled and ddf installed outside of user home dir'() {
        setup:
        def exitedDueToSystemServiceFailureToExit = false
        def userHomeSysProp = userHome.newFolder("tomPenny").toString()
        def ddfHomeSysProp = ddfHome.newFolder("ddf").toString()
        def systemService = Mock(SystemService)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp

        when:
        def secureBoot = new SecureBoot(systemService) {
            boolean securityManagerEnabled() {
                return null;
            }
            void systemExit(Exception e) {
                exitedDueToSystemServiceFailureToExit = true
            }
        }
        secureBoot.init()

        then:
        0 * systemService.halt("0")
        exitedDueToSystemServiceFailureToExit == false
    }

    def 'SystemService throws exception when asked to halt'() {
        setup:
        def exitedDueToSystemServiceFailureToExit = false
        def userHomeSysProp = userHome.getRoot().toString()
        def ddfHomeSysProp = userHome.newFolder("ddf").toString()
        def systemService = Mock(SystemService)
        def securityManager = Mock(SecurityManager)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp

        when:
        def secureBoot = new SecureBoot(systemService) {
            boolean securityManagerEnabled() {
                return securityManager;
            }

            void systemExit(Exception e) {
                exitedDueToSystemServiceFailureToExit = true
            }
        }
        secureBoot.init()

        then:
        1 * systemService.halt("0") >> { throw new Exception("unable to halt")}
        exitedDueToSystemServiceFailureToExit == true
    }

    def 'System exits normally when attempting to read invalid ddf.home'() {
        setup:
        def exitedDueToSystemServiceFailureToExit = false
        def userHomeSysProp = userHome.getRoot().toString()
        def ddfHomeSysProp = "/" + System.currentTimeMillis() + "/" + UUID.randomUUID() + "/ddf"
        def systemService = Mock(SystemService)
        def securityManager = Mock(SecurityManager)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp

        when:
        def secureBoot = new SecureBoot(systemService) {
            boolean securityManagerEnabled() {
                return securityManager;
            }

            void systemExit(Exception e) {
                exitedDueToSystemServiceFailureToExit = true
            }
        }
        secureBoot.init()

        then:
        1 * systemService.halt("0")
        exitedDueToSystemServiceFailureToExit == false
    }

    def 'System exits normally when attempting to read invalid user.home'() {
        setup:
        def exitedDueToSystemServiceFailureToExit = false
        def userHomeSysProp = "/" + System.currentTimeMillis() + "/" + UUID.randomUUID() + "/tomPenny"
        def ddfHomeSysProp = ddfHome.newFolder("ddf").toString()
        def systemService = Mock(SystemService)
        def securityManager = Mock(SecurityManager)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp

        when:
        def secureBoot = new SecureBoot(systemService) {
            boolean securityManagerEnabled() {
                return securityManager;
            }

            void systemExit(Exception e) {
                exitedDueToSystemServiceFailureToExit = true
            }
        }
        secureBoot.init()

        then:
        1 * systemService.halt("0")
        exitedDueToSystemServiceFailureToExit == false
    }

    def 'System exits normally when attempting to read invalid ddf.home and invalid user.home'() {
        setup:
        def exitedDueToSystemServiceFailureToExit = false
        def userHomeSysProp = "/" + System.currentTimeMillis() + "/" + UUID.randomUUID() + "/tomPenny"
        def ddfHomeSysProp = "/" + System.currentTimeMillis() + "/" + UUID.randomUUID() + "/ddf"
        def systemService = Mock(SystemService)
        def securityManager = Mock(SecurityManager)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp

        when:
        def secureBoot = new SecureBoot(systemService) {
            boolean securityManagerEnabled() {
                return securityManager;
            }

            void systemExit(Exception e) {
                exitedDueToSystemServiceFailureToExit = true
            }
        }
        secureBoot.init()

        then:
        1 * systemService.halt("0")
        exitedDueToSystemServiceFailureToExit == false
    }
}