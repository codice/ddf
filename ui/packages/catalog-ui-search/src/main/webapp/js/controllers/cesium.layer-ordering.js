import { isEqual, intersection } from 'lodash'
export function addLayer({
  prev: previousLayerOrder,
  cur: currentLayerOrder,
  layer: layerId,
}) {
  const previousLayers = new Set(previousLayerOrder)
  return currentLayerOrder.filter(
    id => id === layerId || previousLayers.has(id)
  )
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
    console.warn(`getShift(): arrays must contain the same ids`)
    return {}
  }

  if (isEqual(previousLayerOrder, currentLayerOrder)) {
    return { layer: previousLayerOrder[0], method: 'lower', count: 0 }
  }

  const shiftLayerToIndex = ({ layerOrder, layer: layerId, index }) => {
    const layerRemoved = layerOrder.filter(id => id !== layerId)
    return [
      ...layerRemoved.slice(0, index),
      layerId,
      ...layerRemoved.slice(index),
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
    if (_.isEqual(shiftLayer, currentLayerOrder)) {
      return {
        layer,
        method: currentOrder > previousOrder ? 'raise' : 'lower', // raise means move to higher index :(
        count: Math.abs(currentOrder - previousOrder),
      }
    }
  }
  return {}
}
