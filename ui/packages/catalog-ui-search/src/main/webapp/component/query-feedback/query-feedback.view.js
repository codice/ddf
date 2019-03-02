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
/*global require*/
var Marionette = require('marionette')
var CustomElements = require('../../js/CustomElements.js')
var store = require('../../js/store.js')
var template = require('./query-feedback.hbs')
var PropertyView = require('../property/property.view.js')
var PropertyModel = require('../property/property.js')
var _ = require('underscore')
var router = require('../router/router.js')
var user = require('../singletons/user-instance.js')
var $ = require('jquery')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('query-feedback'),
  events: {
    'click .editor-cancel': 'handleCancel',
    'click .editor-send': 'handleSend',
  },
  regions: {
    comments: '.properties-comments',
  },
  initialize: function() {
    this.listenTo(router, 'change', this.handleCancel)
  },
  onBeforeShow: function() {
    this.comments.show(
      new PropertyView({
        model: new PropertyModel({
          value: [''],
          id: 'Please enter some comments to include',
          type: 'TEXTAREA',
        }),
      })
    )
    this.edit()
  },
  edit: function() {
    this.$el.addClass('is-editing')
    this.regionManager.forEach(function(region) {
      if (region.currentView && region.currentView.turnOnEditing) {
        region.currentView.turnOnEditing()
      }
    })
  },
  handleCancel: function() {
    this.$el.trigger(CustomElements.getNamespace() + 'close-lightbox')
  },
  handleSend: function() {
    var payload = {
      user: {
        email: user.get('user').get('email'),
        name: user.get('user').get('username'),
      },
      search: {
        initiated: new Date(
          this.model.get('result').get('initiated')
        ).toISOString(),
        cql: this.model.get('cql'),
        results: this.model
          .get('result')
          .get('results')
          .toJSON(),
        status: this.model
          .get('result')
          .get('status')
          .toJSON(),
      },
      workspace: {
        id: store.getCurrentWorkspace().id,
        name: store.getCurrentWorkspace().get('title'),
      },
      comments: this.comments.currentView.model.getValue()[0],
    }
    $.post('./internal/feedback', JSON.stringify(payload))
    this.$el.trigger(CustomElements.getNamespace() + 'close-lightbox')
  },
})
