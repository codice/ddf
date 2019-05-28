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

const template = require('./input-location.hbs')
const InputView = require('../input.view')
const LocationView = require('../../location-old/location-old.view.js')

module.exports = InputView.extend({
  template,
  events: {
    'click .input-revert': 'revert',
  },
  regions: {
    locationRegion: '.location-region',
  },
  serializeData() {
    const value = this.model.get('value')
    return {
      label: value,
    }
  },
  onRender() {
    this.initializeRadio()
    InputView.prototype.onRender.call(this)
  },
  listenForChange() {
    this.listenTo(
      this.locationRegion.currentView.model,
      'change',
      this.triggerChange
    )
  },
  isValid() {
    return this.locationRegion.currentView.isValid()
  },
  initializeRadio() {
    this.locationRegion.show(
      new LocationView({
        model: this.model,
      })
    )
  },
  handleReadOnly() {
    this.$el.toggleClass('is-readOnly', this.model.isReadOnly())
  },
  handleValue() {
    this.locationRegion.currentView.model.set('value', this.model.get('value'))
  },
  getCurrentValue() {
    return this.locationRegion.currentView.getCurrentValue()
  },
  triggerChange() {
    this.model.set('value', this.getCurrentValue())
  },
})
