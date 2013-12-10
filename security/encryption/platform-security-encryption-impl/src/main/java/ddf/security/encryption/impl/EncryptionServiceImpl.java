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
package ddf.security.encryption.impl;

import ddf.security.encryption.EncryptionService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jasypt.util.text.BasicTextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryptionServiceImpl implements EncryptionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionServiceImpl.class);

    private String key;

    public EncryptionServiceImpl() {
    }

    /**
     * Encrypts a plain text value using jasypt.
     * 
     * @param plainTextValue
     *            The value to encrypt.
     */
    public String encrypt(String plainTextValue) {
        BasicTextEncryptor encryptor = new BasicTextEncryptor();
        encryptor.setPassword(this.key);
        return encryptor.encrypt(plainTextValue);
    }

    /**
     * Decrypts a plain text value using jasypt.
     * 
     * @param encryptedValue
     *            The value to decrypt.
     */
    public String decrypt(String encryptedValue) {
        BasicTextEncryptor dencryptor = new BasicTextEncryptor();
        dencryptor.setPassword(this.key);
        return dencryptor.decrypt(encryptedValue);
    }

    /**
     * @param key
     *            The master key used for encryption and decryption.
     */
    public void setKey(String key) {
        this.key = key;
    }

    // @formatter:off
	/**
	 * @param wrappedEncryptedValue
	 *            An wrapped encrypted password.
	 * 
	 *            <pre>
	 *   {@code
	 *     One can encrypt passwords using the security:encrypt console command.
	 *     
	 *     user@local>security:encrypt secret
	 *     c+GitDfYAMTDRESXSDDsMw==
	 *     
	 *     A wrapped encrypted password is wrapped in ENC() as follows: ENC(HsOcGt8seSKc34sRUYpakQ==)
	 *   }
	 * </pre>
	 */
	// @formatter:on
    public String decryptValue(String wrappedEncryptedValue) {
        String decryptedValue = null;
        String encryptedValue = unwrapEncryptedValue(wrappedEncryptedValue);

        if (wrappedEncryptedValue == null || wrappedEncryptedValue.isEmpty()) {
            LOGGER.error("A blank password was provided in the configuration.");
            decryptedValue = null;
        } else if (!wrappedEncryptedValue.equals(encryptedValue)) {
            LOGGER.debug("Unwrapped encrypted password is now being decrypted");
            decryptedValue = decrypt(encryptedValue);
        } else if (wrappedEncryptedValue.equals(encryptedValue)) {
            /**
             * If the password is not in the form ENC(my-encrypted-password), we assume the password
             * is not encrypted.
             */
            decryptedValue = wrappedEncryptedValue;
            LOGGER.warn("A plain text password was provided in the configuration in the console.  Consider using an encrypted password.");
        }

        return decryptedValue;
    }

    public String unwrapEncryptedValue(String wrappedEncryptedValue) {
        /**
         * The wrapped encrypted password should be in the form ENC(my-encrypted-password)
         */
        final String pattern = "^ENC\\((.*)\\)$";
        String unwrappedEncryptedValue = null;

        if (wrappedEncryptedValue != null) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(wrappedEncryptedValue);
            if (m.find()) {
                LOGGER.debug("Wrapped encrypted password value found.");
                /**
                 * Get the value in parenthesis. In this example, ENC(my-encrypted-password),
                 * m.group(1) would return my-encrypted-password.
                 */
                unwrappedEncryptedValue = m.group(1);
            } else {
                /**
                 * If the password is not in the form ENC(my-encrypted-password), we assume the
                 * password is not encrypted.
                 */
                unwrappedEncryptedValue = wrappedEncryptedValue;
                LOGGER.warn("You have provided a plain text password in your configuration.  Consider using an encrypted password.");
            }

            return unwrappedEncryptedValue;
        } else {
            LOGGER.warn("You have provided a null password in your configuration.");

            return null;
        }
    }
}
