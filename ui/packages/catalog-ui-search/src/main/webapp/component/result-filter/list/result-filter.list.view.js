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

const ResultFilter = require('../result-filter.view')
const CustomElements = require('../../../js/CustomElements.js')

module.exports = ResultFilter.extend({
  className: 'is-list',
  getResultFilter() {
    return this.model.get('value')
  },
  removeFilter() {
    this.model.set('value', '')
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  saveFilter() {
    this.model.set('value', this.getFilter())
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
})
