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
/*global define, setTimeout*/
const Marionette = require('marionette')
const _ = require('lodash')
const $ = require('jquery')
const template = require('./metacard-actions.hbs')
const CustomElements = require('js/CustomElements')
const store = require('js/store')
const MapActions = require('component/map-actions/map-actions.view')

module.exports = Marionette.LayoutView.extend({
  setDefaultModel: function() {
    this.model = this.selectionInterface.getSelectedResults().first()
  },
  template: template,
  tagName: CustomElements.register('metacard-actions'),
  regions: {
    mapActions: '.map-actions',
  },
  events: {},
  ui: {},
  selectionInterface: store,
  initialize: function(options) {
    this.selectionInterface =
      options.selectionInterface || this.selectionInterface
    if (!options.model) {
      this.setDefaultModel()
    }
  },
  serializeData: function() {
    return {
      exportActions: _.sortBy(
        this.model.getExportActions().map(action => ({
          url: action.get('url'),
          title: action.getExportType(),
        })),
        action => action.title.toLowerCase()
      ),
      otherActions: _.sortBy(
        this.model.getOtherActions().map(action => ({
          url: action.get('url'),
          title: action.get('title'),
        })),
        action => action.title.toLowerCase()
      ),
    }
  },
  onRender: function() {
    this.mapActions.show(new MapActions({ model: this.model }), {
      replaceElement: true,
    })
  },
})
