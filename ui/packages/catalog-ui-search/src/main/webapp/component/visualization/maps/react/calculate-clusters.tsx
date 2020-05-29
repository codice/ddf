import * as React from 'react'
import { hot } from 'react-hot-loader'
const _ = require('underscore')

const Clustering = require('../Clustering')
import { ClusterType } from './geometries'
import { LazyResultsType } from '../../../selection-interface/hooks'
import { LazyQueryResult } from '../../../../js/model/LazyQueryResult/LazyQueryResult'

type Props = {
  isClustering: boolean
  map: any
  setClusters: React.Dispatch<React.SetStateAction<ClusterType[]>>
  lazyResults: LazyResultsType
}

const CalculateClusters = ({
  map,
  setClusters,
  isClustering,
  lazyResults,
}: Props) => {
  const clusteringAnimationFrameId = React.useRef(undefined as
    | number
    | undefined)

  const getResultsWithGeometry = () => {
    return Object.values(lazyResults).filter(lazyResult =>
      lazyResult.hasGeometry()
    )
  }

  const calculateClusters = _.debounce(() => {
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
  }, 500)

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

  React.useEffect(
    () => {
      handleResultsChange()
    },
    [lazyResults]
  )

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
    [isClustering, lazyResults]
  )

  return <></>
}

export default hot(module)(CalculateClusters)
