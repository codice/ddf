/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

const Marionette = require('marionette')
const iconHelper = require('../../../js/IconHelper.js')
const _ = require('underscore')
const _debounce = require('lodash/debounce')
const wkx = require('wkx')
const metacardDefinitions = require('../../singletons/metacard-definitions.js')

const GeometryView = Marionette.ItemView.extend({
  template: false,
  geometry: undefined,
  isSelected: undefined,
  isClustered: undefined,
  initialize() {
    this.updateGeometry()
    this.listenTo(
      this.model,
      'change:metacard>properties change:metacard',
      this.updateGeometry
    )
  },
  updateGeometry(propertiesModel) {
    if (
      propertiesModel &&
      _.find(
        Object.keys(propertiesModel.changedAttributes()),
        attribute =>
          (metacardDefinitions.metacardTypes[attribute] &&
            metacardDefinitions.metacardTypes[attribute].type === 'GEOMETRY') ||
          attribute === 'id'
      ) === undefined
    ) {
      return
    }
    this.onDestroy()
    this.isSelected = undefined
    this.isClustered = undefined
    const geometry = this.model.getGeometries()
    if (geometry.length > 0) {
      this.geometry = []
      _.forEach(geometry, property => {
        this.handleGeometry(wkx.Geometry.parse(property).toGeoJSON())
      })
      this.updateSelected = _debounce(this.updateSelected, 100, {
        trailing: true,
        leading: true,
      })
      this.updateSelected()
      this.checkIfClustered()
      this.stopListening(this.options.selectionInterface.getSelectedResults())
      this.stopListening(this.options.clusterCollection)
      this.listenTo(
        this.options.selectionInterface.getSelectedResults(),
        'update add remove reset',
        this.updateSelected
      )
      this.listenTo(
        this.options.clusterCollection,
        'add remove update',
        this.checkIfClustered
      )
    } else {
      this.stopListening(this.options.selectionInterface.getSelectedResults())
      this.stopListening(this.options.clusterCollection)
    }
  },
  handleGeometry(geometry) {
    switch (geometry.type) {
      case 'Point':
        this.handlePoint(geometry.coordinates)
        break
      case 'Polygon':
        geometry.coordinates.forEach(polygon => {
          this.handlePoint(polygon[0])
          this.handleLine(polygon)
          //this.handlePolygon(polygon);
        })
        break
      case 'LineString':
        this.handlePoint(geometry.coordinates[0])
        this.handleLine(geometry.coordinates)
        break
      case 'MultiLineString':
        geometry.coordinates.forEach(line => {
          this.handlePoint(line[0])
          this.handleLine(line)
        })
        break
      case 'MultiPoint':
        geometry.coordinates.forEach(point => {
          this.handlePoint(point)
        })
        break
      case 'MultiPolygon':
        geometry.coordinates.forEach(multipolygon => {
          multipolygon.forEach(polygon => {
            this.handlePoint(polygon[0])
            this.handleLine(polygon)
            //this.handlePolygon(polygon);
          })
        })
        break
      case 'GeometryCollection':
        geometry.geometries.forEach(subgeometry => {
          this.handleGeometry(subgeometry)
        })
        break
    }
  },
  handlePoint(point) {
    this.geometry.push(
      this.options.map.addPoint(point, {
        id: this.model.id,
        title: this.model
          .get('metacard')
          .get('properties')
          .get('title'),
        color: this.model.get('metacard').get('color'),
        icon: iconHelper.getFull(this.model),
        view: this,
      })
    )
  },
  handleLine(line) {
    this.geometry.push(
      this.options.map.addLine(line, {
        id: this.model.id,
        color: this.model.get('metacard').get('color'),
        title: this.model
          .get('metacard')
          .get('properties')
          .get('title'),
        view: this,
      })
    )
  },
  handlePolygon(polygon) {
    this.geometry = this.geometry.concat(
      this.options.map.addPolygon(polygon, {
        id: this.model.id,
        color: this.model.get('metacard').get('color'),
        title: this.model
          .get('metacard')
          .get('properties')
          .get('title'),
        view: this,
      })
    )
  },
  updateSelected() {
    const selected = this.options.selectionInterface
      .getSelectedResults()
      .some(function(result) {
        return result.id === this.model.id
      }, this)
    if (selected) {
      this.updateDisplay(true)
    } else {
      this.updateDisplay(false)
    }
  },
  updateDisplay(isSelected) {
    if (!this.isClustered && this.isSelected !== isSelected) {
      this.isSelected = isSelected
      this.geometry.forEach(geometry => {
        this.options.map.updateGeometry(geometry, {
          color: this.model.get('metacard').get('color'),
          icon: iconHelper.getFull(this.model),
          isSelected,
        })
      })
    }
  },
  checkIfClustered() {
    const isClustered = this.options.clusterCollection.isClustered(this.model)
    if (this.isClustered !== isClustered) {
      this.isClustered = isClustered
      if (isClustered) {
        this.hideGeometry()
      } else {
        this.updateSelected()
        this.showGeometry()
      }
    }
  },
  showGeometry() {
    this.geometry.forEach(geometry => {
      this.options.map.showGeometry(geometry)
    })
  },
  hideGeometry() {
    this.geometry.forEach(geometry => {
      this.options.map.hideGeometry(geometry)
    })
  },
  onDestroy() {
    if (this.geometry) {
      this.geometry.forEach(geometry => {
        this.options.map.removeGeometry(geometry)
      })
    }
  },
})

module.exports = GeometryView
