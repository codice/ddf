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
const _ = require('underscore')
const _debounce = require('lodash/debounce')
const calculateConvexHull = require('geo-convex-hull')

const ClusterView = Marionette.ItemView.extend({
  template: false,
  geometry: undefined,
  convexHull: undefined,
  selectionType: undefined,
  initialize: function(options) {
    this.geometry = []
    this.geoController = options.geoController
    this.handleCluster()
    this.addConvexHull()
    this.updateSelected()
    this.updateSelected = _debounce(this.updateSelected, 100, {
      trailing: true,
      leading: true,
    })
    this.listenTo(
      this.options.selectionInterface.getSelectedResults(),
      'update add remove reset',
      this.updateSelected
    )
  },
  handleCluster: function() {
    const center = this.options.map.getCartographicCenterOfClusterInDegrees(
      this.model
    )
    this.geometry.push(
      this.options.map.addPointWithText(center, {
        id: this.model.get('results').map(function(result) {
          return result.id
        }),
        color: this.model
          .get('results')
          .first()
          .get('metacard')
          .get('color'),
        view: this,
      })
    )
  },
  addConvexHull: function() {
    const points = this.model.get('results').map(function(result) {
      return result
        .get('metacard')
        .get('properties')
        .getPoints()
    })
    const data = _.flatten(points, true).map(function(coord) {
      return {
        longitude: coord[0],
        latitude: coord[1],
      }
    })
    const convexHull = calculateConvexHull(data).map(function(coord) {
      return [coord.longitude, coord.latitude]
    })
    convexHull.push(convexHull[0])
    const geometry = this.options.map.addLine(convexHull, {
      id: this.model.get('results').map(function(result) {
        return result.id
      }),
      color: this.model
        .get('results')
        .first()
        .get('metacard')
        .get('color'),
      view: this,
    })
    this.options.map.hideGeometry(geometry)
    this.geometry.push(geometry)
  },
  handleHover: function(id) {
    if (
      id &&
      this.model
        .get('results')
        .map(function(result) {
          return result.id
        })
        .toString() === id.toString()
    ) {
      this.options.map.showGeometry(this.geometry[1])
    } else {
      this.options.map.hideGeometry(this.geometry[1])
    }
  },
  updateSelected: function() {
    let selected = 0
    const selectedResults = this.options.selectionInterface.getSelectedResults()
    const results = this.model.get('results')
    // if there are less selected results, loop over those instead of this model's results
    if (selectedResults.length < results.length) {
      selectedResults.some(
        function(result) {
          if (results.get(result.id)) {
            selected++
          }
          return selected === results.length
        }.bind(this)
      )
    } else {
      results.forEach(
        function(result) {
          if (selectedResults.get(result.id)) {
            selected++
          }
        }.bind(this)
      )
    }
    if (selected === results.length) {
      this.updateDisplay('fullySelected')
    } else if (selected > 0) {
      this.updateDisplay('partiallySelected')
    } else {
      this.updateDisplay('unselected')
    }
  },
  updateDisplay: function(selectionType) {
    if (this.selectionType !== selectionType) {
      this.selectionType = selectionType
      switch (selectionType) {
        case 'fullySelected':
          this.showFullySelected()
          break
        case 'partiallySelected':
          this.showPartiallySelected()
          break
        case 'unselected':
          this.showUnselected()
          break
      }
    }
  },
  showFullySelected: function() {
    this.options.map.updateCluster(this.geometry, {
      color: this.model
        .get('results')
        .first()
        .get('metacard')
        .get('color'),
      isSelected: true,
      count: this.model.get('results').length,
      outline: 'black',
      textFill: 'black',
    })
  },
  showPartiallySelected: function() {
    this.options.map.updateCluster(this.geometry, {
      color: this.model
        .get('results')
        .first()
        .get('metacard')
        .get('color'),
      isSelected: false,
      count: this.model.get('results').length,
      outline: 'black',
      textFill: 'white',
    })
  },
  showUnselected: function() {
    this.options.map.updateCluster(this.geometry, {
      color: this.model
        .get('results')
        .first()
        .get('metacard')
        .get('color'),
      isSelected: false,
      count: this.model.get('results').length,
      outline: 'white',
      textFill: 'white',
    })
  },
  onDestroy: function() {
    if (this.geometry) {
      this.geometry.forEach(
        function(geometry) {
          this.options.map.removeGeometry(geometry)
        }.bind(this)
      )
    }
  },
})

module.exports = ClusterView
