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
const _ = require('underscore')
const user = require('../../singletons/user-instance.js')
const Dropdown = require('../dropdown')
const DropdownView = require('../dropdown.view')
const template = require('./dropdown.login-form.hbs')
const CustomElements = require('../../../js/CustomElements.js')
const ComponentView = require('component/login-form/login-form.view')

var getName = function(user) {
  if (user.isGuestUser()) {
    return 'Sign In'
  }

  return user.get('username')
}

module.exports = DropdownView.extend({
  template: template,
  tagName: CustomElements.register('login-dropdown'),
  componentToShow: ComponentView,
  initializeComponentModel: function() {
    this.modelForComponent = user
    this.model.set('value', getName(this.modelForComponent.get('user')))
  },
  listenToComponent: function() {
    this.listenTo(
      this.modelForComponent,
      'change',
      function() {
        this.model.set('value', getName(this.modelForComponent.get('user')))
      }.bind(this)
    )
  },
  isCentered: true,
  getCenteringElement: function() {
    return this.el
  },
  hasTail: true,
})
