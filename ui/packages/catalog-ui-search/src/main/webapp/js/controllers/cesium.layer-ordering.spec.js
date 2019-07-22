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
import { addLayer, shiftLayers, getShift } from './cesium.layer-ordering'

describe('Cesium Layer Ordering', () => {
  const checkOrdering = ({ actual, expected }) =>
    expect(actual).to.have.same.ordered.members(expected)

  describe('addLayer()', () => {
    describe('Returns correct layer order when adding to:', () => {
      const initialized = ['b', 'd']
      const all = ['a', 'b', 'c', 'd', 'e']
      const testData = [
        { test: 'beginning', layer: 'a', expectedOrder: ['a', 'b', 'd'] },
        { test: 'middle', layer: 'c', expectedOrder: ['b', 'c', 'd'] },
        { test: 'end', layer: 'e', expectedOrder: ['b', 'd', 'e'] },
      ]

      testData.forEach(({ test, layer, expectedOrder }) => {
        it(`${test}`, () => {
          const newLayerOrder = addLayer({
            initialized,
            all,
            layer,
          })
          checkOrdering({ actual: newLayerOrder, expected: expectedOrder })
        })
      })
    })
    it('Can add layer to empty ordering', () => {
      const newLayerOrder = addLayer({
        initialized: [],
        all: ['a', 'b', 'c', 'd', 'e'],
        layer: 'a',
      })
      checkOrdering({ actual: newLayerOrder, expected: ['a'] })
    })
    it('Does not duplicate an existing layer', () => {
      const newLayerOrder = addLayer({
        initialized: ['a', 'c', 'd'],
        all: ['a', 'b', 'c', 'd', 'e'],
        layer: 'c',
      })
      checkOrdering({ actual: newLayerOrder, expected: ['a', 'c', 'd'] })
    })
    it('Does not add a layer that does not exist', () => {
      const newLayerOrder = addLayer({
        initialized: ['a', 'c', 'd'],
        all: ['a', 'b', 'c', 'd', 'e'],
        layer: 'g',
      })
      checkOrdering({ actual: newLayerOrder, expected: ['a', 'c', 'd'] })
    })
    it('Throws the correct error when the passed in layer orders have different orders', () => {
      expect(
        addLayer.bind(this, {
          initialized: ['d', 'a', 'b'],
          all: ['a', 'b', 'c', 'd', 'e'],
          layer: 'c',
        })
      ).to.throw(
        Error,
        'addLayer: the two layer orders cannot have different orders'
      )
    })
    it('Throws the correct error when the when the set of all layers is not a superset of the initialized layers', () => {
      expect(
        addLayer.bind(this, {
          initialized: ['a', 'g', 'd'],
          all: ['a', 'b', 'c', 'd', 'e'],
          layer: 'c',
        })
      ).to.throw(
        Error,
        'addLayer: the set of all layers must be a superset of initialized layers'
      )
    })
  })

  describe('Shift Layers', () => {
    const testData = [
      //shift from ['a', 'b', 'c', 'd', 'e']
      { test: 'from beginning to middle', cur: ['b', 'c', 'a', 'd', 'e'] },
      { test: 'from beginning to end', cur: ['b', 'c', 'd', 'e', 'a'] },
      { test: 'from middle to beginning', cur: ['c', 'a', 'b', 'd', 'e'] },
      { test: 'from middle to end', cur: ['a', 'b', 'd', 'e', 'c'] },
      { test: 'from middle leftwards', cur: ['a', 'c', 'b', 'd', 'e'] },
      { test: 'from middle rightwards', cur: ['a', 'b', 'd', 'c', 'e'] },
      { test: 'from end to middle', cur: ['a', 'b', 'e', 'c', 'd'] },
      { test: 'from end to beginning', cur: ['e', 'a', 'b', 'c', 'd'] },
      { test: 'no change in ordering', cur: ['a', 'b', 'c', 'd', 'e'] },
    ]
    describe('shiftLayers()', () => {
      describe('All layers initialized', () => {
        const prev = ['a', 'b', 'c', 'd', 'e']

        describe('Returns correct layer order for shifts:', () => {
          testData.forEach(({ test, cur }) => {
            it(`${test}`, () => {
              const newLayerOrder = shiftLayers({ prev, cur })
              checkOrdering({ actual: newLayerOrder, expected: cur })
            })
          })
        })
      })
      describe('Not all layers initialized', () => {
        const prev = ['b', 'c', 'e']
        const previousLayers = new Set(prev)
        describe('Returns correct layer order for shifts:', () => {
          testData.forEach(({ test, cur }) => {
            it(`${test}`, () => {
              const newLayerOrder = shiftLayers({ prev, cur })
              checkOrdering({
                actual: newLayerOrder,
                expected: cur.filter(layer => previousLayers.has(layer)),
              })
            })
          })
        })
      })
    })
    describe('getShift()', () => {
      const prev = ['a', 'b', 'c', 'd', 'e']
      const applyShift = ({
        prev: previousLayerOrder,
        layer,
        method,
        count,
      }) => {
        const METHOD_RAISE = 'raise'
        const shiftLayerToIndex = ({ layerOrder, layer: layerId, index }) => {
          const layerIdRemoved = layerOrder.filter(id => id !== layerId)
          return [
            ...layerIdRemoved.slice(0, index),
            layerId,
            ...layerIdRemoved.slice(index),
          ]
        }
        const modifier = method === METHOD_RAISE ? 1 : -1
        const index = previousLayerOrder.indexOf(layer) + modifier * count
        return shiftLayerToIndex({
          layerOrder: previousLayerOrder,
          layer,
          index,
        })
      }
      describe('Returns the correct shift:', () => {
        testData.forEach(({ test, cur }) => {
          it(`${test}`, () => {
            const { layer, method, count } = getShift({
              prev,
              cur,
            })
            const appliedShift = applyShift({ prev, layer, method, count })
            checkOrdering({ actual: appliedShift, expected: cur })
          })
        })
        it('Throws the correct error when the passed in layer orders do not contain the same ids', () => {
          expect(
            getShift.bind(this, {
              prev: ['a', 'b', 'c', 'd'],
              cur: ['a', 'b'],
            })
          ).to.throw(Error, 'getShift: arrays must contain the same ids')
        })
        it('Throws the correct error when more than one shift is required', () => {
          expect(
            getShift.bind(this, {
              prev: ['d', 'a', 'b'],
              cur: ['b', 'a', 'd'],
            })
          ).to.throw(Error, 'getShift: unable to find shift')
        })
      })
    })
  })
})
