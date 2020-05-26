import * as React from 'react'
import { hot } from 'react-hot-loader'
import { LazyQueryResult } from '../../../../js/model/LazyQueryResult/LazyQueryResult'
import { ClusterType } from './geometries'
const _ = require('underscore')
const _debounce = require('lodash/debounce')
const wkx = require('wkx')
const metacardDefinitions = require('../../../singletons/metacard-definitions.js')
const iconHelper = require('../../../../js/IconHelper.js')
import { useUpdateEffect } from 'react-use'
import { useSelectionOfLazyResult } from '../../../../js/model/LazyQueryResult/hooks'
type Props = {
  lazyResult: LazyQueryResult
  map: any
  clusters: ClusterType[]
}

const determineIfClustered = ({
  clusters,
  result,
}: {
  clusters: ClusterType[]
  result: LazyQueryResult
}) => {
  return Boolean(
    clusters.find(cluster =>
      Boolean(
        cluster.results.find(
          clusteredResult =>
            clusteredResult['metacard.id'] === result['metacard.id']
        )
      )
    )
  )
}

const Geometry = ({ lazyResult, map, clusters }: Props) => {
  const isClustered = React.useRef(false)
  const geometries = React.useRef([] as any[])
  const isSelected = useSelectionOfLazyResult({ lazyResult })

  useUpdateEffect(
    () => {
      updateDisplay(isSelected)
    },
    [isSelected, lazyResult.plain]
  )

  React.useEffect(
    () => {
      checkIfClustered()
    },
    [clusters]
  )
  React.useEffect(
    () => {
      updateGeometries()

      return () => {
        // cleanup
        destroyGeometries()
      }
    },
    [lazyResult.plain]
  )

  const handlePoint = React.useMemo(
    () => {
      return (point: any) => {
        geometries.current.push(
          map.addPoint(point, {
            id: lazyResult['metacard.id'],
            title: lazyResult.plain.metacard.properties.title,
            color,
            icon,
            isSelected,
          })
        )
      }
    },
    [lazyResult.plain]
  )

  const handleLine = React.useMemo(
    () => {
      return (line: any) => {
        geometries.current.push(
          map.addLine(line, {
            id: lazyResult['metacard.id'],
            title: lazyResult.plain.metacard.properties.title,
            color,
            isSelected,
          })
        )
      }
    },
    [lazyResult.plain]
  )

  const handleGeometry = React.useMemo(() => {
    return (geometry: any) => {
      switch (geometry.type) {
        case 'Point':
          handlePoint(geometry.coordinates)
          break
        case 'Polygon':
          geometry.coordinates.forEach((polygon: any) => {
            handlePoint(polygon[0])
            handleLine(polygon)
          })
          break
        case 'LineString':
          handlePoint(geometry.coordinates[0])
          handleLine(geometry.coordinates)
          break
        case 'MultiLineString':
          geometry.coordinates.forEach((line: any) => {
            handlePoint(line[0])
            handleLine(line)
          })
          break
        case 'MultiPoint':
          geometry.coordinates.forEach((point: any) => {
            handlePoint(point)
          })
          break
        case 'MultiPolygon':
          geometry.coordinates.forEach((multipolygon: any) => {
            multipolygon.forEach((polygon: any) => {
              handlePoint(polygon[0])
              handleLine(polygon)
            })
          })
          break
        case 'GeometryCollection':
          geometry.geometries.forEach((subgeometry: any) => {
            handleGeometry(subgeometry)
          })
          break
      }
    }
  }, [])

  const checkIfClustered = React.useMemo(
    () => {
      return () => {
        const updateIsClustered = determineIfClustered({
          clusters,
          result: lazyResult,
        })
        if (isClustered.current !== updateIsClustered) {
          isClustered.current = updateIsClustered
          if (isClustered.current) {
            hideGeometries()
          } else {
            showGeometries()
          }
        }
      }
    },
    [clusters]
  )

  const color = React.useMemo(() => {
    return lazyResult.getColor()
  }, [])

  const icon = React.useMemo(
    () => {
      return iconHelper.getFullByMetacardObject(lazyResult.plain)
    },
    [lazyResult.plain]
  )

  const updateDisplay = React.useMemo(() => {
    return _debounce(
      (updateIsSelected: boolean) => {
        geometries.current.forEach(geometry => {
          map.updateGeometry(geometry, {
            color,
            icon,
            isSelected: updateIsSelected,
          })
        })
      },
      100,
      {
        leading: false,
        trailing: true,
      }
    )
  }, []) as (updateIsSelected: boolean) => void
  const updateGeometries = React.useMemo(() => {
    return (propertiesModel?: any) => {
      if (
        propertiesModel &&
        _.find(
          Object.keys(propertiesModel.changedAttributes()),
          (attribute: any) =>
            (metacardDefinitions.metacardTypes[attribute] &&
              metacardDefinitions.metacardTypes[attribute].type ===
                'GEOMETRY') ||
            attribute === 'id'
        ) === undefined
      ) {
        return
      }
      destroyGeometries()
      isClustered.current = false
      const lazyResultGeometries = _.flatten(lazyResult.getGeometries())
      if (lazyResultGeometries.length > 0) {
        geometries.current = []
        _.forEach(lazyResultGeometries, (property: any) => {
          handleGeometry(wkx.Geometry.parse(property).toGeoJSON())
        })
        checkIfClustered()
      }
    }
  }, [])

  const destroyGeometries = React.useMemo(() => {
    return () => {
      geometries.current.forEach(geometry => {
        map.removeGeometry(geometry)
      })
    }
  }, [])
  const showGeometries = React.useMemo(() => {
    return () => {
      geometries.current.forEach(geometry => {
        map.showGeometry(geometry)
      })
    }
  }, [])
  const hideGeometries = React.useMemo(() => {
    return () => {
      geometries.current.forEach(geometry => {
        map.hideGeometry(geometry)
      })
    }
  }, [])

  return null
}

export default hot(module)(Geometry)
