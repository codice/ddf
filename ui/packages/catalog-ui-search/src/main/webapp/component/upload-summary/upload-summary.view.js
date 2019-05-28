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

const wreqr = require('../../js/wreqr.js')
const Marionette = require('marionette')
const template = require('./upload-summary.hbs')
const CustomElements = require('../../js/CustomElements.js')

module.exports = Marionette.ItemView.extend({
  template,
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
  initialize() {},
  onRender() {
    this.handleSending()
    this.handlePercentage()
    this.handleError()
    this.handleSuccess()
    this.handleIssues()
    this.handleFileInfo()
    this.handleInterrupted()
  },
  handleFileInfo() {
    const amount = this.model.get('amount')
    const complete = this.model.get('complete')
    this.$el
      .find('.info-files .files-text')
      .html(complete + ' / ' + amount + ' Completed')
  },
  handleSending() {
    const sending = this.model.get('sending')
    this.$el.toggleClass('show-progress', sending)
  },
  handlePercentage() {
    const percentage = this.model.get('percentage')
    this.$el.find('.summary-progress').css('width', percentage + '%')
    this.$el.find('.info-percentage').html(Math.floor(percentage) + '%')
  },
  handleError() {
    const error = this.model.get('error')
    this.$el.toggleClass('has-error', error)
    this.$el.find('.error-message').html(this.model.escape('message'))
  },
  handleSuccess(file, response) {
    const success = this.model.get('success')
    this.$el.toggleClass('has-success', success)
    this.$el.find('.success-message').html(this.model.escape('message'))
  },
  handleIssues() {
    const issues = this.model.get('issues')
    this.$el.toggleClass('has-issues', issues > 0)
  },
  handleInterrupted() {
    this.$el.toggleClass('was-interrupted', this.model.get('interrupted'))
  },
  expandUpload() {
    wreqr.vent.trigger('router:navigate', {
      fragment: 'uploads/' + this.model.id,
      options: {
        trigger: true,
      },
    })
  },
  serializeData() {
    const modelJSON = this.model.toJSON()
    return modelJSON
  },
})
