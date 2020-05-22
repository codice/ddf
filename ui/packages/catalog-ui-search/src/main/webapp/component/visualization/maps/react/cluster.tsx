import * as React from 'react'
import { hot } from 'react-hot-loader'
import { ClusterType } from './geometries'
const _ = require('underscore')
const _debounce = require('lodash/debounce')
const calculateConvexHull = require('geo-convex-hull')

type Props = {
  cluster: ClusterType
  map: any
  selectionInterface: any
}

type SelectionTypeType = 'fullySelected' | 'partiallySelected' | 'unselected'

const Cluster = ({ cluster, map, selectionInterface }: Props) => {
  const geometries = React.useRef([] as any[])
  const selectionType = React.useRef('unselected' as SelectionTypeType)

  const handleCluster = () => {
    const center = map.getCartographicCenterOfClusterInDegrees(cluster)
    geometries.current.push(
      map.addPointWithText(center, {
        id: cluster.results.map(result => result.plain.id),
        color: cluster.results[0].getColor(),
      })
    )
  }

  const addConvexHull = () => {
    const points = cluster.results.map(result => result.getPoints())
    const data = _.flatten(points, true).map((coord: any) => ({
      longitude: coord[0],
      latitude: coord[1],
    }))
    const convexHull = calculateConvexHull(data).map((coord: any) => [
      coord.longitude,
      coord.latitude,
    ])
    convexHull.push(convexHull[0])
    const geometry = map.addLine(convexHull, {
      id: cluster.results.map(result => result.plain.id),
      color: cluster.results[0].getColor(),
    })
    map.hideGeometry(geometry)
    geometries.current.push(geometry)
  }

  const updateDisplay = (updatedSelectionType: any) => {
    if (selectionType.current !== updatedSelectionType) {
      selectionType.current = updatedSelectionType
      switch (selectionType.current) {
        case 'fullySelected':
          map.updateCluster(geometries.current, {
            color: cluster.results[0].getColor(),
            isSelected: true,
            count: cluster.results.length,
            outline: 'black',
            textFill: 'black',
          })
          break
        case 'partiallySelected':
          map.updateCluster(geometries.current, {
            color: cluster.results[0].getColor(),
            isSelected: false,
            count: cluster.results.length,
            outline: 'black',
            textFill: 'white',
          })
          break
        case 'unselected':
          map.updateCluster(geometries.current, {
            color: cluster.results[0].getColor(),
            isSelected: false,
            count: cluster.results.length,
            outline: 'white',
            textFill: 'white',
          })
          break
      }
    }
  }

  const updateSelected = () => {
    let selected = 0
    const selectedResults = selectionInterface.getSelectedResults()
    const results = cluster.results
    // if there are less selected results, loop over those instead of this model's results
    if (selectedResults.length < results.length) {
      selectedResults.some((selectedResult: any) => {
        if (results.find(result => (selectedResult.id = result.plain.id))) {
          selected++
        }
        return selected === results.length
      })
    } else {
      results.forEach(result => {
        if (selectedResults.get(result.plain.id)) {
          selected++
        }
      })
    }
    if (selected === results.length) {
      updateDisplay('fullySelected')
    } else if (selected > 0) {
      updateDisplay('partiallySelected')
    } else {
      updateDisplay('unselected')
    }
  }

  React.useEffect(() => {
    handleCluster()
    addConvexHull()
    // map.onMouseMove((_event: any, mapEvent: any) => {
    //   const id = mapEvent.mapTarget
    //   if (
    //     id &&
    //     cluster.results.map(result => result.plain.id).toString() ===
    //       id.toString()
    //   ) {
    //     map.showGeometry(geometries.current[1])
    //   } else {
    //     map.hideGeometry(geometries.current[1])
    //   }
    // })
    return () => {
      geometries.current.forEach(geometry => {
        map.removeGeometry(geometry)
      })
    }
  }, [])
  return <></>
}

export default hot(module)(Cluster)
