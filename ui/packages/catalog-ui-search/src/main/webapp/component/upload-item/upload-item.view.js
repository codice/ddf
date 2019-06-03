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
const $ = require('jquery')
const template = require('./upload-item.hbs')
const CustomElements = require('../../js/CustomElements.js')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('upload-item'),
  events: {
    'click .upload-cancel': 'cancelUpload',
    'click .upload-expand': 'expandUpload',
    click: 'expandIfSuccess',
  },
  modelEvents: {
    'change:percentage': 'handlePercentage',
    'change:sending': 'handleSending',
    'change:success': 'handleSuccess',
    'change:error': 'handleError',
    'change:validating': 'handleValidating',
    'change:issues': 'handleIssues',
  },
  initialize() {},
  onRender() {
    this.handleSending()
    this.handlePercentage()
    this.handleError()
    this.handleSuccess()
    this.handleIssues()
    this.handleValidating()
  },
  handleSending() {
    const sending = this.model.get('sending')
    this.$el.toggleClass('show-progress', sending)
  },
  handlePercentage() {
    const percentage = this.model.get('percentage')
    this.$el.find('.info-progress').css('width', percentage + '%')
    this.$el.find('.bottom-percentage').html(Math.floor(percentage) + '%')
  },
  handleError() {
    const error = this.model.get('error')
    this.$el.toggleClass('has-error', error)
    this.$el.find('.error-message').html(this.model.escape('message'))
  },
  handleSuccess(file, response) {
    const success = this.model.get('success')
    this.$el.toggleClass('has-success', success)
    this.$el
      .find('.success-message .message-text')
      .html(this.model.escape('message'))
    this.handleChildren()
  },
  handleChildren() {
    this.$el.toggleClass('has-children', this.model.hasChildren())
  },
  handleValidating() {
    const validating = this.model.get('validating')
    this.$el.toggleClass('checking-validation', validating)
  },
  handleIssues() {
    const issues = this.model.get('issues')
    this.$el.toggleClass('has-validation-issues', issues)
  },
  serializeData() {
    const modelJSON = this.model.toJSON()
    modelJSON.file = {
      name: modelJSON.file.name,
      size: (modelJSON.file.size / 1000000).toFixed(2) + 'MB, ',
      type: modelJSON.file.type,
    }
    return modelJSON
  },
  cancelUpload() {
    this.cancelUpload = $.noop
    this.$el.toggleClass('is-removed', true)
    setTimeout(() => {
      this.model.cancel()
    }, 250)
  },
  expandUpload() {
    wreqr.vent.trigger('router:navigate', {
      fragment: 'metacards/' + this.model.get('id'),
      options: {
        trigger: true,
      },
    })
  },
  expandIfSuccess() {
    if (this.model.get('success') && !this.model.hasChildren()) {
      this.expandUpload()
    }
  },
})
