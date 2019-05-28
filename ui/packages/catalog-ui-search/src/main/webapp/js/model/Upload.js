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

const Backbone = require('backbone')
const $ = require('jquery')
require('../jquery.whenAll.js')

function fileMatches(file, model) {
  return file === model.get('file')
}

function checkValidation(model) {
  if (model.get('id')) {
    model.set('validating', true)
    //wait for solr
    setTimeout(function() {
      $.whenAll
        .apply(this, [
          $.get(
            './internal/metacard/' + model.get('id') + '/attribute/validation'
          ).then(response => {
            model.set({
              issues: model.get('issues') || response.length > 0,
            })
          }),
          $.get('./internal/metacard/' + model.get('id') + '/validation').then(
            response => {
              model.set({
                issues: model.get('issues') || response.length > 0,
              })
            }
          ),
        ])
        .always(() => {
          model.set({
            validating: false,
          })
        })
    }, 2000)
  }
}

module.exports = Backbone.Model.extend({
  options: undefined,
  defaults() {
    return {
      id: undefined,
      children: undefined,
      result: undefined,
      file: undefined,
      percentage: 0,
      sending: false,
      success: false,
      error: false,
      message: '',
      validating: false,
      issues: false,
    }
  },
  bindCallbacks() {
    this.handleUploadProgress = this.handleUploadProgress.bind(this)
    this.handleSending = this.handleSending.bind(this)
    this.handleSuccess = this.handleSuccess.bind(this)
    this.handleError = this.handleError.bind(this)
    this.handleComplete = this.handleComplete.bind(this)
    this.handleQueueComplete = this.handleQueueComplete.bind(this)
  },
  initialize(attributes, options) {
    this.bindCallbacks()
    this.options = options
    this.setupDropzoneListeners()
  },
  setupDropzoneListeners() {
    if (this.options.dropzone) {
      this.options.dropzone.on('sending', this.handleSending)
      this.options.dropzone.on('uploadprogress', this.handleUploadProgress)
      this.options.dropzone.on('error', this.handleError)
      this.options.dropzone.on('success', this.handleSuccess)
      this.options.dropzone.on('complete', this.handleComplete)
      this.options.dropzone.on('queuecomplete', this.handleQueueComplete)
    }
  },
  handleSending(file) {
    if (fileMatches(file, this)) {
      this.set({
        sending: true,
      })
    }
  },
  handleUploadProgress(file, percentage) {
    if (fileMatches(file, this)) {
      this.set('percentage', percentage)
    }
  },
  handleError(file) {
    if (fileMatches(file, this)) {
      const message = file.name + ' could not be uploaded successfully.'
      this.set({
        error: true,
        message,
      })
    }
  },
  hasChildren() {
    return this.get('children') && this.get('children').length > 1
  },
  handleQueueComplete() {
    // https://github.com/enyo/dropzone/blob/v4.3.0/dist/dropzone.js#L56
    // if we remove callbacks too early this loop will fail, look to see if updating to latest fixes this
    setTimeout(() => {
      this.unlistenToDropzone()
    }, 0)
  },
  unlistenToDropzone() {
    this.options.dropzone.off('sending', this.handleSending)
    this.options.dropzone.off('queuecomplete', this.handleQueueComplete)
    this.options.dropzone.off('uploadprogress', this.handleUploadProgress)
    this.options.dropzone.off('success', this.handleSuccess)
    this.options.dropzone.off('error', this.handleError)
    this.options.dropzone.off('complete', this.handleComplete)
  },
  handleSuccess(file) {
    if (fileMatches(file, this)) {
      let message = `${file.name} uploaded successfully.`
      const addedIdsHeader = file.xhr.getResponseHeader('added-ids')
      const children = addedIdsHeader ? addedIdsHeader.split(',') : undefined
      if (children && children.length > 1) {
        message += ` ${children.length} items found.`
      }
      this.set({
        id: file.xhr.getResponseHeader('id'),
        children,
        success: true,
        message,
      })

      checkValidation(this)
    }
  },
  handleComplete(file) {
    if (fileMatches(file, this) && file.status === 'canceled') {
      this.collection.remove(this)
    }
  },
  checkValidation() {
    checkValidation(this)
  },
  cancel() {
    if (this.options.dropzone) {
      this.options.dropzone.removeFile(this.get('file'))
      if (this.collection) {
        this.collection.remove(this)
      }
    }
  },
})
