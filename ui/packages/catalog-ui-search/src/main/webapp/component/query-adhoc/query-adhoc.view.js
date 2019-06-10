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

const Marionette = require('marionette')
const template = require('./query-adhoc.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const PropertyView = require('../property/property.view.js')
const properties = require('../../js/properties.js')
const CQLUtils = require('../../js/CQLUtils.js')
const cql = require('../../js/cql.js')

function isNested(filter) {
  let nested = false
  filter.filters.forEach(subfilter => {
    nested = nested || subfilter.filters
  })
  return nested
}

function isTypeLimiter(filter) {
  let typesFound = {}
  filter.filters.forEach(subfilter => {
    typesFound[CQLUtils.getProperty(subfilter)] = true
  })
  typesFound = Object.keys(typesFound)
  return (
    typesFound.length === 2 &&
    typesFound.indexOf('metadata-content-type') >= 0 &&
    typesFound.indexOf('datatype') >= 0
  )
}

function isAnyDate(filter) {
  const propertiesToCheck = [
    'created',
    'modified',
    'effective',
    'metacard.created',
    'metacard.modified',
  ]
  const typesFound = {}
  const valuesFound = {}
  if (filter.filters.length === propertiesToCheck.length) {
    filter.filters.forEach(subfilter => {
      typesFound[subfilter.type] = true
      valuesFound[subfilter.value] = true
      const indexOfType = propertiesToCheck.indexOf(
        CQLUtils.getProperty(subfilter)
      )
      if (indexOfType >= 0) {
        propertiesToCheck.splice(indexOfType, 1)
      }
    })
    return (
      propertiesToCheck.length === 0 &&
      Object.keys(typesFound).length === 1 &&
      Object.keys(valuesFound).length === 1
    )
  }
  return false
}

function translateFilterToBasicMap(filter) {
  const propertyValueMap = {}
  let downConversion = false
  if (filter.filters) {
    filter.filters.forEach(filter => {
      if (!filter.filters) {
        propertyValueMap[CQLUtils.getProperty(filter)] =
          propertyValueMap[CQLUtils.getProperty(filter)] || []
        if (
          propertyValueMap[CQLUtils.getProperty(filter)].filter(
            existingFilter => existingFilter.type === filter.type
          ).length === 0
        ) {
          propertyValueMap[CQLUtils.getProperty(filter)].push(filter)
        }
      } else if (!isNested(filter) && isAnyDate(filter)) {
        propertyValueMap['anyDate'] = propertyValueMap['anyDate'] || []
        if (
          propertyValueMap['anyDate'].filter(
            existingFilter => existingFilter.type === filter.filters[0].type
          ).length === 0
        ) {
          propertyValueMap['anyDate'].push(filter.filters[0])
        }
      } else if (!isNested(filter) && isTypeLimiter(filter)) {
        propertyValueMap[CQLUtils.getProperty(filter.filters[0])] =
          propertyValueMap[CQLUtils.getProperty(filter.filters[0])] || []
        filter.filters.forEach(subfilter => {
          propertyValueMap[CQLUtils.getProperty(filter.filters[0])].push(
            subfilter
          )
        })
      } else {
        downConversion = true
      }
    })
  } else {
    propertyValueMap[CQLUtils.getProperty(filter)] =
      propertyValueMap[CQLUtils.getProperty(filter)] || []
    propertyValueMap[CQLUtils.getProperty(filter)].push(filter)
  }
  return {
    propertyValueMap,
    downConversion,
  }
}

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('query-adhoc'),
  modelEvents: {},
  events: {
    'click .editor-edit': 'turnOnEditing',
    'click .editor-cancel': 'cancel',
    'click .editor-save': 'save',
  },
  regions: {
    textField: '.properties-text',
  },
  ui: {},
  focus() {
    this.textField.currentView.focus()
  },
  initialize() {
    this.model = this.model._cloneOf
      ? store.getQueryById(this.model._cloneOf)
      : this.model
  },
  onBeforeShow() {
    this.setupTextField()
    this.turnOnEditing()
  },
  setupTextField() {
    const translationToBasicMap = translateFilterToBasicMap(
      cql.simplify(cql.read(this.model.get('cql')))
    )
    this.textField.show(
      PropertyView.getPropertyView({
        id: 'Text',
        value: [
          translationToBasicMap.propertyValueMap.anyText
            ? translationToBasicMap.propertyValueMap.anyText[0].value
            : '',
        ],
        label: '',
        type: 'STRING',
        showValidationIssues: false,
        showLabel: false,
        placeholder: 'Search ' + properties.branding + ' ' + properties.product,
      })
    )
    this.textField.currentView.$el.keyup(event => {
      switch (event.keyCode) {
        case 13:
          this.$el.trigger('saveQuery.' + CustomElements.getNamespace())
          break
        default:
          break
      }
    })
    this.listenTo(
      this.textField.currentView.model,
      'change:value',
      this.saveToModel
    )
  },
  turnOnEditing() {
    this.$el.addClass('is-editing')
    this.regionManager.forEach(region => {
      if (region.currentView && region.currentView.turnOnEditing) {
        region.currentView.turnOnEditing()
      }
    })
    this.focus()
  },
  edit() {
    this.$el.addClass('is-editing')
    this.turnOnEditing()
  },
  cancel() {
    this.$el.removeClass('is-editing')
    this.onBeforeShow()
  },
  saveToModel() {
    const text = this.textField.currentView.model.getValue()[0]
    let cql
    if (text.length === 0) {
      cql = CQLUtils.generateFilter('ILIKE', 'anyText', '*')
    } else {
      cql = CQLUtils.generateFilter('ILIKE', 'anyText', text)
    }
    this.model.set('cql', CQLUtils.transformFilterToCQL(cql))
  },
  save() {
    this.$el.find('form')[0].submit()
    this.saveToModel()
  },
  isValid() {
    return this.textField.currentView.isValid()
  },
  setDefaultTitle() {
    let title = this.textField.currentView.model.getValue()[0]
    if (title.length === 0) {
      title = '*'
    }
    this.model.set('title', title)
  },
})
