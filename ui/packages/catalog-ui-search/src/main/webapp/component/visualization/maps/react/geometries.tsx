import * as React from 'react'
import { hot } from 'react-hot-loader'
import { Drawing } from '../../../singletons/drawing'
import {
  useLazyResults,
  useLazySelectionInterface,
} from '../../../selection-interface/hooks'
import Geometry from './geometry'
import CalculateClusters from './calculate-clusters'
import Cluster from './cluster'
import { LazyQueryResult } from '../../../../js/model/LazyQueryResult'

type Props = {
  selectionInterface: any
  map: any
  isClustering: boolean
}

export type ClusterType = {
  results: LazyQueryResult[]
  id: string
}

const Geometries = (props: Props) => {
  const { map, selectionInterface, isClustering } = props
  const lazySelectionInterface = useLazySelectionInterface({
    key: 'search',
  })
  const lazySelectionInterfaceRef = React.useRef(lazySelectionInterface)
  lazySelectionInterfaceRef.current = lazySelectionInterface
  const lazyResults = useLazyResults({ selectionInterface })
  const lazyResultsRef = React.useRef(lazyResults)
  lazyResultsRef.current = lazyResults
  const [clusters, setClusters] = React.useState([] as ClusterType[])
  React.useEffect(() => {
    const handleShiftClick = (id: string) => {
      if (id.constructor === String) {
        //   selectionInterface.addSelectedResult(this.collection.get(id))
      }
    }
    const handleCtrlClick = (id: string) => {
      if (id.constructor === String) {
        // selectionInterface.addSelectedResult(
        //   this.collection.get(id)
        // )
      }
    }
    const handleClick = (id: string) => {
      if (id.constructor === String) {
        lazySelectionInterfaceRef.current.set([lazyResultsRef.current[id]])
      }
    }
    const handleLeftClick = (event: any, mapEvent: any) => {
      if (
        mapEvent.mapTarget &&
        mapEvent.mapTarget !== 'userDrawing' &&
        !Drawing.isDrawing()
      ) {
        if (event.shiftKey) {
          handleShiftClick(mapEvent.mapTarget)
        } else if (event.ctrlKey || event.metaKey) {
          handleCtrlClick(mapEvent.mapTarget)
        } else {
          handleClick(mapEvent.mapTarget)
        }
      }
    }
    map.onLeftClick(handleLeftClick)
    return () => {
      console.log('cleanup')
    }
  }, [])

  const IndividualGeometries = React.useMemo(
    () => {
      return Object.values(lazyResults).map(lazyResult => {
        return (
          <Geometry
            key={lazyResult['metacard.id']}
            lazyResult={lazyResult}
            map={map}
            clusters={clusters}
          />
        )
      })
    },
    [lazyResults, clusters]
  )

  const Clusters = React.useMemo(
    () => {
      return clusters.map(cluster => {
        return (
          <Cluster
            key={cluster.id}
            cluster={cluster}
            selectionInterface={selectionInterface}
            map={map}
          />
        )
      })
    },
    [clusters]
  )

  const CalculateClustersMemo = React.useMemo(
    () => {
      return (
        <CalculateClusters
          key="clusters"
          isClustering={isClustering}
          selectionInterface={selectionInterface}
          map={map}
          lazyResults={lazyResults}
          setClusters={setClusters}
        />
      )
    },
    [lazyResults, isClustering]
  )

  return (
    <>
      {CalculateClustersMemo}
      {Clusters}
      {IndividualGeometries}
    </>
  )
}

export default hot(module)(Geometries)
