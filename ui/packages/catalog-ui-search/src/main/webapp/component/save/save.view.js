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

const Marionette = require('marionette');
const template = require('./save.hbs');
const CustomElements = require('../../js/CustomElements.js');
require('../../behaviors/button.behavior.js')

// Base View, meant to be extended for whatever needs a save button
module.exports = Marionette.ItemView.extend({
  template: template,
  tagName: CustomElements.register('save'),
  className: 'is-button',
  events: {
    click: 'triggerSave',
  },
  behaviors: {
    button: {},
  },
  initialize: function() {
    // overwrite with listeners
  },
  onBeforeShow: function() {
    this.handleSaved()
  },
  isSaved: function() {
    // overwrite with save check
  },
  triggerSave: function() {
    // overwrite with save action
  },
  handleSaved: function() {
    this.$el.toggleClass('is-saved', this.isSaved())
    this.$el.attr('tabindex', this.isSaved() ? -1 : 0)
  },
})
