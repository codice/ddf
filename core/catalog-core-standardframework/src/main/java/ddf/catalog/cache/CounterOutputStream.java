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
package ddf.catalog.cache;

import java.io.IOException;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapped @OutputStream that maintains a count of the number of bytes written to the @OutputStream.
 * 
 * @author rodgers
 *
 */
public class CounterOutputStream extends OutputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(CounterOutputStream.class);
            
    private OutputStream os;
    private long bytesWritten = 0;
    
    
    public CounterOutputStream(OutputStream os) {
        this.os = os;
    }
    
    public long getBytesWritten() {
        return bytesWritten;
    }
    
    @Override
    public void write(int b) throws IOException {
        os.write(b);
        bytesWritten++;
    }
    
    @Override
    public void write(byte[] b) throws IOException {
        os.write(b);
        bytesWritten += b.length;
    }
    
    @Override
    public void write(byte[] b, int offset, int len) throws IOException {
        os.write(b, offset, len);
        bytesWritten += len;
    }
    
    @Override
    public void close() throws IOException {
        os.close();
    }

    @Override
    public void flush() throws IOException {
        os.flush();
    }
}
