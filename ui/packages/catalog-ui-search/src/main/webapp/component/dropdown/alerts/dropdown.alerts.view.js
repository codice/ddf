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

const _ = require('underscore')
const $ = require('jquery')
const DropdownView = require('../dropdown.view')
const template = require('./dropdown.alerts.hbs')
const ComponentView = require('../../alerts/alerts.view.js')
const user = require('../../singletons/user-instance.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-alerts is-button',
  componentToShow: ComponentView,
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = user
      .get('user')
      .get('preferences')
      .get('alerts')
    this.handleAlerts()
  },
  listenToComponent: function() {
    this.listenTo(this.modelForComponent, 'add remove reset', this.handleAlerts)
  },
  handleAlerts: function() {
    this.$el.toggleClass('has-alerts', this.modelForComponent.length > 0)
  },
  serializeData: function() {
    return this.modelForComponent.toJSON()
  },
  isCentered: true,
  getCenteringElement: function() {
    return this.el.querySelector('.notification-icon')
  },
  hasTail: true,
})
