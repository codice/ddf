/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.platform.util;

import java.security.SecureRandom;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.prng.BasicEntropySourceProvider;
import org.bouncycastle.crypto.prng.EntropySourceProvider;
import org.bouncycastle.crypto.prng.drbg.DualECSP800DRBG;

public class RandomNumberGenerator {

  public static SecureRandom create() {
    return new SecureRandom(createSeed());
  }

  public static byte[] createSeed() {
    EntropySourceProvider esp = new BasicEntropySourceProvider(new SecureRandom(), true);
    byte[] nonce = new byte[256];
    new SecureRandom().nextBytes(nonce);

    DualECSP800DRBG bcRbg = new DualECSP800DRBG(new SHA256Digest(), 256, esp.get(256), null, nonce);
    byte[] seed = new byte[256];
    bcRbg.generate(seed, null, true);
    return seed;
  }
}
