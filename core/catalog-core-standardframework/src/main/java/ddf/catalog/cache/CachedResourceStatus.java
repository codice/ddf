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

/**
 * Provides the status of the product caching thread @CallableCacheProduct, and the
 * total number of bytes read from the product's @InputStream. If the entire product
 * @InputStream was read successfully a value of -1 is returned for bytes read.
 * The @CachingStatus indicates if the caching was successful or whether an exception
 * in reading or writing to one of the streams was encountered.
 * 
 * @author rodgers
 *
 */
public class CachedResourceStatus {
    
    private long bytesRead;
    private long cachedFileBytesWritten;
    private long posBytesWritten;
    private CachingStatus cachingStatus;
    
    
    public CachedResourceStatus(CachingStatus cachingStatus, long bytesRead, long cachedFileBytesWritten, long posBytesWritten) {
        this.cachingStatus = cachingStatus;
        this.bytesRead = bytesRead;
        this.cachedFileBytesWritten = cachedFileBytesWritten;
        this.posBytesWritten = posBytesWritten;
    }
    
    public void setBytesRead(long bytesRead) {
        this.bytesRead = bytesRead;
    }
    
    public long getBytesRead() {
        return bytesRead;
    }
    
    public long getCachedFileBytesWritten() {
        return cachedFileBytesWritten;
    }

    public void setCachedFileBytesWritten(long cachedFileBytesWritten) {
        this.cachedFileBytesWritten = cachedFileBytesWritten;
    }

    public long getPosBytesWritten() {
        return posBytesWritten;
    }

    public void setPosBytesWritten(long posBytesWritten) {
        this.posBytesWritten = posBytesWritten;
    }

    
    public void setCachingStatus(CachingStatus cachingStatus) {
        this.cachingStatus = cachingStatus;
    }
    
    public CachingStatus getCachingStatus() {
        return cachingStatus;
    }
    
    public String toString() {
        String s = "bytesRead = " + bytesRead + ",  cachedFileBytesWritten = "
                + cachedFileBytesWritten + ",  posBytesWritten = " + posBytesWritten
                + ",  cachingStatus = " + cachingStatus.toString();
        
        return s;
    }
}
