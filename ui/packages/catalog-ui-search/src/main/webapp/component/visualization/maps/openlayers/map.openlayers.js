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
/* global require, window */

import wrapNum from '../../../../react-component/utils/wrap-num/wrap-num.tsx'

const $ = require('jquery')
const _ = require('underscore')
const Map = require('../map')
const utility = require('./utility')
const DrawingUtility = require('../DrawingUtility')

const DrawBBox = require('../../../../js/widgets/openlayers.bbox.js')
const DrawCircle = require('../../../../js/widgets/openlayers.circle.js')
const DrawPolygon = require('../../../../js/widgets/openlayers.polygon.js')
const DrawLine = require('../../../../js/widgets/openlayers.line.js')

const properties = require('../../../../js/properties.js')
const Openlayers = require('openlayers')
const Geocoder = require('../../../../js/view/openlayers.geocoder.js')
const LayerCollectionController = require('../../../../js/controllers/ol.layerCollection.controller.js')
const user = require('../../../singletons/user-instance.js')
const User = require('../../../../js/model/User.js')
const wreqr = require('../../../../js/wreqr.js')

const defaultColor = '#3c6dd5'
const rulerColor = '#506f85'

const OpenLayerCollectionController = LayerCollectionController.extend({
  initialize() {
    // there is no automatic chaining of initialize.
    LayerCollectionController.prototype.initialize.apply(this, arguments)
  },
})

function createMap(insertionElement) {
  const layerPrefs = user.get('user>preferences>mapLayers')
  User.updateMapLayers(layerPrefs)
  const layerCollectionController = new OpenLayerCollectionController({
    collection: layerPrefs,
  })
  const map = layerCollectionController.makeMap({
    zoom: 3,
    minZoom: 1.9,
    element: insertionElement,
  })

  // TODO DDF-4200 Revisit map loading forever when this is removed
  if (properties.gazetteer) {
    const geocoder = new Geocoder.View({
      el: $(insertionElement).siblings('#mapTools'),
    })
    geocoder.render()
  }
  return map
}

function determineIdFromPosition(position, map) {
  const features = []
  map.forEachFeatureAtPixel(position, feature => {
    features.push(feature)
  })
  if (features.length > 0) {
    return features[0].getId()
  }
}

function convertPointCoordinate(point) {
  const coords = [point[0], point[1]]
  return Openlayers.proj.transform(coords, 'EPSG:4326', properties.projection)
}

function unconvertPointCoordinate(point) {
  return Openlayers.proj.transform(point, properties.projection, 'EPSG:4326')
}

function offMap([longitude, latitude]) {
  return latitude < -90 || latitude > 90
}

module.exports = function OpenlayersMap(
  insertionElement,
  selectionInterface,
  notificationEl,
  componentElement,
  mapModel
) {
  let overlays = {}
  let shapes = []
  const map = createMap(insertionElement)
  listenToResize()
  setupTooltip(map)
  const drawingTools = setupDrawingTools(map)
  const labelVectorLayer = setupLabelLayer(map)

  /*
   * Sets up a new Vector Layer for holding labels with the declutter property.
   * Declutter is used for making sure the label texts don't overlap on each
   * other.
   */
  function setupLabelLayer(map) {
    const vectorSource = new Openlayers.source.Vector({
      features: [],
    })
    const vectorLayer = new Openlayers.layer.Vector({
      source: vectorSource,
      zIndex: 1,
      declutter: true,
    })

    map.addLayer(vectorLayer)

    return vectorLayer
  }

  function setupTooltip(map) {
    map.on('pointermove', e => {
      const point = unconvertPointCoordinate(e.coordinate)
      if (!offMap(point)) {
        mapModel.updateMouseCoordinates({
          lat: point[1],
          lon: point[0],
        })
      } else {
        mapModel.clearMouseCoordinates()
      }
    })
  }

  function setupDrawingTools(map) {
    return {
      bbox: new DrawBBox.Controller({
        map,
        notificationEl,
      }),
      circle: new DrawCircle.Controller({
        map,
        notificationEl,
      }),
      polygon: new DrawPolygon.Controller({
        map,
        notificationEl,
      }),
      line: new DrawLine.Controller({
        map,
        notificationEl,
      }),
    }
  }

  function resizeMap() {
    map.updateSize()
  }

  function listenToResize() {
    wreqr.vent.on('resize', resizeMap)
  }

  function unlistenToResize() {
    wreqr.vent.off('resize', resizeMap)
  }

  const exposedMethods = _.extend({}, Map, {
    drawLine(model) {
      drawingTools.line.draw(model)
    },
    drawBbox(model) {
      drawingTools.bbox.draw(model)
    },
    drawCircle(model) {
      drawingTools.circle.draw(model)
    },
    drawPolygon(model) {
      drawingTools.polygon.draw(model)
    },
    destroyDrawingTools() {
      drawingTools.line.destroy()
      drawingTools.polygon.destroy()
      drawingTools.circle.destroy()
      drawingTools.bbox.destroy()
    },
    onLeftClick(callback) {
      $(map.getTargetElement()).on('click', e => {
        const boundingRect = map.getTargetElement().getBoundingClientRect()
        callback(e, {
          mapTarget: determineIdFromPosition(
            [e.clientX - boundingRect.left, e.clientY - boundingRect.top],
            map
          ),
        })
      })
    },
    onRightClick(callback) {
      $(map.getTargetElement()).on('contextmenu', e => {
        callback(e)
      })
    },
    onMouseMove(callback) {
      $(map.getTargetElement()).on('mousemove', e => {
        const boundingRect = map.getTargetElement().getBoundingClientRect()
        callback(e, {
          mapTarget: determineIdFromPosition(
            [e.clientX - boundingRect.left, e.clientY - boundingRect.top],
            map
          ),
        })
      })
    },
    onCameraMoveStart(callback) {
      map.on('movestart', callback)
    },
    onCameraMoveEnd(callback) {
      map.on('moveend', callback)
    },
    doPanZoom(coords) {
      const that = this
      that.zoomOut({ duration: 1000 }, () => {
        setTimeout(() => {
          that.zoomToExtent(coords, { duration: 2000 })
        }, 0)
      })
    },
    zoomOut(opts, next) {
      next()
    },
    zoomToSelected() {
      if (selectionInterface.getSelectedResults().length === 1) {
        this.panToResults(selectionInterface.getSelectedResults())
      }
    },
    panToResults(results) {
      const coordinates = _.flatten(
        results.map(result => result.getPoints()),
        true
      )
      this.panToExtent(coordinates)
    },
    panToExtent(coords) {
      if (coords.constructor === Array && coords.length > 0) {
        const lineObject = coords.map(coordinate =>
          convertPointCoordinate(coordinate)
        )

        const extent = Openlayers.extent.boundingExtent(lineObject)

        map.getView().fit(extent, {
          size: map.getSize(),
          maxZoom: map.getView().getZoom(),
          duration: 500,
        })
      }
    },
    zoomToExtent(coords, opts = {}) {
      const lineObject = coords.map(coordinate =>
        convertPointCoordinate(coordinate)
      )

      const extent = Openlayers.extent.boundingExtent(lineObject)

      map.getView().fit(extent, {
        size: map.getSize(),
        duration: 500,
        ...opts,
      })
    },
    zoomToBoundingBox({ north, east, south, west }) {
      this.zoomToExtent([[west, south], [east, north]])
    },
    limit(value, min, max) {
      return Math.min(Math.max(value, min), max)
    },
    getBoundingBox() {
      const extent = map.getView().calculateExtent(map.getSize())
      let longitudeEast = wrapNum(extent[2], -180, 180)
      const longitudeWest = wrapNum(extent[0], -180, 180)
      //add 360 degrees to longitudeEast to accommodate bounding boxes that span across the anti-meridian
      if (longitudeEast < longitudeWest) {
        longitudeEast += 360
      }
      return {
        north: this.limit(extent[3], -90, 90),
        east: longitudeEast,
        south: this.limit(extent[1], -90, 90),
        west: longitudeWest,
      }
    },
    overlayImage(model) {
      const metacardId = model.get('properties').get('id')
      this.removeOverlay(metacardId)

      const coords = model.getPoints('location')
      const array = _.map(coords, coord => convertPointCoordinate(coord))

      const polygon = new Openlayers.geom.Polygon([array])
      const extent = polygon.getExtent()
      const projection = Openlayers.proj.get(properties.projection)

      const overlayLayer = new Openlayers.layer.Image({
        source: new Openlayers.source.ImageStatic({
          url: model.get('currentOverlayUrl'),
          projection,
          imageExtent: extent,
        }),
      })

      map.addLayer(overlayLayer)
      overlays[metacardId] = overlayLayer
    },
    removeOverlay(metacardId) {
      if (overlays[metacardId]) {
        map.removeLayer(overlays[metacardId])
        delete overlays[metacardId]
      }
    },
    removeAllOverlays() {
      for (const overlay in overlays) {
        if (overlays.hasOwnProperty(overlay)) {
          map.removeLayer(overlays[overlay])
        }
      }
      overlays = {}
    },
    getCartographicCenterOfClusterInDegrees(cluster) {
      return utility.calculateCartographicCenterOfGeometriesInDegrees(
        cluster.get('results').map(result => result)
      )
    },
    getWindowLocationsOfResults(results) {
      return results.map(result => {
        const openlayersCenterOfGeometry = utility.calculateOpenlayersCenterOfGeometry(
          result
        )
        const center = map.getPixelFromCoordinate(openlayersCenterOfGeometry)
        if (center) {
          return center
        } else {
          return undefined
        }
      })
    },
    /*
     * Calculates the distance (in meters) between the two positions in the given array of
     * Coordinates.
     */
    calculateDistanceBetweenPositions(coords) {
      const line = new Openlayers.geom.LineString(coords)
      const sphereLength = Openlayers.Sphere.getLength(line)

      return sphereLength
    },
    /*
     * Draws a marker on the map designating a start/end point for the ruler measurement. The given
     * coordinates should be an object with 'lat' and 'lon' keys with degrees values. The given
     * marker label should be a single character or digit that is displayed on the map marker.
     */
    addRulerPoint(coordinates, markerLabel) {
      const { lat, lon } = coordinates
      const point = [lon, lat]
      const options = {
        id: markerLabel,
        color: rulerColor,
      }
      const useCustomText = true

      return this.addPoint(point, options)
    },
    /*
     * Removes the given point Layer from the map.
     */
    removeRulerPoint(pointLayer) {
      map.removeLayer(pointLayer)
    },
    /*
     * Draws a line on the map between the points in the given array of point Vectors.
     */
    addRulerLine(pointVectors) {
      // creates an array of lon/lat coordinates from the vectors array
      const coords = pointVectors.map(vector =>
        vector
          .getSource()
          .getFeatures()[0]
          .getGeometry()
          .getCoordinates()
      )
      // transforms each lon/lat coordinate to a Coordinate on the Web Mercator projection
      const transformedCoords = coords.map(coord =>
        Openlayers.proj.fromLonLat(coord)
      )

      const options = {
        id: 'ruler-line',
        title: 'Line for ruler measurement',
        color: '#506F85',
      }
      const line = this.addLine(coords, options)

      // calculates the distance between the two positions in the array of transformed Coordinates
      const distance = this.calculateDistanceBetweenPositions(transformedCoords)
      mapModel.setCurrentDistance(distance)

      return line
    },
    /*
     * Removes the given line Layer from the map.
     */
    removeRulerLine(line) {
      map.removeLayer(line)
    },
    /*
        Adds a billboard point utilizing the passed in point and options.
        Options are a view to relate to, and an id, and a color.
    */
    addPointWithText(point, options, useCustomText = false) {
      const pointObject = convertPointCoordinate(point)
      const feature = new Openlayers.Feature({
        geometry: new Openlayers.geom.Point(pointObject),
      })
      feature.setId(options.id)

      feature.setStyle(
        new Openlayers.style.Style({
          image: new Openlayers.style.Icon({
            img: DrawingUtility.getCircleWithText({
              fillColor: options.color,
              text: useCustomText ? options.id : options.id.length,
            }),
            imgSize: [44, 44],
          }),
        })
      )

      const vectorSource = new Openlayers.source.Vector({
        features: [feature],
      })

      const vectorLayer = new Openlayers.layer.Vector({
        source: vectorSource,
        zIndex: 1,
      })

      map.addLayer(vectorLayer)

      return vectorLayer
    },
    /*
          Adds a billboard point utilizing the passed in point and options.
          Options are a view to relate to, and an id, and a color.
        */
    addPoint(point, options) {
      const pointObject = convertPointCoordinate(point)
            const feature = new Openlayers.Feature({
              geometry: new Openlayers.geom.Point(pointObject),
            })
            feature.setId(options.id)

            feature.setStyle(
              new Openlayers.style.Style({
                image: new Openlayers.style.Icon({
                  img: DrawingUtility.getCircle({
                    fillColor: options.color,
                  }),
                  imgSize: [22, 22],
                }),
              })
            )

            const vectorSource = new Openlayers.source.Vector({
              features: [feature],
            })

            const vectorLayer = new Openlayers.layer.Vector({
              source: vectorSource,
              zIndex: 1,
            })

            map.addLayer(vectorLayer)

            return vectorLayer
    },
    /*
     * Draws a label on the map by adding to the features in the label Vector
     * Layer.
     */
    addLabel(point, options) {
      const pointObject = convertPointCoordinate(point)
      const feature = new Openlayers.Feature({
        geometry: new Openlayers.geom.Point(pointObject),
        name: options.text,
        isLabel: true,
      })
      feature.setId(options.id)

      feature.setStyle(
        new Openlayers.style.Style({
          text: new Openlayers.style.Text({
            text: options.text,
            overflow: true,
          }),
        })
      )

      const labelVectorLayerFeatures = labelVectorLayer
        .getSource()
        .getFeatures()
      // creates a new source with the new feature appended to the current list
      // of features
      const newVectorSource = new Openlayers.source.Vector({
        features: [...labelVectorLayerFeatures, feature],
      })
      // updates the vector layer containing the labels with the new source
      labelVectorLayer.setSource(newVectorSource)

      return labelVectorLayer
    },
    /*
          Adds a polyline utilizing the passed in line and options.
          Options are a view to relate to, and an id, and a color.
        */
    addLine(line, options) {
      const lineObject = line.map(coordinate =>
        convertPointCoordinate(coordinate)
      )

      const feature = new Openlayers.Feature({
        geometry: new Openlayers.geom.LineString(lineObject),
        name: options.title,
      })
      feature.setId(options.id)

      const styles = [
        new Openlayers.style.Style({
          stroke: new Openlayers.style.Stroke({
            color: 'white',
            width: 8,
          }),
        }),
        new Openlayers.style.Style({
          stroke: new Openlayers.style.Stroke({
            color: options.color || defaultColor,
            width: 4,
          }),
        }),
      ]

      feature.setStyle(styles)

      const vectorSource = new Openlayers.source.Vector({
        features: [feature],
      })

      const vectorLayer = new Openlayers.layer.Vector({
        source: vectorSource,
      })

      map.addLayer(vectorLayer)

      return vectorLayer
    },
    /*
          Adds a polygon fill utilizing the passed in polygon and options.
          Options are a view to relate to, and an id.
        */
    addPolygon(polygon, options) {},
    /*
         Updates a passed in geometry to reflect whether or not it is selected.
         Options passed in are color and isSelected.
         */
    updateCluster(geometry, options) {
      if (geometry.constructor === Array) {
        geometry.forEach(innerGeometry => {
          this.updateCluster(innerGeometry, options)
        })
      } else {
        const feature = geometry.getSource().getFeatures()[0]
        const geometryInstance = feature.getGeometry()
        if (geometryInstance.constructor === Openlayers.geom.Point) {
          geometry.setZIndex(options.isSelected ? 2 : 1)
          feature.setStyle(
            new Openlayers.style.Style({
              image: new Openlayers.style.Icon({
                img: DrawingUtility.getCircleWithText({
                  fillColor: options.color,
                  strokeColor: options.outline,
                  text: options.count,
                  textColor: options.textFill,
                }),
                imgSize: [44, 44],
              }),
            })
          )
        } else if (
          geometryInstance.constructor === Openlayers.geom.LineString
        ) {
          const styles = [
            new Openlayers.style.Style({
              stroke: new Openlayers.style.Stroke({
                color: 'rgba(255,255,255, .1)',
                width: 8,
              }),
            }),
            new Openlayers.style.Style({
              stroke: new Openlayers.style.Stroke({
                color: 'rgba(0,0,0, .1)',
                width: 4,
              }),
            }),
          ]
          feature.setStyle(styles)
        }
      }
    },
    /*
          Updates a passed in geometry to reflect whether or not it is selected.
          Options passed in are color and isSelected.
        */
    updateGeometry(geometry, options) {
      if (geometry.constructor === Array) {
        geometry.forEach(innerGeometry => {
          this.updateGeometry(innerGeometry, options)
        })
      } else {
        const features = geometry.getSource().getFeatures()
        features.forEach(feature =>
          this.setGeometryStyle(geometry, options, feature)
        )
      }
    },
    setGeometryStyle(geometry, options, feature) {
      const geometryInstance = feature.getGeometry()
      if (geometryInstance.getType() === 'Point') {
        let pointWidth = 39
        let pointHeight = 40
        if (options.size) {
          pointWidth = options.size.x
          pointHeight = options.size.y
        }
        geometry.setZIndex(options.isSelected ? 2 : 1)
        if (!feature.getProperties().isLabel) {
          feature.setStyle(
            new Openlayers.style.Style({
              image: new Openlayers.style.Icon({
                img: DrawingUtility.getPin({
                  fillColor: options.color,
                  strokeColor: options.isSelected ? 'black' : 'white',
                  icon: options.icon,
                }),
                imgSize: [pointWidth, pointHeight],
                anchor: [pointWidth / 2, 0],
                anchorOrigin: 'bottom-left',
                anchorXUnits: 'pixels',
                anchorYUnits: 'pixels',
              }),
            })
          )
        } else {
          feature.setStyle(
            new Openlayers.style.Style({
              text: this.createTextStyle(
                feature,
                map.getView().getResolution()
              ),
            })
          )
        }
      } else if (geometryInstance.getType() === 'LineString') {
        const styles = [
          new Openlayers.style.Style({
            stroke: new Openlayers.style.Stroke({
              color: options.isSelected ? 'black' : 'white',
              width: 8,
            }),
          }),
          new Openlayers.style.Style({
            stroke: new Openlayers.style.Stroke({
              color: options.color || defaultColor,
              width: 4,
            }),
          }),
        ]
        feature.setStyle(styles)
      }
    },
    createTextStyle(feature, resolution) {
      const fillColor = '#000000'
      const outlineColor = '#ffffff'
      const outlineWidth = 3

      return new Openlayers.style.Text({
        text: this.getText(feature, resolution),
        fill: new Openlayers.style.Fill({ color: fillColor }),
        stroke: new Openlayers.style.Stroke({
          color: outlineColor,
          width: outlineWidth,
        }),
        offsetX: 20,
        offsetY: -15,
        placement: 'point',
        maxAngle: 45,
        overflow: true,
        rotation: 0,
        textAlign: 'left',
        padding: [5, 5, 5, 5],
      })
    },
    getText(feature, resolution) {
      const maxResolution = 1200
      const text =
        resolution > maxResolution ? '' : this.trunc(feature.get('name'), 20)

      return text
    },
    trunc(str, n) {
      return str.length > n ? str.substr(0, n - 1) + '...' : str.substr(0)
    },
    /*
         Updates a passed in geometry to be hidden
         */
    hideGeometry(geometry) {
      geometry.setVisible(false)
    },
    /*
         Updates a passed in geometry to be shown
         */
    showGeometry(geometry) {
      geometry.setVisible(true)
    },
    removeGeometry(geometry) {
      map.removeLayer(geometry)
    },
    showPolygonShape(locationModel) {
      const polygon = new DrawPolygon.PolygonView({
        model: locationModel,
        map,
      })
      shapes.push(polygon)
    },
    showCircleShape(locationModel) {
      const circle = new DrawCircle.CircleView({
        model: locationModel,
        map,
      })
      shapes.push(circle)
    },
    showLineShape(locationModel) {
      const line = new DrawLine.LineView({
        model: locationModel,
        map,
      })
      shapes.push(line)
    },
    showMultiLineShape(locationModel) {
      let lineObject = locationModel
        .get('multiline')
        .map(line => line.map(coords => convertPointCoordinate(coords)))

      let feature = new Openlayers.Feature({
        geometry: new Openlayers.geom.MultiLineString(lineObject),
      })

      feature.setId(locationModel.cid)

      const styles = [
        new Openlayers.style.Style({
          stroke: new Openlayers.style.Stroke({
            color: locationModel.get('color') || defaultColor,
            width: 4,
          }),
        }),
      ]

      feature.setStyle(styles)

      return this.createVectorLayer(locationModel, feature)
    },
    createVectorLayer(locationModel, feature) {
      let vectorSource = new Openlayers.source.Vector({
        features: [feature],
      })

      let vectorLayer = new Openlayers.layer.Vector({
        source: vectorSource,
      })

      map.addLayer(vectorLayer)
      overlays[locationModel.cid] = vectorLayer

      return vectorLayer
    },
    destroyShape(cid) {
      const shapeIndex = shapes.findIndex(shape => cid === shape.model.cid)
      if (shapeIndex >= 0) {
        shapes[shapeIndex].destroy()
        shapes.splice(shapeIndex, 1)
      }
    },
    destroyShapes() {
      shapes.forEach(shape => {
        shape.destroy()
      })
      shapes = []
    },
    getOpenLayersMap() {
      return map
    },
    destroy() {
      this.destroyDrawingTools()
      unlistenToResize()
    },
  })

  return exposedMethods
}
