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
  'jquery',
  'backbone.marionette',
  'underscore',
  './feature-item.hbs',
  'js/CustomElements',
], function($, Marionette, _, template, CustomElements) {
  'use strict'

  function testFilter(blob, filter) {
    let filtered = false;
    if (filter.name !== undefined && filter.name !== '') {
      if (blob.name.toLowerCase().indexOf(filter.name.toLowerCase()) === -1) {
        filtered = true
        return filtered
      }
    }
    if (filter.status !== undefined && filter.status !== 'All') {
      if (blob.status.toLowerCase() !== filter.status.toLowerCase()) {
        filtered = true
        return filtered
      }
    }
    return filtered
  }

  const FeatureRow = Marionette.ItemView.extend({
    template: template,
    tagName: CustomElements.register('feature-item'),
    attributes: function() {
      return {
        title: this.model.get('name'),
      }
    },
    modelEvents: {
      'change:status': 'render',
    },
    events: {
      'click button': function() {
        if (!this.$el.hasClass('active')) {
          this.onSelect()
        }
      },
    },
    initialize: function() {
      this.handleFilter()
    },
    updateFilter: function(filter) {
      this.options.filter = filter
      this.handleFilter()
    },
    handleFilter: function() {
      this.$el.toggleClass(
        'is-filtered',
        testFilter(this.model.toJSON(), this.options.filter)
      )
    },
    onSelect: function() {
      this.model.trigger('selected', this.model)
      this.$el.toggleClass('active', true)
    },

    onBeforeClose: function() {
      this.$el.off('click', this.onSelect)
    },
    onRender: function() {
      this.$el.toggleClass('active', false)
      this.$el.toggleClass(
        'is-installed',
        this.model.get('status') === 'Installed'
      )
    },
  });

  return FeatureRow
})
