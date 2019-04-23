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
const template = require('./confirmation.query.hbs')
const ConfirmationView = require('../confirmation.view')
const QuerySelectDropdown = require('../../dropdown/query-select/dropdown.query-select.view.js')
const DropdownModel = require('../../dropdown/dropdown.js')

module.exports = ConfirmationView.extend({
  template: template,
  className: 'is-query',
  modelEvents: {
    'change:choice': 'close',
  },
  events: {
    click: 'handleOutsideClick',
    'click .confirmation-no': 'handleNo',
    'click .confirmation-replace': 'handleReplace',
    'click .confirmation-new': 'handleNew',
    mousedown: 'handleMousedown',
  },
  regions: {
    querySelect: '.confirmation-query',
  },
  handleMousedown: function(e) {
    e.stopPropagation()
    this.querySelect.currentView.model.close()
  },
  onRender: function() {
    this.querySelect.show(
      new QuerySelectDropdown({
        model: new DropdownModel({
          value: undefined,
        }),
        hideActions: true,
        dropdownCompanionBehaviors: {
          navigation: {},
        },
      })
    )
    this.listenTo(
      this.querySelect.currentView.model,
      'change:value',
      this.handleValue
    )
    ConfirmationView.prototype.onRender.call(this)
    this.handleValue()
  },
  handleValue: function() {
    const value = this.getValue()
    this.$el.toggleClass('has-value', value !== undefined && value !== '')
  },
  getValue: function() {
    return this.querySelect.currentView.model.get('value')
  },
  handleNew: function() {
    this.model.makeChoice(true)
  },
  handleReplace: function() {
    this.model.makeChoice(this.getValue())
  },
})
