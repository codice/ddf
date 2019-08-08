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
import MockDrawingContext from './test/mock-drawing-context'
import LineDrawingControl from './line-drawing-control'

describe('LineDrawingControl', () => {
  const makeFeature = () =>
    new ol.Feature({
      geometry: new ol.geom.LineString([[50, 50], [10, 10], [20, 20]]),
      color: 'blue',
      shape: 'Line',
      id: '',
      buffer: 0,
      bufferUnit: 'meters',
    })
  const makeGeoJSON = () => ({
    type: 'Feature',
    properties: {
      color: 'blue',
      shape: 'Line',
      id: '',
      buffer: 0,
      bufferUnit: 'meters',
    },
    geometry: {
      type: 'LineString',
      coordinates: [[50, 50], [10, 10], [20, 20]],
    },
    bbox: [10, 10, 50, 50],
  })
  let context = null
  let recievedGeo = null
  const receiver = geoJSON => {
    recievedGeo = geoJSON
  }
  let control = null
  beforeEach(() => {
    recievedGeo = null
    context = new MockDrawingContext()
    control = new LineDrawingControl(context, receiver)
  })
  describe('constructor', () => {
    it('default', () => {
      expect(control).to.not.equal(undefined)
      expect(control).to.not.equal(null)
    })
  })
  describe('onCompleteDrawing', () => {
    it('default', () => {
      control.onCompleteDrawing({
        feature: makeFeature(),
      })
      const expected = makeGeoJSON()
      expect(recievedGeo).to.deep.equal(expected)
      expect(context.getMethodCalls().updateFeature.length).to.equal(1)
    })
    it('startDrawing -> onCompleteDrawing', () => {
      const startGeo = makeGeoJSON()
      startGeo.geometry.coordinates = [[88, 5], [22, 15], [64, 20], [88, 5]]
      control.startDrawing(startGeo)
      control.onCompleteDrawing({
        feature: makeFeature(),
      })
      const expected = makeGeoJSON()
      expected.properties.color = 'blue'
      expect(recievedGeo).to.deep.equal(expected)
    })
  })
  describe('onCompleteModify', () => {
    it('default', () => {
      control.onCompleteModify({
        features: {
          getArray: () => [makeFeature()],
        },
      })
      const expected = makeGeoJSON()
      expected.properties.id = ''
      expect(recievedGeo).to.deep.equal(expected)
    })
  })
  describe('startDrawing', () => {
    it('default', () => {
      control.startDrawing(makeGeoJSON())
      expect(context.getMethodCalls().addInteractions.length).to.equal(1)
      expect(context.getMethodCalls().setEvent.length).to.equal(3)
      expect(context.getMethodCalls().setDrawInteraction.length).to.equal(1)
      expect(context.getMethodCalls().updateFeature.length).to.equal(1)
      expect(control.isDrawing()).to.equal(true)
    })
  })
  describe('cancelDrawing', () => {
    it('default', () => {
      control.cancelDrawing()
      expect(context.getMethodCalls().removeListeners.length).to.equal(1)
      expect(context.getMethodCalls().removeInteractions.length).to.equal(1)
      expect(control.isDrawing()).to.equal(false)
    })
  })
})
