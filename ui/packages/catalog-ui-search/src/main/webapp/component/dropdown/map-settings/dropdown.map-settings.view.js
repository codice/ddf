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
/*global require*/
const React = require('react')
var Marionette = require('marionette')
var DropdownView = require('../dropdown.view')
var template = require('./dropdown.map-settings.hbs')
var SettingsView = require('../../map-settings/map-settings.view.js')
var user = require('../../singletons/user-instance.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-mapSettings',
  componentToShow: SettingsView,
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = user.get('user>preferences')
  },
})