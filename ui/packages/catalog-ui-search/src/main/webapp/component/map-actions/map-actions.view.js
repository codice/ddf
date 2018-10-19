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
/*global define*/

const _ = require('underscore')
const Marionette = require('marionette')
const Backbone = require('backbone')
const wreqr = require('wreqr')
const template = require('./map-actions.hbs')
const CustomElements = require('js/CustomElements')

var mapActionsView = Marionette.ItemView.extend({
  template: template,
  tagName: CustomElements.register('map-actions'),
  events: {
    'click a': 'overlayImage',
  },
  initialize: function() {
    this.handleEmpty()
  },
  handleEmpty: function() {
    this.$el.toggleClass('is-empty', this.getMapActions().length === 0)
  },

  serializeData: function() {
    return _.extend(this.model.toJSON(), {
      mapActions: this.getMapActions(),
      overlayActions: this.getOverlayActions(),
    })
  },

  getActions: function() {
    return this.model.get('actions')
  },

  getMapActions: function() {
    return this.getActions().filter(function(action) {
      return action.get('id').indexOf('catalog.data.metacard.map.') === 0
    })
  },

  getOverlayActions: function() {
    var modelOverlayActions = this.getActions().filter(function(action) {
      return (
        action.get('id').indexOf('catalog.data.metacard.map.overlay.') === 0
      )
    })

    var _this = this
    return _.map(modelOverlayActions, function(modelOverlayAction) {
      return {
        description: modelOverlayAction.get('description'),
        url: modelOverlayAction.get('url'),
        overlayText: _this.getOverlayText(modelOverlayAction.get('url')),
      }
    })
  },

  getOverlayText: function(actionUrl) {
    var overlayTransformerPrefix = 'overlay.'
    var overlayTransformerIndex = actionUrl.lastIndexOf(
      overlayTransformerPrefix
    )
    if (overlayTransformerIndex >= 0) {
      var overlayName = actionUrl.substr(
        overlayTransformerIndex + overlayTransformerPrefix.length
      )
      return 'Overlay ' + overlayName + ' on the map'
    }

    return ''
  },

  overlayImage: function(event) {
    var clickedOverlayUrl = event.target.getAttribute('data-url')
    var currentOverlayUrl = this.model.get('currentOverlayUrl')

    var removeOverlay = clickedOverlayUrl === currentOverlayUrl

    if (removeOverlay) {
      this.model.unset('currentOverlayUrl', { silent: true })
      wreqr.vent.trigger(
        'metacard:overlay:remove',
        this.model.get('metacard').get('id')
      )
    } else {
      this.model.set('currentOverlayUrl', clickedOverlayUrl, { silent: true })
      this.model.get('metacard').set('currentOverlayUrl', clickedOverlayUrl)
      wreqr.vent.trigger('metacard:overlay', this.model.get('metacard'))
    }
    this.render()
  },
})

module.exports = mapActionsView
