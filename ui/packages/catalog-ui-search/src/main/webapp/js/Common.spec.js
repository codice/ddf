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
  describe('generateUUID', () => {
    it('has dashes', () => {
      const uuid = Common.generateUUID({ useHyphensInUuid: true })
      expect(uuid).to.satisfy(value => value.indexOf('-') >= 0)
    })
    it('does not have dashes', () => {
      const uuid = Common.generateUUID({})
      expect(uuid).to.satisfy(value => value.indexOf('-') === -1)
    })
  })
  describe('getImageSrc', () => {
    it('prepends data:image to null', () => {
      const image = Common.getImageSrc(null)
      expect(image).to.equal('data:image/png;base64,null')
    })
    it('prepends data:image to undefined', () => {
      const image = Common.getImageSrc(undefined)
      expect(image).to.equal('data:image/png;base64,undefined')
    })
    it('prepends data:image to non-empty and non-http string', () => {
      const image = Common.getImageSrc('iVBORw0KGgoAAAANSUhEUgAABkAAAAH')
      expect(image).to.equal(
        'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAABkAAAAH'
      )
    })
    it('prepends data:image to non-string value', () => {
      const image = Common.getImageSrc(123456789)
      expect(image).to.equal('data:image/png;base64,123456789')
    })
    it('returns url unchanged', () => {
      const image = Common.getImageSrc('http://some.url/cx.png')
      expect(image).to.equal('http://some.url/cx.png')
    })
    it('returns empty string unchanged', () => {
      const image = Common.getImageSrc('')
      expect(image).to.equal('')
    })
  })
})
