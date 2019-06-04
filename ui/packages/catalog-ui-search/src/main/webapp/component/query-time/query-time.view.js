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
/* eslint-disable no-var */

const Marionette = require('marionette')
const template = require('./query-time.hbs')
const CustomElements = require('../../js/CustomElements.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const properties = require('../../js/properties.js')
const CQLUtils = require('../../js/CQLUtils.js')
const metacardDefinitions = require('../singletons/metacard-definitions.js')
const RelativeTimeView = require('../relative-time/relative-time.view.js')
const BetweenTimeView = require('../between-time/between-time.view.js')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('query-time'),
  regions: {
    basicTime: '.basic-time',
    basicTimeField: '.basic-time-field',
    basicTemporalSelections: '.basic-temporal-selections',
    basicTimeBefore: '.basic-time-before',
    basicTimeAfter: '.basic-time-after',
    basicTimeBetween: '.basic-time-between',
    basicTimeRelative: '.basic-time-relative',
  },
  onBeforeShow() {
    this.turnOnEditing()
    this.setupTimeInput()
    this.setupTemporalSelections()
    this.setupTimeBefore()
    this.setupTimeAfter()
    this.setupTimeBetween()
    this.setupTimeRelative()
    this.listenTo(
      this.basicTime.currentView.model,
      'change:value',
      this.handleTimeRangeValue
    )
    this.handleTimeRangeValue()
  },
  turnOnEditing() {
    this.$el.addClass('is-editing')
    this.regionManager.forEach(region => {
      if (region.currentView && region.currentView.turnOnEditing) {
        region.currentView.turnOnEditing()
      }
    })
  },
  constructFilter() {
    const filters = []
    const timeRange = this.basicTime.currentView.model.getValue()[0]
    let timeSelection = this.basicTemporalSelections.currentView.model.getValue()[0]
    timeSelection = !timeSelection.length ? undefined : timeSelection
    let timeBefore, timeAfter, timeDuring, relativeFunction
    switch (timeRange) {
      case 'before':
        timeBefore = this.basicTimeBefore.currentView.model.getValue()[0]
        break
      case 'after':
        timeAfter = this.basicTimeAfter.currentView.model.getValue()[0]
        break
      case 'between':
        timeDuring = this.basicTimeBetween.currentView.getViewValue()
        break
      case 'relative':
        relativeFunction = this.basicTimeRelative.currentView.getViewValue()
        break
    }
    if (timeDuring && timeSelection) {
      const timeFilter = {
        type: 'OR',
        filters: timeSelection.map(selection =>
          CQLUtils.generateFilter('DURING', selection, timeDuring)
        ),
      }
      filters.push(timeFilter)
    }
    if (timeBefore && timeSelection) {
      var timeFilter = {
        type: 'OR',
        filters: timeSelection.map(selection =>
          CQLUtils.generateFilter('BEFORE', selection, timeBefore)
        ),
      }
      filters.push(timeFilter)
    }
    if (timeAfter && timeSelection) {
      var timeFilter = {
        type: 'OR',
        filters: timeSelection.map(selection =>
          CQLUtils.generateFilter('AFTER', selection, timeAfter)
        ),
      }
      filters.push(timeFilter)
    }
    if (relativeFunction && timeSelection) {
      const timeDuration = {
        type: 'OR',
        filters: timeSelection.map(selection =>
          CQLUtils.generateFilter('=', selection, relativeFunction)
        ),
      }
      filters.push(timeDuration)
    }
    return filters
  },
  handleTimeRangeValue() {
    const timeRange = this.basicTime.currentView.model.getValue()[0]
    this.$el.toggleClass('is-timeRange-any', timeRange === 'any')
    this.$el.toggleClass('is-timeRange-before', timeRange === 'before')
    this.$el.toggleClass('is-timeRange-after', timeRange === 'after')
    this.$el.toggleClass('is-timeRange-between', timeRange === 'between')
    this.$el.toggleClass('is-timeRange-relative', timeRange === 'relative')
  },
  setupTemporalSelections() {
    const definitions = metacardDefinitions.sortedMetacardTypes
      .filter(definition => !definition.hidden && definition.type === 'DATE')
      .map(definition => ({
        label: definition.alias || definition.id,
        value: definition.id,
      }))

    let value = properties.basicSearchTemporalSelectionDefault
      ? [properties.basicSearchTemporalSelectionDefault]
      : [[]]

    if (this.options.filter.anyDate) {
      value = [this.options.filter.anyDate[0].property]
    }

    value = [
      value[0].filter(
        attribute => !metacardDefinitions.metacardTypes[attribute].hidden
      ),
    ]

    this.basicTemporalSelections.show(
      new PropertyView({
        model: new Property({
          enumFiltering: true,
          enumMulti: true,
          enum: definitions,
          isEditing: true,
          value,
          id: 'Apply Time Range To',
        }),
      })
    )
  },
  setupTimeBefore() {
    let currentBefore = ''
    if (this.options.filter.anyDate) {
      this.options.filter.anyDate.forEach(subfilter => {
        if (subfilter.type === 'BEFORE') {
          currentBefore = subfilter.value
        }
      })
    }

    this.basicTimeBefore.show(
      new PropertyView({
        model: new Property({
          value: [currentBefore],
          id: 'Before',
          placeholder: 'Limit search to before this time.',
          type: 'DATE',
        }),
      })
    )
  },
  setupTimeAfter() {
    let currentAfter = ''

    if (this.options.filter.anyDate) {
      this.options.filter.anyDate.forEach(subfilter => {
        if (subfilter.type === 'AFTER') {
          currentAfter = subfilter.value
        }
      })
    }

    this.basicTimeAfter.show(
      new PropertyView({
        model: new Property({
          value: [currentAfter],
          id: 'After',
          placeholder: 'Limit search to after this time.',
          type: 'DATE',
        }),
      })
    )
  },
  setupTimeBetween() {
    let value = ''

    // Pre-fill the last edited value or the load value from query
    if (this.options.filter.anyDate) {
      this.options.filter.anyDate.forEach(subfilter => {
        if (subfilter.type === 'DURING') {
          value = `${subfilter.from}/${subfilter.to}`
        }
      })
    }

    this.basicTimeBetween.show(
      new BetweenTimeView({
        value,
      })
    )
  },
  setupTimeInput() {
    let currentValue = 'any'
    if (this.options.filter.anyDate) {
      if (this.options.filter.anyDate[0].type === 'DURING') {
        currentValue = 'between'
      } else if (this.options.filter.anyDate[0].type === 'AFTER') {
        currentValue = 'after'
      } else if (this.options.filter.anyDate[0].type === 'BEFORE') {
        currentValue = 'before'
      } else {
        currentValue = 'relative'
      }
    }
    this.basicTime.show(
      new PropertyView({
        model: new Property({
          value: [currentValue],
          id: 'Time Range',
          enum: [
            {
              label: 'Any',
              value: 'any',
            },
            {
              label: 'After',
              value: 'after',
            },
            {
              label: 'Before',
              value: 'before',
            },
            {
              label: 'Between',
              value: 'between',
            },
            {
              label: 'Relative',
              value: 'relative',
            },
          ],
        }),
      })
    )
  },
  setupTimeRelative() {
    let currentValue
    if (this.options.filter.anyDate) {
      this.options.filter.anyDate.forEach(subfilter => {
        if (subfilter.type === '=') {
          currentValue = subfilter.value
        }
      })
    }
    this.basicTimeRelative.show(
      new RelativeTimeView({
        value: currentValue,
      })
    )
  },
})
