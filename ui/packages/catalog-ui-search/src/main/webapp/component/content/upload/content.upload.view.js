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
/*global define, window*/
const wreqr = require('../../../js/wreqr.js')
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const CustomElements = require('../../../js/CustomElements.js')
const ContentView = require('../content.view')
const properties = require('../../../js/properties.js')
const WorkspaceContentTabs = require('../../tabs/workspace-content/tabs-workspace-content.js')
const WorkspaceContentTabsView = require('../../tabs/workspace-content/tabs-workspace-content.view.js')
const QueryTabsView = require('../../tabs/query/tabs-query.view.js')
const store = require('../../../js/store.js')
const MetacardTabsView = require('../../tabs/metacard/tabs-metacard.view.js')
const MetacardsTabsView = require('../../tabs/metacards/tabs-metacards.view.js')
const Common = require('../../../js/Common.js')
const MetacardTitleView = require('../../metacard-title/metacard-title.view.js')
const uploadInstance = require('../../upload/upload.js')
const ResultSelectorView = require('../../result-selector/result-selector.view.js')
const VisualizationView = require('../../golden-layout/golden-layout.view.js')

module.exports = ContentView.extend({
  className: 'is-upload',
  selectionInterface: uploadInstance,
  initialize: function() {
    this._mapView = new VisualizationView({
      selectionInterface: uploadInstance,
      configName: 'goldenLayoutUpload',
    })
  },
  onFirstRender() {
    this.listenTo(
      uploadInstance,
      'change:currentUpload',
      this.updateContentLeft
    )
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
        model: uploadInstance.get('currentQuery'),
        selectionInterface: uploadInstance,
      })
    )
  },
  unselectQueriesAndResults: function() {
    uploadInstance.clearSelectedResults()
  },
})
