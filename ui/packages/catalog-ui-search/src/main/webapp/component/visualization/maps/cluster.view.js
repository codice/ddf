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

const Marionette = require('marionette')
const _ = require('underscore')
const _debounce = require('lodash/debounce')
const calculateConvexHull = require('geo-convex-hull')

const ClusterView = Marionette.ItemView.extend({
  template: false,
  geometry: undefined,
  convexHull: undefined,
  selectionType: undefined,
  initialize(options) {
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
  handleCluster() {
    const center = this.options.map.getCartographicCenterOfClusterInDegrees(
      this.model
    )
    this.geometry.push(
      this.options.map.addPointWithText(center, {
        id: this.model.get('results').map(result => result.id),
        color: this.model
          .get('results')
          .first()
          .get('metacard')
          .get('color'),
        view: this,
      })
    )
  },
  addConvexHull() {
    const points = this.model.get('results').map(result =>
      result
        .get('metacard')
        .get('properties')
        .getPoints()
    )
    const data = _.flatten(points, true).map(coord => ({
      longitude: coord[0],
      latitude: coord[1],
    }))
    const convexHull = calculateConvexHull(data).map(coord => [
      coord.longitude,
      coord.latitude,
    ])
    convexHull.push(convexHull[0])
    const geometry = this.options.map.addLine(convexHull, {
      id: this.model.get('results').map(result => result.id),
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
  handleHover(id) {
    if (
      id &&
      this.model
        .get('results')
        .map(result => result.id)
        .toString() === id.toString()
    ) {
      this.options.map.showGeometry(this.geometry[1])
    } else {
      this.options.map.hideGeometry(this.geometry[1])
    }
  },
  updateSelected() {
    let selected = 0
    const selectedResults = this.options.selectionInterface.getSelectedResults()
    const results = this.model.get('results')
    // if there are less selected results, loop over those instead of this model's results
    if (selectedResults.length < results.length) {
      selectedResults.some(result => {
        if (results.get(result.id)) {
          selected++
        }
        return selected === results.length
      })
    } else {
      results.forEach(result => {
        if (selectedResults.get(result.id)) {
          selected++
        }
      })
    }
    if (selected === results.length) {
      this.updateDisplay('fullySelected')
    } else if (selected > 0) {
      this.updateDisplay('partiallySelected')
    } else {
      this.updateDisplay('unselected')
    }
  },
  updateDisplay(selectionType) {
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
  showFullySelected() {
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
  showPartiallySelected() {
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
  showUnselected() {
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
  onDestroy() {
    if (this.geometry) {
      this.geometry.forEach(geometry => {
        this.options.map.removeGeometry(geometry)
      })
    }
  },
})

module.exports = ClusterView
