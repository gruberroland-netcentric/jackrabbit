/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.jackrabbit.core.data;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.jackrabbit.core.data.AsyncUploadCallback;
import org.apache.jackrabbit.core.data.Backend;
import org.apache.jackrabbit.core.data.CachingDataStore;
import org.apache.jackrabbit.core.data.DataIdentifier;
import org.apache.jackrabbit.core.data.DataStoreException;

/**
 * An in-memory backend implementation used to speed up testing.
 */
public class InMemoryBackend implements Backend {

    private HashMap<DataIdentifier, byte[]> data = new HashMap<DataIdentifier, byte[]>();

    private HashMap<DataIdentifier, Long> timeMap = new HashMap<DataIdentifier, Long>();

    @Override
    public void init(CachingDataStore store, String homeDir, String config)
            throws DataStoreException {
        // ignore
        log("init");
    }

    @Override
    public void close() {
        // ignore
        log("close");
    }

    @Override
    public boolean exists(final DataIdentifier identifier) {
        log("exists " + identifier);
        return data.containsKey(identifier);
    }

    @Override
    public Iterator<DataIdentifier> getAllIdentifiers()
            throws DataStoreException {
        log("getAllIdentifiers");
        return data.keySet().iterator();
    }

    @Override
    public InputStream read(final DataIdentifier identifier)
            throws DataStoreException {
        log("read " + identifier);
        return new ByteArrayInputStream(data.get(identifier));
    }

    @Override
    public void writeAsync(final DataIdentifier identifier, final File file,
            final AsyncUploadCallback callback) throws DataStoreException {
        this.write(identifier, file, true, callback);
    }

    @Override
    public void write(final DataIdentifier identifier, final File file)
            throws DataStoreException {
        this.write(identifier, file, false, null);
    }

    @Override
    public long getLastModified(final DataIdentifier identifier)
            throws DataStoreException {
        log("getLastModified " + identifier);
        return timeMap.get(identifier);
    }

    @Override
    public void deleteRecord(final DataIdentifier identifier)
            throws DataStoreException {
        timeMap.remove(identifier);
        data.remove(identifier);
    }

    @Override
    public Set<DataIdentifier> deleteAllOlderThan(final long min) {
        log("deleteAllOlderThan " + min);
        Set<DataIdentifier> tobeDeleted = new HashSet<DataIdentifier>();
        for (Map.Entry<DataIdentifier, Long> entry : timeMap.entrySet()) {
            DataIdentifier identifier = entry.getKey();
            long timestamp = entry.getValue();
            if (timestamp < min) {
                tobeDeleted.add(identifier);
            }
        }
        for (DataIdentifier identifier : tobeDeleted) {
            timeMap.remove(identifier);
            data.remove(identifier);
        }
        return tobeDeleted;
    }

    @Override
    public long getLength(final DataIdentifier identifier)
            throws DataStoreException {
        try {
            return data.get(identifier).length;
        } catch (Exception e) {
            throw new DataStoreException(e);
        }
    }

    @Override
    public boolean exists(final DataIdentifier identifier, final boolean touch)
            throws DataStoreException {
        boolean retVal = data.containsKey(identifier);
        if (retVal && touch) {
            timeMap.put(identifier, System.currentTimeMillis());
        }
        return retVal;
    }

    private void write(final DataIdentifier identifier, final File file,
            final boolean async, final AsyncUploadCallback callback)
            throws DataStoreException {
        log("write " + identifier + " " + file.length());
        byte[] buffer = new byte[(int) file.length()];
        try {
            if (async && callback == null) {
                throw new IllegalArgumentException(
                    "callback parameter cannot be null");
            }
            DataInputStream din = new DataInputStream(new FileInputStream(file));
            din.readFully(buffer);
            din.close();
            data.put(identifier, buffer);
            timeMap.put(identifier, System.currentTimeMillis());
        } catch (IOException e) {
            if (async) {
                callback.call(identifier, file,
                    AsyncUploadCallback.RESULT.ABORTED);
            }
            throw new DataStoreException(e);
        }
        if (async) {
            callback.call(identifier, file, AsyncUploadCallback.RESULT.SUCCESS);
        }
    }

    /**
     * Log a message if logging is enabled.
     * 
     * @param message
     *            the message
     */
    private void log(final String message) {
        // System.out.println(message);
    }
}
