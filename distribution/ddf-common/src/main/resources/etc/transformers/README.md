# Summary

JSON files located under this directory list sets of Input Transformer IDs that are installed by default for a distribution.
The IDs in these files are combined and used by the Content Directory Monitor (CDM) to wait for Input Transformer services which match
those IDs. This prevents the CDM from ingesting resources too soon on system restarts.

To allow the CDM to wait for additional Input Transformers, drop a new JSON file in this directory with the IDs of the Input Transformers.

## File Format

The file contains a single JSON array of string elements. For example:
```json
[
  "transformerId1",
  "transformerId2"
]
```
