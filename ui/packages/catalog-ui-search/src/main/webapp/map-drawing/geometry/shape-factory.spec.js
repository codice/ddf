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
import { expect } from 'chai'
import {
  makeBBoxGeo,
  makeLineGeo,
  makePointGeo,
  makePointRadiusGeo,
  makePolygonGeo,
} from './shape-factory'

describe('shape-factory', () => {
  describe('makePointGeo', () => {
    it('default', () => {
      const geo = makePointGeo('id', 10, 50)
      expect(geo.geometry.coordinates).to.deep.equal([50, 10])
      expect(geo.geometry.type).to.equal('Point')
      expect(geo.properties.buffer).to.equal(0)
    })
  })
  describe('makePointRadiusGeo', () => {
    it('default', () => {
      const geo = makePointRadiusGeo('id', 10, 50, 600, 'miles')
      expect(geo.geometry.coordinates).to.deep.equal([50, 10])
      expect(geo.geometry.type).to.equal('Point')
      expect(geo.properties.buffer).to.equal(600)
      expect(geo.properties.bufferUnit).to.equal('miles')
    })
  })
  describe('makePolygonGeo', () => {
    it('default', () => {
      const geo = makePolygonGeo(
        'id',
        [[10, 50], [20, 60], [30, 80]],
        5,
        'kilometers'
      )
      expect(geo.geometry.coordinates).to.deep.equal([
        [[10, 50], [20, 60], [30, 80], [10, 50]],
      ])
      expect(geo.geometry.type).to.equal('Polygon')
      expect(geo.properties.buffer).to.equal(5)
      expect(geo.properties.bufferUnit).to.equal('kilometers')
    })
  })
  describe('makeLineGeo', () => {
    it('default', () => {
      const geo = makeLineGeo(
        'id',
        [[10, 50], [20, 60], [30, 80]],
        50,
        'meters'
      )
      expect(geo.geometry.coordinates).to.deep.equal(
        [[10, 50], [20, 60], [30, 80]],
        5,
        'kilometers'
      )
      expect(geo.geometry.type).to.equal('LineString')
      expect(geo.properties.buffer).to.equal(50)
      expect(geo.properties.bufferUnit).to.equal('meters')
    })
  })
  describe('makeBBoxGeo', () => {
    it('default', () => {
      const geo = makeBBoxGeo('id', [10, 20, 50, 55])
      expect(geo.bbox).to.deep.equal([10, 20, 50, 55])
      expect(geo.geometry.type).to.equal('Polygon')
      expect(geo.properties.buffer).to.equal(0)
    })
  })
})
