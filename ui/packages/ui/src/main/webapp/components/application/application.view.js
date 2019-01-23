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
/*global define*/
define([
  'backbone',
  'backbone.marionette',
  'js/wreqr',
  './application.hbs',
  'js/CustomElements',
], function(Backbone, Marionette, wreqr, template, CustomElements) {
  'use strict'

  var DetailedApplicationLayout = Marionette.Layout.extend({
    template: template,
    tagName: CustomElements.register('application'),
    className: 'well',
    regions: {
      content: '.content',
      tabs: '.tab-container',
      tabContent: '.tab-content-container',
    },
    events: {
      'click .nav-to-applications': 'navToApplications',
    },
    navToApplications: function(e) {
      e.preventDefault()
      wreqr.vent.trigger('navigateTo:applicationHome')
    },

    selectFirstTab: function() {
      this.$('.tab-container a:first').tab('show')
    },
  })

  return DetailedApplicationLayout
})
