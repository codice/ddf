/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import { expect } from 'chai'
import Common from './Common'

describe('Common', () => {
  describe('wrapMapCoordinates', () => {
    it('overflow +1/-1', () => {
      expect(Common.wrapMapCoordinates(181, [-180, 180])).to.equal(-179)
      expect(Common.wrapMapCoordinates(-181, [-180, 180])).to.equal(179)
    })
    it('overflow +/-a lot', () => {
      expect(Common.wrapMapCoordinates(64.25 + 180 * 7, [-180, 180])).to.equal(
        -180 + 64.25
      )
      expect(Common.wrapMapCoordinates(-64.25 - 180 * 7, [-180, 180])).to.equal(
        180 - 64.25
      )
    })
    it('no overflow mid', () => {
      expect(Common.wrapMapCoordinates(-179, [-180, 180])).to.equal(-179)
      expect(Common.wrapMapCoordinates(179, [-180, 180])).to.equal(179)
      expect(Common.wrapMapCoordinates(0, [-180, 180])).to.equal(0)
      expect(Common.wrapMapCoordinates(5, [-180, 180])).to.equal(5)
      expect(Common.wrapMapCoordinates(-15, [-180, 180])).to.equal(-15)
    })
    it('max should map to min', () => {
      expect(Common.wrapMapCoordinates(180, [-180, 180])).to.equal(-180)
    })
    it('min should remain min', () => {
      expect(Common.wrapMapCoordinates(-180, [-180, 180])).to.equal(-180)
    })
  })
  describe('wrapMapCoordinatesArray', () => {
    it('wraps as expected', () => {
      const coordinates = [[-181, -91], [181, 91], [0, 0]]
      const results = Common.wrapMapCoordinatesArray(coordinates)
      expect(results[0][0]).to.equal(179)
      expect(results[0][1]).to.equal(89)
      expect(results[1][0]).to.equal(-179)
      expect(results[1][1]).to.equal(-89)
      expect(results[2][0]).to.equal(0)
      expect(results[2][1]).to.equal(0)
      expect(results.length).to.equal(3)
    })
  })
})
