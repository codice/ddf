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
define([
  'wreqr',
  'marionette',
  'underscore',
  'jquery',
  'js/CustomElements',
  '../content.view',
  'properties',
  'component/tabs/workspace-content/tabs-workspace-content',
  'component/tabs/workspace-content/tabs-workspace-content.view',
  'component/tabs/query/tabs-query.view',
  'js/store',
  'component/tabs/metacard/tabs-metacard.view',
  'component/tabs/metacards/tabs-metacards.view',
  'js/Common',
  'component/metacard-title/metacard-title.view',
  'component/upload/upload',
  'component/result-selector/result-selector.view',
  'component/golden-layout/golden-layout.view',
], function(
  wreqr,
  Marionette,
  _,
  $,
  CustomElements,
  ContentView,
  properties,
  WorkspaceContentTabs,
  WorkspaceContentTabsView,
  QueryTabsView,
  store,
  MetacardTabsView,
  MetacardsTabsView,
  Common,
  MetacardTitleView,
  uploadInstance,
  ResultSelectorView,
  VisualizationView
) {
  return ContentView.extend({
    className: 'is-upload',
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
})
