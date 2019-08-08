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
import * as ol from 'openlayers'
import { expect } from 'chai'
import ShapeDetector from './shape-detector'

describe('ShapeDetector', () => {
  let shapeDetector = null
  beforeEach(() => {
    shapeDetector = new ShapeDetector()
  })
  describe('isBoundingBoxFeature', () => {
    it('Clockwise From Bottom Left', () => {
      const feature = new ol.Feature(
        new ol.geom.Polygon([[[0, 0], [0, 1], [1, 1], [1, 0], [0, 0]]])
      )
      const actual = shapeDetector.isBoundingBoxFeature(feature)
      expect(actual).to.equal(true)
    })
    it('Clockwise From Top Left', () => {
      const feature = new ol.Feature(
        new ol.geom.Polygon([[[0, 1], [1, 1], [1, 0], [0, 0], [0, 1]]])
      )
      const actual = shapeDetector.isBoundingBoxFeature(feature)
      expect(actual).to.equal(true)
    })
    it('Clockwise From Top Right', () => {
      const feature = new ol.Feature(
        new ol.geom.Polygon([[[1, 1], [1, 0], [0, 0], [0, 1], [1, 1]]])
      )
      const actual = shapeDetector.isBoundingBoxFeature(feature)
      expect(actual).to.equal(true)
    })
    it('Clockwise Bottom Right', () => {
      const feature = new ol.Feature(
        new ol.geom.Polygon([[[1, 0], [0, 0], [0, 1], [1, 1], [1, 0]]])
      )
      const actual = shapeDetector.isBoundingBoxFeature(feature)
      expect(actual).to.equal(true)
    })
    it('Old Editor Bounding Box', () => {
      const feature = new ol.Feature(
        new ol.geom.Polygon([
          [
            [37.15079928954003, 35.252426841484066],
            [38.8031053182774, 35.252426841484066],
            [38.8031053182774, 34.092763897976624],
            [37.15079928954003, 34.092763897976624],
            [37.15079928954003, 35.252426841484066],
          ],
        ])
      )
      const actual = shapeDetector.isBoundingBoxFeature(feature)
      expect(actual).to.equal(true)
    })
    it('Random Bounding Box', () => {
      const feature = new ol.Feature(
        new ol.geom.Polygon([
          [
            [-103.23955535888672, 41.966400146484375],
            [-102.4200439453125, 41.966400146484375],
            [-102.4200439453125, 42.591590881347656],
            [-103.23955535888672, 42.591590881347656],
            [-103.23955535888672, 41.966400146484375],
          ],
        ])
      )
      const actual = shapeDetector.isBoundingBoxFeature(feature)
      expect(actual).to.equal(true)
    })
    it('Not a bounding box first and last coordinates', () => {
      const feature = new ol.Feature(
        new ol.geom.Polygon([[[1, 0.1], [0, 0], [0, 1], [1, 1], [1, 0.1]]])
      )
      const actual = shapeDetector.isBoundingBoxFeature(feature)
      expect(actual).to.equal(false)
    })
    it('Not a bounding box middle coordinate', () => {
      const feature = new ol.Feature(
        new ol.geom.Polygon([[[1, 0], [0, 1], [0, 1], [1, 1], [1, 0]]])
      )
      const actual = shapeDetector.isBoundingBoxFeature(feature)
      expect(actual).to.equal(false)
    })
  })
  describe('shapeFromFeature', () => {
    it('Line', () => {
      const feature = new ol.Feature(new ol.geom.LineString([[5, 5], [0, 0]]))
      const actual = shapeDetector.shapeFromFeature(feature)
      expect(actual).to.equal('Line')
    })
    it('Line closed loop', () => {
      const feature = new ol.Feature(
        new ol.geom.LineString([[5, 5], [0, 0], [5, 5]])
      )
      const actual = shapeDetector.shapeFromFeature(feature)
      expect(actual).to.equal('Line')
    })
    it('Bounding Box', () => {
      const feature = new ol.Feature(
        new ol.geom.Polygon([
          [
            [0.212, 0.389],
            [0.212, 0.503],
            [0.513, 0.503],
            [0.513, 0.389],
            [0.212, 0.389],
          ],
        ])
      )
      const actual = shapeDetector.shapeFromFeature(feature)
      expect(actual).to.equal('Bounding Box')
    })
    it('Point Radius', () => {
      const feature = new ol.Feature(new ol.geom.Point([0, 0]))
      feature.set('buffer', 1)
      feature.set('bufferUnit', 'meters')
      const actual = shapeDetector.shapeFromFeature(feature)
      expect(actual).to.equal('Point Radius')
    })
    it('Polygon near circular', () => {
      const feature = new ol.Feature(
        new ol.geom.Polygon([
          [
            [0.0, -2.3125],
            [-2.0027, -1.1563],
            [-2.0027, 1.1563],
            [0.0, 2.3125],
            [2.0027, 1.1563],
            [2.0127, -1.1673],
            [0.0, -2.3125],
          ],
        ])
      )
      const actual = shapeDetector.shapeFromFeature(feature)
      expect(actual).to.equal('Polygon')
    })
    it('Polygon triangle', () => {
      const feature = new ol.Feature(
        new ol.geom.Polygon([[[0.0, 0.0], [3.0, 3.0], [3.0, 0.0], [0.0, 0.0]]])
      )
      const actual = shapeDetector.shapeFromFeature(feature)
      expect(actual).to.equal('Polygon')
    })
  })
})
