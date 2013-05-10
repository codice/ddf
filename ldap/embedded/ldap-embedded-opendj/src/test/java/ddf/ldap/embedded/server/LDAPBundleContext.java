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
package ddf.ldap.embedded.server;


import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;


public class LDAPBundleContext implements BundleContext
{

    private String dataFolderPath;
    private MockBundle bundle;

    public LDAPBundleContext( String folderPath )
    {
        dataFolderPath = folderPath;
        bundle = new MockBundle();
    }

    @Override
    public String getProperty( String key )
    {
        return null;
    }

    @Override
    public Bundle getBundle()
    {
        return bundle;
    }

    @Override
    public Bundle installBundle( String location, InputStream input ) throws BundleException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle installBundle( String location ) throws BundleException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle getBundle( long id )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Bundle[] getBundles()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void addServiceListener( ServiceListener listener, String filter ) throws InvalidSyntaxException
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void addServiceListener( ServiceListener listener )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeServiceListener( ServiceListener listener )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void addBundleListener( BundleListener listener )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeBundleListener( BundleListener listener )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void addFrameworkListener( FrameworkListener listener )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeFrameworkListener( FrameworkListener listener )
    {
        // TODO Auto-generated method stub

    }

    @Override
    public ServiceRegistration registerService( String[] clazzes, Object service, Dictionary properties )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceRegistration registerService( String clazz, Object service, Dictionary properties )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference[] getServiceReferences( String clazz, String filter ) throws InvalidSyntaxException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference[] getAllServiceReferences( String clazz, String filter ) throws InvalidSyntaxException
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceReference getServiceReference( String clazz )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getService( ServiceReference reference )
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean ungetService( ServiceReference reference )
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public File getDataFile( String filename )
    {
        if (dataFolderPath != null)
        {
            return new File(dataFolderPath + "/" + filename);
        }
        else
        {
            return null;
        }
    }

    @Override
    public Filter createFilter( String filter ) throws InvalidSyntaxException
    {
        // TODO Auto-generated method stub
        return null;
    }

    private class MockBundle implements Bundle
    {

        @Override
        public int getState()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void start( int options ) throws BundleException
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void start() throws BundleException
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void stop( int options ) throws BundleException
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void stop() throws BundleException
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void update( InputStream input ) throws BundleException
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void update() throws BundleException
        {
            // TODO Auto-generated method stub

        }

        @Override
        public void uninstall() throws BundleException
        {
            // TODO Auto-generated method stub

        }

        @Override
        public Dictionary getHeaders()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getBundleId()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public String getLocation()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServiceReference[] getRegisteredServices()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ServiceReference[] getServicesInUse()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean hasPermission( Object permission )
        {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public URL getResource( String name )
        {
            return this.getClass().getResource(name);
        }

        @Override
        public Dictionary getHeaders( String locale )
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getSymbolicName()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Class loadClass( String name ) throws ClassNotFoundException
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Enumeration getResources( String name ) throws IOException
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Enumeration getEntryPaths( String path )
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public URL getEntry( String path )
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getLastModified()
        {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public Enumeration findEntries( String path, String filePattern, boolean recurse )
        {
            final URL url = this.getClass().getResource(path);
            File pathFile = null;
            try
            {
                pathFile = new File(url.toURI());
            }
            catch (URISyntaxException e)
            {
                throw new RuntimeException("Unable to resolve file path", e);
            }
            final File[] files = pathFile.listFiles((FileFilter) new WildcardFileFilter(filePattern));
            Enumeration<URL> enumer = new Enumeration<URL>()
            {
                int place = 0;
                List<File> urlList = Arrays.asList(files);
                @Override
                public boolean hasMoreElements()
                {
                    return place < urlList.size();
                }

                @Override
                public URL nextElement()
                {
                    File file = urlList.get(place++);
                    try
                    {
                        return file.toURL();
                    }
                    catch (MalformedURLException e)
                    {
                        throw new RuntimeException("Unable to convert to URL", e);
                    }
                }
            };
            return enumer;
        }

        @Override
        public BundleContext getBundleContext()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Map getSignerCertificates( int signersType )
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Version getVersion()
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

}
