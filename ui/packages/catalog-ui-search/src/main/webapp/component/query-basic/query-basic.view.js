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
const _ = require('underscore')
const $ = require('jquery')
const template = require('./query-basic.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const IconHelper = require('../../js/IconHelper.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const properties = require('../../js/properties.js')
const cql = require('../../js/cql.js')
const metacardDefinitions = require('../singletons/metacard-definitions.js')
const sources = require('../singletons/sources-instance.js')
const CQLUtils = require('../../js/CQLUtils.js')
const QuerySettingsView = require('../query-settings/query-settings.view.js')
const QueryTimeView = require('../query-time/query-time.view.js')

function isNested(filter) {
  let nested = false
  filter.filters.forEach(subfilter => {
    nested = nested || subfilter.filters
  })
  return nested
}

function getMatchTypeAttribute() {
  return metacardDefinitions.metacardTypes[properties.basicSearchMatchType]
    ? properties.basicSearchMatchType
    : 'datatype'
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
    typesFound.indexOf(getMatchTypeAttribute()) >= 0
  )
}

// strip extra quotes
const stripQuotes = property => {
  return property.replace(/^"(.+(?="$))"$/, '$1')
}

function isAnyDate(filter) {
  if (!filter.filters) {
    return (
      metacardDefinitions.metacardTypes[stripQuotes(filter.property)].type ===
      'DATE'
    )
  }
  let typesFound = {}
  let valuesFound = {}
  filter.filters.forEach(subfilter => {
    typesFound[subfilter.type] = true
    valuesFound[subfilter.value] = true
  })
  typesFound = Object.keys(typesFound)
  valuesFound = Object.keys(valuesFound)
  if (typesFound.length > 1 || valuesFound.length > 1) {
    return false
  } else {
    const attributes = filter.filters.map(subfilter => subfilter.property)
    return (
      metacardDefinitions.metacardTypes[stripQuotes(attributes[0])].type ===
      'DATE'
    )
  }
}

function handleAnyDateFilter(propertyValueMap, filter) {
  propertyValueMap['anyDate'] = propertyValueMap['anyDate'] || []
  let existingFilter = propertyValueMap['anyDate'].filter(
    anyDateFilter =>
      anyDateFilter.type ===
      (filter.filters ? filter.filters[0].type : filter.type)
  )[0]
  if (!existingFilter) {
    existingFilter = {
      property: [],
    }
    propertyValueMap['anyDate'].push(existingFilter)
  }
  existingFilter.property = existingFilter.property.concat(
    filter.filters
      ? filter.filters.map(subfilter => stripQuotes(subfilter.property))
      : [stripQuotes(filter.property)]
  )
  existingFilter.type = filter.filters ? filter.filters[0].type : filter.type
  existingFilter.value = filter.filters ? filter.filters[0].value : filter.value

  if (existingFilter.type === 'DURING') {
    existingFilter.from = filter.filters ? filter.filters[0].from : filter.from
    existingFilter.to = filter.filters ? filter.filters[0].to : filter.to
  }
}

function translateFilterToBasicMap(filter) {
  const propertyValueMap = {}
  let downConversion = false

  if (!filter.filters && isAnyDate(filter)) {
    handleAnyDateFilter(propertyValueMap, filter)
  }

  if (filter.filters) {
    filter.filters.forEach(filter => {
      if (!filter.filters && isAnyDate(filter)) {
        handleAnyDateFilter(propertyValueMap, filter)
      } else if (!filter.filters) {
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
        handleAnyDateFilter(propertyValueMap, filter)
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

function getFilterTree(model) {
  if (typeof model.get('filterTree') === 'object') {
    return model.get('filterTree')
  }
  return cql.simplify(cql.read(model.get('cql')))
}

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('query-basic'),
  modelEvents: {},
  events: {
    'click .editor-edit': 'edit',
    'click .editor-cancel': 'cancel',
    'click .editor-save': 'save',
  },
  regions: {
    basicSettings: '.basic-settings',
    basicText: '.basic-text',
    basicTextMatch: '.basic-text-match',
    basicTime: '.basic-time-details',
    basicLocation: '.basic-location',
    basicLocationSpecific: '.basic-location-specific',
    basicType: '.basic-type',
    basicTypeSpecific: '.basic-type-specific',
  },
  ui: {},
  filter: undefined,
  onBeforeShow() {
    this.model = this.model._cloneOf
      ? store.getQueryById(this.model._cloneOf)
      : this.model
    const filter = getFilterTree(this.model)
    const translationToBasicMap = translateFilterToBasicMap(filter)
    this.filter = translationToBasicMap.propertyValueMap
    this.handleDownConversion(translationToBasicMap.downConversion)
    this.setupSettings()
    this.setupTime()
    this.setupTextInput()
    this.setupTextMatchInput()

    this.setupLocation()
    this.setupLocationInput()
    this.setupType()
    this.setupTypeSpecific()

    this.listenTo(
      this.basicLocation.currentView.model,
      'change:value',
      this.handleLocationValue
    )
    this.listenTo(
      this.basicType.currentView.model,
      'change:value',
      this.handleTypeValue
    )

    this.handleLocationValue()
    this.handleTypeValue()
    this.edit()
  },
  setupSettings() {
    this.basicSettings.show(
      new QuerySettingsView({
        model: this.model,
      })
    )
  },
  setupTime() {
    this.basicTime.show(
      new QueryTimeView({
        model: this.model,
        filter: this.filter,
      })
    )
  },
  setupTypeSpecific() {
    let currentValue = []
    if (this.filter['metadata-content-type']) {
      currentValue = _.uniq(
        this.filter['metadata-content-type'].map(subfilter => subfilter.value)
      )
    }
    this.basicTypeSpecific.show(
      new PropertyView({
        model: new Property({
          enumFiltering: true,
          showValidationIssues: false,
          enumMulti: true,
          enum: sources.toJSON().reduce(
            (enumArray, source) => {
              source.contentTypes.forEach(contentType => {
                if (
                  contentType.value &&
                  enumArray.filter(option => option.value === contentType.value)
                    .length === 0
                ) {
                  enumArray.push({
                    label: contentType.name,
                    value: contentType.value,
                    class:
                      'icon ' + IconHelper.getClassByName(contentType.value),
                  })
                }
              })
              return enumArray
            },
            metacardDefinitions.enums.datatype
              ? metacardDefinitions.enums.datatype.map(value => ({
                  label: value,
                  value,
                  class: 'icon ' + IconHelper.getClassByName(value),
                }))
              : []
          ),
          value: [currentValue],
          id: 'Types',
        }),
      })
    )
  },
  setupType() {
    let currentValue = 'any'
    if (this.filter['metadata-content-type']) {
      currentValue = 'specific'
    }
    this.basicType.show(
      new PropertyView({
        model: new Property({
          value: [currentValue],
          id: 'Match Types',
          radio: [
            {
              label: 'Any',
              value: 'any',
            },
            {
              label: 'Specific',
              value: 'specific',
            },
          ],
        }),
      })
    )
  },
  setupLocation() {
    let currentValue = 'any'
    if (this.filter.anyGeo) {
      currentValue = 'specific'
    }
    this.basicLocation.show(
      new PropertyView({
        model: new Property({
          value: [currentValue],
          id: 'Located',
          radio: [
            {
              label: 'Anywhere',
              value: 'any',
            },
            {
              label: 'Somewhere Specific',
              value: 'specific',
            },
          ],
        }),
      })
    )
  },
  setupLocationInput() {
    let currentValue = ''
    if (this.filter.anyGeo) {
      currentValue = this.filter.anyGeo[0]
    }
    this.basicLocationSpecific.show(
      new PropertyView({
        model: new Property({
          value: [currentValue],
          id: 'Location',
          type: 'LOCATION',
        }),
      })
    )
  },
  handleTypeValue() {
    const type = this.basicType.currentView.model.getValue()[0]
    this.$el.toggleClass('is-type-any', type === 'any')
    this.$el.toggleClass('is-type-specific', type === 'specific')
  },
  handleLocationValue() {
    const location = this.basicLocation.currentView.model.getValue()[0]
    this.$el.toggleClass('is-location-any', location === 'any')
    this.$el.toggleClass('is-location-specific', location === 'specific')
  },
  setupTextMatchInput() {
    this.basicTextMatch.show(
      new PropertyView({
        model: new Property({
          value: [
            this.filter.anyText && this.filter.anyText[0].type === 'LIKE'
              ? 'LIKE'
              : 'ILIKE',
          ],
          id: 'Match Case',
          placeholder: 'Text to search for.  Use "*" for wildcard.',
          radio: [
            {
              label: 'Yes',
              value: 'LIKE',
            },
            {
              label: 'No',
              value: 'ILIKE',
            },
          ],
        }),
      })
    )
  },
  setupTextInput() {
    this.basicText.show(
      new PropertyView({
        model: new Property({
          value: [this.filter.anyText ? this.filter.anyText[0].value : ''],
          id: 'Text',
          placeholder: 'Text to search for.  Use "*" for wildcard.',
        }),
      })
    )
  },
  turnOffEdit() {
    this.regionManager.forEach(region => {
      if (region.currentView && region.currentView.turnOffEditing) {
        region.currentView.turnOffEditing()
      }
    })
  },
  edit() {
    this.$el.addClass('is-editing')
    this.regionManager.forEach(region => {
      if (region.currentView && region.currentView.turnOnEditing) {
        region.currentView.turnOnEditing()
      }
    })
    const tabbable = _.filter(
      this.$el.find('[tabindex], input, button'),
      element => element.offsetParent !== null
    )
    if (tabbable.length > 0) {
      $(tabbable[0]).focus()
    }
  },
  focus() {
    this.basicText.currentView.focus()
  },
  cancel() {
    this.$el.removeClass('is-editing')
    this.onBeforeShow()
  },
  handleDownConversion(downConversion) {
    this.$el.toggleClass('is-down-converted', downConversion)
  },
  save() {
    this.$el.removeClass('is-editing')
    this.basicSettings.currentView.saveToModel()

    const filter = this.constructFilter()
    const generatedCQL = CQLUtils.transformFilterToCQL(filter)
    this.model.set({
      filterTree: filter,
      cql: generatedCQL,
    })
  },
  isValid() {
    return this.basicSettings.currentView.isValid()
  },
  constructFilter() {
    const filters = []

    const text = this.basicText.currentView.model.getValue()[0]
    if (text !== '') {
      const matchCase = this.basicTextMatch.currentView.model.getValue()[0]
      filters.push(CQLUtils.generateFilter(matchCase, 'anyText', text))
    }

    this.basicTime.currentView.constructFilter().forEach(timeFilter => {
      filters.push(timeFilter)
    })

    const locationSpecific = this.basicLocation.currentView.model.getValue()[0]
    const location = this.basicLocationSpecific.currentView.model.getValue()[0]
    const locationFilter = CQLUtils.generateFilter(
      undefined,
      'anyGeo',
      location
    )
    if (locationSpecific === 'specific' && locationFilter) {
      filters.push(locationFilter)
    }

    const types = this.basicType.currentView.model.getValue()[0]
    const typesSpecific = this.basicTypeSpecific.currentView.model.getValue()[0]
    if (types === 'specific' && typesSpecific.length !== 0) {
      const typeFilter = {
        type: 'OR',
        filters: typesSpecific
          .map(specificType =>
            CQLUtils.generateFilter(
              'ILIKE',
              'metadata-content-type',
              specificType
            )
          )
          .concat(
            typesSpecific.map(specificType =>
              CQLUtils.generateFilter(
                'ILIKE',
                getMatchTypeAttribute(),
                specificType
              )
            )
          ),
      }
      filters.push(typeFilter)
    }

    if (filters.length === 0) {
      filters.unshift(CQLUtils.generateFilter('ILIKE', 'anyText', '*'))
    }

    return {
      type: 'AND',
      filters,
    }
  },
  setDefaultTitle() {
    const text = this.basicText.currentView.model.getValue()[0]
    let title
    if (text === '') {
      title = this.model.get('cql')
    } else {
      title = text
    }
    this.model.set('title', title)
  },
})
