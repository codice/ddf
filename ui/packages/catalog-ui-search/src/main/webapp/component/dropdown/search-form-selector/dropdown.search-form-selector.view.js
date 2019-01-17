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
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const DropdownView = require('../dropdown.view')
const template = require('./dropdown.search-form-selector.hbs')
const SearchForms = require('../../search-form-selector/search-form-selector.view.js')
const SearchFormsList = require('../../search-form-list/search-form-list.view')
const store = require('../../../js/store.js')
const SearchFormCollection = require('../../search-form/search-form-all-collection-instance')
const Backbone = require('backbone')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-search-form-selector',
  componentToShow: SearchFormsList,
  initialize: function() {
    DropdownView.prototype.initialize.call(this)
    this.listenTo(this.model, 'change:isOpen', this.handleClose)
  },
  handleClose: function() {
    if (!this.model.get('isOpen')) {
      this.onDestroy()
      this.initializeDropdown()
    }
  },
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = new Backbone.Model({
      currentQuery: this.options.modelForComponent,
      searchForms: SearchFormCollection.getCollection().sort(),
    })
  },
  listenToComponent: function() {
    //override if you need more functionality
  },
})
