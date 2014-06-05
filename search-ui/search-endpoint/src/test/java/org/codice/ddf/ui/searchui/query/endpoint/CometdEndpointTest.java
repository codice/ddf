/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.ui.searchui.query.endpoint;

import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterBuilder;
import org.codice.ddf.notifications.store.NotificationStore;
import org.codice.ddf.ui.searchui.query.controller.NotificationController;
import org.cometd.bayeux.ChannelId;
import org.cometd.bayeux.MarkedReference;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.LocalSession;
import org.cometd.bayeux.server.SecurityPolicy;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.BayeuxServerImpl;
import org.cometd.server.CometdServlet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CometdEndpointTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CometdEndpointTest.class);
    
    private BayeuxServer bayeuxServer = mock(BayeuxServerImpl.class);
    private CometdEndpoint cometdEndpoint;
    
    private static final String mockSessionId = "1234-5678-9012-3456";
    
    private ServerSession mockServerSession = mock(ServerSession.class);
    private ServerMessage mockServerMessage = mock(ServerMessage.class);
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        
        when(mockServerSession.getId()).thenReturn(mockSessionId);
        
        // Return a new mock of LocalSession each time newLocalSession is
        // called on the BayeuxServer
        when(bayeuxServer.newLocalSession(Mockito.anyString())).thenAnswer(
                new Answer<LocalSession>() {
                    public LocalSession answer(InvocationOnMock invocation) {
                        LocalSession localSession = mock(LocalSession.class);
                        when(localSession.getServerSession()).thenReturn(mock(ServerSession.class));
                        return localSession;
                    }
                });
        
        // Mock enough of the behavior of the createChannelIfAbsent method to
        // ensure proper processing of CometD Service annotations
        when(bayeuxServer.createChannelIfAbsent(Mockito.anyString())).thenAnswer(
                new Answer<MarkedReference<ServerChannel>>() {
                    public MarkedReference<ServerChannel> answer(InvocationOnMock invokation) {
                        String channelName = invokation.getArguments()[0].toString();
                        LOGGER.debug("Channel Name: " + channelName);
                        
                        if (null == channelName) {
                            return null;
                        }
                    
                        ChannelId channelId = new ChannelId(channelName);
                        
                        ServerChannel serverChannel = mock(ServerChannel.class);
                        when(serverChannel.getChannelId()).thenReturn(channelId);
                        when(serverChannel.getId()).thenReturn(channelName);
                        
                        @SuppressWarnings("unchecked")
                        MarkedReference<ServerChannel> markedReference = 
                                (MarkedReference<ServerChannel>) mock(MarkedReference.class);
                        // Mark with value of true indicates that the serverChannel
                        // did not previously exist. Implementation of test setup
                        // needs to change if there is a later need to maintain
                        // whether a channel has already been created
                        when(markedReference.isMarked()).thenReturn(true);
                        
                        when(markedReference.getReference()).thenReturn(serverChannel); 
                        
                        return markedReference;
                    }
                });
        
        // Call the actual BayeuxServer methods, rather than the mock methods, 
        // when setting/getting the security policy.
        Mockito.doCallRealMethod().when(bayeuxServer).setSecurityPolicy(
                Mockito.any(SecurityPolicy.class));
        Mockito.doCallRealMethod().when(bayeuxServer).getSecurityPolicy();
        
        // Associate the BayeuxServer with a CometdServlet
        CometdServlet cometdServlet = mock(CometdServlet.class);
        ServletContext servletContext = mock(ServletContext.class);
        when(cometdServlet.getServletContext()).thenReturn(servletContext);
        when(servletContext.getAttribute(BayeuxServer.ATTRIBUTE)).thenReturn(bayeuxServer);
        
        // Create the CometdEndpoint, passing in the mocked CometdServlet
        cometdEndpoint = new CometdEndpoint(cometdServlet,
                mock(CatalogFramework.class), mock(FilterBuilder.class), mock(NotificationStore.class),
                mock(BundleContext.class));
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }
    

    /**
     * Test method that verifies the {@link org.cometd.bayeux.server.SecurityPolicy#canHandshake(BayeuxServer, ServerSession, ServerMessage)}
     * method of the custom {@link org.cometd.bayeux.server.SecurityPolicy} 
     * associated with the {@link org.cometd.bayeux.server.BayeuxServer} created
     * by the {@link CometdEndpoint} registers users with the 
     * {@link NotificationController}.
     * 
     * @throws ServletException
     */
    @Test
    public void testCanHandshakeRegistersUserWithNotificationController() throws ServletException {
        cometdEndpoint.init();
        SecurityPolicy securityPolicy = bayeuxServer.getSecurityPolicy();
        assertNotNull("BayeuxServer's SecurityPolicy is null", securityPolicy); 
  
        // Verify that the mock ServerSession is not already being managed by
        // the NotificationController
        assertNull(cometdEndpoint.notificationController.getSessionByUserId(mockSessionId));
        
        // Invoke the canHandshake method of the SecurityPolicy
        securityPolicy.canHandshake(bayeuxServer, mockServerSession, 
                mockServerMessage);
        
        // Verify that the user userId/ServerSession pair are now being managed
        // by the NotificationController
        assertEquals("NotificationController did not return the expected ServerSession",
                mockServerSession,
                cometdEndpoint.notificationController.getSessionByUserId(mockSessionId));
    }

}
