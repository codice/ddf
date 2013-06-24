/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

package ddf.ldap.ldaplogin;

import java.security.Principal;
import java.security.acl.Group;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.config.KeystoreManager;
import org.apache.karaf.jaas.modules.ldap.LDAPLoginModule;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.common.audit.SecurityLogger;
import ddf.security.encryption.EncryptionService;

public class SslLdapLoginModule extends LDAPLoginModule {

    private static final Logger LOGGER = LoggerFactory.getLogger(SslLdapLoginModule.class);

    static final long CREATE_SSL_FACTORY_ARG_6 = 10000;

	@Override
	protected void setupSsl(Hashtable env) throws LoginException {

		LOGGER.trace("Entering: setupSsl");
		ServiceReference ref = null;
		try {
			LOGGER.debug("Setting up SSL");
			env.put(Context.SECURITY_PROTOCOL, "ssl");
			env.put("java.naming.ldap.factory.socket",
					ManagedSSLSocketFactory.class.getName());
			ref = bundleContext.getServiceReference(KeystoreManager.class
					.getName());
			KeystoreManager manager = (KeystoreManager) bundleContext
					.getService(ref);

// The following lines of code may be needed when working DDF-2471.
//			SSLSocketFactory factory = CommonSSLFactory.createSocket((String) options.get(SSL_TRUSTSTORE),
//                    "changeit", (String) options.get(SSL_KEYSTORE),
//                    "changeit");

			LOGGER.debug("parameters passed to createSSLFactory: "
					+ options.get(SSL_PROVIDER) + " "
					+ options.get(SSL_PROTOCOL) + " "
					+ options.get(SSL_ALGORITHM) + " "
					+ options.get(SSL_KEYSTORE) + " "
					+ options.get(SSL_KEYALIAS) + " "
					+ options.get(SSL_TRUSTSTORE) + " " + "10000");
			
			
			SSLSocketFactory factory = manager.createSSLFactory(
					(String) options.get(SSL_PROVIDER),
					(String) options.get(SSL_PROTOCOL),
					(String) options.get(SSL_ALGORITHM),
					(String) options.get(SSL_KEYSTORE),
					(String) options.get(SSL_KEYALIAS),
					(String) options.get(SSL_TRUSTSTORE), CREATE_SSL_FACTORY_ARG_6);

			ManagedSSLSocketFactory.setSocketFactory(factory);
			Thread.currentThread().setContextClassLoader(
					ManagedSSLSocketFactory.class.getClassLoader());
		} catch (Exception e) {
			LOGGER.error("Error configuring SSL: ", e);
			throw new LoginException("Unable to setup SSL support for LDAP: " +
					 e.getMessage());
		} finally {
			if (ref != null) {
				bundleContext.ungetService(ref);
			}
		}
	}
	
	@Override
	public void initialize(Subject subject, CallbackHandler callbackHandler,
			Map<String, ?> sharedState, Map<String, ?> options) {

		BundleContext bundleContext = BundleReference.class
				.cast(this.getClass().getClassLoader()).getBundle()
				.getBundleContext();
		ServiceReference ref = null;
		try {
			Map<String, String> option = (Map<String, String>) options;

			ref = bundleContext.getServiceReference(EncryptionService.class.getName());
			EncryptionService encryptionService = (EncryptionService) bundleContext.getService(ref);

			if(encryptionService != null)
			{
				String decryptedPassword = encryptionService.decryptValue((String) option.get(CONNECTION_PASSWORD));
				option.put(CONNECTION_PASSWORD, decryptedPassword);
			}
			else
			{
				option.put(CONNECTION_PASSWORD, null);
				LOGGER.error("Encryption service reference for ldap was null. Setting connection password to null.");
			}

			super.initialize(subject, callbackHandler, sharedState, option);

		} catch (SecurityException e) {
			LOGGER.error("Error decrypting connection password passed into ldap configuration: ", e);
		} catch (IllegalStateException e) {
			LOGGER.error("Error decrypting connection password passed into ldap configuration: ", e);
		} finally {
			if (ref != null) {
				bundleContext.ungetService(ref);
			}
		}
	}
	
	/**
	 * Added additional logging to the security logger.
	 */
	@Override
    protected boolean doLogin() throws LoginException {
	    try{
	        boolean isLoggedIn = super.doLogin();
	        
	        if(isLoggedIn)
	        {
	            SecurityLogger.logInfo("Username [" + user + "] successfully logged in using LDAP authentication.");
	        }
	        else
	        {
	            SecurityLogger.logWarn("Username [" + user + "] failed LDAP authentication.");
	        }
	        return isLoggedIn;
	        
	    }
	    catch (LoginException le)
	    {
	        SecurityLogger.logWarn("Username [" + user + "] could not log in successfuly using LDAP authentication due to an exception", le);
	        throw le;
	    }
    }
}
