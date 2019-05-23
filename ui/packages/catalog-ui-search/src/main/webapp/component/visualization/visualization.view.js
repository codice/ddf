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
const CustomElements = require('../../js/CustomElements.js')
const template = require('./visualization.hbs')
const OpenlayersView = require('./maps/openlayers/openlayers.view.js')
const CombinedMapView = require('./combined-map/combined-map.view.js')
const HistogramView = require('./histogram/histogram.view.js')
const TableView = require('./table/table-viz.view.js')
const user = require('../singletons/user-instance.js')
import ExtensionPoints from '../../extension-points'

function getActiveVisualization() {
  return user
    .get('user')
    .get('preferences')
    .get('visualization')
}

function getPreferences() {
  return user.get('user').get('preferences')
}

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('visualization'),
  template: template,
  regions: {
    activeVisualization: '.visualization-container',
  },
  events: {},
  initialize: function() {
    this.listenTo(getPreferences(), 'change:visualization', this.onBeforeShow)
  },
  onBeforeShow: function() {
    const id = getActiveVisualization()
    const viz = ExtensionPoints.visualizations.find(viz => viz.id === id)
    if (viz !== undefined) {
      this.activeVisualization.show(
        new viz.view({
          selectionInterface: this.options.selectionInterface,
        })
      )
    }
  },
  showOpenlayers: function() {
    this.activeVisualization.show(
      new OpenlayersView({
        selectionInterface: this.options.selectionInterface,
      })
    )
  },
  showCesium: function() {
    this.activeVisualization.show(
      new CombinedMapView({
        selectionInterface: this.options.selectionInterface,
      })
    )
  },
  showHistogram: function() {
    this.activeVisualization.show(
      new HistogramView({
        selectionInterface: this.options.selectionInterface,
      })
    )
  },
  showTable: function() {
    this.activeVisualization.show(
      new TableView({
        selectionInterface: this.options.selectionInterface,
      })
    )
  },
})
