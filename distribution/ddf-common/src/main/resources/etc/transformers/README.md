# Summary

JSON files located under this directory list sets of Input Transformer IDs that are installed by default for a distribution.
The IDs in these files are combined and used by the Content Directory Monitor (CDM) to wait for Input Transformer services which match
those IDs. This prevents the CDM from ingesting resources too soon on system restarts.

To allow the CDM to wait for additional Input Transformers, drop a new JSON file in this directory with the IDs of the Input Transformers.

To increase or decrease the amount of time required to wait for Input Transformers use the following system property. The timeout
cannot be set to below 30 seconds. The default timeout is set to 5 minutes.
```
org.codice.ddf.cdm.transformerWaitTimeoutSeconds
```

## File Format

The file contains a single JSON array of string elements. For example:
```json
[
  "transformerId1",
  "transformerId2"
]
```
