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
const template = require('./upload-batch-item.hbs')
const CustomElements = require('../../js/CustomElements.js')
const Common = require('../../js/Common.js')
const user = require('../singletons/user-instance.js')
const UploadSummaryView = require('../upload-summary/upload-summary.view.js')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('upload-batch-item'),
  modelEvents: {
    'change:finished': 'handleFinished',
  },
  events: {
    'click > .upload-actions .actions-stop': 'stopUpload',
    'click > .upload-actions .actions-remove': 'removeModel',
    'click > .upload-details': 'expandUpload',
  },
  regions: {
    uploadDetails: '> .upload-details .details-summary',
  },
  initialize() {
    const modelJSON = this.model.toJSON()
  },
  onBeforeShow() {
    this.uploadDetails.show(
      new UploadSummaryView({
        model: this.model,
      })
    )
    this.handleFinished()
  },
  handleFinished() {
    const finished = this.model.get('finished')
    this.$el.toggleClass('is-finished', finished)
  },
  removeModel() {
    this.$el.toggleClass('is-destroyed', true)
    setTimeout(() => {
      this.model.collection.remove(this.model)
      user
        .get('user')
        .get('preferences')
        .savePreferences()
    }, 250)
  },
  stopUpload() {
    this.model.cancel()
  },
  expandUpload() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    this.$el.trigger('closeSlideout.' + CustomElements.getNamespace())
    wreqr.vent.trigger('router:navigate', {
      fragment: 'uploads/' + this.model.id,
      options: {
        trigger: true,
      },
    })
  },
  serializeData() {
    return {
      when: Common.getMomentDate(this.model.get('sentAt')),
    }
  },
})
