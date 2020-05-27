import * as React from 'react'
import { hot } from 'react-hot-loader'
import { ClusterType } from './geometries'
import { useSelectionOfLazyResults } from '../../../../js/model/LazyQueryResult/hooks'
const _ = require('underscore')
const calculateConvexHull = require('geo-convex-hull')

type Props = {
  cluster: ClusterType
  map: any
}

const Cluster = ({ cluster, map }: Props) => {
  const geometries = React.useRef([] as any[])
  const isSelected = useSelectionOfLazyResults({ lazyResults: cluster.results })

  React.useEffect(
    () => {
      switch (isSelected) {
        case 'selected':
          map.updateCluster(geometries.current, {
            color: cluster.results[0].getColor(),
            isSelected,
            count: cluster.results.length,
            outline: 'black',
            textFill: 'black',
          })
          break
        case 'partially':
          map.updateCluster(geometries.current, {
            color: cluster.results[0].getColor(),
            isSelected,
            count: cluster.results.length,
            outline: 'black',
            textFill: 'white',
          })
          break
        case 'unselected':
          map.updateCluster(geometries.current, {
            color: cluster.results[0].getColor(),
            isSelected,
            count: cluster.results.length,
            outline: 'white',
            textFill: 'white',
          })
          break
      }
    },
    [isSelected]
  )

  const handleCluster = () => {
    const center = map.getCartographicCenterOfClusterInDegrees(cluster)
    geometries.current.push(
      map.addPointWithText(center, {
        id: cluster.results.map(result => result['metacard.id']),
        color: cluster.results[0].getColor(),
        isSelected,
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
      id: cluster.results.map(result => result['metacard.id']),
      color: cluster.results[0].getColor(),
    })
    map.hideGeometry(geometry)
    geometries.current.push(geometry)
  }

  React.useEffect(() => {
    handleCluster()
    addConvexHull()
    return () => {
      geometries.current.forEach(geometry => {
        map.removeGeometry(geometry)
      })
    }
  }, [])
  return <></>
}

export default hot(module)(Cluster)
