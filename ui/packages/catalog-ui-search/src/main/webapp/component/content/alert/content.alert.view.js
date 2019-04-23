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
const ContentView = require('../content.view')
const alertInstance = require('../../alert/alert.js')
const ResultSelectorView = require('../../result-selector/result-selector.view.js')
const VisualizationView = require('../../golden-layout/golden-layout.view.js')

module.exports = ContentView.extend({
  className: 'is-alert',
  selectionInterface: alertInstance,
  initialize: function() {
    this._mapView = new VisualizationView({
      selectionInterface: alertInstance,
      configName: 'goldenLayoutAlert',
    })
  },
  onFirstRender() {
    this.listenTo(alertInstance, 'change:currentAlert', this.updateContentLeft)
  },
  onRender: function() {
    this.updateContentLeft()
    if (this._mapView) {
      this.contentRight.show(this._mapView)
    }
  },
  updateContentLeft: function() {
    this.contentLeft.show(
      new ResultSelectorView({
        model: alertInstance.get('currentQuery'),
        selectionInterface: alertInstance,
      })
    )
  },
  unselectQueriesAndResults: function() {
    alertInstance.clearSelectedResults()
  },
})
