/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package org.codice.ddf.platform.error.injector

import org.eclipse.jetty.servlet.ErrorPageErrorHandler
import org.junit.platform.runner.JUnitPlatform
import org.junit.runner.RunWith
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil
import org.osgi.framework.ServiceEvent
import org.osgi.framework.ServiceReference
import spock.lang.Shared
import spock.lang.Specification

import org.eclipse.jetty.servlet.ErrorPageErrorHandler

import javax.servlet.ServletContext
import javax.servlet.Servlet
import javax.servlet.SessionCookieConfig
import java.lang.reflect.Field
import java.util.concurrent.ScheduledExecutorService

@RunWith(JUnitPlatform.class)
class ErrorPageInjectorSpec extends Specification {

    def curEvent
    def serviceReference
    def bundle
    def bundleContext
    def servletContext
    def service
    def executorService

    def setup() {

        curEvent         = Mock(ServiceEvent.class)
        serviceReference = Mock(ServiceReference.class)
        bundle           = Mock(Bundle.class)
        bundleContext    = Mock(BundleContext.class)
        servletContext   = Mock(ServletContext.class)
        service          = Mock(Servlet.class)

        bundle.getBundleContext() >> bundleContext
        serviceReference.getBundle() >> bundle
        curEvent.getServiceReference() >> serviceReference
        bundleContext.getService(serviceReference) >> servletContext
    }

    def 'test inject error handles only servlet context'() {

        setup:
        def injector = new ErrorPageInjector()
        curEvent.getType() >> ServiceEvent.REGISTERED

        when:
        injector.event(curEvent, null)

        then:
        1 * curEvent.getServiceReference()
        //  Because Field is used it will get into the if statement and then throw a null pointer
        thrown NullPointerException
    }

    def 'test inject error ignores Service Event #e '(int e) {

        setup:
        def injector = new ErrorPageInjector()
        curEvent.getType() >> e

        when:
        injector.event(curEvent, null)

        then:
        0 * curEvent.getServiceReference()

        where:
        e << [ ServiceEvent.UNREGISTERING, ServiceEvent.MODIFIED_ENDMATCH, ServiceEvent.MODIFIED ]
    }

    def 'test init returns on a null bundle '() {
        setup:
        def injector = new ErrorPageInjector()
        //  Because optionals can't be mocked this leads to a null pointer if a mocked bundle is returned
        bundle.getBundleContext() >> null
        bundleContext.getAllServiceReferences(null, _) >> null

        when:
        injector.init()

        then:
        0 * bundleContext.getAllServiceReferences(_,_);
    }

}