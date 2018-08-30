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
/*global define, setTimeout*/
define([
  'wreqr',
  'marionette',
  'underscore',
  'jquery',
  './upload-batch-item.hbs',
  'js/CustomElements',
  'js/store',
  'js/Common',
  'component/singletons/user-instance',
  'component/upload-summary/upload-summary.view',
], function(
  wreqr,
  Marionette,
  _,
  $,
  template,
  CustomElements,
  store,
  Common,
  user,
  UploadSummaryView
) {
  return Marionette.LayoutView.extend({
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
      var modelJSON = this.model.toJSON()
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
      var finished = this.model.get('finished')
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
})
