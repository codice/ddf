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

const user = require('../../singletons/user-instance.js')
const DropdownView = require('../dropdown.view')
const template = require('./dropdown.login-form.hbs')
const CustomElements = require('../../../js/CustomElements.js')
const ComponentView = require('component/login-form/login-form.view')

const getName = function(user) {
  if (user.isGuestUser()) {
    return 'Sign In'
  }

  return user.get('username')
}

module.exports = DropdownView.extend({
  template,
  tagName: CustomElements.register('login-dropdown'),
  componentToShow: ComponentView,
  initializeComponentModel() {
    this.modelForComponent = user
    this.model.set('value', getName(this.modelForComponent.get('user')))
  },
  listenToComponent() {
    this.listenTo(this.modelForComponent, 'change', () => {
      this.model.set('value', getName(this.modelForComponent.get('user')))
    })
  },
  isCentered: true,
  getCenteringElement() {
    return this.el
  },
  hasTail: true,
})
