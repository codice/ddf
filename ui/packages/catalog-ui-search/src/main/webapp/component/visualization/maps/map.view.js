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

import wrapNum from '../../../react-component/utils/wrap-num/wrap-num.tsx'
import * as React from 'react'
import ZoomToHomeButton from '../../../react-component/button/split-button/zoomToHome.tsx'

const wreqr = require('../../../js/wreqr.js')
const template = require('./map.hbs')
const Marionette = require('marionette')
const CustomElements = require('../../../js/CustomElements.js')
const LoadingCompanionView = require('../../loading-companion/loading-companion.view.js')
const store = require('../../../js/store.js')
const GeometryCollectionView = require('./geometry.collection.view')
const ClusterCollectionView = require('./cluster.collection.view')
const ClusterCollection = require('./cluster.collection')
const CQLUtils = require('../../../js/CQLUtils.js')
const LocationModel = require('../../location-old/location-old.js')
const user = require('../../singletons/user-instance.js')
const LayersDropdown = require('../../dropdown/layers/dropdown.layers.view.js')
const DropdownModel = require('../../dropdown/dropdown.js')
const MapContextMenuDropdown = require('../../dropdown/map-context-menu/dropdown.map-context-menu.view.js')
const MapModel = require('./map.model')
const properties = require('../../../js/properties.js')
const Common = require('../../../js/Common.js')
const announcement = require('../../announcement')

const Gazetteer = require('../../../react-component/location/gazetteer.js')

import MapSettings from '../../../react-component/container/map-settings/map-settings'
import MapInfo from '../../../react-component/container/map-info/map-info'

function findExtreme({ objArray, property, comparator }) {
  if (objArray.length === 0) {
    return undefined
  }
  return objArray.reduce(
    (extreme, coordinateObj) =>
      (extreme = comparator(extreme, coordinateObj[property])),
    objArray[0][property]
  )
}

function getHomeCoordinates() {
  if (properties.mapHome !== '') {
    const separateCoordinates = properties.mapHome.replace(/\s/g, '').split(',')
    if (separateCoordinates.length % 2 === 0) {
      return separateCoordinates
        .reduce((coordinates, coordinate, index) => {
          if (index % 2 === 0) {
            coordinates.push({
              lon: coordinate,
              lat: separateCoordinates[index + 1],
            })
          }
          return coordinates
        }, [])
        .map(coordinateObj => {
          let lon = parseFloat(coordinateObj.lon)
          let lat = parseFloat(coordinateObj.lat)
          if (isNaN(lon) || isNaN(lat)) {
            return undefined
          }
          lon = wrapNum(lon, -180, 180)
          lat = wrapNum(lat, -90, 90)
          return {
            lon,
            lat,
          }
        })
        .filter(coordinateObj => {
          return coordinateObj !== undefined
        })
    }
  } else {
    return []
  }
}

function getBoundingBox(coordinates) {
  const north = findExtreme({
    objArray: coordinates,
    property: 'lat',
    comparator: Math.max,
  })
  const south = findExtreme({
    objArray: coordinates,
    property: 'lat',
    comparator: Math.min,
  })
  const east = findExtreme({
    objArray: coordinates,
    property: 'lon',
    comparator: Math.max,
  })
  const west = findExtreme({
    objArray: coordinates,
    property: 'lon',
    comparator: Math.min,
  })
  if (
    north === undefined ||
    south === undefined ||
    east === undefined ||
    west === undefined
  ) {
    return undefined
  }
  return {
    north,
    east,
    south,
    west,
  }
}

const homeBoundingBox = getBoundingBox(getHomeCoordinates())
const defaultHomeBoundingBox = {
  west: -128,
  south: 24,
  east: -63,
  north: 52,
}

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('map'),
  template,
  regions: {
    mapDrawingPopup: '#mapDrawingPopup',
    mapContextMenu: '.map-context-menu',
    mapInfo: '.mapInfo',
  },
  events: {
    'click .cluster-button': 'toggleClustering',
  },
  clusterCollection: undefined,
  clusterCollectionView: undefined,
  geometryCollectionView: undefined,
  map: undefined,
  mapModel: undefined,
  hasLoadedMap: false,
  initialize(options) {
    if (!options.selectionInterface) {
      throw 'Selection interface has not been provided'
    }
    this.onMapLoaded = options.onMapLoaded || (() => {})
    this.mapModel = new MapModel()
    this.listenTo(store.get('content'), 'change:drawing', this.handleDrawing)
    this.handleDrawing()
    this.setupMouseLeave()
    this.listenTo(store.get('workspaces'), 'add', this.zoomToHome)
  },
  setupMouseLeave() {
    this.$el.on('mouseleave', () => {
      this.mapModel.clearMouseCoordinates()
    })
  },
  setupCollections() {
    if (!this.map) {
      throw 'Map has not been set.'
    }
    this.clusterCollection = new ClusterCollection()
    this.geometryCollectionView = new GeometryCollectionView({
      collection: this.options.selectionInterface.getActiveSearchResults(),
      map: this.map,
      selectionInterface: this.options.selectionInterface,
      clusterCollection: this.clusterCollection,
    })
    this.clusterCollectionView = new ClusterCollectionView({
      collection: this.clusterCollection,
      map: this.map,
      selectionInterface: this.options.selectionInterface,
    })
  },
  setupListeners() {
    this.listenTo(
      wreqr.vent,
      'metacard:overlay',
      this.map.overlayImage.bind(this.map)
    )
    this.listenTo(
      wreqr.vent,
      'metacard:overlay:remove',
      this.map.removeOverlay.bind(this.map)
    )
    this.listenTo(
      wreqr.vent,
      'search:maprectanglefly',
      this.map.zoomToExtent.bind(this.map)
    )
    this.listenTo(
      this.options.selectionInterface,
      'reset:activeSearchResults',
      this.map.removeAllOverlays.bind(this.map)
    )

    this.listenTo(
      this.options.selectionInterface.getSelectedResults(),
      'update',
      this.map.zoomToSelected.bind(this.map)
    )
    this.listenTo(
      this.options.selectionInterface.getSelectedResults(),
      'add',
      this.map.zoomToSelected.bind(this.map)
    )
    this.listenTo(
      this.options.selectionInterface.getSelectedResults(),
      'remove',
      this.map.zoomToSelected.bind(this.map)
    )

    this.listenTo(
      user.get('user').get('preferences'),
      'change:resultFilter',
      this.handleCurrentQuery
    )
    this.listenTo(
      this.options.selectionInterface,
      'change:currentQuery',
      this.handleCurrentQuery
    )
    this.handleCurrentQuery()

    if (this.options.selectionInterface.getSelectedResults().length > 0) {
      this.map.zoomToSelected(
        this.options.selectionInterface.getSelectedResults()
      )
    } else {
      Common.queueExecution(this.zoomToHome.bind(this))
    }
    this.map.onMouseMove(this.onMapHover.bind(this))
    this.map.onRightClick(this.onRightClick.bind(this))
    this.setupRightClickMenu()
    this.setupMapInfo()
  },
  zoomToHome() {
    const home = [
      user
        .get('user')
        .get('preferences')
        .get('mapHome'),
      homeBoundingBox,
      defaultHomeBoundingBox,
    ].find(element => element !== undefined)

    this.map.zoomToBoundingBox(home)
  },
  saveAsHome() {
    const boundingBox = this.map.getBoundingBox()
    const userPreferences = user.get('user').get('preferences')
    userPreferences.set('mapHome', boundingBox)
    announcement.announce({
      title: 'Success!',
      message: 'New map home location set.',
      type: 'success',
    })
  },
  addPanZoom() {
    const PanZoomView = Marionette.ItemView.extend({
      template: () => (
        <Gazetteer setState={({ polygon }) => this.map.doPanZoom(polygon)} />
      ),
    })
    this.$el
      .find('.cesium-viewer-toolbar')
      .append('<div class="toolbar-panzoom is-button"></div>')
    this.addRegion('toolbarPanZoom', '.toolbar-panzoom')
    this.toolbarPanZoom.show(new PanZoomView())
  },
  addHome() {
    const containerClass = 'zoomToHome-container'
    const ZoomToHomeButtonView = Marionette.ItemView.extend({
      template: () => (
        <ZoomToHomeButton
          goHome={() => this.zoomToHome()}
          saveHome={() => this.saveAsHome()}
        />
      ),
    })
    this.$el
      .find('.cesium-viewer-toolbar')
      .append(`<div class="${containerClass}"></div>`)
    this.addRegion('zoomToHomeButtonView', `.${containerClass}`)
    this.zoomToHomeButtonView.show(new ZoomToHomeButtonView())
  },
  addClustering() {
    this.$el
      .find('.cesium-viewer-toolbar')
      .append(
        '<div class="is-button cluster cluster-button">' +
          '<span> Cluster </span>' +
          '<span class="fa fa-toggle-on is-clustering"/>' +
          '<span class="fa fa-cubes"/>' +
          '</div>'
      )
  },
  addSettings() {
    const MapSettingsView = Marionette.ItemView.extend({
      template() {
        return <MapSettings />
      },
    })
    this.$el
      .find('.cesium-viewer-toolbar')
      .append('<div class="toolbar-settings is-button"></div>')
    this.addRegion('toolbarSettings', '.toolbar-settings')
    this.toolbarSettings.show(new MapSettingsView())
  },
  onMapHover(event, mapEvent) {
    const metacard = this.options.selectionInterface
      .getActiveSearchResults()
      .get(mapEvent.mapTarget)
    this.updateTarget(metacard)
    this.$el.toggleClass(
      'is-hovering',
      Boolean(mapEvent.mapTarget && mapEvent.mapTarget !== 'userDrawing')
    )
  },
  updateTarget(metacard) {
    let target
    let targetMetacard
    if (metacard) {
      target = metacard
        .get('metacard')
        .get('properties')
        .get('title')
      targetMetacard = metacard
    }
    this.mapModel.set({
      target,
      targetMetacard,
    })
  },
  onRightClick(event, mapEvent) {
    event.preventDefault()
    this.$el
      .find('.map-context-menu')
      .css('left', event.offsetX)
      .css('top', event.offsetY)
    this.mapModel.updateClickCoordinates()
    this.mapContextMenu.currentView.model.open()
  },
  setupRightClickMenu() {
    this.mapContextMenu.show(
      new MapContextMenuDropdown({
        model: new DropdownModel(),
        mapModel: this.mapModel,
        selectionInterface: this.options.selectionInterface,
        dropdownCompanionBehaviors: {
          navigation: {},
        },
      })
    )
  },
  setupMapInfo() {
    const map = this.mapModel
    const MapInfoView = Marionette.ItemView.extend({
      template() {
        return <MapInfo map={map} />
      },
    })

    this.mapInfo.show(new MapInfoView())
  },
  /*
        Map creation is deferred to this method, so that all resources pertaining to the map can be loaded lazily and
        not be included in the initial page payload.
        Because of this, make sure to return a deferred that will resolve when your respective map implementation
        is finished loading / starting up.
        Also, make sure you resolve that deferred by passing the reference to the map implementation.
    */
  loadMap() {
    throw 'Map not implemented'
  },
  createMap(Map) {
    this.map = Map(
      this.el.querySelector('#mapContainer'),
      this.options.selectionInterface,
      this.mapDrawingPopup.el,
      this.el,
      this.mapModel
    )
    this.setupCollections()
    this.setupListeners()
    this.addPanZoom()
    this.addHome()
    this.addClustering()
    this.addLayers()
    this.addSettings()
    this.endLoading()
    this.zoomToHome()
  },
  addLayers() {
    this.$el
      .find('.cesium-viewer-toolbar')
      .append('<div class="toolbar-layers is-button"></div>')
    this.addRegion('toolbarLayers', '.toolbar-layers')
    this.toolbarLayers.show(
      new LayersDropdown({
        model: new DropdownModel(),
      })
    )
  },
  initializeMap() {
    this.loadMap().then(Map => {
      this.createMap(Map)
      this.hasLoadedMap = true
      this.onMapLoaded(this.map.getOpenLayersMap())
    })
  },
  startLoading() {
    LoadingCompanionView.beginLoading(this)
  },
  endLoading() {
    LoadingCompanionView.endLoading(this)
  },
  onRender() {
    this.startLoading()
    setTimeout(() => {
      this.initializeMap()
    }, 1000)
  },
  toggleClustering() {
    this.$el.toggleClass('is-clustering')
    this.clusterCollectionView.toggleActive()
  },
  handleDrawing() {
    this.$el.toggleClass('is-drawing', store.get('content').get('drawing'))
  },
  handleCurrentQuery() {
    this.removePreviousLocations()
    const currentQuery = this.options.selectionInterface.get('currentQuery')
    if (currentQuery) {
      this.handleFilter(
        CQLUtils.transformCQLToFilter(currentQuery.get('cql')),
        currentQuery.get('color')
      )
    }
    const resultFilter = user
      .get('user')
      .get('preferences')
      .get('resultFilter')
    if (resultFilter) {
      this.handleFilter(CQLUtils.transformCQLToFilter(resultFilter), '#c89600')
    }
  },
  handleFilter(filter, color) {
    if (filter.filters) {
      filter.filters.forEach(subfilter => {
        this.handleFilter(subfilter, color)
      })
    } else {
      let pointText
      let locationModel
      switch (filter.type) {
        case 'DWITHIN':
          if (CQLUtils.isPolygonFilter(filter.value)) {
            this.handleFilterAsPolygon(filter.value, color, filter.distance)
            break
          }
          if (CQLUtils.isPointRadiusFilter(filter.value)) {
            pointText = filter.value.value.substring(6)
            pointText = pointText.substring(0, pointText.length - 1)
            const latLon = pointText.split(' ')
            locationModel = new LocationModel({
              lat: latLon[1],
              lon: latLon[0],
              radius: filter.distance,
              color,
            })
            this.map.showCircleShape(locationModel)
          } else {
            pointText = filter.value.value.substring(11)
            pointText = pointText.substring(0, pointText.length - 1)
            locationModel = new LocationModel({
              lineWidth: filter.distance,
              line: pointText
                .split(',')
                .map(coordinate =>
                  coordinate.split(' ').map(value => Number(value))
                ),
              color,
            })
            this.map.showLineShape(locationModel)
          }
          break
        case 'INTERSECTS':
          this.handleFilterAsPolygon(filter.value, color, filter.distance)
          break
      }
    }
  },
  handleFilterAsPolygon(value, color, distance) {
    const filterValue = typeof value === 'string' ? value : value.value
    const locationModel = new LocationModel({
      polygon: CQLUtils.arrayFromPolygonWkt(filterValue),
      color,
      ...(distance && { polygonBufferWidth: distance }),
    })
    this.map.showPolygonShape(locationModel)
  },
  removePreviousLocations() {
    this.map.destroyShapes()
  },
  onDestroy() {
    if (this.geometryCollectionView) {
      this.geometryCollectionView.destroy()
    }
    if (this.clusterCollectionView) {
      this.clusterCollectionView.destroy()
    }
    if (this.clusterCollection) {
      this.clusterCollection.reset()
    }
    if (this.map) {
      this.map.destroy()
    }
  },
})
