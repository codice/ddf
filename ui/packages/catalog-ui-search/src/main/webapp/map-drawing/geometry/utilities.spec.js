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
import { makeGeometry } from './utilities'

describe('geometry', () => {
  describe('makeGeometry', () => {
    it('basic line', () => {
      const geometry = makeGeometry(
        'identifier',
        {
          type: 'LineString',
          coordinates: [[10, 30], [15, 40], [20, 25]],
        },
        'purple',
        'Line'
      )
      expect(geometry.type).to.equal('Feature')
      expect(geometry.properties.id).to.equal('identifier')
      expect(geometry.properties.color).to.equal('purple')
      expect(geometry.properties.shape).to.equal('Line')
      expect(geometry.properties.buffer).to.equal(0)
      expect(geometry.properties.bufferUnit).to.equal('meters')
      expect(geometry.geometry.type).to.equal('LineString')
      expect(geometry.geometry.coordinates).to.deep.equal([
        [10, 30],
        [15, 40],
        [20, 25],
      ])
      expect(geometry.bbox).to.deep.equal([10, 25, 20, 40])
    })
    it('buffered line', () => {
      const geometry = makeGeometry(
        'identifier',
        {
          type: 'LineString',
          coordinates: [[10, 30], [15, 40], [20, 25]],
        },
        'purple',
        'Line',
        50,
        'miles'
      )
      expect(geometry.type).to.equal('Feature')
      expect(geometry.properties.id).to.equal('identifier')
      expect(geometry.properties.color).to.equal('purple')
      expect(geometry.properties.shape).to.equal('Line')
      expect(geometry.properties.buffer).to.equal(50)
      expect(geometry.properties.bufferUnit).to.equal('miles')
      expect(geometry.geometry.type).to.equal('LineString')
      expect(geometry.geometry.coordinates).to.deep.equal([
        [10, 30],
        [15, 40],
        [20, 25],
      ])
      expect(geometry.bbox[0]).to.be.below(10)
      expect(geometry.bbox[1]).to.be.below(25)
      expect(geometry.bbox[2]).to.be.above(20)
      expect(geometry.bbox[3]).to.be.above(40)
    })
  })
})
