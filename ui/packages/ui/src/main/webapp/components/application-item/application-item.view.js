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

define([
  'backbone.marionette',
  'underscore',
  'js/wreqr.js',
  './application-item.hbs',
  'js/CustomElements',
], function(Marionette, _, wreqr, template, CustomElements) {
  'use strict'

  // List of apps that cannot have any actions performed on them through
  // the applications module
  const disableList = ['platform-app', 'admin-app'];

  // Itemview for each individual application
  const AppInfoView = Marionette.Layout.extend({
    template: template,
    tagName: CustomElements.register('application-item'),
    regions: {
      modalRegion: '.modal-region',
    },
    events: {
      click: 'selectApplication',
      keyup: 'emulateClick',
    },
    emulateClick: function(e) {
      if (e.target === this.el && (e.keyCode === 13 || e.keyCode === 32)) {
        e.preventDefault()
        e.stopPropagation()
        this.$el.mousedown().click()
      } else if (e.keyCode === 27) {
        this.$el.popover('hide')
      }
    },
    attributes: function() {
      return {
        id: this.model.get('appId') + '-card',
        tabindex: 0,
      }
    },
    // Will disable functionality for certain applications
    serializeData: function() {
      const that = this;
      let disable = false;
      disableList.forEach(function(child) {
        if (that.model.get('appId') === child) {
          disable = true
        }
      })

      return _.extend(this.model.toJSON(), { isDisabled: disable })
    },

    selectApplication: function() {
      wreqr.vent.trigger('application:reqestSelection', this.model)
    },
    onRender: function() {
      this.$el.popover({
        title: this.model.get('displayName'),
        content: this.model.get('description'),
        trigger: 'hover focus',
        placement: 'bottom',
        container: 'body',
      })
    },
    onBeforeClose: function() {
      this.$el.popover('destroy')
    },
  });

  return AppInfoView
})
