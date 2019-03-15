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
const template = require('./dropdown.search-settings.hbs')
const ComponentView = require('../../search-settings/search-settings.view.js')
const CustomElements = require('../../../js/CustomElements.js')

import React from 'react'
import SearchSettings from '../../../react-component/presentation/search-settings/search-settings'
const store = require('../../../js/store.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-search-settings',
  componentToShow: Marionette.LayoutView.extend({
    template() {
      return <SearchSettings
        onClose={() => {
          this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
        }}
      />
    }
  }),
  initialize: function() {
    DropdownView.prototype.initialize.call(this)
  },
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = this.options.modelForComponent
  },
  listenToComponent: function() {
    //override if you need more functionality
  },
})
