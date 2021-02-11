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

import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import spock.lang.IgnoreIf

import java.nio.file.Paths
import org.apache.karaf.system.SystemService
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

import java.nio.file.Files

@RestoreSystemProperties
@RunWith(JUnitPlatform.class)
class SecureBootSpec  extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    @Rule
    TemporaryFolder ddfHome

    @Rule
    TemporaryFolder userHome

    def 'System exits normally when security manager enabled and ddf installed inside of user home dir'() {
        setup:
        def userHomeSysProp = userHome.getRoot().toString()
        def ddfHomeSysProp = userHome.newFolder('ddf').toString()
        def systemService = Mock(SystemService)
        def securityManager = Mock(SecurityManager)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp
        def secureBoot = Spy(SecureBoot, constructorArgs: [systemService])
        secureBoot.securityManagerEnabled() >> securityManager

        when:
        secureBoot.init()

        then:
        1 * systemService.halt('0')
        0 * secureBoot.systemExit(_)
    }

    def 'System boots when security manager enabled and ddf installed outside of user home dir'() {
        setup:
        def userHomeSysProp = userHome.newFolder('tomPenny').toString()
        def ddfHomeSysProp = ddfHome.newFolder('ddf').toString()
        def systemService = Mock(SystemService)
        def securityManager = Mock(SecurityManager)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp
        def secureBoot = Spy(SecureBoot, constructorArgs: [systemService])
        secureBoot.securityManagerEnabled() >> securityManager

        when:
        secureBoot.init()

        then:
        0 * systemService.halt('0')
        0 * secureBoot.systemExit(_)
    }

    def 'System boots when security manager disabled and ddf installed inside of user home dir'() {
        setup:
        def userHomeSysProp = userHome.getRoot().toString()
        def ddfHomeSysProp = userHome.newFolder('ddf').toString()
        def systemService = Mock(SystemService)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp
        def secureBoot = Spy(SecureBoot, constructorArgs: [systemService])
        secureBoot.securityManagerEnabled() >> null

        when:
        secureBoot.init()

        then:
        0 * systemService.halt('0')
        0 * secureBoot.systemExit(_)
    }

    def 'System boots when security manager disabled and ddf installed outside of user home dir'() {
        setup:
        def userHomeSysProp = userHome.newFolder('tomPenny').toString()
        def ddfHomeSysProp = ddfHome.newFolder('ddf').toString()
        def systemService = Mock(SystemService)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp
        def secureBoot = Spy(SecureBoot, constructorArgs: [systemService])
        secureBoot.securityManagerEnabled() >> null

        when:
        secureBoot.init()

        then:
        0 * systemService.halt('0')
        0 * secureBoot.systemExit(_)
    }

    def 'SystemService throws exception when asked to halt and forcefully exits'() {
        setup:
        def exception = new Exception('unable to halt')
        def userHomeSysProp = userHome.getRoot().toString()
        def ddfHomeSysProp = userHome.newFolder('ddf').toString()
        def systemService = Mock(SystemService)
        def securityManager = Mock(SecurityManager)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp
        def secureBoot = Spy(SecureBoot, constructorArgs: [systemService])
        secureBoot.securityManagerEnabled() >> securityManager

        when:
        secureBoot.init()

        then:
        1 * systemService.halt('0') >> { throw exception }
        1 * secureBoot.systemExit(exception) >> null
    }

    def 'System exits normally when attempting to read invalid ddf.home'() {
        setup:
        def userHomeSysProp = userHome.getRoot().toString()
        def ddfHomeSysProp = '/' + System.currentTimeMillis() + '/' + UUID.randomUUID() + '/ddf'
        def systemService = Mock(SystemService)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp

        when:
        new SecureBoot(systemService)

        then:
        1 * systemService.halt('0')
    }

    def 'System exits normally when attempting to read invalid user.home'() {
        setup:
        def userHomeSysProp = '/' + System.currentTimeMillis() + '/' + UUID.randomUUID() + '/tomPenny'
        def ddfHomeSysProp = ddfHome.newFolder('ddf').toString()
        def systemService = Mock(SystemService)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp

        when:
        new SecureBoot(systemService)

        then:
        1 * systemService.halt('0')
    }

    def 'System exits normally when attempting to read invalid ddf.home and invalid user.home'() {
        setup:
        def userHomeSysProp = '/' + System.currentTimeMillis() + '/' + UUID.randomUUID() + '/tomPenny'
        def ddfHomeSysProp = '/' + System.currentTimeMillis() + '/' + UUID.randomUUID() + '/ddf'
        def systemService = Mock(SystemService)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp

        when:
        new SecureBoot(systemService)

        then:
        1 * systemService.halt('0')
    }

    /**
     * This is the case where the SystemService has commanded the system to halt as a result of error
     * (invalid ddf.home or invalid user.home) encountered in the constructor of SecureBoot and blueprint
     * calls SecureBoot.init(). Since the system is already in the process of shutting down, we do not need
     * to reissue the shutdown command.
     */
    def 'A shutdown command is not issued to the system if it is already in the process of shutting down'() {
        setup:
        def userHomeSysProp = userHome.getRoot().toString()
        def ddfHomeSysProp = '/' + System.currentTimeMillis() + '/' + UUID.randomUUID() + '/ddf'
        def systemService = Mock(SystemService)
        System.properties.'ddf.home' = ddfHomeSysProp
        System.properties.'user.home' = userHomeSysProp

        when:
        def secureBoot = new SecureBoot(systemService)
        secureBoot.init()

        then:
        1 * systemService.halt('0')
    }

    /**
     * ie. /opt/ddfHomeSymLink -> /users/tomPenny/ddf
     *
     * NOTE: This test is ignored on Windows as the Create Symbolic Link security policy setting only allows members of the Administrators group to
     * create symbolic links.
     */
    @IgnoreIf({ System.properties['os.name'].toLowerCase().contains('windows') })
    def 'System exits normally when security manager enabled and symbolic link outside of user home dir pointing to ddf installation inside of user home dir'() {
        setup:
        def optDir = temporaryFolder.newFolder('opt').toPath()
        def tomPennyHomeDir = temporaryFolder.newFolder('tomPenny')
        def ddfHomeDir = Files.createDirectory(Paths.get(tomPennyHomeDir.toString(), 'ddf'))
        def ddfHomeSymLink = Files.createSymbolicLink(Paths.get(optDir.toString(), 'ddfHomeSymLink'), ddfHomeDir)
        def systemService = Mock(SystemService)
        def securityManager = Mock(SecurityManager)
        System.properties.'ddf.home' = ddfHomeSymLink.toString()
        System.properties.'user.home' = tomPennyHomeDir.toString()
        def secureBoot = Spy(SecureBoot, constructorArgs: [systemService])
        secureBoot.securityManagerEnabled() >> securityManager

        when:
        secureBoot.init()

        then:
        1 * systemService.halt('0')
        0 * secureBoot.systemExit(_)
    }

    /**
     * ie. /users/tomPenny/ddfHomeSymLink -> /opt/ddf
     *
     * NOTE: This test is ignored on Windows as the Create Symbolic Link security policy setting only allows members of the Administrators group to
     * create symbolic links.
     */
    @IgnoreIf({ System.properties['os.name'].toLowerCase().contains('windows') })
    def 'System boots when security manager enabled and symbolic link inside of user home dir pointing to ddf installation outside of user home dir'() {
        setup:
        def optDir = temporaryFolder.newFolder('opt').toPath()
        def ddfHomeDir = Files.createDirectory(Paths.get(optDir.toString(), 'ddf'))
        def tomPennyHomeDir = temporaryFolder.newFolder('tomPenny')
        def ddfHomeSymLink = Files.createSymbolicLink(Paths.get(tomPennyHomeDir.toString(), 'ddfHomeSymLink'), ddfHomeDir)
        def systemService = Mock(SystemService)
        def securityManager = Mock(SecurityManager)
        System.properties.'ddf.home' = ddfHomeSymLink.toString()
        System.properties.'user.home' = tomPennyHomeDir.toString()
        def secureBoot = Spy(SecureBoot, constructorArgs: [systemService])
        secureBoot.securityManagerEnabled() >> securityManager

        when:
        secureBoot.init()

        then:
        0 * systemService.halt('0')
        0 * secureBoot.systemExit(_)
    }
}