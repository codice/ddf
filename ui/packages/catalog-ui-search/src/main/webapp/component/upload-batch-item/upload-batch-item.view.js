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
const _ = require('underscore')
const $ = require('jquery')
const template = require('./upload-batch-item.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const Common = require('../../js/Common.js')
const user = require('../singletons/user-instance.js')
const UploadSummaryView = require('../upload-summary/upload-summary.view.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
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
  initialize: function() {
    const modelJSON = this.model.toJSON()
  },
  onBeforeShow: function() {
    this.uploadDetails.show(
      new UploadSummaryView({
        model: this.model,
      })
    )
    this.handleFinished()
  },
  handleFinished: function() {
    const finished = this.model.get('finished')
    this.$el.toggleClass('is-finished', finished)
  },
  removeModel: function() {
    this.$el.toggleClass('is-destroyed', true)
    setTimeout(
      function() {
        this.model.collection.remove(this.model)
        user
          .get('user')
          .get('preferences')
          .savePreferences()
      }.bind(this),
      250
    )
  },
  stopUpload: function() {
    this.model.cancel()
  },
  expandUpload: function() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    this.$el.trigger('closeSlideout.' + CustomElements.getNamespace())
    wreqr.vent.trigger('router:navigate', {
      fragment: 'uploads/' + this.model.id,
      options: {
        trigger: true,
      },
    })
  },
  serializeData: function() {
    return {
      when: Common.getMomentDate(this.model.get('sentAt')),
    }
  },
})
