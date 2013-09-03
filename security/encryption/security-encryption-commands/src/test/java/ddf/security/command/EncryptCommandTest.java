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
package ddf.security.command;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.apache.log4j.Logger;
import org.junit.Test;

import ddf.security.command.EncryptCommand;
import ddf.security.encryption.impl.EncryptionServiceImpl;


public class EncryptCommandTest
{
    private static Logger LOGGER = Logger.getLogger( EncryptCommandTest.class );


    @Test
    public void testDoExecute_NonNullPlainTextValue()
    {
        final String key = "secret";
        final String plainTextValue = "protect";
        LOGGER.debug( "key: " + key );

        final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
        encryptionService.setKey( key );
        final EncryptCommand encryptCommand = new EncryptCommand();
        encryptCommand.setEncryptionService( encryptionService );
        setPlainTextValueField( encryptCommand, plainTextValue );
        LOGGER.debug( "Plain text value: " + plainTextValue );

        Object encryptedValue = null;

        try
        {
            encryptedValue = encryptCommand.doExecute();

        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        LOGGER.debug( "Encrypted Value: " + encryptedValue );
        assertNull( encryptedValue );
    }


    @Test
    public void testDoExecute_NullPlainTextValue()
    {
        final String key = "secret";
        final String plainTextValue = null;
        LOGGER.debug( "key: " + key );

        final EncryptionServiceImpl encryptionService = new EncryptionServiceImpl();
        encryptionService.setKey( key );
        final EncryptCommand encryptCommand = new EncryptCommand();
        encryptCommand.setEncryptionService( encryptionService );
        setPlainTextValueField( encryptCommand, plainTextValue );
        LOGGER.debug( "Plain text value: " + plainTextValue );

        Object encryptedValue = null;

        try
        {
            encryptedValue = encryptCommand.doExecute();
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }

        LOGGER.debug( "Encrypted Value: " + encryptedValue );
        assertNull( encryptedValue );
    }


    private void setPlainTextValueField( EncryptCommand encryptCommand, String value )
    {
        final String plainTextValueField = "plainTextValue";
        final Class<? extends EncryptCommand> clazz = encryptCommand.getClass();

        try
        {
            final Field field = clazz.getDeclaredField( plainTextValueField );
            field.setAccessible( true );
            field.set( encryptCommand, value );
        }
        catch ( SecurityException e )
        {
            LOGGER.debug( e );
            fail( e.getMessage() );
        }
        catch ( NoSuchFieldException e )
        {
            LOGGER.debug( e );
            fail( e.getMessage() );
        }
        catch ( IllegalArgumentException e )
        {
            LOGGER.debug( e );
            fail( e.getMessage() );
        }
        catch ( IllegalAccessException e )
        {
            LOGGER.debug( e );
            fail( e.getMessage() );
        }

    }
}
