package org.codice.ddf.catalog.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;

public class ResourceUriSecurityPolicyTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testTwoEmptyUris() throws URISyntaxException, StopProcessingException {
        Metacard input = mock(MetacardImpl.class);
        final URI catalogUri = new URI("");
        when(input.getId()).thenReturn("id");
        when(input.getResourceURI()).thenReturn(new URI(""));
        ResourceUriSecurityPolicy policyPlugin = new ResourceUriSecurityPolicy() {
            protected URI getResourceUriValueFor(String id) {
                return catalogUri;
            }
        };

        policyPlugin.setUpdateResourceUriPermissions(new String[] {"role=admin"});
        PolicyResponse result = policyPlugin.processPreUpdate(input, null);
    }

}