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
const template = require('./input-bulk.hbs')
const InputView = require('../input.view.js')
const MultivalueView = require('../../multivalue/multivalue.view.js')
const DropdownView = require('../../dropdown/dropdown.view.js')
const Common = require('../../../js/Common.js')
const moment = require('moment')
const user = require('../../singletons/user-instance.js')

function sortNoValueToTop(a, b) {
  if (a.value === 'bulkDefault') {
    return -1
  }
  if (b.value === 'bulkDefault') {
    return 1
  }
  if (a.value === 'bulkCustom') {
    return -1
  }
  if (b.value === 'bulkCustom') {
    return 1
  }
  if (a.hasNoValue === true && b.hasNoValue === false) {
    return -1
  }
  if (b.hasNoValue === true && a.hasNoValue === false) {
    return 1
  }
  return 0
}

module.exports = InputView.extend({
  className: 'is-bulk',
  template,
  regions: {
    enumRegion: '.enum-region',
    otherInput: '.input-other',
  },
  events: {},
  listenForChange() {
    this.listenTo(
      this.enumRegion.currentView.model,
      'change:value',
      function() {
        const value = this.enumRegion.currentView.model.get('value')[0]
        switch (value) {
          case 'bulkDefault':
            this.model.revert()
            break
          case 'bulkCustom':
            this.model.setValue(this.otherInput.currentView.model.getValue())
            this.model.set('hasChanged', true)
            break
          default:
            this.model.setValue(value)
            this.model.set('hasChanged', true)
            break
        }
        this.handleChange()
      }
    )
    this.listenTo(
      this.otherInput.currentView.model,
      'change:value',
      function() {
        this.model.setValue(this.otherInput.currentView.model.getValue())
        if (!this.model.isHomogeneous()) {
          this.model.set('hasChanged', true)
        }
        this.handleChange()
      }
    )
  },
  onRender() {
    this.initializeDropdown()
    InputView.prototype.onRender.call(this)
    this.handleOther()
    this.handleBulk()
  },
  serializeData() {
    // need duplicate (usually toJSON returns a side-effect free version, but this has a nested object that isn't using backbone associations)
    const modelJSON = Common.duplicate(this.model.toJSON())
    const type = this.model.getCalculatedType()
    modelJSON.isThumbnail = type === 'thumbnail'
    switch (type) {
      case 'date':
        modelJSON.values = _.map(modelJSON.values, valueInfo => {
          if (valueInfo.hasNoValue) {
            valueInfo.value[0] = 'No Value'
          } else {
            valueInfo.value = valueInfo.value.map(value =>
              user.getUserReadableDateTime(value)
            )
            return valueInfo
          }
          return valueInfo
        })
        break
      case 'thumbnail':
        modelJSON.values = _.map(modelJSON.values, valueInfo => {
          if (valueInfo.hasNoValue) {
            valueInfo.value[0] = 'No Value'
          } else {
            valueInfo.value = valueInfo.value.map(value =>
              Common.getImageSrc(value)
            )
            return valueInfo
          }
          return valueInfo
        })
        break
      default:
        modelJSON.values = _.map(modelJSON.values, valueInfo => {
          if (valueInfo.hasNoValue) {
            valueInfo.value[0] = 'No Value'
          }
          return valueInfo
        })
        break
    }
    modelJSON.values.sort(sortNoValueToTop)
    return modelJSON
  },
  initializeDropdown() {
    const enumValues = [
      {
        label: 'Multiple Values',
        value: 'bulkDefault',
        help:
          'This is the default.  Selecting it will cause no changes to the results, allowing them to keep their multiple values.',
      },
      {
        label: 'Custom',
        value: 'bulkCustom',
        help: 'Select this to enter a custom value.',
      },
    ]
    const type = this.model.getCalculatedType()
    _.forEach(this.model.get('values'), valueInfo => {
      let value = valueInfo.value
      let label = valueInfo.hasNoValue ? 'No Value' : value
      const type = this.model.getCalculatedType()
      if (!valueInfo.hasNoValue) {
        switch (type) {
          case 'date':
            label = label.map(text => user.getUserReadableDateTime(text))
            value = value.map(text => moment(text))
            break
          default:
            break
        }
      }
      if (type !== 'thumbnail' || valueInfo.hasNoValue) {
        enumValues.push({
          label,
          value,
          hits: valueInfo.hits,
          hasNoValue: valueInfo.hasNoValue,
          isThumbnail: type === 'thumbnail',
        })
      }
    })
    enumValues.sort(sortNoValueToTop)
    this.enumRegion.show(
      DropdownView.createSimpleDropdown({
        list: enumValues,
        defaultSelection: ['bulkDefault'],
        hasFiltering: true,
      })
    )
  },
  onBeforeShow() {
    this.otherInput.show(
      new MultivalueView({
        model: this.model.isHomogeneous() ? this.model : this.model.clone(), // in most cases this view is the real input, except for the heterogenous case
      })
    )
    this.otherInput.currentView.listenTo(this.model, 'change:isEditing', () => {
      this.otherInput.currentView.model.set(
        'isEditing',
        this.model.get('isEditing')
      )
    })
    if (!this.model.isHomogeneous() && this.model.isMultivalued()) {
      this.otherInput.currentView.addNewValue()
    }
  },
  handleChange() {
    this.handleOther()
  },
  handleOther() {
    if (this.enumRegion.currentView.model.get('value')[0] === 'bulkCustom') {
      this.$el.addClass('is-other')
    } else {
      this.$el.removeClass('is-other')
    }
  },
  handleBulk() {
    if (this.model.isHomogeneous()) {
      this.turnOffBulk()
    }
  },
  turnOffBulk() {
    this.$el.addClass('is-homogeneous')
  },
})
