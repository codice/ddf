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
const Marionette = require('marionette')
const template = require('./between-time.hbs')
const CustomElements = require('../../js/CustomElements.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('between-time'),
  regions: {
    betweenFrom: '.between-from',
    betweenTo: '.between-to',
  },
  onBeforeShow() {
    this.setupTimeBetween()
    this.turnOnEditing()
    this.listenTo(
      this.betweenFrom.currentView.model,
      'change:value',
      this.updateModelValue
    )
    this.listenTo(
      this.betweenTo.currentView.model,
      'change:value',
      this.updateModelValue
    )
    this.updateModelValue()
  },
  turnOnEditing() {
    this.$el.addClass('is-editing')
    this.regionManager.forEach(function(region) {
      if (region.currentView && region.currentView.turnOnEditing) {
        region.currentView.turnOnEditing()
      }
    })
  },
  getViewValue() {
    const from = this.betweenFrom.currentView.model.getValue()[0]
    const to = this.betweenTo.currentView.model.getValue()[0]
    return `${from}/${to}`
  },
  parseValue(value) {
    if (value === null || value === undefined || !value.includes('/')) {
      return
    }
    const dates = value.split('/')
    const from = dates[0]
    const to = dates[1]
    return {
      from,
      to,
    }
  },
  getModelValue() {
    const currentValue = this.model.toJSON().value[0]
    return this.parseValue(currentValue)
  },
  updateModelValue() {
    if (this.model === undefined) {
      return
    }
    this.model.setValue([this.getViewValue()])
  },
  getOptionsValue() {
    return this.parseValue(this.options.value)
  },
  getStartingValue() {
    if (this.model !== undefined) {
      return this.getModelValue()
    } else if (this.options !== undefined) {
      return this.getOptionsValue()
    }
  },
  setupTimeBetween() {
    const { from = '', to = '' } = this.getStartingValue() || {}
    this.betweenFrom.show(
      new PropertyView({
        model: new Property({
          value: [from],
          id: 'From',
          placeholder: 'Limit search to after this time.',
          type: 'DATE',
        }),
      })
    )
    this.betweenTo.show(
      new PropertyView({
        model: new Property({
          value: [to],
          id: 'To',
          placeholder: 'Limit search to before this time.',
          type: 'DATE',
        }),
      })
    )
  },
  isValid() {
    const value = this.getModelValue()
    return value.from && value.to && new Date(value.from) < new Date(value.to)
  },
})
