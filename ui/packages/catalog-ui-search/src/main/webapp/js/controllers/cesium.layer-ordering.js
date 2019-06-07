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
import { isEqual, intersection } from 'lodash'

export function addLayer({
  initialized: initializedLayerOrder,
  all: allLayerOrder,
  layer: layerId,
}) {
  const initializedLayers = new Set(initializedLayerOrder)
  const filtered = allLayerOrder.filter(id => initializedLayers.has(id))

  if (filtered.length < initializedLayerOrder.length) {
    throw new Error(
      `addLayer: the set of all layers must be a superset of initialized layers`
    )
  }
  if (!isEqual(filtered, initializedLayerOrder)) {
    throw new Error(
      `addLayer: the two layer orders cannot have different orders`
    )
  }
  return allLayerOrder.filter(id => id === layerId || initializedLayers.has(id))
}

export function shiftLayers({
  prev: previousLayerOrder,
  cur: currentLayerOrder,
}) {
  const previousLayers = new Set(previousLayerOrder)
  return currentLayerOrder.filter(id => previousLayers.has(id))
}

export function getShift({ prev: previousLayerOrder, cur: currentLayerOrder }) {
  if (
    intersection(previousLayerOrder, currentLayerOrder).length !==
      previousLayerOrder.length ||
    currentLayerOrder.length !== previousLayerOrder.length
  ) {
    throw new Error(`getShift: arrays must contain the same ids`)
  }

  if (isEqual(previousLayerOrder, currentLayerOrder)) {
    return { layer: previousLayerOrder[0], method: 'lower', count: 0 }
  }

  const shiftLayerToIndex = ({ layerOrder, layer: layerId, index }) => {
    const layerIdRemoved = layerOrder.filter(id => id !== layerId)
    return [
      ...layerIdRemoved.slice(0, index),
      layerId,
      ...layerIdRemoved.slice(index),
    ]
  }

  const changedLayers = previousLayerOrder.filter(
    (id, index) => currentLayerOrder[index] !== id
  )

  for (let i = 0; i < changedLayers.length; i++) {
    const layer = changedLayers[i]
    const previousOrder = previousLayerOrder.indexOf(layer)
    const currentOrder = currentLayerOrder.indexOf(layer)
    const shiftLayer = shiftLayerToIndex({
      layerOrder: previousLayerOrder,
      layer,
      index: currentOrder,
    })
    if (isEqual(shiftLayer, currentLayerOrder)) {
      return {
        layer,
        method: currentOrder > previousOrder ? 'raise' : 'lower', // raise means move to higher index :(
        count: Math.abs(currentOrder - previousOrder),
      }
    }
  }
  throw new Error(`getShift: unable to find shift`)
}
