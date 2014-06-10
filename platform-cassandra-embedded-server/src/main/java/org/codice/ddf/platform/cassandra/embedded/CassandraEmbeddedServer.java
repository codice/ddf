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
package org.codice.ddf.platform.cassandra.embedded;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.cassandra.config.CFMetaData;
import org.apache.cassandra.config.DatabaseDescriptor;
import org.apache.cassandra.config.Schema;
import org.apache.cassandra.db.ColumnFamilyStore;
import org.apache.cassandra.db.Keyspace;
import org.apache.cassandra.db.MeteredFlusher;
import org.apache.cassandra.db.SystemKeyspace;
import org.apache.cassandra.db.commitlog.CommitLog;
import org.apache.cassandra.db.compaction.CompactionManager;
import org.apache.cassandra.db.compaction.LeveledManifest;
import org.apache.cassandra.exceptions.ConfigurationException;
import org.apache.cassandra.io.FSError;
import org.apache.cassandra.io.sstable.CorruptSSTableException;
import org.apache.cassandra.io.util.FileUtils;
import org.apache.cassandra.metrics.StorageMetrics;
import org.apache.cassandra.service.CacheService;
import org.apache.cassandra.service.CassandraDaemon.Server;
import org.apache.cassandra.service.GCInspector;
import org.apache.cassandra.service.StorageService;
import org.apache.cassandra.thrift.ThriftServer;
import org.apache.cassandra.tracing.Tracing;
import org.apache.cassandra.utils.CLibrary;
import org.apache.cassandra.utils.Mx4jTool;
import org.apache.cassandra.utils.Pair;
import org.codice.ddf.persistentstorage.PersistentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.policies.Policies;
import com.google.common.collect.Iterables;

public class CassandraEmbeddedServer implements PersistentStore {

    private static Logger LOGGER = LoggerFactory.getLogger(CassandraEmbeddedServer.class);
    
    private static final String TABLE_NAME = "catalog";
    
    // Have a dedicated thread to call exit to avoid deadlock in the case where the thread that wants to invoke exit
    // belongs to an executor that our shutdown hook wants to wait to exit gracefully. See CASSANDRA-5273.
    private static final Thread exitThread = new Thread(new Runnable()
    {
        public void run()
        {
            //HUGH System.exit(100);
            LOGGER.info("Cassandra exitThread called - how to exit???");
        }
    }, "Exit invoker");
    
    private String ddfHomeDir = System.getProperty("karaf.home");
    private String cassandraDir;
    private String keyspaceName;
    private Cluster cluster;
    
    private ExecutorService executor;
    public Server thriftServer;
    public Server nativeServer;
    
    
    public CassandraEmbeddedServer(String keyspaceName, CassandraConfig config) {
        LOGGER.info("Embedded Cassandra Server starting up ...");
        this.keyspaceName = keyspaceName;       
        
        cassandraDir = ddfHomeDir + "/data/cassandra";
        
        String commitDirName = cassandraDir + "/commitlog";
        LOGGER.info("Cassandra commitlog dir = {}", commitDirName);
        File dir = new File(commitDirName);
        if (!dir.exists()) {
            LOGGER.info("Creating commitlog dir");
            dir.mkdirs();
        }
        
        String dataDirName = cassandraDir + "/data";
        LOGGER.info("Cassandra data dir = {}", dataDirName);
        dir = new File(dataDirName);
        if (!dir.exists()) {
            LOGGER.info("Creating data dir");
            dir.mkdirs();
        }
        
        String savedCachesDirName = cassandraDir + "/saved_caches";
        LOGGER.info("Cassandra saved_caches dir = {}", savedCachesDirName);
        dir = new File(savedCachesDirName);
        if (!dir.exists()) {
            LOGGER.info("Creating saved_caches dir");
            dir.mkdirs();
        }
        
        String configYamlFilename = cassandraDir + "/conf/cassandra.yaml";
        LOGGER.info("Cassandra config YAML file = {}", configYamlFilename);
        File configYamlFile = new File(configYamlFilename);
//        if (!configYamlFile.exists()) {
//            LOGGER.error("Cassandra config YAML file {} does not exist", configYamlFilename);
//        }
        
        //ACHILLES  final File triggersDir = new File(System.getProperty("java.io.tmpdir") + "/cassandra_triggers");
        final File triggersDir = new File(cassandraDir + "/cassandra_triggers");
        if (!triggersDir.exists()) {
            triggersDir.mkdir();
        }
        
        LOGGER.info(" Embedded Cassandra RPC port/Thrift port = {}", config.getRPCPort());
        LOGGER.info(" Embedded Cassandra Native port/CQL3 port = {}", config.getCqlPort());
        LOGGER.info(" Embedded Cassandra Storage port = {}", config.getStoragePort());
        LOGGER.info(" Embedded Cassandra Storage SSL port = {}", config.getStorageSSLPort());
        LOGGER.info(" Embedded Cassandra triggers directory = {}", triggersDir);

        LOGGER.info("Starting Cassandra...");
        config.write(configYamlFile);
        
        System.setProperty("cassandra.triggers_dir", triggersDir.getAbsolutePath());
        System.setProperty("cassandra.config", "file:" + configYamlFilename);  //ACHILLES  config.getConfigFile().getAbsolutePath());
        System.setProperty("cassandra-foreground", "true");
        
        final CountDownLatch startupLatch = new CountDownLatch(1);
        executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (setup()) {
                    start();
                    startupLatch.countDown();
                }
            }
        });

        try {
            startupLatch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("Timeout starting Cassandra embedded", e);
            throw new IllegalStateException("Timeout starting Cassandra embedded", e);
        }
        
        this.cluster = createCluster("localhost", config);
        
        LOGGER.info("Creating {} keyspace", keyspaceName);
        createKeyspace(keyspaceName, true);
        // Sets the keyspace to be used for all subsequent CQL statements
        setKeyspace(keyspaceName);
        createTable(keyspaceName, TABLE_NAME, 
                "id uuid PRIMARY KEY, type text, properties map");
    }
    
    // Based on CassadraDaemon.setup()
    private boolean setup() {
        LOGGER.info("Setting up embedded Cassandra ...");
        // log warnings for different kinds of sub-optimal JVMs.  tldr use 64-bit Oracle >= 1.6u32
        if (!DatabaseDescriptor.hasLargeAddressSpace())
            LOGGER.info("32bit JVM detected.  It is recommended to run Cassandra on a 64bit JVM for better performance.");
        String javaVersion = System.getProperty("java.version");
        String javaVmName = System.getProperty("java.vm.name");
        LOGGER.info("JVM vendor/version: {}/{}", javaVmName, javaVersion);
        if (javaVmName.contains("OpenJDK"))
        {
            // There is essentially no QA done on OpenJDK builds, and
            // clusters running OpenJDK have seen many heap and load issues.
            LOGGER.warn("OpenJDK is not recommended. Please upgrade to the newest Oracle Java release");
        }
        else if (!javaVmName.contains("HotSpot"))
        {
            LOGGER.warn("Non-Oracle JVM detected.  Some features, such as immediate unmap of compacted SSTables, may not work as intended");
        }

        LOGGER.info("Heap size: {}/{}", Runtime.getRuntime().totalMemory(), Runtime.getRuntime().maxMemory());
        for(MemoryPoolMXBean pool: ManagementFactory.getMemoryPoolMXBeans())
            LOGGER.info("{} {}: {}", pool.getName(), pool.getType(), pool.getPeakUsage());
        LOGGER.info("Classpath: {}", System.getProperty("java.class.path"));
        CLibrary.tryMlockall();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            public void uncaughtException(Thread t, Throwable e)
            {
                StorageMetrics.exceptions.inc();
                LOGGER.error("Exception in thread " + t, e);
                Tracing.trace("Exception in thread " + t, e);
                for (Throwable e2 = e; e2 != null; e2 = e2.getCause())
                {
                    // some code, like FileChannel.map, will wrap an OutOfMemoryError in another exception
                    if (e2 instanceof OutOfMemoryError)
                        exitThread.start();

                    if (e2 instanceof FSError)
                    {
                        if (e2 != e) // make sure FSError gets logged exactly once.
                            LOGGER.error("Exception in thread " + t, e2);
                        FileUtils.handleFSError((FSError) e2);
                    }

                    if (e2 instanceof CorruptSSTableException)
                    {
                        if (e2 != e)
                            LOGGER.error("Exception in thread " + t, e2);
                        FileUtils.handleCorruptSSTable((CorruptSSTableException) e2);
                    }
                }
            }
        });

        // check all directories(data, commitlog, saved cache) for existence and permission
        Iterable<String> dirs = Iterables.concat(Arrays.asList(DatabaseDescriptor.getAllDataFileLocations()),
                                                 Arrays.asList(DatabaseDescriptor.getCommitLogLocation(),
                                                               DatabaseDescriptor.getSavedCachesLocation()));
        for (String dataDir : dirs)
        {
            LOGGER.debug("Checking directory {}", dataDir);
            File dir = new File(dataDir);
            if (dir.exists())
                assert dir.isDirectory() && dir.canRead() && dir.canWrite() && dir.canExecute()
                    : String.format("Directory %s is not accessible.", dataDir);
        }

        if (CacheService.instance == null) // should never happen
            throw new RuntimeException("Failed to initialize Cache Service.");

        // check the system keyspace to keep user from shooting self in foot by changing partitioner, cluster name, etc.
        // we do a one-off scrub of the system keyspace first; we can't load the list of the rest of the keyspaces,
        // until system keyspace is opened.
        for (CFMetaData cfm : Schema.instance.getKeyspaceMetaData(Keyspace.SYSTEM_KS).values())
            ColumnFamilyStore.scrubDataDirectories(Keyspace.SYSTEM_KS, cfm.cfName);
        try
        {
            SystemKeyspace.checkHealth();
        }
        catch (ConfigurationException e)
        {
            LOGGER.error("Fatal exception during initialization", e);
            //HUGH System.exit(100);
            return false;
        }

        // load keyspace descriptions.
        DatabaseDescriptor.loadSchemas();

        try
        {
            LeveledManifest.maybeMigrateManifests();
        }
        catch(IOException e)
        {
            LOGGER.error("Could not migrate old leveled manifest. Move away the .json file in the data directory", e);
            //HUGH System.exit(100);
            return false;
        }

        // clean up compaction leftovers
        Map<Pair<String, String>, Map<Integer, UUID>> unfinishedCompactions = SystemKeyspace.getUnfinishedCompactions();
        for (Pair<String, String> kscf : unfinishedCompactions.keySet())
            ColumnFamilyStore.removeUnfinishedCompactionLeftovers(kscf.left, kscf.right, unfinishedCompactions.get(kscf));
        SystemKeyspace.discardCompactionsInProgress();

        // clean up debris in the rest of the keyspaces
        for (String keyspaceName : Schema.instance.getKeyspaces())
        {
            // Skip system as we've already cleaned it
            if (keyspaceName.equals(Keyspace.SYSTEM_KS))
                continue;

            for (CFMetaData cfm : Schema.instance.getKeyspaceMetaData(keyspaceName).values())
                ColumnFamilyStore.scrubDataDirectories(keyspaceName, cfm.cfName);
        }

        // initialize keyspaces
        for (String keyspaceName : Schema.instance.getKeyspaces())
        {
            if (LOGGER.isDebugEnabled())
                LOGGER.debug("opening keyspace " + keyspaceName);
            // disable auto compaction until commit log replay ends
            for (ColumnFamilyStore cfs : Keyspace.open(keyspaceName).getColumnFamilyStores())
            {
                for (ColumnFamilyStore store : cfs.concatWithIndexes())
                {
                    store.disableAutoCompaction();
                }
            }
        }

        if (CacheService.instance.keyCache.size() > 0)
            LOGGER.info("completed pre-loading ({} keys) key cache.", CacheService.instance.keyCache.size());

        if (CacheService.instance.rowCache.size() > 0)
            LOGGER.info("completed pre-loading ({} keys) row cache.", CacheService.instance.rowCache.size());

        try
        {
            GCInspector.instance.start();
        }
        catch (Throwable t)
        {
            LOGGER.warn("Unable to start GCInspector (currently only supported on the Sun JVM)");
        }

        // MeteredFlusher can block if flush queue fills up, so don't put on scheduledTasks
        // Start it before commit log, so memtables can flush during commit log replay
        StorageService.optionalTasks.scheduleWithFixedDelay(new MeteredFlusher(), 1000, 1000, TimeUnit.MILLISECONDS);

        // replay the log if necessary
        try
        {
            CommitLog.instance.recover();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        // enable auto compaction
        for (Keyspace keyspace : Keyspace.all())
        {
            for (ColumnFamilyStore cfs : keyspace.getColumnFamilyStores())
            {
                for (final ColumnFamilyStore store : cfs.concatWithIndexes())
                {
                    store.enableAutoCompaction();
                }
            }
        }
        // start compactions in five minutes (if no flushes have occurred by then to do so)
        Runnable runnable = new Runnable()
        {
            public void run()
            {
                for (Keyspace keyspaceName : Keyspace.all())
                {
                    for (ColumnFamilyStore cf : keyspaceName.getColumnFamilyStores())
                    {
                        for (ColumnFamilyStore store : cf.concatWithIndexes())
                            CompactionManager.instance.submitBackground(store);
                    }
                }
            }
        };
        StorageService.optionalTasks.schedule(runnable, 5 * 60, TimeUnit.SECONDS);

        SystemKeyspace.finishStartup();

        // start server internals
        //HUGH StorageService.instance.registerDaemon(this);
        try
        {
            StorageService.instance.initServer();
        }
        catch (ConfigurationException e)
        {
            LOGGER.error("Fatal configuration error", e);
            System.err.println(e.getMessage() + "\nFatal configuration error; unable to start server.  See log for stacktrace.");
            //HUGH System.exit(1);
            return false;
        }

        Mx4jTool.maybeLoad();

        // Metrics
        /*HUGH
        String metricsReporterConfigFile = System.getProperty("cassandra.metricsReporterConfigFile");
        if (metricsReporterConfigFile != null)
        {
            LOGGER.info("Trying to load metrics-reporter-config from file: {}", metricsReporterConfigFile);
            try
            {
                String reportFileLocation = CassandraDaemon.class.getClassLoader().getResource(metricsReporterConfigFile).getFile();
                ReporterConfig.loadFromFile(reportFileLocation).enableAll();
            }
            catch (Exception e)
            {
                LOGGER.warn("Failed to load metrics-reporter-config, metric sinks will not be activated", e);
            }
        }
        END HUGH*/

        /*HUGH
        if (!FBUtilities.getBroadcastAddress().equals(InetAddress.getLoopbackAddress()))
            waitForGossipToSettle();
        END HUGH*/

        // Thift
        InetAddress rpcAddr = DatabaseDescriptor.getRpcAddress();
        int rpcPort = DatabaseDescriptor.getRpcPort();
        thriftServer = new ThriftServer(rpcAddr, rpcPort);

        // Native transport
        InetAddress nativeAddr = DatabaseDescriptor.getNativeTransportAddress();
        int nativePort = DatabaseDescriptor.getNativeTransportPort();
        nativeServer = new org.apache.cassandra.transport.Server(nativeAddr, nativePort);
        
        return true;
    }
    
    public void start()
    {
        LOGGER.info("Starting embedded Cassandra ...");
        String nativeFlag = System.getProperty("cassandra.start_native_transport");
        if ((nativeFlag != null && Boolean.parseBoolean(nativeFlag)) || (nativeFlag == null && DatabaseDescriptor.startNativeTransport()))
            nativeServer.start();
        else
            LOGGER.info("Not starting native transport as requested. Use JMX (StorageService->startNativeTransport()) or nodetool (enablebinary) to start it");

        String rpcFlag = System.getProperty("cassandra.start_rpc");
        if ((rpcFlag != null && Boolean.parseBoolean(rpcFlag)) || (rpcFlag == null && DatabaseDescriptor.startRpc()))
            thriftServer.start();
        else
            LOGGER.info("Not starting RPC server as requested. Use JMX (StorageService->startRPCServer()) or nodetool (enablethrift) to start it");
    }
    
    private Cluster createCluster(String hostname, CassandraConfig config) {
        return Cluster.builder()
                .addContactPoint(hostname)
                .withPort(config.getCqlPort())
                .withClusterName(config.getClusterName())
                .withCompression(Compression.SNAPPY)
                .withLoadBalancingPolicy(Policies.defaultLoadBalancingPolicy())
                .withRetryPolicy(Policies.defaultRetryPolicy())
                .withReconnectionPolicy(Policies.defaultReconnectionPolicy())
                .build();
    }
    
    public void createKeyspace(String keyspaceName) {
        createKeyspace(keyspaceName, true);
    }
    
    public void createKeyspace(String keyspaceName, boolean keyspaceDurableWrite) {
        final Session session = cluster.connect("system");
        final Row row = session.execute(
                "SELECT count(1) FROM schema_keyspaces WHERE keyspace_name='" + keyspaceName + "'").one();
        if (row.getLong(0) != 1) {
            StringBuilder createKeyspaceStatement = new StringBuilder("CREATE keyspace ");
            createKeyspaceStatement.append(keyspaceName);
            createKeyspaceStatement.append(" WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':1}");
            if (!keyspaceDurableWrite) {
                createKeyspaceStatement.append(" AND DURABLE_WRITES=false");
            }
            session.execute(createKeyspaceStatement.toString());
        }
        session.close();
    }
    
    
    //TODO:  Pass in Java object to be morphed into CQL column descriptions (see Achilles TableBuilder, etc. code for ideas)   
    public void createTable(String keyspaceName, String tableName, String cqlColumnDescriptions) {        
        try {
            if (!hasTable(tableName, keyspaceName)) {               
                String query = "CREATE TABLE " + keyspaceName + "." + tableName + " (" + cqlColumnDescriptions + ")";
                LOGGER.info("Executing query: {}", query);
                getSession(keyspaceName).execute(query);
            } else {
                LOGGER.info("Table {} already exists", tableName);
            }
        } catch (QueryExecutionException e) {
            LOGGER.info("QueryExecutionException: ", e);
        } catch (NoHostAvailableException e) {
            LOGGER.info("NoHostAvailableException: ", e);
        }
    }
    
    public Session getSession(String keyspaceName) {
        return cluster.connect(keyspaceName);
    }
    
    public void setKeyspace(String keyspaceName) {
        getSession(keyspaceName).execute("USE " + keyspaceName);
    }
    
    public List<String> getKeyspaceNames() {
        Metadata metadata = cluster.getMetadata();
        List<KeyspaceMetadata> keyspaceMetadataList = metadata.getKeyspaces();
        List<String> keyspaceNames = new ArrayList<String>();
        
        for (KeyspaceMetadata ksMetadata : keyspaceMetadataList) {
            keyspaceNames.add(ksMetadata.getName());
        }
        
        return keyspaceNames;
    }
    
    public boolean hasKeyspace(String keyspaceName) {
        return getKeyspaceNames().contains(keyspaceName);
    }
    
    public Map<String, TableMetadata> getTables(String keyspaceName) {
        Metadata metadata = cluster.getMetadata();
        List<KeyspaceMetadata> keyspaceMetadataList = metadata.getKeyspaces();
        Map<String, TableMetadata> tables = new HashMap<String, TableMetadata>();
        LOGGER.info("keyspaceMetadataList.size() = {}", keyspaceMetadataList.size());
        
        for (KeyspaceMetadata ksMetadata : keyspaceMetadataList) {
            LOGGER.info("keyspace name = {}", ksMetadata.getName());
            if (ksMetadata.getName().equals(keyspaceName)) {               
                for (TableMetadata tableMetadata : ksMetadata.getTables()) {
                    LOGGER.info("table name = {}", tableMetadata.getName());
                    tables.put(tableMetadata.getName(), tableMetadata);
                }
            }
        }
        
        return tables;
    }
    
    public TableMetadata getTable(String keyspaceName, String tableName) {
        Metadata metadata = cluster.getMetadata();
        List<KeyspaceMetadata> keyspaceMetadataList = metadata.getKeyspaces();
        Map<String, TableMetadata> tables = new HashMap<String, TableMetadata>();
        LOGGER.info("keyspaceMetadataList.size() = {}", keyspaceMetadataList.size());
        
        for (KeyspaceMetadata ksMetadata : keyspaceMetadataList) {
            LOGGER.info("keyspace name = {}", ksMetadata.getName());
            if (ksMetadata.getName().equals(keyspaceName)) {               
                for (TableMetadata tableMetadata : ksMetadata.getTables()) {
                    if (tableMetadata.getName().equals(tableName)) {
                        return tableMetadata;
                    }
                }
            }
        }
        
        return null;
    }
    
    public boolean hasTable(String tableName, String keyspaceName) {
        Map<String, TableMetadata> tables = getTables(keyspaceName);
        
        return tables.containsKey(tableName);
    }
    
    public void addEntry(String keyspaceName, String tableName, String cql) {
        LOGGER.info("Executing CQL:  {}", cql);
        Session session = getSession(keyspaceName);
        session.execute(cql);
    }
    
    public void shutdown() {
        LOGGER.info("Embedded Cassandra shutdown() invoked ...");
        //TODO ...
        //session.close();   //will this do it???
    }

    @Override
    public void addEntry(String type, Map<String, Object> properties) {
        if (type.equalsIgnoreCase("saved_query")) {
            addSavedQuery(properties);
        } else {
            LOGGER.info("Unsupported entry type {}", type);
        }
        
    }

    @Override
    public void getEntry(String type, String cql) {
        LOGGER.info("Not yet implemented ...");
    }
    
    private void addSavedQuery(Map<String, Object> properties) {
        LOGGER.info("ENTERING: addSavedQuery()");
        
        String cql = "INSERT INTO " + keyspaceName + "." + TABLE_NAME + "(id, type, properties) VALUES (" + 
        		UUID.randomUUID().toString() + ", 'saved_query', {";
        int count = 1;
        for (String name : properties.keySet()) {
            cql += "'" + name + "' : '" + properties.get(name);
            if (count == properties.size()) {
                cql += "'";
            } else {
                cql += "',";
            }
            count++;
        }
        cql += "})";
        LOGGER.info("cql = {}", cql);
        Session session = getSession(keyspaceName);
        session.execute(cql);
        
        LOGGER.info("EXITING: addSavedQuery()");
    }
}
