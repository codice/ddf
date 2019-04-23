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

const Marionette = require('marionette')
const DropdownView = require('../dropdown.view')
const template = require('./dropdown.search-form-interactions.hbs')
const SearchFormInteractionsView = require('../../search-form-interactions/search-form-interactions.view.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-search-form-interactions',
  componentToShow: SearchFormInteractionsView,
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = this.options.modelForComponent
  },
  listenToComponent: function() {
    //override if you need more functionality
  },
  isCentered: true,
  getCenteringElement: function() {
    return this.el
  },
  hasTail: true,
})
