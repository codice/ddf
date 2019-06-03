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
const template = require('./query-altitude.hbs')
const CustomElements = require('../../js/CustomElements.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const CQLUtils = require('../../js/CQLUtils.js')
const DistanceUtils = require('../../js/DistanceUtils.js')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('query-altitude'),
  regions: {
    basicAltitude: '.basic-altitude',
    basicAltitudeUnits: '.basic-altitude-units',
    basicAltitudeAbove: '.basic-altitude-above',
    basicAltitudeBelow: '.basic-altitude-below',
    basicAltitudeBetweenAbove: '.basic-altitude-between .between-above',
    basicAltitudeBetweenBelow: '.basic-altitude-between .between-below',
  },
  previousAltitudeUnit: 'meters',
  onBeforeShow() {
    this.turnOnEditing()
    this.setupAltitudeInput()
    this.setupAltitudeAbove()
    this.setupAltitudeBelow()
    this.setupAltitudeBetween()
    this.setupAltitudeUnit()
    this.listenTo(
      this.basicAltitude.currentView.model,
      'change:value',
      this.handleAltitudeRangeValue
    )
    this.listenTo(
      this.basicAltitudeUnits.currentView.model,
      'change:value',
      this.handleAltitudeUnitValue
    )
    this.handleAltitudeRangeValue()
  },
  turnOnEditing() {
    this.$el.addClass('is-editing')
    this.regionManager.forEach(region => {
      if (region.currentView && region.currentView.turnOnEditing) {
        region.currentView.turnOnEditing()
      }
    })
  },
  handleAltitudeRangeValue() {
    const altitudeRange = this.basicAltitude.currentView.model.getValue()[0]
    this.$el.toggleClass('is-altitudeRange-any', altitudeRange === 'any')
    this.$el.toggleClass('is-altitudeRange-above', altitudeRange === 'above')
    this.$el.toggleClass('is-altitudeRange-below', altitudeRange === 'below')
    this.$el.toggleClass(
      'is-altitudeRange-between',
      altitudeRange === 'between'
    )

    this.$el.toggleClass(
      'is-altitudeUnit',
      altitudeRange === 'above' ||
        altitudeRange === 'below' ||
        altitudeRange === 'between'
    )
  },
  setupAltitudeUnit() {
    this.basicAltitudeUnits.show(
      new PropertyView({
        model: new Property({
          value: ['meters'],
          id: 'Altitude Unit',
          radio: [
            {
              label: 'meters',
              value: 'meters',
            },
            {
              label: 'kilometers',
              value: 'kilometers',
            },
            {
              label: 'feet',
              value: 'feet',
            },
            {
              label: 'yards',
              value: 'yards',
            },
            {
              label: 'miles',
              value: 'miles',
            },
          ],
        }),
      })
    )
  },
  handleAltitudeUnitValue() {
    const unit = this.basicAltitudeUnits.currentView.model.getValue()[0]

    const fields = [
      this.basicAltitudeAbove,
      this.basicAltitudeBelow,
      this.basicAltitudeBetweenAbove,
      this.basicAltitudeBetweenBelow,
    ]

    for (let i = 0; i < fields.length; i++) {
      const field = fields[i].currentView.model
      let value = parseFloat(field.getValue()[0])

      // convert to meters and convert to any units.
      value = DistanceUtils.getDistanceInMeters(
        value,
        this.previousAltitudeUnit
      )
      value = DistanceUtils.getDistanceFromMeters(value, unit)

      value = DistanceUtils.altitudeRound(value)

      field.setValue([value.toString()])
      fields[i].$el.find('input').val(value)
    }

    this.previousAltitudeUnit = unit
  },
  setupAltitudeAbove() {
    let currentAbove = 0

    const altFilters = this.options.filter['location.altitude-meters']

    if (altFilters !== undefined) {
      // Search for the Above value
      for (let i = 0; i < altFilters.length; i++) {
        var value = altFilters[i].value
        if (altFilters[i].type === '>=') {
          if (value > currentAbove || currentAbove === 0) {
            currentAbove = value
          }
        }
      }
    }

    this.basicAltitudeAbove.show(
      new PropertyView({
        model: new Property({
          value: [currentAbove],
          id: 'Above',
          type: 'INTEGER',
        }),
      })
    )
  },
  setupAltitudeBelow() {
    let currentBelow = 0

    const altFilters = this.options.filter['location.altitude-meters']

    if (altFilters !== undefined) {
      // Search for the Before value
      for (let i = 0; i < altFilters.length; i++) {
        var value = altFilters[i].value
        if (altFilters[i].type === '<=') {
          if (value < currentBelow || currentBelow === 0) {
            currentBelow = value
          }
        }
      }
    }

    this.basicAltitudeBelow.show(
      new PropertyView({
        model: new Property({
          value: [currentBelow],
          id: 'Below',
          type: 'INTEGER',
        }),
      })
    )
  },
  setupAltitudeBetween() {
    let currentBelow = 0
    let currentAbove = 0

    const altFilters = this.options.filter['location.altitude-meters']

    if (altFilters !== undefined) {
      // Search for the Before/Above values
      for (let i = 0; i < altFilters.length; i++) {
        var type = altFilters[i].type
        var value = altFilters[i].value

        if (type === '<=') {
          if (value < currentBelow || currentBelow === 0) {
            currentBelow = value
          }
        } else if (type === '>=') {
          if (value > currentAbove || currentAbove === 0) {
            currentAbove = value
          }
        }
      }
    }

    this.basicAltitudeBetweenAbove.show(
      new PropertyView({
        model: new Property({
          value: [currentAbove],
          id: 'Above',
          type: 'INTEGER',
        }),
      })
    )
    this.basicAltitudeBetweenBelow.show(
      new PropertyView({
        model: new Property({
          value: [currentBelow],
          id: 'Below',
          type: 'INTEGER',
        }),
      })
    )
  },
  setupAltitudeInput() {
    let currentValue = 'any'

    const altFilters = this.options.filter['location.altitude-meters']

    if (altFilters !== undefined) {
      /* If the only filter is a <=, then it is a Before altitude filter.
               If the only filter is a >=, then it is a Above altitude filter.
               If there is a <= and >= filter, then it is a between altitude filter.
               If anything else, no filters - Select 'any'
            */

      let hasAbove = false
      let hasBefore = false

      for (let i = 0; i < altFilters.length; i++) {
        const type = altFilters[i].type

        if (type === '>=') {
          hasAbove = true
        } else if (type === '<=') {
          hasBefore = true
        }
      }

      if (hasBefore && !hasAbove) {
        currentValue = 'below'
      } else if (!hasBefore && hasAbove) {
        currentValue = 'above'
      } else if (hasBefore && hasAbove) {
        currentValue = 'between'
      }
    }

    this.basicAltitude.show(
      new PropertyView({
        model: new Property({
          value: [currentValue],
          id: 'Altitude Range',
          radio: [
            {
              label: 'Any',
              value: 'any',
            },
            {
              label: 'Above',
              value: 'above',
            },
            {
              label: 'Below',
              value: 'below',
            },
            {
              label: 'Between',
              value: 'between',
            },
          ],
        }),
      })
    )
  },
  constructFilter() {
    // Determine which option is selected for altitude range
    const filters = []
    const altitudeSelect = this.basicAltitude.currentView.model.getValue()[0]
    const altitudeUnit = this.basicAltitudeUnits.currentView.model.getValue()[0]

    switch (altitudeSelect) {
      // Build filters for altitude
      case 'above':
        // Handle Above altitude selected
        var aboveAltitude = parseFloat(
          this.basicAltitudeAbove.currentView.model.getValue()[0]
        )

        aboveAltitude = DistanceUtils.getDistanceInMeters(
          aboveAltitude,
          altitudeUnit
        )
        aboveAltitude = DistanceUtils.altitudeRound(aboveAltitude)

        var aboveAltitudeFilter = CQLUtils.generateFilter(
          '>=',
          'location.altitude-meters',
          aboveAltitude
        )
        filters.push(aboveAltitudeFilter)

        break
      case 'below':
        // Handle Below altitude selected
        var belowAltitude = parseFloat(
          this.basicAltitudeBelow.currentView.model.getValue()[0]
        )

        belowAltitude = DistanceUtils.getDistanceInMeters(
          belowAltitude,
          altitudeUnit
        )
        belowAltitude = DistanceUtils.altitudeRound(belowAltitude)

        var belowAltitudeFilter = CQLUtils.generateFilter(
          '<=',
          'location.altitude-meters',
          belowAltitude
        )
        filters.push(belowAltitudeFilter)
        break
      case 'between':
        // Handle between altitude selected

        var aboveAltitude = parseFloat(
          this.basicAltitudeBetweenAbove.currentView.model.getValue()[0]
        )

        var belowAltitude = parseFloat(
          this.basicAltitudeBetweenBelow.currentView.model.getValue()[0]
        )

        aboveAltitude = DistanceUtils.getDistanceInMeters(
          aboveAltitude,
          altitudeUnit
        )
        aboveAltitude = DistanceUtils.altitudeRound(aboveAltitude)

        belowAltitude = DistanceUtils.getDistanceInMeters(
          belowAltitude,
          altitudeUnit
        )
        belowAltitude = DistanceUtils.altitudeRound(belowAltitude)

        var aboveAltitudeFilter = CQLUtils.generateFilter(
          '>=',
          'location.altitude-meters',
          aboveAltitude
        )

        var belowAltitudeFilter = CQLUtils.generateFilter(
          '<=',
          'location.altitude-meters',
          belowAltitude
        )

        const altitudeFilters = {
          type: 'AND',
          filters: [aboveAltitudeFilter, belowAltitudeFilter],
        }

        filters.push(altitudeFilters)
        break
      case 'any':
      default:
        break
    }
    return filters
  },
})
