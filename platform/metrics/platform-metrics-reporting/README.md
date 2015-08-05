## Sample Data

This module includes a tool to populate your instance with sample metric data.

Invoke it with `mvn clean install exec:java -Dexec.mainClass="ddf.metrics.reporting.internal.rrd4j.SampleDataGenerator" 
-Dexec.classpathScope=test -Dexec.args="/path/to/installation/root"`