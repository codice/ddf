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
var _ = require('underscore')
var $ = require('jquery')
var template = require('./metacard-overwrite.hbs')
var CustomElements = require('js/CustomElements')
var store = require('js/store')
var ConfirmationView = require('component/confirmation/confirmation.view')
var Dropzone = require('dropzone')
var Common = require('js/Common')
var OverwritesInstance = require('component/singletons/overwrites-instance')

function getOverwriteModel(view) {
  return OverwritesInstance.get(view.model.get('metacard').id)
}

module.exports = Marionette.ItemView.extend({
  setDefaultModel: function() {
    this.model = this.selectionInterface.getSelectedResults().first()
  },
  template: template,
  tagName: CustomElements.register('metacard-overwrite'),
  events: {
    'click button.overwrite-confirm': 'archive',
    'click button.overwrite-back': 'startOver',
  },
  selectionInterface: store,
  initialize: function(options) {
    this.selectionInterface =
      options.selectionInterface || this.selectionInterface
    if (!options.model) {
      this.setDefaultModel()
    }
  },
  onRender: function() {
    this.setupDropzone()
  },
  setupDropzone: function() {
    this.dropzone = new Dropzone(this.el.querySelector('.overwrite-dropzone'), {
      url: './internal/catalog/' + this.model.get('metacard').id,
      maxFilesize: 5000000, //MB
      method: 'put',
    })
    this.trackOverwrite()
    this.setupEventListeners()
    this.handleSending()
    this.handlePercentage()
    this.handleError()
    this.handleSuccess()
  },
  trackOverwrite: function() {
    if (!getOverwriteModel(this)) {
      OverwritesInstance.add({
        id: this.model.get('metacard').id,
        dropzone: this.dropzone,
        result: this.model,
      })
    }
  },
  setupEventListeners: function() {
    var overwriteModel = getOverwriteModel(this)
    this.listenTo(overwriteModel, 'change:percentage', this.handlePercentage)
    this.listenTo(overwriteModel, 'change:sending', this.handleSending)
    this.listenTo(overwriteModel, 'change:error', this.handleError)
    this.listenTo(overwriteModel, 'change:success', this.handleSuccess)
  },
  handleSending: function() {
    var sending = getOverwriteModel(this).get('sending')
    this.$el.toggleClass('show-progress', sending)
  },
  handlePercentage: function() {
    var percentage = getOverwriteModel(this).get('percentage')
    this.$el
      .find('.overwrite-progress > .progress-bar')
      .css('width', percentage + '%')
    this.$el.find('.progress-percentage').html(Math.floor(percentage) + '%')
  },
  handleError: function() {
    var error = getOverwriteModel(this).get('error')
    this.$el.toggleClass('has-error', error)
    this.$el
      .find('.error-message')
      .html(getOverwriteModel(this).escape('message'))
  },
  handleSuccess: function(file, response) {
    var success = getOverwriteModel(this).get('success')
    this.$el.toggleClass('has-success', success)
    this.$el
      .find('.success-message')
      .html(getOverwriteModel(this).escape('message'))
  },
  archive: function() {
    this.listenTo(
      ConfirmationView.generateConfirmation({
        prompt: 'Are you sure you want to overwrite the content?',
        no: 'Cancel',
        yes: 'Overwrite',
      }),
      'change:choice',
      function(confirmation) {
        if (confirmation.get('choice')) {
          this.$el.find('.overwrite-dropzone').click()
        }
      }.bind(this)
    )
  },
  startOver: function() {
    OverwritesInstance.remove(this.model.get('metacard').id)
    this.render()
  },
  onDestroy: function() {
    OverwritesInstance.removeIfUnused(this.model.get('metacard').id)
  },
})
