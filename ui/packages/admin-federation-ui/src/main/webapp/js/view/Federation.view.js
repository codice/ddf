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
  'marionette',
  'backbone',
  'underscore',
  'jquery',
  'templates/federationPage.handlebars',
], function(Marionette, Backbone, _, $, federationPage) {
  return Marionette.LayoutView.extend({
    template: federationPage,
    initialize: function() {
      _.bindAll.apply(_, [this].concat(_.functions(this)))
      this.listenTo(this.model, 'change', this.render)
    },
    events: {
      'click .tab-item': 'updateTab',
    },
    onShow: function() {
      $('#' + this.model.get('selectedTab')).addClass('is-active')
    },
    updateTab: function(e) {
      $('#' + this.model.get('selectedTab')).removeClass('is-active')
      this.model.set('selectedTab', e.target.id)
      this.model.set(
        'url',
        this.model.get('tabs')[this.model.get('selectedTab')].url
      )
      $('#' + this.model.get('selectedTab')).addClass('is-active')
    },
  })
})
