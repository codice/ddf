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

import * as React from 'react'
import { HistogramContainer } from '../../../react-component/container/histogram'

const wreqr = require('../../../js/wreqr.js')
const $ = require('jquery')
const _ = require('underscore')
const Marionette = require('marionette')
const CustomElements = require('../../../js/CustomElements.js')
const Plotly = require('plotly.js/dist/plotly.js')
const Property = require('../../property/property.js')
const PropertyView = require('../../property/property.view.js')
const metacardDefinitions = require('../../singletons/metacard-definitions.js')
const Common = require('../../../js/Common.js')
const properties = require('../../../js/properties.js')
const moment = require('moment')
const user = require('../../singletons/user-instance.js')

const zeroWidthSpace = '\u200B'
const plotlyDateFormat = 'YYYY-MM-DD HH:mm:ss.SS'

function getPlotlyDate(date) {
  return moment(date).format(plotlyDateFormat)
}

function calculateAvailableAttributes(results) {
  let availableAttributes = []
  results.forEach(result => {
    availableAttributes = _.union(
      availableAttributes,
      Object.keys(
        result
          .get('metacard')
          .get('properties')
          .toJSON()
      )
    )
  })
  return availableAttributes
    .filter(
      attribute => metacardDefinitions.metacardTypes[attribute] !== undefined
    )
    .filter(attribute => !metacardDefinitions.isHiddenType(attribute))
    .filter(attribute => !properties.isHidden(attribute))
    .map(attribute => ({
      label: metacardDefinitions.metacardTypes[attribute].alias || attribute,
      value: attribute,
    }))
}

function calculateAttributeArray(results, attribute) {
  const attributes = []
  results.forEach(result => {
    if (metacardDefinitions.metacardTypes[attribute].multivalued) {
      const resultValues = result
        .get('metacard')
        .get('properties')
        .get(attribute)
      if (resultValues && resultValues.forEach) {
        resultValues.forEach(value => {
          addValueForAttributeToArray(attributes, attribute, value)
        })
      } else {
        addValueForAttributeToArray(attributes, attribute, resultValues)
      }
    } else {
      addValueForAttributeToArray(
        attributes,
        attribute,
        result
          .get('metacard')
          .get('properties')
          .get(attribute)
      )
    }
  })
  return attributes
}

function findMatchesForAttributeValues(results, attribute, values) {
  return results.filter(result => {
    if (metacardDefinitions.metacardTypes[attribute].multivalued) {
      const resultValues = result
        .get('metacard')
        .get('properties')
        .get(attribute)
      if (resultValues && resultValues.forEach) {
        for (let i = 0; i < resultValues.length; i++) {
          if (checkIfValueIsValid(values, attribute, resultValues[i])) {
            return true
          }
        }
        return false
      } else {
        return checkIfValueIsValid(values, attribute, resultValues)
      }
    } else {
      return checkIfValueIsValid(
        values,
        attribute,
        result
          .get('metacard')
          .get('properties')
          .get(attribute)
      )
    }
  })
}

function checkIfValueIsValid(values, attribute, value) {
  if (value !== undefined) {
    switch (metacardDefinitions.metacardTypes[attribute].type) {
      case 'DATE':
        const plotlyDate = getPlotlyDate(value)
        return plotlyDate >= values[0] && plotlyDate <= values[1]
      case 'BOOLEAN':
      case 'STRING':
      case 'GEOMETRY':
        return values.indexOf(value.toString() + zeroWidthSpace) >= 0
      default:
        return value >= values[0] && value <= values[1]
    }
  }
}

function addValueForAttributeToArray(valueArray, attribute, value) {
  if (value !== undefined) {
    switch (metacardDefinitions.metacardTypes[attribute].type) {
      case 'DATE':
        valueArray.push(getPlotlyDate(value))
        break
      case 'BOOLEAN':
      case 'STRING':
      case 'GEOMETRY':
        valueArray.push(value.toString() + zeroWidthSpace)
        break
      default:
        valueArray.push(parseFloat(value))
        break
    }
  }
}

function getIndexClicked(data) {
  return Math.max.apply(this, data.points.map(point => point.pointNumber))
}

function getValueFromClick(data, categories) {
  switch (data.points[0].xaxis.type) {
    case 'category':
      return [data.points[0].x]
    case 'date':
      const currentDate = moment(data.points[0].x).format(plotlyDateFormat)
      return _.find(categories, category => {
        return currentDate >= category[0] && currentDate <= category[1]
      })
    default:
      return _.find(categories, category => {
        return (
          data.points[0].x >= category[0] && data.points[0].x <= category[1]
        )
      })
  }
}

function getTheme(theme) {
  const config = {
    margin: {
      t: 10,
      l: 50,
      r: 115,
      b: 90,
      pad: 0,
      autoexpand: true,
    },
  }
  switch (theme) {
    case 'comfortable':
      config.margin.b = 140
      return config
    case 'cozy':
      config.margin.b = 115
      return config
    case 'compact':
      config.margin.b = 90
      return config
    default:
      return config
  }
}

function getLayout(plot) {
  const prefs = user.get('user').get('preferences')
  const theme = getTheme(prefs.get('theme').get('spacingMode'))

  const baseLayout = {
    autosize: true,
    paper_bgcolor: 'rgba(0,0,0,0)',
    plot_bgcolor: 'rgba(0,0,0,0)',
    font: {
      family: '"Open Sans Light","Helvetica Neue",Helvetica,Arial,sans-serif',
      size: prefs.get('fontSize'),
      color: 'white',
    },
    margin: theme.margin,
    barmode: 'overlay',
    xaxis: {
      fixedrange: true,
    },
    yaxis: {
      fixedrange: true,
    },
    showlegend: true,
  }
  if (plot) {
    baseLayout.xaxis.autorange = false
    baseLayout.xaxis.range = plot._fullLayout.xaxis.range
    baseLayout.yaxis.range = plot._fullLayout.yaxis.range
    baseLayout.yaxis.autorange = false
  }
  return baseLayout
}

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('histogram'),
  template: () => <HistogramContainer />,
  regions: {
    histogramAttribute: '.histogram-attribute',
  },
  events: {},
  defaultValue: undefined,
  initialize() {
    this.showHistogram = _.debounce(this.showHistogram, 30)
    this.updateHistogram = _.debounce(this.updateHistogram, 30)
    this.handleResize = _.debounce(this.handleResize, 30)
    this.removeResizeHandler().addResizeHandler()
    this.setupListeners()
  },
  showHistogram() {
    if (
      this.histogramAttribute.currentView.model.getValue()[0] &&
      this.options.selectionInterface.getActiveSearchResults().length !== 0
    ) {
      this.defaultValue = this.histogramAttribute.currentView.model.getValue()
      const histogramElement = this.el.querySelector('.histogram-container')
      const initialData = this.determineInitialData()
      if (initialData[0].x.length === 0) {
        this.$el.addClass('no-matching-data')
        this.el.querySelector('.histogram-container').innerHTML = ''
      } else {
        this.$el.removeClass('no-matching-data')
        Plotly.newPlot(histogramElement, initialData, getLayout(), {
          displayModeBar: false,
        }).then(plot => {
          Plotly.newPlot(
            histogramElement,
            this.determineData(plot),
            getLayout(plot),
            {
              displayModeBar: false,
            }
          )
          this.handleResize()
          this.listenToHistogram()
        })
      }
    } else {
      this.el.querySelector('.histogram-container').innerHTML = ''
    }
  },
  updateHistogram() {
    const histogramElement = this.el.querySelector('.histogram-container')
    if (
      histogramElement !== null &&
      histogramElement.children.length !== 0 &&
      this.histogramAttribute.currentView.model.getValue()[0] &&
      this.options.selectionInterface.getActiveSearchResults().length !== 0
    ) {
      Plotly.deleteTraces(histogramElement, 1)
      Plotly.addTraces(
        histogramElement,
        this.determineData(histogramElement)[1]
      )
      this.handleResize()
    } else {
      const container = this.el.querySelector('.histogram-container')
      if (container) {
        container.innerHTML = ''
      }
    }
  },
  updateTheme(e) {
    const histogramElement = this.el.querySelector('.histogram-container')
    if (
      histogramElement.children.length !== 0 &&
      this.histogramAttribute.currentView.model.getValue()[0] &&
      this.options.selectionInterface.getActiveSearchResults().length !== 0
    ) {
      const theme = getTheme(e.get('spacingMode'))
      histogramElement.layout.margin = theme.margin
    }
  },
  updateFontSize(e) {
    const histogramElement = this.el.querySelector('.histogram-container')
    if (
      histogramElement.children.length !== 0 &&
      this.histogramAttribute.currentView.model.getValue()[0] &&
      this.options.selectionInterface.getActiveSearchResults().length !== 0
    ) {
      histogramElement.layout.font.size = e.get('fontSize')
    }
  },
  showHistogramAttributeSelector() {
    let defaultValue = []
    defaultValue = this.defaultValue || defaultValue
    this.histogramAttribute.show(
      new PropertyView({
        model: new Property({
          showValidationIssues: false,
          enumFiltering: true,
          enum: calculateAvailableAttributes(
            this.options.selectionInterface.getActiveSearchResults()
          ),
          value: defaultValue,
          id: 'Group by',
        }),
      })
    )
    this.histogramAttribute.currentView.turnOnEditing()
    this.listenTo(
      this.histogramAttribute.currentView.model,
      'change:value',
      this.showHistogram
    )
  },
  onRender() {
    this.showHistogramAttributeSelector()
    this.showHistogram()
    this.handleEmpty()
  },
  determineInitialData() {
    const activeResults = this.options.selectionInterface.getActiveSearchResults()
    return [
      {
        x: calculateAttributeArray(
          activeResults,
          this.histogramAttribute.currentView.model.getValue()[0]
        ),
        opacity: 1,
        type: 'histogram',
        name: 'Hits        ',
        marker: {
          color: 'rgba(255, 255, 255, .05)',
          line: {
            color: 'rgba(255,255,255,.2)',
            width: '2',
          },
        },
      },
    ]
  },
  determineData(plot) {
    const activeResults = this.options.selectionInterface.getActiveSearchResults()
    const selectedResults = this.options.selectionInterface.getSelectedResults()
    const xbins = Common.duplicate(plot._fullData[0].xbins)
    if (xbins.size.constructor !== String) {
      xbins.end = xbins.end + xbins.size //https://github.com/plotly/plotly.js/issues/1229
    } else {
      // soooo plotly introduced this cool bin size shorthand where M3 means 3 months, M6 6 months etc.
      xbins.end =
        xbins.end + parseInt(xbins.size.substring(1)) * 31 * 24 * 3600000 //https://github.com/plotly/plotly.js/issues/1229
    }
    return [
      {
        x: calculateAttributeArray(
          activeResults,
          this.histogramAttribute.currentView.model.getValue()[0]
        ),
        opacity: 1,
        type: 'histogram',
        hoverinfo: 'y+x+name',
        name: 'Hits        ',
        marker: {
          color: 'rgba(255, 255, 255, .05)',
          line: {
            color: 'rgba(255,255,255,.2)',
            width: '2',
          },
        },
        autobinx: false,
        xbins,
      },
      {
        x: calculateAttributeArray(
          selectedResults,
          this.histogramAttribute.currentView.model.getValue()[0]
        ),
        opacity: 1,
        type: 'histogram',
        hoverinfo: 'y+x+name',
        name: 'Selected',
        marker: {
          color: 'rgba(255, 255, 255, .2)',
        },
        autobinx: false,
        xbins,
      },
    ]
  },
  handleEmpty() {
    this.$el.toggleClass(
      'is-empty',
      this.options.selectionInterface.getActiveSearchResults().length === 0
    )
  },
  handleResize() {
    const histogramElement = this.el.querySelector('.histogram-container')
    this.$el.find('rect.drag').off('mousedown')
    if (histogramElement._context) {
      Plotly.Plots.resize(histogramElement)
    }
    this.$el.find('rect.drag').on('mousedown', event => {
      this.shiftKey = event.shiftKey
      this.metaKey = event.metaKey
      this.ctrlKey = event.ctrlKey
    })
  },
  addResizeHandler() {
    this.listenTo(wreqr.vent, 'resize', this.handleResize)
    $(window).on('resize.histogram', this.handleResize.bind(this))
    return this
  },
  removeResizeHandler() {
    $(window).off('resize.histogram')
    return this
  },
  onDestroy() {
    this.removeResizeHandler()
  },
  setupListeners() {
    this.listenTo(
      this.options.selectionInterface,
      'reset:completeActiveSearchResults',
      this.render
    )
    this.listenTo(
      this.options.selectionInterface.getSelectedResults(),
      'update',
      this.updateHistogram
    )
    this.listenTo(
      this.options.selectionInterface.getSelectedResults(),
      'add',
      this.updateHistogram
    )
    this.listenTo(
      this.options.selectionInterface.getSelectedResults(),
      'remove',
      this.updateHistogram
    )
    this.listenTo(
      this.options.selectionInterface.getSelectedResults(),
      'reset',
      this.updateHistogram
    )

    this.listenTo(
      user.get('user').get('preferences'),
      'change:fontSize',
      this.updateFontSize
    )
    this.listenTo(
      user.get('user').get('preferences'),
      'change:theme',
      this.updateTheme
    )
  },
  listenToHistogram() {
    this.el
      .querySelector('.histogram-container')
      .on('plotly_click', this.plotlyClickHandler.bind(this))
  },
  plotlyClickHandler(data) {
    const indexClicked = getIndexClicked(data)
    const alreadySelected = this.pointsSelected.indexOf(indexClicked) >= 0
    if (this.shiftKey) {
      this.handleShiftClick(data)
    } else if (this.ctrlKey || this.metaKey) {
      this.handleControlClick(data, alreadySelected)
    } else {
      this.options.selectionInterface.clearSelectedResults()
      this.resetPointSelection()
      this.handleControlClick(data, alreadySelected)
    }
    this.resetKeyTracking()
  },
  handleControlClick(data, alreadySelected) {
    const attributeToCheck = this.histogramAttribute.currentView.model.getValue()[0]
    const categories = this.retrieveCategoriesFromPlotly()
    if (alreadySelected) {
      this.options.selectionInterface.removeSelectedResult(
        findMatchesForAttributeValues(
          this.options.selectionInterface.getActiveSearchResults(),
          attributeToCheck,
          getValueFromClick(data, categories)
        )
      )
      this.pointsSelected.splice(
        this.pointsSelected.indexOf(getIndexClicked(data)),
        1
      )
    } else {
      this.options.selectionInterface.addSelectedResult(
        findMatchesForAttributeValues(
          this.options.selectionInterface.getActiveSearchResults(),
          attributeToCheck,
          getValueFromClick(data, categories)
        )
      )
      this.pointsSelected.push(getIndexClicked(data))
    }
  },
  handleShiftClick(data, alreadySelected) {
    const indexClicked = getIndexClicked(data)
    const firstIndex =
      this.pointsSelected.length === 0
        ? -1
        : this.pointsSelected.reduce(
            (currentMin, point) => Math.min(currentMin, point),
            this.pointsSelected[0]
          )
    const lastIndex =
      this.pointsSelected.length === 0
        ? -1
        : this.pointsSelected.reduce(
            (currentMin, point) => Math.max(currentMin, point),
            this.pointsSelected[0]
          )
    if (firstIndex === -1 && lastIndex === -1) {
      this.options.selectionInterface.clearSelectedResults()
      this.handleControlClick(data, alreadySelected)
    } else if (indexClicked <= firstIndex) {
      this.selectBetween(indexClicked, firstIndex)
    } else if (indexClicked >= lastIndex) {
      this.selectBetween(lastIndex, indexClicked + 1)
    } else {
      this.selectBetween(firstIndex, indexClicked + 1)
    }
  },
  selectBetween(firstIndex, lastIndex) {
    for (let i = firstIndex; i <= lastIndex; i++) {
      if (this.pointsSelected.indexOf(i) === -1) {
        this.pointsSelected.push(i)
      }
    }
    const attributeToCheck = this.histogramAttribute.currentView.model.getValue()[0]
    const categories = this.retrieveCategoriesFromPlotly()
    const validCategories = categories.slice(firstIndex, lastIndex)
    const activeSearchResults = this.options.selectionInterface.getActiveSearchResults()
    this.options.selectionInterface.addSelectedResult(
      validCategories.reduce((results, category) => {
        results = results.concat(
          findMatchesForAttributeValues(
            activeSearchResults,
            attributeToCheck,
            category.constructor === Array ? category : [category]
          )
        )
        return results
      }, [])
    )
  },
  // This is an internal variable for Plotly, so it might break if we update Plotly in the future.
  // Regardless, there was no other way to reliably get the categories.
  retrieveCategoriesFromPlotly() {
    const histogramElement = this.el.querySelector('.histogram-container')
    const xaxis = histogramElement._fullLayout.xaxis
    switch (xaxis.type) {
      case 'category':
        return xaxis._categories
      case 'date':
        return this.retrieveCategoriesFromPlotlyForDates()
      default:
        const xbins = histogramElement._fullData[0].xbins
        const min = xbins.start
        const max = xbins.end
        const binSize = xbins.size
        const categories = []
        var start = min
        while (start < max) {
          categories.push([start, start + binSize])
          start += binSize
        }
        return categories
    }
  },
  retrieveCategoriesFromPlotlyForDates() {
    const histogramElement = this.el.querySelector('.histogram-container')
    const categories = []
    const xbins = histogramElement._fullData[0].xbins
    const min = xbins.start
    const max = xbins.end
    var start = min
    const inMonths = xbins.size.constructor === String
    const binSize = inMonths ? parseInt(xbins.size.substring(1)) : xbins.size
    while (start < max) {
      const startDate = moment(start).format(plotlyDateFormat)
      const endDate = inMonths
        ? moment(start)
            .add(binSize, 'months')
            .format(plotlyDateFormat)
        : moment(start)
            .add(binSize, 'ms')
            .format(plotlyDateFormat)
      categories.push([startDate, endDate])
      start = parseInt(
        inMonths
          ? moment(start)
              .add(binSize, 'months')
              .format('x')
          : moment(start)
              .add(binSize, 'ms')
              .format('x')
      )
    }
    return categories
  },
  resetKeyTracking() {
    this.shiftKey = false
    this.metaKey = false
    this.ctrlKey = false
  },
  resetPointSelection() {
    this.pointsSelected = []
  },
  shiftKey: false,
  metaKey: false,
  ctrlKey: false,
  pointsSelected: [],
})
