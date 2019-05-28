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

const DropdownView = require('../dropdown.view')
const template = require('./dropdown.query-status.hbs')
const ComponentView = require('../../query-status/query-status.view.js')

module.exports = DropdownView.extend({
  template,
  className: 'is-query-status',
  componentToShow: ComponentView,
  initialize() {
    DropdownView.prototype.initialize.call(this)
    this.handleSchedule()
    this.listenTo(this.options.modelForComponent, 'change', this.handleSchedule)
  },
  initializeComponentModel() {
    //override if you need more functionality
    this.modelForComponent = this.options.modelForComponent
  },
  listenToComponent() {
    //override if you need more functionality
  },
  handleSchedule() {
    this.$el.toggleClass(
      'is-polling',
      this.options.modelForComponent.get('polling') !== false
    )
  },
})
