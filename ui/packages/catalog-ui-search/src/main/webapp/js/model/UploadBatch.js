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

const UploadModel = require('./Upload')
const Backbone = require('backbone')
const Common = require('../Common.js')
const wreqr = require('../wreqr.js')
const _ = require('underscore')

const updatePreferences = _.throttle(() => {
  wreqr.vent.trigger('preferences:save')
}, 1000)

module.exports = Backbone.AssociatedModel.extend({
  options: undefined,
  defaults() {
    return {
      unseen: true,
      uploads: [],
      percentage: 0,
      errors: 0,
      successes: 0,
      complete: 0,
      amount: 0,
      issues: 0,
      sending: false,
      finished: false,
      interrupted: false,
      sentAt: undefined,
    }
  },
  relations: [
    {
      type: Backbone.Many,
      key: 'uploads',
      relatedModel: UploadModel,
    },
  ],
  bindCallbacks() {
    this.handleAddFile = this.handleAddFile.bind(this)
    this.handleTotalUploadProgress = this.handleTotalUploadProgress.bind(this)
    this.handleSending = this.handleSending.bind(this)
    this.handleQueueComplete = this.handleQueueComplete.bind(this)
    this.handleSuccess = this.handleSuccess.bind(this)
    this.handleError = this.handleError.bind(this)
    this.handleComplete = this.handleComplete.bind(this)
  },
  initialize(attributes, options) {
    this.bindCallbacks()
    this.options = options
    if (!this.id) {
      this.set('id', Common.generateUUID())
    }
    this.listenTo(
      this.get('uploads'),
      'add remove reset update',
      this.handleUploadUpdate
    )
    this.listenTo(
      this.get('uploads'),
      'change:issues',
      this.handleIssuesUpdates
    )
    this.listenToDropzone()
  },
  listenToDropzone() {
    if (this.options.dropzone) {
      this.options.dropzone.on('addedfile', this.handleAddFile)
      this.options.dropzone.on(
        'totaluploadprogress',
        this.handleTotalUploadProgress
      )
      this.options.dropzone.on('sending', this.handleSending)
      this.options.dropzone.on('queuecomplete', this.handleQueueComplete)
      this.options.dropzone.on('success', this.handleSuccess)
      this.options.dropzone.on('error', this.handleError)
      this.options.dropzone.on('complete', this.handleComplete)
    } else {
      this.set('interrupted', this.get('interrupted') || !this.get('finished'))
      this.set('finished', true)
    }
  },
  handleAddFile(file) {
    this.get('uploads').add(
      {
        file,
      },
      {
        dropzone: this.options.dropzone,
      }
    )
  },
  handleSuccess(file) {
    if (file.status !== 'canceled') {
      this.set('successes', this.get('successes') + 1)
    }
  },
  handleError(file) {
    if (file.status !== 'canceled') {
      this.set('errors', this.get('errors') + 1)
    }
  },
  handleComplete(file) {
    if (file.status === 'success') {
      this.set('complete', this.get('complete') + 1)
    }
    updatePreferences()
  },
  handleSending() {
    this.set({
      sending: true,
    })
  },
  handleTotalUploadProgress() {
    this.set({
      percentage: this.calculatePercentageDone(),
    })
  },
  unlistenToDropzone() {
    this.options.dropzone.off('addedfile', this.handleAddFile)
    this.options.dropzone.off(
      'totaluploadprogress',
      this.handleTotalUploadProgress
    )
    this.options.dropzone.off('sending', this.handleSending)
    this.options.dropzone.off('queuecomplete', this.handleQueueComplete)
    this.options.dropzone.off('success', this.handleSuccess)
    this.options.dropzone.off('error', this.handleError)
    this.options.dropzone.off('complete', this.handleComplete)
  },
  handleQueueComplete() {
    // https://github.com/enyo/dropzone/blob/v4.3.0/dist/dropzone.js#L56
    // if we remove callbacks too early this loop will fail, look to see if updating to latest fixes this
    setTimeout(() => {
      this.unlistenToDropzone()
    }, 0)
    this.set({
      finished: true,
      percentage: 100,
    })
    updatePreferences()
  },
  handleUploadUpdate() {
    this.set({
      amount: this.get('uploads').length,
    })
  },
  handleIssuesUpdates() {
    this.set({
      issues: this.get('uploads').reduce((issues, upload) => {
        issues += upload.get('issues') ? 1 : 0
        return issues
      }, 0),
    })
  },
  clear() {
    this.cancel()
    this.get('uploads').reset()
  },
  cancel() {
    if (this.options.dropzone) {
      this.options.dropzone.removeAllFiles(true)
    }
  },
  start() {
    if (this.options.dropzone) {
      this.set({
        sending: true,
        sentAt: Date.now(), //- Math.random() * 14 * 86400000
      })
      wreqr.vent.trigger('uploads:add', this)
      this.listenTo(this, 'change', updatePreferences)
      this.options.dropzone.options.autoProcessQueue = true
      this.options.dropzone.processQueue()
    }
  },
  getTimeComparator() {
    return this.get('sentAt')
  },
  calculatePercentageDone() {
    const files = this.options.dropzone.files
    if (files.length === 0) {
      return 100
    }
    const totalBytes = files.reduce((total, file) => {
      total += file.upload.total
      return total
    }, 0)
    const bytesSent = files.reduce((total, file) => {
      total += file.upload.bytesSent
      return total
    }, 0)
    let progress = 100 * (bytesSent / totalBytes)
    if (progress >= 100 && !this.get('finished')) {
      progress = 99
    }
    return progress
  },
})
