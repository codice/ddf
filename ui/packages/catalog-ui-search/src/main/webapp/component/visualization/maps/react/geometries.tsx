import * as React from 'react'
import { hot } from 'react-hot-loader'
import { Drawing } from '../../../singletons/drawing'
import { useLazyResults } from '../../../selection-interface/hooks'
import Geometry from './geometry'
import CalculateClusters from './calculate-clusters'
import Cluster from './cluster'
import { LazyQueryResult } from '../../../../js/model/LazyQueryResult/LazyQueryResult'
import ZoomToSelection from './zoom-to-selection'
type Props = {
  selectionInterface: any
  map: any
  isClustering: boolean
  mapView: any
}

export type ClusterType = {
  results: LazyQueryResult[]
  id: string
}

const Geometries = (props: Props) => {
  console.log('rendering geometries')
  const { map, selectionInterface, isClustering, mapView } = props
  const lazyResults = useLazyResults({ selectionInterface })
  const lazyResultsRef = React.useRef(lazyResults)
  lazyResultsRef.current = lazyResults
  const [clusters, setClusters] = React.useState([] as ClusterType[])
  React.useEffect(() => {
    const handleCtrlClick = (id: string | string[]) => {
      if (id.constructor === String) {
        lazyResultsRef.current.results[id as string].controlSelect()
      } else {
        ;(id as string[]).map(subid => {
          return lazyResultsRef.current.results[subid as string].controlSelect()
        })
      }
    }
    const handleClick = (id: string | string[]) => {
      if (id.constructor === String) {
        lazyResultsRef.current.results[id as string].select()
      } else {
        const resultIds = id as string[]
        let shouldJustDeselect = resultIds.some(
          subid => lazyResultsRef.current.results[subid].isSelected
        )
        lazyResultsRef.current.deselect()
        if (!shouldJustDeselect) {
          resultIds.map(subid => {
            return lazyResultsRef.current.results[
              subid as string
            ].controlSelect()
          })
        }
      }
    }
    const handleLeftClick = (event: any, mapEvent: any) => {
      if (
        mapEvent.mapTarget &&
        mapEvent.mapTarget !== 'userDrawing' &&
        !Drawing.isDrawing()
      ) {
        // we get click events on normal drawn features from the location drawing
        if (
          mapEvent.mapTarget.constructor === String &&
          mapEvent.mapTarget.length < 8
        ) {
          return
        }
        if (event.shiftKey) {
          handleCtrlClick(mapEvent.mapTarget)
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
      return Object.values(lazyResults.filteredResults).map(lazyResult => {
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
    [lazyResults.filteredResults, clusters]
  )

  const Clusters = React.useMemo(
    () => {
      return clusters.map(cluster => {
        return <Cluster key={cluster.id} cluster={cluster} map={map} />
      })
    },
    [clusters, lazyResults.filteredResults]
  )

  const CalculateClustersMemo = React.useMemo(
    () => {
      return (
        <CalculateClusters
          key="clusters"
          isClustering={isClustering}
          map={map}
          lazyResults={lazyResults.filteredResults}
          setClusters={setClusters}
        />
      )
    },
    [lazyResults.filteredResults, isClustering]
  )

  const ZoomToSelectionMemo = React.useMemo(
    () => {
      return (
        <ZoomToSelection
          map={map}
          lazyResults={lazyResults}
          mapView={mapView}
        />
      )
    },
    [lazyResults]
  )

  return (
    <>
      {ZoomToSelectionMemo}
      {CalculateClustersMemo}
      {Clusters}
      {IndividualGeometries}
    </>
  )
}

export default hot(module)(Geometries)
