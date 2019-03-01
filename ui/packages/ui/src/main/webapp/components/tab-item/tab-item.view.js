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
/* global define */
define([
  'backbone.marionette',
  './tab-item.hbs',
  'js/CustomElements',
  'js/wreqr.js',
], function(Marionette, template, CustomElements, wreqr) {
  return Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('tab-item'),
    className: 'itemview',
    triggerShown: function() {
      wreqr.vent.trigger('application:tabShown', this.model.id)
      this.$el.toggleClass('is-active', true)
    },
    triggerHidden: function() {
      wreqr.vent.trigger('application:tabHidden', this.model.id)
      this.$el.toggleClass('is-active', false)
    },
  })
})
