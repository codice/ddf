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

const template = require('./input-radio.hbs')
const InputView = require('../input.view')
const RadioView = require('../../radio/radio.view.js')

module.exports = InputView.extend({
  template,
  events: {
    'click .input-revert': 'revert',
  },
  regions: {
    radioRegion: '.radio-region',
  },
  listenForChange() {
    this.$el.on('click', () => {
      this.model.set('value', this.getCurrentValue())
    })
  },
  serializeData() {
    const value = this.model.get('value')
    const choice = this.model
      .get('property')
      .get('radio')
      .filter(
        choice =>
          JSON.stringify(choice.value) === JSON.stringify(value) ||
          JSON.stringify(choice) === JSON.stringify(value)
      )[0]
    return {
      label: choice ? choice.label : value,
    }
  },
  onRender() {
    this.initializeRadio()
    InputView.prototype.onRender.call(this)
  },
  initializeRadio() {
    this.radioRegion.show(
      RadioView.createRadio({
        options: this.model
          .get('property')
          .get('radio')
          .map(value => {
            if (value.label) {
              return {
                label: value.label,
                value: value.value,
                title: value.title,
              }
            } else {
              return {
                label: value,
                value,
                title: value.title,
              }
            }
          }),
        defaultValue: [this.model.get('value')],
      })
    )
  },
  handleReadOnly() {
    this.$el.toggleClass('is-readOnly', this.model.isReadOnly())
  },
  handleValue() {
    this.radioRegion.currentView.model.set('value', this.model.get('value'))
  },
  getCurrentValue() {
    const currentValue = this.radioRegion.currentView.model.get('value')
    return currentValue
  },
})
