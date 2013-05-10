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
package ddf.security.encryption;

public interface EncryptionService
{
    /**
     * Encrypts a plain text value.
     * 
     * @param value The value to encrypt.
     */
    String encrypt(String value);
    
    /**
     * Decrypts a plain text value.
     * 
     * @param value The value to decrypt.
     */
    String decrypt(String value);
	
	/**
     * Decrypts the unwrapped encrypted value
     * @param wrappedEncryptedValue
     * @return
     */
	String decryptValue(String wrappedEncryptedValue);

	/**
	 * Unwraps an encrypted value with the "ENC(*)" format
	 * 
	 * @param wrappedEncryptedValue
	 */
	String unwrapEncryptedValue(String wrappedEncryptedValue);
}
