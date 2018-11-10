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
const wreqr = require('../../js/wreqr.js')
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const contentTemplate = require('./content.hbs')
const CustomElements = require('../../js/CustomElements.js')
const properties = require('../../js/properties.js')
const WorkspaceContentTabs = require('../tabs/workspace-content/tabs-workspace-content.js')
const WorkspaceContentTabsView = require('../tabs/workspace-content/tabs-workspace-content.view.js')
const QueryTabsView = require('../tabs/query/tabs-query.view.js')
const store = require('../../js/store.js')
const MetacardTabsView = require('../tabs/metacard/tabs-metacard.view.js')
const MetacardsTabsView = require('../tabs/metacards/tabs-metacards.view.js')
const Common = require('../../js/Common.js')
const MetacardTitleView = require('../metacard-title/metacard-title.view.js')
const VisualizationView = require('../visualization/visualization.view.js')
const QueryTitleView = require('../query-title/query-title.view.js')
const GoldenLayoutView = require('../golden-layout/golden-layout.view.js')

var ContentView = Marionette.LayoutView.extend({
  template: contentTemplate,
  tagName: CustomElements.register('content'),
  regions: {
    contentLeft: '.content-left',
    contentRight: '.content-right',
  },
  initialize: function() {
    this._mapView = new GoldenLayoutView({
      selectionInterface: store.get('content'),
      configName: 'goldenLayout',
    })
  },
  onFirstRender() {
    this.listenTo(
      store.get('content'),
      'change:currentWorkspace',
      this.updateContentLeft
    )
  },
  onRender: function() {
    this.updateContentLeft()
    if (this._mapView) {
      this.contentRight.show(this._mapView)
    }
  },
  updateContentLeft: function(workspace) {
    if (workspace) {
      if (
        Object.keys(workspace.changedAttributes())[0] === 'currentWorkspace'
      ) {
        this.updateContentLeft()
        store.clearSelectedResults()
      }
    } else {
      this.contentLeft.show(
        new WorkspaceContentTabsView({
          model: new WorkspaceContentTabs(),
          selectionInterface: store.get('content'),
        })
      )
    }
  },
  _mapView: undefined,
})

module.exports = ContentView
