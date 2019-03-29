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
/*global define, setTimeout*/
const Marionette = require('marionette')
import React from 'react'
const CustomElements = require('../../js/CustomElements.js')
var metacardDefinitions = require('../singletons/metacard-definitions.js')

module.exports = Marionette.ItemView.extend({
  tagName: CustomElements.register('selection-checkbox'),
  events: {
    'click span': 'handleClick',
  },
  handleClick(e) {
    e.stopPropagation()
    this.check(!this.isSelected)
    this.options.onClick(this.isSelected)
  },
  template() {
    return (
      <React.Fragment>
        <span className="checked fa fa-check-square-o" />
        <span className="not-checked fa fa-square-o" />
      </React.Fragment>
    )
  },
  initialize() {
    this.check(this.options.isSelected)
  },
  check: function(isSelected) {
    this.isSelected = isSelected
    this.$el.toggleClass('is-checked', isSelected)
  },
})
