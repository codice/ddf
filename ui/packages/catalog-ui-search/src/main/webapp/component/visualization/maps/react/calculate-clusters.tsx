import * as React from 'react'
import { hot } from 'react-hot-loader'
import { Drawing } from '../../../singletons/drawing'
const _ = require('underscore')

const Clustering = require('../Clustering')
const metacardDefinitions = require('../../../singletons/metacard-definitions.js')
import { LazyQueryResult } from '../../../../js/model/LazyQueryResult'
import { useBackbone } from '../../../selection-checkbox/useBackbone.hook'
import { ClusterType } from './geometries'
import { LazyResultsType } from '../../../selection-interface/hooks'

type Props = {
  isClustering: boolean
  selectionInterface: any
  map: any
  setClusters: React.Dispatch<React.SetStateAction<ClusterType[]>>
  lazyResults: LazyResultsType
}

const CalculateClusters = ({
  selectionInterface,
  map,
  setClusters,
  isClustering,
  lazyResults,
}: Props) => {
  const clusteringAnimationFrameId = React.useRef(undefined as
    | number
    | undefined)
  const { listenTo } = useBackbone()

  const getResultsWithGeometry = () => {
    return Object.values(lazyResults).filter(lazyResult =>
      lazyResult.hasGeometry()
    )
  }

  // const onMapLeftClick = React.useMemo(() => {
  //   return (event: any, mapEvent: any) => {
  //     const getModelsForId = (id: any) => {
  //       return (clusters.find(
  //         cluster => cluster.id === id.sort().toString()
  //       ) as ClusterType).results
  //     }
  //     if (
  //       mapEvent.mapTarget &&
  //       mapEvent.mapTarget !== 'userDrawing' &&
  //       !Drawing.isDrawing()
  //     ) {
  //       if (event.shiftKey) {
  //         // handleShiftClick
  //         if (mapEvent.mapTarget.constructor === Array) {
  //           selectionInterface.addSelectedResult(
  //             getModelsForId(mapEvent.mapTarget)
  //           )
  //         }
  //       } else if (event.ctrlKey || event.metaKey) {
  //         // handle ctrl click
  //         if (mapEvent.mapTarget.constructor === Array) {
  //           selectionInterface.addSelectedResult(
  //             getModelsForId(mapEvent.mapTarget)
  //           )
  //         }
  //       } else {
  //         // handle click
  //         if (mapEvent.mapTarget.constructor === Array) {
  //           selectionInterface.clearSelectedResults()
  //           selectionInterface.addSelectedResult(
  //             getModelsForId(mapEvent.mapTarget)
  //           )
  //         }
  //       }
  //     }
  //   }
  // }, [])

  const calculateClusters = _.throttle(() => {
    if (isClustering) {
      // const now = Date.now() look into trying to boost perf here
      const calculatedClusters = Clustering.calculateClusters(
        getResultsWithGeometry(),
        map
      ) as LazyQueryResult[][]
      // console.log(`Time to cluster: ${Date.now() - now}`)
      setClusters(
        calculatedClusters.map(calculatedCluster => {
          return {
            results: calculatedCluster,
            id: calculatedCluster
              .map(result => result['metacard.id'])
              .sort()
              .toString(),
          }
        })
      )
    }
  }, 200)

  const handleResultsChange = () => {
    setClusters([])
    calculateClusters()
  }

  const startClusterAnimating = () => {
    if (isClustering) {
      clusteringAnimationFrameId.current = window.requestAnimationFrame(() => {
        calculateClusters()
        startClusterAnimating()
      })
    }
  }

  const stopClusterAnimating = () => {
    window.cancelAnimationFrame(clusteringAnimationFrameId.current as number)
    calculateClusters()
  }

  React.useEffect(() => {
    // map.onLeftClick(onMapLeftClick)
    // listenTo(
    //   selectionInterface.getActiveSearchResults(),
    //   'reset',
    //   (propertiesModel: any) => {
    //     if (
    //       _.find(
    //         Object.keys(propertiesModel.changedAttributes()),
    //         (attribute: any) =>
    //           metacardDefinitions.metacardTypes[attribute] &&
    //           metacardDefinitions.metacardTypes[attribute].type === 'GEOMETRY'
    //       ) !== undefined
    //     ) {
    //       handleResultsChange()
    //     }
    //   }
    // )
    // listenTo(
    //   selectionInterface.getActiveSearchResults(),
    //   'change:metacard>properties',
    //   handleResultsChange
    // )
  }, [])

  React.useEffect(
    () => {
      if (isClustering) {
        calculateClusters()
      } else {
        setClusters([])
      }
      map.onCameraMoveStart(startClusterAnimating)
      map.onCameraMoveEnd(stopClusterAnimating)
      return () => {
        map.offCameraMoveStart(startClusterAnimating)
        map.offCameraMoveEnd(stopClusterAnimating)
      }
    },
    [isClustering]
  )

  return <></>
}

export default hot(module)(CalculateClusters)
