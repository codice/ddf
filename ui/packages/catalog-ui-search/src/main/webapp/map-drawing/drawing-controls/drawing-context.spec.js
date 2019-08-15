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
import MockMap from './test/mock-map'
import DrawingContext from './drawing-context'
import { makeEmptyGeometry } from '../geometry'

const DRAW_LAYER_INDEX = 1
const BUFFER_LAYER_INDEX = 0

describe('DrawingContext', () => {
  let context = null
  let map = null
  beforeEach(() => {
    map = new MockMap()
    context = new DrawingContext({
      map,
      drawingStyle: new ol.style.Style(),
      userProjection: 'user',
      mapProjection: 'map',
    })
  })
  describe('constructor', () => {
    it('default', () => {
      expect(map.getTestData().layerCount).to.equal(2)
      expect(map.getTestData().interactionsCount).to.equal(0)
    })
  })
  describe('updateFeature', () => {
    it('default', () => {
      const geometry = new ol.geom.Polygon([
        [[50, 50], [10, 10], [20, 20], [50, 50]],
      ])
      const feature = new ol.Feature(geometry)
      const source = map.getTestData().layers[DRAW_LAYER_INDEX].getSource()
      expect(source.getFeatures().length).to.equal(0)
      context.updateFeature(feature)
      expect(source.getFeatures().length).to.equal(1)
      context.updateFeature(feature)
      expect(source.getFeatures().length).to.equal(1)
    })
  })
  describe('updateBufferFeature', () => {
    it('no buffer', () => {
      const geometry = new ol.geom.Polygon([
        [[50, 50], [10, 10], [20, 20], [50, 50]],
      ])
      const feature = new ol.Feature(geometry)
      const source = map.getTestData().layers[BUFFER_LAYER_INDEX].getSource()
      expect(source.getFeatures().length).to.equal(0)
      context.updateBufferFeature(feature)
      expect(source.getFeatures().length).to.equal(0)
      context.updateBufferFeature(feature)
      expect(source.getFeatures().length).to.equal(0)
      expect(map.getTestData().eventListeners['pointerdrag'].size).to.equal(0)
      context.removeListeners()
      expect(source.getFeatures().length).to.equal(0)
      expect(map.getTestData().eventListeners['pointerdrag'].size).to.equal(0)
    })
    it('has buffer', () => {
      const geometry = new ol.geom.Polygon([
        [[50, 50], [10, 10], [20, 20], [50, 50]],
      ])
      const feature = new ol.Feature({
        geometry,
        buffer: 1,
        bufferUnit: 'meters',
      })
      const source = map.getTestData().layers[BUFFER_LAYER_INDEX].getSource()
      expect(source.getFeatures().length).to.equal(0)
      context.updateBufferFeature(feature)
      expect(source.getFeatures().length).to.equal(1)
      context.updateBufferFeature(feature)
      expect(source.getFeatures().length).to.equal(1)
      expect(map.getTestData().eventListeners['pointerdrag'].size).to.equal(1)
      context.removeListeners()
      context.removeInteractions()
      expect(source.getFeatures().length).to.equal(0)
      expect(map.getTestData().eventListeners['pointerdrag'].size).to.equal(0)
    })
  })
  describe('setEvent', () => {
    it('snap', () => {
      context.setEvent('snap', 'event', () => {})
      context.removeListeners()
    })
    it('modify', () => {
      context.setEvent('modify', 'event', () => {})
      context.removeListeners()
    })
    it('draw', () => {
      context.setDrawInteraction(new ol.interaction.Extent())
      context.setEvent('draw', 'event', () => {})
      context.removeListeners()
    })
  })
  describe('removeListeners', () => {
    it('default', () => {
      context.setDrawInteraction(new ol.interaction.Extent())
      context.setEvent('snap', 'event', () => {})
      context.setEvent('draw', 'event', () => {})
      context.setEvent('modify', 'event', () => {})
      context.removeListeners()
    })
  })
  describe('addInteractions', () => {
    it('without draw interaction', () => {
      context.addInteractions()
      expect(map.getTestData().interactionsCount).to.equal(2)
    })
    it('with draw interaction', () => {
      context.setDrawInteraction(new ol.interaction.Extent())
      context.addInteractions()
      expect(map.getTestData().interactionsCount).to.equal(3)
    })
  })
  describe('addInteractionsWithoutModify', () => {
    it('without draw interaction', () => {
      context.addInteractionsWithoutModify()
      expect(map.getTestData().interactionsCount).to.equal(1)
    })
    it('with draw interaction', () => {
      context.setDrawInteraction(new ol.interaction.Extent())
      context.addInteractionsWithoutModify()
      expect(map.getTestData().interactionsCount).to.equal(2)
    })
  })
  describe('removeInteractions', () => {
    it('default', () => {
      const geometry = new ol.geom.Polygon([
        [[50, 50], [10, 10], [20, 20], [50, 50]],
      ])
      const feature = new ol.Feature(geometry)
      const source = map.getTestData().layers[0].getSource()
      context.updateFeature(feature)
      context.addInteractions()
      context.removeInteractions()
      expect(map.getTestData().interactionsCount).to.equal(0)
    })
  })
})
