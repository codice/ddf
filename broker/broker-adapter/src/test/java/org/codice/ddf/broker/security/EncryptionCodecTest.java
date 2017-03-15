/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.broker.security;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ddf.security.encryption.EncryptionService;

@RunWith(MockitoJUnitRunner.class)
public class EncryptionCodecTest {

    private final EncryptionCodec encryptionCodec = new EncryptionCodec();

    @Mock
    private EncryptionService encryptionService;

    @Before
    public void setUp() throws Exception {
        encryptionCodec.encryptionService = encryptionService;

    }

    @Test
    public void decode() throws Exception {
        encryptionCodec.decode("decode");
        verify(encryptionService).decryptValue(eq("decode"));
    }

    @Test
    public void encode() throws Exception {
        encryptionCodec.encode("decode");
        verify(encryptionService).encrypt(eq("decode"));
    }

}