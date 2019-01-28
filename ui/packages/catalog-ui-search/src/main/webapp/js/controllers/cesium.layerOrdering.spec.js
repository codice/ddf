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
import { CesiumLayerOrdering } from './cesium.layerCollection.controller'

describe('CesiumLayerOrdering', () => {
  const checkOrdering = ({ actual, expected }) =>
    expect(actual).to.have.same.ordered.members(expected)

  describe('addLayer()', () => {
    it('Can add layer to empty ordering', () => {
      const newLayerOrder = CesiumLayerOrdering.addLayer({
        prev: [],
        cur: ['a', 'b', 'c', 'd', 'e'],
        layer: 'a',
      })
      checkOrdering({ actual: newLayerOrder, expected: ['a'] })
    })
    it('Adds layer to correct location', () => {
      const prev = ['b', 'd']
      const cur = ['a', 'b', 'c', 'd', 'e']
      const testData = [
        { layer: 'a', expectedOrder: ['a', 'b', 'd'] },
        { layer: 'c', expectedOrder: ['b', 'c', 'd'] },
        { layer: 'e', expectedOrder: ['b', 'd', 'e'] },
      ]
      testData.forEach(({ layer, expectedOrder }) => {
        const newLayerOrder = CesiumLayerOrdering.addLayer({ prev, cur, layer })
        checkOrdering({ actual: newLayerOrder, expected: expectedOrder })
      })
    })
    it('Does not duplicate an existing layer', () => {
      const newLayerOrder = CesiumLayerOrdering.addLayer({
        prev: ['a', 'c', 'd'],
        cur: ['a', 'b', 'c', 'd', 'e'],
        layer: 'c',
      })
      checkOrdering({ actual: newLayerOrder, expected: ['a', 'c', 'd'] })
    })
  })

  describe('Shift Layers', () => {
    const testData = [
      //shift from ['a', 'b', 'c', 'd', 'e']
      ['b', 'c', 'a', 'd', 'e'], //shift from beginning
      ['b', 'c', 'd', 'e', 'a'],
      ['c', 'a', 'b', 'd', 'e'], //shift from middle
      ['a', 'b', 'd', 'e', 'c'],
      ['a', 'b', 'd', 'c', 'e'],
      ['a', 'c', 'b', 'd', 'e'],
      ['a', 'b', 'e', 'c', 'd'], //shift from end
      ['e', 'a', 'b', 'c', 'd'],
      ['a', 'b', 'c', 'd', 'e'], //no shift
    ]
    describe('shiftLayers()', () => {
      describe('All layers initialized', () => {
        const prev = ['a', 'b', 'c', 'd', 'e']
        it('Shifts layers correctly', () => {
          testData.forEach(cur => {
            const newLayerOrder = CesiumLayerOrdering.shiftLayers({ prev, cur })
            checkOrdering({ actual: newLayerOrder, expected: cur })
          })
        })
        it('Shifts correctly when there is no change in ordering', () => {
          const newLayerOrder = CesiumLayerOrdering.shiftLayers({
            prev,
            cur: prev,
          })
          checkOrdering({ actual: newLayerOrder, expected: prev })
        })
      })
      describe('Not all layers initialized', () => {
        const prev = ['b', 'c', 'e']
        const previousLayers = new Set(prev)
        it('Shifts layers correctly', () => {
          testData.forEach(cur => {
            const newLayerOrder = CesiumLayerOrdering.shiftLayers({ prev, cur })
            checkOrdering({
              actual: newLayerOrder,
              expected: cur.filter(layer => previousLayers.has(layer)),
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
          const layerRemoved = layerOrder.filter(id => id !== layerId)
          return [
            ...layerRemoved.slice(0, index),
            layerId,
            ...layerRemoved.slice(index),
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
      it('Gets the correct shift', () => {
        testData.forEach(cur => {
          const { layer, method, count } = CesiumLayerOrdering.getShift({
            prev,
            cur,
          })
          const appliedShift = applyShift({ prev, layer, method, count })
          checkOrdering({ actual: appliedShift, expected: cur })
        })
      })
    })
  })
})
