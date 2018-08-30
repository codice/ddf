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
const errorMessages = {
  malformedWkt: `Malformed WKT. Syntax for supported geometries:
                   POINT (50 40)
                   LINESTRING (30 10, 10 30, 40 40)
                   POLYGON ((30 10, 40 40, 20 40, 10 20, 30 10))
                   MULTIPOINT (10 40, 40 30, 20 20, 30 10)
                   MULTILINESTRING ((10 10, 20 20, 10 40), (40 40, 30 30, 40 20, 30 10))
                   MULTIPOLYGON (((30 20, 45 40, 10 40, 30 20)), ((15 5, 40 10, 10 20, 5 10, 15 5)))
                   GEOMETRYCOLLECTION(POINT(4 6),LINESTRING(4 6,7 10))
                  `,
  invalidWktCoordinates:
    'Invalid coordinates. Note that WKT coordinates are ordered longitude then latitude.',
  invalidCoordinates: 'Invalid coordinates',
  invalidUsngGrid: 'Invalid USNG / MGRS grid',
  invalidRadius: 'Radius must be greater than 0 and at most 10,000 kilometers',
  invalidList: 'One or more entries are invalid',
  invalidBoundingBoxDd: `Invalid bounding box. Coordinates must satisfy the following conditions:
                           North > South
                           East > West
                           North - South >= 0.0001°
                           East - West >= 0.0001°
                          `,
  invalidBoundingBoxDms: `Invalid bounding box. Coordinates must satisfy the following conditions:
                            North > South
                            East > West
                            North - South >= 0.36" (seconds)
                            East - West >= 0.36" (seconds)
                           `,
  tooFewPointsLine: 'Lines must contain 2 or more points',
  tooFewPointsPolygon: 'Polygons must contain 3 or more points',
}

module.exports = errorMessages
