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

var wreqr = require('../../js/wreqr.js')
var Marionette = require('marionette')
var _ = require('underscore')
var $ = require('jquery')
var template = require('./upload-summary.hbs')
var CustomElements = require('../../js/CustomElements.js')

module.exports = Marionette.ItemView.extend({
  template: template,
  tagName: CustomElements.register('upload-summary'),
  modelEvents: {
    'change:amount': 'handleFileInfo',
    'change:errors': 'handleError',
    'change:complete': 'handleFileInfo',
    'change:percentage': 'handlePercentage',
    'change:sending': 'handleSending',
    'change:issues': 'handleIssues',
  },
  events: {
    click: 'expandUpload',
  },
  initialize: function() {},
  onRender: function() {
    this.handleSending()
    this.handlePercentage()
    this.handleError()
    this.handleSuccess()
    this.handleIssues()
    this.handleFileInfo()
    this.handleInterrupted()
  },
  handleFileInfo: function() {
    var amount = this.model.get('amount')
    var complete = this.model.get('complete')
    this.$el
      .find('.info-files .files-text')
      .html(complete + ' / ' + amount + ' Completed')
  },
  handleSending: function() {
    var sending = this.model.get('sending')
    this.$el.toggleClass('show-progress', sending)
  },
  handlePercentage: function() {
    var percentage = this.model.get('percentage')
    this.$el.find('.summary-progress').css('width', percentage + '%')
    this.$el.find('.info-percentage').html(Math.floor(percentage) + '%')
  },
  handleError: function() {
    var error = this.model.get('error')
    this.$el.toggleClass('has-error', error)
    this.$el.find('.error-message').html(this.model.escape('message'))
  },
  handleSuccess: function(file, response) {
    var success = this.model.get('success')
    this.$el.toggleClass('has-success', success)
    this.$el.find('.success-message').html(this.model.escape('message'))
  },
  handleIssues: function() {
    var issues = this.model.get('issues')
    this.$el.toggleClass('has-issues', issues > 0)
  },
  handleInterrupted: function() {
    this.$el.toggleClass('was-interrupted', this.model.get('interrupted'))
  },
  expandUpload: function() {
    wreqr.vent.trigger('router:navigate', {
      fragment: 'uploads/' + this.model.id,
      options: {
        trigger: true,
      },
    })
  },
  serializeData: function() {
    var modelJSON = this.model.toJSON()
    return modelJSON
  },
})
