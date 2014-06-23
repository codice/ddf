OpenGIS(r) filter schema - ReadMe.txt
=====================================

OpenGIS Filter Encoding Implementation Standard
-----------------------------------------------------------------------

The OpenGIS Filter Encoding Standard (FES) defines an XML encoding for
filter expressions. A filter expression logically combines constraints
on the properties of a feature in order to identify a particular subset
of features to be operated upon. For example, a subset of features might
be identified to render them in a particular color or convert them into
a user-specified format.  Constraints can be specified on values of
spatial, temporal and scalar properties.

The Filter Encoding 2.0 standard is defined in OGC document 09-026r1 
and ISO/DIS 19143.

More information may be found at
 http://www.opengeospatial.org/standards/filter

The most current schema are available at http://schemas.opengis.net/ .

The root (all-components) XML Schema Document, which includes
directly and indirectly all the XML Schema Documents, defined by
FEX 2.0 is filterAll.xsd .

* Latest version is: http://schemas.opengis.net/filter/2.0/filterAll.xsd *

-----------------------------------------------------------------------

2010-11-22  Panagiotis (Peter) A. Vretanos

   * v2.0: Added FES 2.0.0 from 09-026r1

2010-01-22  Kevin Stegemoller

  * v1.0: update/verify copyright (06-135r7 s#3.2)
  * v1.0: update relative schema imports to absolute URLs (06-135r7 s#15)
  * v1.0: updated xsd:schema:@version attribute (06-135r7 s#13.4)
  * v1.0: add archives (.zip) files of previous versions
  * v1.0: create/update ReadMe.txt (06-135r7 s#17)

2005-11-22  Arliss Whiteside

	* v1.0.0, v1.0.20, v1.1.0: The sets of XML Schema Documents for
	OpenGIS Filter Versions 1.0.0, 1.0.20, and 1.1.0 have been edited to
	reflect the corrigenda to documents OGC 02-059 and OGC 04-095 that are
	based on the change requests: 
	OGC 05-068r1 "Store xlinks.xsd file at a fixed location"
	OGC 05-081r2 "Change to use relative paths"

  * Note: check each OGC numbered document for detailed changes.

-- [ VERSION NOTES ] --------------------------------------------------

  OGC is incrementally changing how schemas will be hosted. A new
  revision of the Specification Best Practice policy document (06-135r7)
  clarifies this practices.

  OGC is moving to host the schemas using a 2 digit version number so
  that dependent documents (schemas) will not have to change each time a
  schema is corrected (by a corrigendum). The schemas actual version
  number will be kept in the version attribute on the schema element
  which will be used to signify that there has been a change to the
  schema. Each previous revision will be available online in a ZIP
  archive.

  The LATEST version is the M.N directory where
   * M is the major version
   * N is the minor version
  The latest bugfix version now is always in the M.N directory and
  documented in the version attribute on the schema element. The older
  versions are now archived in the -M_N_X.zip files.

  Previously the OGC used M.N.X where
   * M is the major version
   * N is the minor version
   * X is the bugfix (corrigendum) version
  These will be left here for historical reasons.

-- 2010-01-21  Kevin Stegemoller

-----------------------------------------------------------------------

Policies, Procedures, Terms, and Conditions of OGC(r) are available
  http://www.opengeospatial.org/ogc/legal/ .

Copyright (c) 2010 Open Geospatial Consortium, Inc. All Rights Reserved.

-----------------------------------------------------------------------

