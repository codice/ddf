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

var Marionette = require('marionette')
var _ = require('underscore')
var DropdownView = require('../dropdown.view')
var template = require('./dropdown.details-filter.hbs')
var ComponentView = require('../../details-filter/details-filter.view.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-detailsFilter',
  componentToShow: ComponentView,
  initialize: function() {
    DropdownView.prototype.initialize.call(this)
    this.handleFilter()
    this.listenTo(this.model, 'change:value', this.handleFilter)
  },
  handleFilter: function() {
    var value = this.model.get('value')
    this.$el.toggleClass('has-filter', value !== undefined && value !== '')
  },
})
