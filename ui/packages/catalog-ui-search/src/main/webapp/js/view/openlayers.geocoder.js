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
const Backbone = require('backbone')
const wreqr = require('../wreqr.js')
const geocoderTemplate = require('templates/geocoder.handlebars')
const $ = require('jquery')

const geocoder = {}
const url = './internal/REST/v1/Locations'
const geocoderModel = new Backbone.Model()
geocoder.View = Marionette.ItemView.extend({
  template: geocoderTemplate,
  events: {
    'keyup #searchfield': 'searchOnEnter',
    'click #searchbutton': 'search',
  },
  initialize() {
    this.model = geocoderModel
    this.modelBinder = new Backbone.ModelBinder()
    this.listenTo(this.model, 'change', this.changedSearchText)
  },
  onRender() {
    const searchBinding = Backbone.ModelBinder.createDefaultBindings(
      this.el,
      'name'
    )
    this.modelBinder.bind(this.model, this.$el, searchBinding)
  },
  searchOnEnter(e) {
    if (e.keyCode === 13) {
      //user pushed enter, perform search
      this.model.set('searchText', this.$('#searchfield').val())
      if (this.model.get('searchText')) {
        this.search()
      }
      e.preventDefault()
    }
  },
  changedSearchText() {
    if (this.model.get('searchText')) {
      this.$('#searchfield').addClass('geocoder-input-wide')
    } else {
      this.$('#searchfield').removeClass('geocoder-input-wide')
    }
  },
  search() {
    const view = this
    if (this.model.get('searchText')) {
      $.ajax({
        url,
        data: 'jsonp=jsonp&query=' + this.model.get('searchText'),
        contentType: 'application/javascript',
        dataType: 'jsonp',
        jsonp: 'jsonp',
        success(result) {
          if (result.resourceSets.length === 0) {
            view.model.set(
              'searchText',
              view.model.get('searchText') + ' (not found)'
            )
            return
          }
          const resourceSet = result.resourceSets[0]
          if (resourceSet.resources.length === 0) {
            view.model.set(
              'searchText',
              view.model.get('searchText') + ' (not found)'
            )
            return
          }
          const resource = resourceSet.resources[0]
          view.model.set('searchText', resource.name)
          const bbox = resource.bbox
          const south = bbox[2]
          const west = bbox[1]
          const north = bbox[0]
          const east = bbox[3]
          wreqr.vent.trigger('search:maprectanglefly', [
            [west, north],
            [east, south],
          ])
        },
        error(data) {
          view.model.set('searchText', data)
        },
      })
    }
  },
})
module.exports = geocoder
