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
package ddf.metrics.collector.rrd4j;

import ddf.metrics.collector.CollectorException;
import ddf.metrics.collector.JmxCollector;
import ddf.metrics.collector.MetricsUtil;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDbPool;
import org.rrd4j.core.RrdDef;
import org.rrd4j.core.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RrdJmxCollector implements JmxCollector {

  public static final String DEFAULT_METRICS_DIR =
      new AbsolutePathResolver("data" + File.separator + "metrics" + File.separator).getPath();

  public static final String RRD_FILENAME_SUFFIX = ".rrd";

  public static final String DERIVE_DATA_SOURCE_TYPE = "DERIVE";

  public static final String DEFAULT_DATA_SOURCE_NAME = "data";

  public static final String GAUGE_DATA_SOURCE_TYPE = "GAUGE";

  public static final int ONE_YEAR_IN_15_MINUTE_STEPS = 4 * 24 * 365;

  public static final int TEN_YEARS_IN_HOURS = ONE_YEAR_IN_15_MINUTE_STEPS * 10;

  private static final Logger LOGGER = LoggerFactory.getLogger(RrdJmxCollector.class);

  private static final int MILLIS_PER_SECOND = 1000;

  private static final int FIVE_MINUTES_MILLIS = 300000;

  /**
   * RRD default X-Files factor. he X-Files factor defines what part of an RRD consolidation
   * interval may be made up from *UNKNOWN* data while the consolidated value is still regarded as
   * known. It is given as the ratio of allowed *UNKNOWN* Primary Data Points (PDPs) to the number
   * of PDPs in the interval. Thus, it ranges from 0 <= xff < 1.
   *
   * <p>Examples: 0.5 --> 50% --> half of the intervals used may be unknown to build one, known
   * interval
   *
   * <p>0.0 --> 0% --> every PDP must be known in order to build a known Consolidated Data Point
   * (CDP).
   *
   * <p>0.999 --> almost 100% --> a known CDP is built even if there's only one known PDP out of
   * many PDPs.
   */
  private static final double DEFAULT_XFF_FACTOR = 0.5;

  private final RrdDbPool pool;

  /**
   * Name of the JMX MBean that contains the metric being collected. (Should be set by <config>
   * stanza in metrics-reporting-app features.xml file)
   */
  private String mbeanName;

  /**
   * Name of the JMX MBean attribute that maps to the metric being collected. (Should be set by
   * <config> stanza in metrics-reporting-app features.xml file)
   */
  private String mbeanAttributeName;

  private String metricName;

  private String metricType;

  /**
   * Name of the RRD file to store the metric's data being collected. The DDF metrics base directory
   * will be prepended to this filename.
   */
  private String rrdPath;

  /**
   * The name of the RRD data source to use for the metric being collected. This can (and should) be
   * the same for all metrics configured, e.g., "data", since there should only be one data source
   * per RRD file they do not need to be unique data source names.
   */
  private String rrdDataSourceName;

  /**
   * Type of RRD data source to use for the metric's data being collected. A DERIVE type is used for
   * metrics that always increment, e.g., query count. A GAUGE is used for metrics whose value can
   * vary up or down at any time, e.g., query response time. NOTE: DERIVE data source type is
   * preferred over COUNTER because it does not rollover when the underlying MBean counter's value
   * is reset to zero, causing spikes in the RRD graph (and the MBean value gets reset to zero after
   * every system restart since all JMX MBeans are recreated).
   */
  private String rrdDataSourceType;

  private String metricsDir;

  private int sampleRate;

  private long minimumUpdateTimeDelta;

  private long sampleSkipCount;

  private int rrdStep;

  private MBeanServer localMBeanServer;

  private RrdDb rrdDb;

  private Sample sample = null;

  private ScheduledThreadPoolExecutor executor;

  private long mbeanTimeoutMillis = FIVE_MINUTES_MILLIS;

  private ExecutorService executorPool;

  public RrdJmxCollector(String mbeanName, String mbeanAttributeName, String metricName) {
    this(
        mbeanName,
        mbeanAttributeName,
        metricName,
        DERIVE_DATA_SOURCE_TYPE,
        DEFAULT_DATA_SOURCE_NAME);
  }

  public RrdJmxCollector(
      String mbeanName, String mbeanAttributeName, String metricName, String metricType) {
    this(mbeanName, mbeanAttributeName, metricName, metricType, DEFAULT_DATA_SOURCE_NAME);
  }

  public RrdJmxCollector(
      String mbeanName,
      String mbeanAttributeName,
      String metricName,
      String metricType,
      String dataSourceName) {

    LOGGER.debug(
        "Creating RrdJmxCollector for {}, {}, {}, {}, {}",
        mbeanName,
        mbeanAttributeName,
        metricName,
        metricType,
        dataSourceName);

    this.mbeanName = mbeanName;
    this.mbeanAttributeName = mbeanAttributeName;
    this.metricName = metricName;
    this.metricType = metricType;

    metricsDir = DEFAULT_METRICS_DIR;
    localMBeanServer = getLocalMBeanServer();

    // Only expose these values via setter/getter methods for unit
    // testing purposes so that unit tests can run in seconds vs. minutes
    // by using a faster (lower value) sample rate and no (zero)
    // minimum update time delta so that sample updates always occur during
    // unit tests.
    this.sampleRate = 60;
    this.minimumUpdateTimeDelta = 1;
    this.sampleSkipCount = 0;

    // Should always be the same as the sample rate
    rrdStep = this.sampleRate;
    pool = RrdDbPool.getInstance();

    this.rrdDataSourceName = dataSourceName;
    this.rrdDataSourceType = metricType;
  }

  /**
   * Determines whether an object's value is a numeric type or a String with a numeric value.
   *
   * @param value the Object to be tested whether it has a numeric value
   * @return true if object's value is numeric, false otherwise
   */
  public static boolean isNumeric(Object value) {
    return ((value instanceof Number)
        || ((value instanceof String) && NumberUtils.isNumber((String) value)));
  }

  @Override
  public String getMbeanName() {
    return mbeanName;
  }

  @Override
  public String getMbeanAttributeName() {
    return mbeanAttributeName;
  }

  @Override
  public String getMetricName() {
    return metricName;
  }

  @Override
  public String getMetricType() {
    return metricType;
  }

  /** Initialization when the JmxCollector is created. Called by blueprint. */
  public void init() {
    LOGGER.trace("ENTERING: init() for metric {}", metricName);

    if (executorPool == null) {
      executorPool =
          Executors.newCachedThreadPool(
              StandardThreadFactoryBuilder.newThreadFactory("rrdJmxCollectorThread"));
    }

    // Creating JmxCollector can be time consuming,
    // so do this in a separate thread to prevent holding up creation
    // of Sources or the Catalog
    final Runnable jmxCollectorCreator =
        new Runnable() {
          public void run() {
            try {
              configureCollector();
            } catch (CollectorException e) {
              // Ignore, it has already been logged
            } catch (IOException e) {
              // Ignore, it has already been logged
            }
          }
        };

    LOGGER.debug("Start configureCollector thread for JmxCollector {}", mbeanAttributeName);
    executorPool.execute(jmxCollectorCreator);

    LOGGER.trace("EXITING: init()");
  }

  void configureCollector() throws CollectorException, IOException {
    LOGGER.trace("ENTERING: configureCollector() for collector {}", mbeanAttributeName);

    if (!isMbeanAccessible()) {
      String errorMessage = " is not accessible";

      if (Thread.interrupted()) {
        errorMessage += " due to thread interrupt";
      }

      errorMessage += " - no collector will be configured for it.";

      LOGGER.debug("MBean attribute {}{}", mbeanAttributeName, errorMessage);
      throw new CollectorException("MBean attribute " + mbeanAttributeName + errorMessage);
    }

    if (rrdDataSourceType == null) {
      LOGGER.debug(
          "Unable to configure collector for MBean attribute {}\nData Source type for the RRD file cannot be null - must be either DERIVE, COUNTER or GAUGE.",
          mbeanAttributeName);
      throw new CollectorException(
          "Unable to configure collector for MBean attribute "
              + mbeanAttributeName
              + "\nData Source type for the RRD file cannot be null - must be either DERIVE, COUNTER or GAUGE.");
    }

    LOGGER.trace("rrdDataSourceType = {}", rrdDataSourceType);
    createRrdFile(metricName, rrdDataSourceName, DsType.valueOf(rrdDataSourceType));

    updateSamples();

    LOGGER.trace("EXITING: configureCollector() for collector {}", mbeanAttributeName);
  }

  /**
   * Cleanup when the JmxCollector is destroyed, e.g., when system is shutdown. Called by blueprint.
   */
  public void destroy() {
    LOGGER.trace("ENTERING: destroy() for metric {}", metricName);

    // Shutdown the scheduled threaded executor that is polling the MBean attribute (metric)
    if (executor != null) {
      List<Runnable> tasks = executor.shutdownNow();
      if (tasks != null) {
        LOGGER.debug("Num tasks awaiting execution = {}", tasks.size());
      } else {
        LOGGER.debug("No tasks awaiting execution");
      }
    }

    // Close the RRD DB
    try {
      if (rrdDb != null) {
        rrdDb.close();
        pool.release(rrdDb);
      }
    } catch (IOException e) {
      LOGGER.info("Unable to close RRD DB", e);
    }

    LOGGER.trace("EXITING: destroy()");
  }

  /**
   * Verify MBean and its attribute exists and can be collected, i.e., is numeric data (vs.
   * CompositeData)
   *
   * @return true if MBean can be accessed, false otherwise
   */
  private boolean isMbeanAccessible() {
    Object attr = null;
    long startTime = System.currentTimeMillis();
    while (attr == null && (System.currentTimeMillis() - startTime < mbeanTimeoutMillis)) {
      try {
        attr = localMBeanServer.getAttribute(new ObjectName(mbeanName), mbeanAttributeName);

        if (!isNumeric(attr)) {
          LOGGER.debug("{} from MBean {} has non-numeric data", mbeanAttributeName, mbeanName);
          return false;
        }

        if (!(attr instanceof Integer)
            && !(attr instanceof Long)
            && !(attr instanceof Float)
            && !(attr instanceof Double)) {
          return false;
        }
      } catch (Exception e) {
        try {
          LOGGER.trace("MBean [{}] not found, sleeping...", mbeanName);
          Thread.sleep(1000);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();

          return false;
        }
      }
    }

    return attr != null;
  }

  private void createRrdFile(final String metricName, final String dsName, final DsType dsType)
      throws IOException, CollectorException {
    createRrdFile(metricName, dsName, dsType, 0, Double.NaN);
  }

  /**
   * Create an RRD file based on the metric's name (path) in the DDF metrics sub-directory. An RRD
   * DB instance is created from this RRD file's definition (if the RRD file did not already exist,
   * which can occur if the RRD file is created then DDF is restarted and this method is called). If
   * the RRD file already exists, then just create an RRD DB instance based on the existing RRD
   * file.
   *
   * @param metricName path where the RRD file is to be created. This is required.
   * @param dsName data source name for the RRD file. This is required.
   * @param dsType data source type, i.e., DERIVE, COUNTER or GAUGE (This is required.) (ABSOLUTE is
   *     not currently supported)
   * @param minValue the minimum value that will be stored in the data source; any values smaller
   *     than this will be stored as NaN (aka Unknown)
   * @param maxValue the maximum value that will be stored in the data source; any values larger
   *     than this will be stored as NaN (aka Unknown)
   * @throws IOException
   * @throws CollectorException
   */
  private void createRrdFile(
      final String metricName,
      final String dsName,
      final DsType dsType,
      double minValue,
      double maxValue)
      throws IOException, CollectorException {
    LOGGER.trace("ENTERING: createRrdFile");

    if (StringUtils.isEmpty(metricName)) {
      throw new CollectorException("Path where RRD file is to be created must be specified.");
    } else {
      rrdPath = metricsDir + metricName + RRD_FILENAME_SUFFIX;
    }

    if (StringUtils.isEmpty(dsName)) {
      throw new CollectorException(
          "The name of the data source used in the RRD file must be specified.");
    }

    if (!dsType.equals(DsType.COUNTER)
        && !dsType.equals(DsType.GAUGE)
        && !dsType.equals(DsType.DERIVE)) {
      throw new CollectorException(
          "Data Source type for the RRD file must be either DERIVE, COUNTER or GAUGE.");
    }

    File file = new File(rrdPath);
    if (!file.exists()) {
      // Create necessary parent directories
      if (!file.getParentFile().exists()) {
        if (!file.getParentFile().mkdirs()) {
          LOGGER.debug("Could not create parent file: {}", file.getParentFile().getAbsolutePath());
        }
      }

      LOGGER.debug("Creating new RRD file {}", rrdPath);

      RrdDef def = new RrdDef(rrdPath, rrdStep);

      // NOTE: Currently restrict each RRD file to only have one data source
      // (even though RRD supports multiple data sources in a single RRD file)
      def.addDatasource(dsName, dsType, 90, minValue, maxValue);

      // NOTE: Separate code segments based on dsType in case in future
      // we want more or less archivers based on data source type.

      // Use a COUNTER or DERIVE (preferred) for continuous incrementing counters, e.g.,
      // number of queries.
      // DERIVE data source type is preferred because it does not rollover when the
      // underlying MBean counter's value is reset to zero, causing spikes in the
      // RRD graph (and the MBean value gets reset to zero after every system restart
      // since all JMX MBeans are recreated).
      if (dsType == DsType.COUNTER || dsType == DsType.DERIVE || dsType == DsType.GAUGE) {
        // 1 minute resolution for last 60 minutes
        def.addArchive(ConsolFun.TOTAL, DEFAULT_XFF_FACTOR, 1, 60);

        // 15 minute resolution for the last year
        def.addArchive(ConsolFun.TOTAL, DEFAULT_XFF_FACTOR, 15, ONE_YEAR_IN_15_MINUTE_STEPS);

        // 1 minute resolution for last 60 minutes
        def.addArchive(ConsolFun.AVERAGE, DEFAULT_XFF_FACTOR, 1, 60);

        // 15 minute resolution for the last year
        def.addArchive(ConsolFun.AVERAGE, DEFAULT_XFF_FACTOR, 15, ONE_YEAR_IN_15_MINUTE_STEPS);

        // 1 minute resolution for last 60 minutes
        def.addArchive(ConsolFun.MAX, DEFAULT_XFF_FACTOR, 1, 60);

        // 15 minute resolution for the last year
        def.addArchive(ConsolFun.MAX, DEFAULT_XFF_FACTOR, 15, ONE_YEAR_IN_15_MINUTE_STEPS);

        // 1 minute resolution for last 60 minutes
        def.addArchive(ConsolFun.MIN, DEFAULT_XFF_FACTOR, 1, 60);

        // 15 minute resolution for the last year
        def.addArchive(ConsolFun.MIN, DEFAULT_XFF_FACTOR, 15, ONE_YEAR_IN_15_MINUTE_STEPS);

        // Use a GAUGE to store the values we measure directly as they are,
        // e.g., response time for an ingest or query
      }

      // Create RRD file based on the RRD file definition
      rrdDb = pool.requestRrdDb(def);
    } else {
      LOGGER.debug(
          "rrd file {} already exists - absolute path = {}", rrdPath, file.getAbsolutePath());
      rrdDb = pool.requestRrdDb(rrdPath);
    }

    LOGGER.trace("EXITING: createRrdFile");
  }

  /**
   * Configures a scheduled threaded executor to poll the metric's MBean periodically and add a
   * sample to the RRD file with the metric's current value.
   *
   * @throws CollectorException
   */
  public void updateSamples() throws CollectorException {
    LOGGER.trace("ENTERING: updateSamples");

    if (executor == null) {
      executor =
          new ScheduledThreadPoolExecutor(
              1, StandardThreadFactoryBuilder.newThreadFactory("rrdJmxCollectorThread"));
    }

    final Runnable updater =
        new Runnable() {
          public void run() {
            Object attr = null;
            try {
              attr = localMBeanServer.getAttribute(new ObjectName(mbeanName), mbeanAttributeName);

              LOGGER.trace("Sampling attribute {} from MBean {}", mbeanAttributeName, mbeanName);

              // Cast the metric's sampled value to the appropriate data type
              double val = 0;
              if (attr instanceof Integer) {
                val = (Integer) attr;
              } else if (attr instanceof Long) {
                val = ((Long) attr).intValue();
              } else if (attr instanceof Float) {
                val = ((Float) attr);
              } else if (attr instanceof Double) {
                val = ((Double) attr);
              } else {
                throw new IllegalArgumentException(
                    "Unsupported type " + attr + " for attribute " + mbeanAttributeName);
              }

              LOGGER.trace("MBean attribute {} has value = {}", mbeanAttributeName, val);

              // If first time this metric has been sampled, then need to create a
              // sample in the RRD file
              if (sample == null) {
                sample = rrdDb.createSample();
              }

              try {
                long now = System.currentTimeMillis() / MILLIS_PER_SECOND;
                long lastUpdateTime = rrdDb.getLastUpdateTime();

                // Add metric's sample to RRD file with current timestamp
                if (now - rrdDb.getLastUpdateTime() >= minimumUpdateTimeDelta) {
                  updateSample(now, val);
                } else {
                  LOGGER.debug(
                      "Skipping sample update because time between updates is less than {} seconds",
                      minimumUpdateTimeDelta);

                  sampleSkipCount++;

                  LOGGER.debug(
                      "now = {},   lastUpdateTime = {}   (sampleSkipCount = {})",
                      now,
                      lastUpdateTime,
                      sampleSkipCount);
                }
              } catch (IllegalArgumentException iae) {
                LOGGER.info("Dropping sample of datasource {}", rrdDataSourceName, iae);
              }
            } catch (MalformedObjectNameException
                | AttributeNotFoundException
                | InstanceNotFoundException
                | MBeanException
                | ReflectionException e) {
              LOGGER.info("Problems getting MBean attribute {}", mbeanAttributeName, e);
            } catch (IOException e) {
              LOGGER.info("Error updating RRD", e);
            }
          }
        };

    // Setup threaded scheduler to retrieve this MBean attribute's value
    // at the specified sample rate
    LOGGER.debug("Setup ScheduledThreadPoolExecutor for MBean {}", mbeanName);
    executor.scheduleWithFixedDelay(updater, 0, sampleRate, TimeUnit.SECONDS);

    LOGGER.trace("EXITING: updateSamples");
  }

  private void updateSample(long now, double val) throws IOException {

    LOGGER.debug(
        "Sample time is [{}], updating metric [{}] with value [{}]",
        MetricsUtil.getCalendarTime(now),
        mbeanName,
        val);

    sample.setTime(now);
    sample.setValue(rrdDataSourceName, val);
    sample.update();
  }

  /** @return local MBean server */
  private MBeanServer getLocalMBeanServer() {
    if (localMBeanServer == null) {
      localMBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    return localMBeanServer;
  }

  public String getMetricsDir() {
    return metricsDir;
  }

  void setMetricsDir(String metricsDir) {
    this.metricsDir = metricsDir;
  }

  public String getRrdPath() {
    return rrdPath;
  }

  void setRrdPath(String rrdPath) {
    this.rrdPath = rrdPath;
  }

  // void setRrdDataSourceName(String rrdDataSourceName) {
  // this.rrdDataSourceName = rrdDataSourceName;
  // }

  public String getRrdDataSourceType() {
    return rrdDataSourceType;
  }

  //
  //
  // public void setRrdDataSourceType( String rrdDataSourceType )
  // {
  // this.rrdDataSourceType = rrdDataSourceType;
  // LOGGER.debug("rrdDataSourceType = {}", rrdDataSourceType);
  // }

  protected int getSampleRate() {
    return sampleRate;
  }

  protected void setSampleRate(int sampleRate) {
    this.sampleRate = sampleRate;
    this.rrdStep = this.sampleRate;
  }

  protected long getSampleSkipCount() {
    return sampleSkipCount;
  }

  public void setMinimumUpdateTimeDelta(long minimumUpdateTimeDelta) {
    this.minimumUpdateTimeDelta = minimumUpdateTimeDelta;
  }

  void setMbeanTimeoutMillis(long mbeanTimeoutMillis) {
    this.mbeanTimeoutMillis = mbeanTimeoutMillis;
  }
}
