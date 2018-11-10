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
var template = require('./annotation.hbs')
var $ = require('jquery')
var _ = require('underscore')
var Marionette = require('marionette')
var CustomElements = require('../../js/CustomElements.js')
var Common = require('../../js/Common.js')
var PropertyView = require('../property/property.view.js')
var Property = require('../property/property.js')
var LoadingCompanionView = require('../loading-companion/loading-companion.view.js')
var userInstance = require('../singletons/user-instance.js')
var announcement = require('../announcement/index.jsx')

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('annotation'),
  template: template,
  regions: {
    annotationContent: '> .annotation-content',
  },
  events: {
    'click .annotation-remove': 'toggleDeleting',
    'click .annotation-edit': 'toggleEditing',
    'click .footer-cancel': 'handleCancel',
    'click .footer-save': 'handleEdit',
    'click .footer-delete': 'handleDelete',
  },
  initialize: function(options) {
    this._useremail = userInstance.get('user').get('email')
    this._parent = options.parent
  },
  handleDelete: function() {
    LoadingCompanionView.beginLoading(this)
    $.ajax({
      url: './internal/annotations/' + this.model.id,
      method: 'DELETE',
      contentType: 'application/json',
    }).always(
      function(response) {
        setTimeout(
          function() {
            this.handleDeleteResponse(response)
            LoadingCompanionView.endLoading(this)
          }.bind(this),
          1000
        )
      }.bind(this)
    )
  },
  handleDeleteResponse: function(response) {
    if (response.responseType === 'success') {
      this.model.collection.remove(this.model)
      announcement.announce({
        title: 'Success!',
        message: 'Annotation Deleted!',
        type: 'success',
      })
    } else {
      announcement.announce({
        title: 'Error!',
        message: 'Could not delete annotation: ' + response.response,
        type: 'error',
      })
    }
  },
  handleCancel: function() {
    if (this.$el.hasClass('is-editing')) {
      this.turnOffEditing()
    }
    if (this.$el.hasClass('is-deleting')) {
      this.turnOffDeleting()
    }
  },
  onBeforeShow: function() {
    this.showannotationContent()
    this.turnOffEditing()
    this.canModify()
  },
  turnOnEditing: function() {
    this.turnOffDeleting()
    this.annotationContent.currentView.turnOnEditing()
    this.$el.toggleClass('is-editing', true)
  },
  turnOnDeleting: function() {
    this.turnOffEditing()
    this.$el.toggleClass('is-deleting', true)
  },
  turnOffEditing: function() {
    this.annotationContent.currentView.turnOffEditing()
    this.$el.toggleClass('is-editing', false)
  },
  toggleEditing: function() {
    if (this.$el.hasClass('is-editing')) {
      this.turnOffEditing()
    } else {
      this.turnOnEditing()
    }
  },
  canModify: function() {
    this.$el.toggleClass(
      'is-modifiable',
      this._useremail === this.model.attributes.owner
    )
  },
  toggleDeleting: function() {
    if (this.$el.hasClass('is-deleting')) {
      this.turnOffDeleting()
    } else {
      this.turnOnDeleting()
    }
  },
  turnOffDeleting: function() {
    this.$el.toggleClass('is-deleting', false)
  },
  handleEdit: function() {
    var requestData = {}
    requestData.note = this.annotationContent.currentView.model.get('value')[0]
    LoadingCompanionView.beginLoading(this)
    $.ajax({
      url: './internal/annotations/' + this.model.id,
      method: 'PUT',
      data: JSON.stringify(requestData),
      contentType: 'application/json',
    }).always(
      function(response) {
        setTimeout(
          function() {
            this.handleEditResponse(response)
            LoadingCompanionView.endLoading(this)
          }.bind(this),
          1000
        )
      }.bind(this)
    )
    LoadingCompanionView.endLoading(this)
    this.turnOffEditing()
  },
  handleEditResponse: function(response) {
    if (response.responseType === 'success') {
      var annotation = JSON.parse(response.response)
      this.model.attributes.annotation = annotation.note
      this.onBeforeShow()

      announcement.announce({
        title: 'Success!',
        message: 'Annotation Updated!',
        type: 'success',
      })
    } else {
      announcement.announce({
        title: 'Error!',
        message: 'Could not update annotation: ' + response.response,
        type: 'error',
      })
    }
  },
  showannotationContent: function() {
    this.annotationContent.show(
      new PropertyView({
        model: new Property({
          value: [this.model.attributes.annotation],
          id: 'annotation-' + this.model.attributes.id,
          label:
            this.model.attributes.owner + ' - ' + this.model.attributes.created,
          type: 'TEXTAREA',
        }),
      })
    )
  },
})
