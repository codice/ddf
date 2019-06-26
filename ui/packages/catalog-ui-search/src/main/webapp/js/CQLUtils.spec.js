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
const chai = require('chai')
const expect = chai.expect
const wkx = require('wkx')
const CQLUtils = require('./CQLUtils.js')

const mockMetacardDefinitions = {
  metacardTypes: {
    anyText: {
      id: 'anyText',
      type: 'STRING',
      multivalued: false,
    },
    anyGeo: {
      id: 'anyGeo',
      type: 'LOCATION',
      multivalued: false,
    },
    created: {
      id: 'created',
      type: 'DATE',
      multivalued: false,
    },
  },
}

function assertPolygon(actual, expected) {
  expect(actual.length).equals(expected.length)
  actual.forEach((point, i) => {
    let expectedPoint = expected[i]
    expect(point[0]).equals(expectedPoint[0])
    expect(point[1]).equals(expectedPoint[1])
  })
}

function assertMultiPolygon(actual, expected) {
  expect(actual.length).equals(expected.length)
  actual.forEach((polygon, i) => {
    assertPolygon(polygon, expected[i])
  })
}

describe('CQL Utils', () => {
  it('strips double quotes from property', () => {
    const prop = CQLUtils.getProperty({ property: '"some property"' })
    expect(prop).to.equal('some property')
  })

  it('returns null if property is not a string', () => {
    const prop = CQLUtils.getProperty({ property: {} })
    expect(prop).to.be.null
  })

  describe('filter to CQL and CQL to filter conversions', () => {
    it('transform filter to CQL', () => {
      const cql = CQLUtils.transformFilterToCQL({
        type: 'INTERSECTS',
        property: 'anyGeo',
        value: 'POLYGON((1 2,3 4,5 6,1 2))',
      })
      expect(cql).to.equal('(INTERSECTS("anyGeo", POLYGON((1 2,3 4,5 6,1 2))))')
    })

    it('transform compound AND filter to CQL', () => {
      const cql = CQLUtils.transformFilterToCQL({
        type: 'AND',
        filters: [
          {
            type: 'INTERSECTS',
            property: 'anyGeo',
            value: 'LINESTRING((1 2,3 4))',
          },
          {
            type: 'INTERSECTS',
            property: 'anyGeo',
            value: 'POLYGON((5 6,7 8,9 10,5 6))',
          },
        ],
      })
      expect(cql).to.equal(
        '((INTERSECTS("anyGeo", LINESTRING((1 2,3 4)))) AND (INTERSECTS("anyGeo", POLYGON((5 6,7 8,9 10,5 6)))))'
      )
    })

    it('transform CQL to filter', () => {
      const cql = CQLUtils.transformCQLToFilter(
        '(INTERSECTS(anyGeo, POLYGON((1 2,3 4,5 6,1 2))))'
      )
      expect(cql).to.deep.equal({
        type: 'INTERSECTS',
        property: 'anyGeo',
        value: { type: 'GEOMETRY', value: 'POLYGON((1 2,3 4,5 6,1 2))' },
      })
    })

    it('transform compound AND CQL to filter', () => {
      const cql = CQLUtils.transformCQLToFilter(
        '((INTERSECTS(anyGeo, LINESTRING((1 2,3 4)))) AND (INTERSECTS(anyGeo, POLYGON((5 6,7 8,9 10,5 6)))))'
      )
      expect(cql).to.deep.equal({
        type: 'AND',
        filters: [
          {
            type: 'INTERSECTS',
            property: 'anyGeo',
            value: { type: 'GEOMETRY', value: 'LINESTRING((1 2,3 4))' },
          },
          {
            type: 'INTERSECTS',
            property: 'anyGeo',
            value: { type: 'GEOMETRY', value: 'POLYGON((5 6,7 8,9 10,5 6))' },
          },
        ],
      })
    })
  })

  describe('transforms CQL', () => {
    it('removes single quotes from POLYGON WKTs in CQL', () => {
      const cql = CQLUtils.sanitizeGeometryCql(
        "(INTERSECTS(anyGeo, 'POLYGON((-112.2 43.6,-102.1 48.3,-90.7 35.6,-112.2 43.6))'))"
      )
      expect(cql).to.equal(
        '(INTERSECTS(anyGeo, POLYGON((-112.2 43.6,-102.1 48.3,-90.7 35.6,-112.2 43.6))))'
      )
    })

    it('removes single quotes from MULTIPOLYGON WKTs in CQL', () => {
      const cql = CQLUtils.sanitizeGeometryCql(
        "(INTERSECTS(anyGeo, 'MULTIPOLYGON(((-112.2 43.6,-102.1 48.3,-90.7 35.6,-112.2 43.6)))'))"
      )
      expect(cql).to.equal(
        '(INTERSECTS(anyGeo, MULTIPOLYGON(((-112.2 43.6,-102.1 48.3,-90.7 35.6,-112.2 43.6)))))'
      )
    })

    it('removes single quotes from POINT WKTs in CQL', () => {
      const cql = CQLUtils.sanitizeGeometryCql(
        "(DWITHIN(anyGeo, 'POINT(-110.4 30.4)', 100, meters))"
      )
      expect(cql).to.equal('(DWITHIN(anyGeo, POINT(-110.4 30.4), 100, meters))')
    })

    it('removes single quotes from LINESTRING WKTs in CQL', () => {
      const cql = CQLUtils.sanitizeGeometryCql(
        "(DWITHIN(anyGeo, 'LINESTRING(-106.7 36.2,-87.5 46.5)', 1, meters))"
      )
      expect(cql).to.equal(
        '(DWITHIN(anyGeo, LINESTRING(-106.7 36.2,-87.5 46.5), 1, meters))'
      )
    })

    it('builds CQL for POINT location', () => {
      const cql = CQLUtils.buildIntersectCQL(wkx.Geometry.parse('POINT(1 2)'))
      expect(cql).to.equal('(DWITHIN(anyGeo, POINT(1 2), 1, meters))')
    })

    it('builds CQL for LINESTRING location', () => {
      const cql = CQLUtils.buildIntersectCQL(
        wkx.Geometry.parse('LINESTRING(1 2, 3 4)')
      )
      expect(cql).to.equal('(DWITHIN(anyGeo, LINESTRING(1 2,3 4), 1, meters))')
    })

    it('builds CQL for POLYGON location', () => {
      const cql = CQLUtils.buildIntersectCQL(
        wkx.Geometry.parse('POLYGON((1 2, 3 4, 5 6, 1 2))')
      )
      expect(cql).to.equal('(INTERSECTS(anyGeo, POLYGON((1 2,3 4,5 6,1 2))))')
    })

    it('builds CQL for MULTIPOINT location', () => {
      const cql = CQLUtils.buildIntersectCQL(
        wkx.Geometry.parse('MULTIPOINT((1 2), (3 4))')
      )
      expect(cql).to.equal(
        '(DWITHIN(anyGeo, POINT(1 2), 1, meters)) OR (DWITHIN(anyGeo, POINT(3 4), 1, meters))'
      )
    })

    it('builds CQL for MULTILINESTRING location', () => {
      const cql = CQLUtils.buildIntersectCQL(
        wkx.Geometry.parse('MULTILINESTRING((1 2, 3 4), (5 6, 7 8))')
      )
      expect(cql).to.equal(
        '(DWITHIN(anyGeo, LINESTRING(1 2,3 4), 1, meters)) OR (DWITHIN(anyGeo, LINESTRING(5 6,7 8), 1, meters))'
      )
    })

    it('builds CQL for MULTIPOLYGON location', () => {
      const cql = CQLUtils.buildIntersectCQL(
        wkx.Geometry.parse(
          'MULTIPOLYGON(((1 2, 3 4, 5 6, 1 2)), ((10 20, 30 40, 50 60, 10 20)))'
        )
      )
      expect(cql).to.equal(
        '(INTERSECTS(anyGeo, POLYGON((1 2,3 4,5 6,1 2)))) OR (INTERSECTS(anyGeo, POLYGON((10 20,30 40,50 60,10 20))))'
      )
    })

    it('builds CQL for GEOMETRYCOLLECTION location', () => {
      const cql = CQLUtils.buildIntersectCQL(
        wkx.Geometry.parse(
          'GEOMETRYCOLLECTION(POINT(1 2), LINESTRING(1 2, 3 4))'
        )
      )
      expect(cql).to.equal(
        '(DWITHIN(anyGeo, POINT(1 2), 1, meters)) OR (DWITHIN(anyGeo, LINESTRING(1 2,3 4), 1, meters))'
      )
    })
  })

  describe('generates filters', () => {
    it('generates filter with anyGeo property and LINE type', () => {
      const filter = CQLUtils.generateFilter(
        'some type',
        'anyGeo',
        { type: 'LINE', line: [[1, 1], [2, 2]], lineWidth: 5.0 },
        mockMetacardDefinitions
      )
      expect(filter.type).equals('DWITHIN')
      expect(filter.property).equals('anyGeo')
      expect(filter.value).equals('LINESTRING(1 1,2 2)')
      expect(filter.distance).equals(5.0)
    })

    it('generates filter with anyGeo property and POLYGON type', () => {
      const filter = CQLUtils.generateFilter(
        'some type',
        'anyGeo',
        { type: 'POLYGON', polygon: [[1, 1], [2, 2], [1, 1]] },
        mockMetacardDefinitions
      )
      expect(filter.type).equals('INTERSECTS')
      expect(filter.property).equals('anyGeo')
      expect(filter.value).equals('POLYGON((1 1,2 2,1 1))')
    })

    it('generates filter with anyGeo property and MULTIPOLYGON type', () => {
      const filter = CQLUtils.generateFilter(
        'some type',
        'anyGeo',
        {
          type: 'MULTIPOLYGON',
          polygon: [
            [[3.0, 50.0], [4.0, 49.0], [4.0, 50.0], [3.0, 50.0]],
            [[8.0, 55.0], [9.0, 54.0], [9.0, 55.0], [8.0, 55.0]],
          ],
        },
        mockMetacardDefinitions
      )
      expect(filter.type).equals('INTERSECTS')
      expect(filter.property).equals('anyGeo')
      expect(filter.value).equals(
        'MULTIPOLYGON(((3 50,4 49,4 50,3 50)),((8 55,9 54,9 55,8 55)))'
      )
    })

    it('generates filter with anyGeo property and BBOX type (latlon)', () => {
      const filter = CQLUtils.generateFilter(
        'some type',
        'anyGeo',
        {
          type: 'BBOX',
          locationType: 'latlon',
          west: -97,
          south: 41,
          east: -90,
          north: 46,
        },
        mockMetacardDefinitions
      )
      expect(filter.type).equals('INTERSECTS')
      expect(filter.property).equals('anyGeo')
      expect(filter.value).equals(
        'POLYGON((-97 41,-97 46,-90 46,-90 41,-97 41))'
      )
    })

    it('generates filter with anyGeo property and BBOX type (usng)', () => {
      const filter = CQLUtils.generateFilter(
        'some type',
        'anyGeo',
        {
          type: 'BBOX',
          locationType: 'usng',
          mapWest: -97,
          mapSouth: 41,
          mapEast: -90,
          mapNorth: 46,
        },
        mockMetacardDefinitions
      )
      expect(filter.type).equals('INTERSECTS')
      expect(filter.property).equals('anyGeo')
      expect(filter.value).equals(
        'POLYGON((-97 41,-97 46,-90 46,-90 41,-97 41))'
      )
    })

    it('generates filter with anyGeo property and POINTRADIUS type', () => {
      const filter = CQLUtils.generateFilter(
        'some type',
        'anyGeo',
        { type: 'POINTRADIUS', lon: 2, lat: 3, radius: 10 },
        mockMetacardDefinitions
      )
      expect(filter.type).equals('DWITHIN')
      expect(filter.property).equals('anyGeo')
      expect(filter.value).equals('POINT(2 3)')
      expect(filter.distance).equals(10)
    })

    it('generates filter with anyText property', () => {
      const filter = CQLUtils.generateFilter(
        'some type',
        'anyText',
        'some value',
        mockMetacardDefinitions
      )
      expect(filter.type).equals('some type')
      expect(filter.property).equals('anyText')
      expect(filter.value).equals('some value')
    })

    it('generates filter for filter function', () => {
      const filter = CQLUtils.generateFilterForFilterFunction(
        'myFunc',
        { param1: 'val1' },
        mockMetacardDefinitions
      )
      expect(filter.type).equals('=')
      expect(filter.value).to.be.true
      expect(filter.property).to.deep.equal({
        type: 'FILTER_FUNCTION',
        filterFunctionName: 'myFunc',
        params: { param1: 'val1' },
      })
    })

    it('generates DURING filter with temporal property', () => {
      const filter = CQLUtils.generateFilter(
        'DURING',
        'created',
        '2018-11-01T19:00:00.000Z/2018-11-30T19:00:00.000Z',
        mockMetacardDefinitions
      )
      expect(filter.type).equals('DURING')
      expect(filter.value).equals(
        '2018-11-01T19:00:00.000Z/2018-11-30T19:00:00.000Z'
      )
      expect(filter.from).equals('2018-11-01T19:00:00.000Z')
      expect(filter.to).equals('2018-11-30T19:00:00.000Z')
    })
  })

  describe('checks filter types', () => {
    it('DWITHIN is a geo filter', () => {
      const isGeoFilter = CQLUtils.isGeoFilter('DWITHIN')
      expect(isGeoFilter).to.be.true
    })

    it('INTERSECTS is a geo filter', () => {
      const isGeoFilter = CQLUtils.isGeoFilter('INTERSECTS')
      expect(isGeoFilter).to.be.true
    })

    it('AFTER is not a geo filter', () => {
      const isGeoFilter = CQLUtils.isGeoFilter('AFTER')
      expect(isGeoFilter).to.be.false
    })

    it('filter with a POINT is a point radius', () => {
      const isPointRadiusFilter = CQLUtils.isPointRadiusFilter({
        value: 'POINT(1 1)',
      })
      expect(isPointRadiusFilter).to.be.true
    })

    it('filter with a POINT is not a polygon', () => {
      const isPolygonFilter = CQLUtils.isPolygonFilter({
        value: 'POINT(1 1)',
      })
      expect(isPolygonFilter).to.be.false
    })

    it('filter with a POLYGON is a polygon', () => {
      const isPolygonFilter = CQLUtils.isPolygonFilter({
        value: 'POLYGON((3 50, 4 49, 4 50, 3 50))',
      })
      expect(isPolygonFilter).to.be.true
    })

    it('filter with a POLYGON is not a point radius', () => {
      const isPointRadiusFilter = CQLUtils.isPointRadiusFilter({
        value: 'POLYGON((3 50, 4 49, 4 50, 3 50))',
      })
      expect(isPointRadiusFilter).to.be.false
    })
  })

  describe('parses WKTs into arrays', () => {
    it('correctly parses a POLYGON into an array', () => {
      const polygon = CQLUtils.arrayFromPolygonWkt(
        'POLYGON((3 50, 4 49, 4 50, 3 50))'
      )
      assertPolygon(polygon, [
        [3.0, 50.0],
        [4.0, 49.0],
        [4.0, 50.0],
        [3.0, 50.0],
      ])
    })

    it('correctly parses a MULTIPOLYGON with one POLYGON into an array', () => {
      const multipolygon = CQLUtils.arrayFromPolygonWkt(
        'MULTIPOLYGON(((3 50, 4 49, 4 50, 3 50)))'
      )
      assertMultiPolygon(multipolygon, [
        [[3.0, 50.0], [4.0, 49.0], [4.0, 50.0], [3.0, 50.0]],
      ])
    })

    it('correctly parses a MULTIPOLYGON with multiple POLYGONs into an array', () => {
      const multipolygon = CQLUtils.arrayFromPolygonWkt(
        'MULTIPOLYGON(((3 50, 4 49, 4 50, 3 50)), ((8 55, 9 54, 9 55, 8 55)))'
      )
      assertMultiPolygon(multipolygon, [
        [[3.0, 50.0], [4.0, 49.0], [4.0, 50.0], [3.0, 50.0]],
        [[8.0, 55.0], [9.0, 54.0], [9.0, 55.0], [8.0, 55.0]],
      ])
    })
  })
})
