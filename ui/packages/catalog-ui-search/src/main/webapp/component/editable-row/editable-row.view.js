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
const Backbone = require('backbone')
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const CustomElements = require('../../js/CustomElements.js')
const template = require('./editable-row.hbs')

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('editable-row'),
  template: template,
  events: { 'click .remove': 'removeRow' },
  regions: { embed: '.embed' },
  removeRow: function() {
    this.model.destroy()
  },
  onRender: function() {
    this.embed.show(this.options.embed(this.model, this.options.embedOptions))
  },
})
