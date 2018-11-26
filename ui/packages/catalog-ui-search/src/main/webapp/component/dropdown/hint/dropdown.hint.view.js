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
const DropdownView = require('../dropdown.view')
const template = require('./dropdown.hint.hbs')
const ComponentView = require('../../hint/hint.view.js')
const HintDropdownCompanion = require('./dropdown.companion.hint.view')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-hint',
  componentToShow: ComponentView,
  initializeDropdown: function() {
    this.dropdownCompanion = HintDropdownCompanion.getNewCompanionView(this)
  },
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = this.options.modelForComponent
  },
  listenToComponent: function() {},
})
