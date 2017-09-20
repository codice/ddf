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
package ddf.metrics.reporting.internal.rrd4j;

import static java.lang.System.exit;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;

public class SampleDataGenerator {
  public static void main(String[] args) {
    if (args.length == 1) {
      try {
        String installLoc = args[0];
        File metricsDir = new File(installLoc, "/data/metrics");
        File[] files = metricsDir.listFiles();
        if (files != null) {
          for (File metricsFile : files) {
            String metricsFileName = metricsFile.getName();
            if (!metricsFileName.endsWith(".rrd")) {
              continue;
            }
            RrdDb oldDb = new RrdDb(metricsFile.getAbsolutePath());
            if (oldDb.getDsCount() > 1) {
              continue;
            }
            DsType dsType = oldDb.getDatasource(0).getType();
            String newDb = "target/" + metricsFileName;
            long startTime = new DateTime().minusYears(1).getMillis();
            int sampleSize = (int) ((new DateTime().getMillis() - startTime) / (60 * 1000));
            new RrdMetricsRetrieverTest.RrdFileBuilder()
                .rrdFileName(newDb)
                .dsType(dsType)
                .numSamples(sampleSize)
                .numRows(sampleSize)
                .startTime(startTime)
                .build();
            FileUtils.copyFile(new File(newDb), metricsFile);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    exit(0);
  }
}
