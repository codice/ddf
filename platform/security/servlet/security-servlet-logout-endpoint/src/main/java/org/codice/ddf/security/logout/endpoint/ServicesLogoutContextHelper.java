package org.codice.ddf.security.logout.endpoint;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContext;

@Component(service = ServletContextHelper.class, scope = ServiceScope.BUNDLE)
@HttpWhiteboardContext(name = "servicesLogoutContextHelper", path = "/services/logout")
public class ServicesLogoutContextHelper extends ServletContextHelper {}
