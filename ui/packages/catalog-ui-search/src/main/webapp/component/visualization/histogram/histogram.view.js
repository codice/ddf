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
/*global define, window*/
const wreqr = require('../../../js/wreqr.js')
const $ = require('jquery')
const _ = require('underscore')
const Marionette = require('marionette')
const CustomElements = require('../../../js/CustomElements.js')
const template = require('./histogram.hbs')
const Plotly = require('plotly.js')
const Property = require('../../property/property.js')
const PropertyView = require('../../property/property.view.js')
const metacardDefinitions = require('../../singletons/metacard-definitions.js')
const Common = require('../../../js/Common.js')
const properties = require('../../../js/properties.js')
const moment = require('moment')
const user = require('../../singletons/user-instance.js')

var zeroWidthSpace = '\u200B'
var plotlyDateFormat = 'YYYY-MM-DD HH:mm:ss.SS'

function getPlotlyDate(date) {
  return moment(date).format(plotlyDateFormat)
}

function calculateAvailableAttributes(results) {
  var availableAttributes = []
  results.forEach(function(result) {
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
    .filter(function(attribute) {
      return metacardDefinitions.metacardTypes[attribute] !== undefined
    })
    .filter(function(attribute) {
      return !metacardDefinitions.isHiddenType(attribute)
    })
    .filter(function(attribute) {
      return !properties.isHidden(attribute)
    })
    .map(function(attribute) {
      return {
        label: metacardDefinitions.metacardTypes[attribute].alias || attribute,
        value: attribute,
      }
    })
}

function calculateAttributeArray(results, attribute) {
  var attributes = []
  results.forEach(function(result) {
    if (metacardDefinitions.metacardTypes[attribute].multivalued) {
      var resultValues = result
        .get('metacard')
        .get('properties')
        .get(attribute)
      if (resultValues && resultValues.forEach) {
        resultValues.forEach(function(value) {
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
  return results.filter(function(result) {
    if (metacardDefinitions.metacardTypes[attribute].multivalued) {
      var resultValues = result
        .get('metacard')
        .get('properties')
        .get(attribute)
      if (resultValues && resultValues.forEach) {
        for (var i = 0; i < resultValues.length; i++) {
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
        var plotlyDate = getPlotlyDate(value)
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
  return Math.max.apply(
    this,
    data.points.map(function(point) {
      return point.pointNumber
    })
  )
}

function getValueFromClick(data, categories) {
  switch (data.points[0].xaxis.type) {
    case 'category':
      return [data.points[0].x]
    case 'date':
      var currentDate = moment(data.points[0].x).format(plotlyDateFormat)
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
  var config = {
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
  var prefs = user.get('user').get('preferences')
  var theme = getTheme(prefs.get('theme').get('spacingMode'))

  var baseLayout = {
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
  template: template,
  regions: {
    histogramAttribute: '.histogram-attribute',
  },
  events: {},
  defaultValue: undefined,
  initialize: function() {
    this.showHistogram = _.debounce(this.showHistogram, 30)
    this.updateHistogram = _.debounce(this.updateHistogram, 30)
    this.handleResize = _.debounce(this.handleResize, 30)
    this.removeResizeHandler().addResizeHandler()
    this.setupListeners()
  },
  showHistogram: function() {
    if (
      this.histogramAttribute.currentView.model.getValue()[0] &&
      this.options.selectionInterface.getCompleteActiveSearchResults()
        .length !== 0
    ) {
      this.defaultValue = this.histogramAttribute.currentView.model.getValue()
      var histogramElement = this.el.querySelector('.histogram-container')
      var initialData = this.determineInitialData()
      if (initialData[0].x.length === 0) {
        this.$el.addClass('no-matching-data')
        this.el.querySelector('.histogram-container').innerHTML = ''
      } else {
        this.$el.removeClass('no-matching-data')
        Plotly.newPlot(histogramElement, initialData, getLayout(), {
          displayModeBar: false,
        }).then(
          function(plot) {
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
          }.bind(this)
        )
      }
    } else {
      this.el.querySelector('.histogram-container').innerHTML = ''
    }
  },
  updateHistogram: function() {
    var histogramElement = this.el.querySelector('.histogram-container')
    if (
      histogramElement.children.length !== 0 &&
      this.histogramAttribute.currentView.model.getValue()[0] &&
      this.options.selectionInterface.getCompleteActiveSearchResults()
        .length !== 0
    ) {
      Plotly.deleteTraces(histogramElement, 1)
      Plotly.addTraces(
        histogramElement,
        this.determineData(histogramElement)[1]
      )
      this.handleResize()
    } else {
      this.el.querySelector('.histogram-container').innerHTML = ''
    }
  },
  updateTheme: function(e) {
    var histogramElement = this.el.querySelector('.histogram-container')
    if (
      histogramElement.children.length !== 0 &&
      this.histogramAttribute.currentView.model.getValue()[0] &&
      this.options.selectionInterface.getCompleteActiveSearchResults()
        .length !== 0
    ) {
      var theme = getTheme(e.get('spacingMode'))
      histogramElement.layout.margin = theme.margin
    }
  },
  updateFontSize: function(e) {
    var histogramElement = this.el.querySelector('.histogram-container')
    if (
      histogramElement.children.length !== 0 &&
      this.histogramAttribute.currentView.model.getValue()[0] &&
      this.options.selectionInterface.getCompleteActiveSearchResults()
        .length !== 0
    ) {
      histogramElement.layout.font.size = e.get('fontSize')
    }
  },
  showHistogramAttributeSelector: function() {
    var defaultValue = []
    defaultValue = this.defaultValue || defaultValue
    this.histogramAttribute.show(
      new PropertyView({
        model: new Property({
          showValidationIssues: false,
          enumFiltering: true,
          enum: calculateAvailableAttributes(
            this.options.selectionInterface.getCompleteActiveSearchResults()
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
  onRender: function() {
    this.showHistogramAttributeSelector()
    this.showHistogram()
    this.handleEmpty()
  },
  determineInitialData: function() {
    var activeResults = this.options.selectionInterface.getCompleteActiveSearchResults()
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
  determineData: function(plot) {
    var activeResults = this.options.selectionInterface.getCompleteActiveSearchResults()
    var selectedResults = this.options.selectionInterface.getSelectedResults()
    var xbins = Common.duplicate(plot._fullData[0].xbins)
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
        xbins: xbins,
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
        xbins: xbins,
      },
    ]
  },
  handleEmpty: function() {
    this.$el.toggleClass(
      'is-empty',
      this.options.selectionInterface.getCompleteActiveSearchResults()
        .length === 0
    )
  },
  handleResize: function() {
    var histogramElement = this.el.querySelector('.histogram-container')
    this.$el.find('rect.drag').off('mousedown')
    if (histogramElement._context) {
      Plotly.Plots.resize(histogramElement)
    }
    this.$el.find('rect.drag').on(
      'mousedown',
      function(event) {
        this.shiftKey = event.shiftKey
        this.metaKey = event.metaKey
        this.ctrlKey = event.ctrlKey
      }.bind(this)
    )
  },
  addResizeHandler: function() {
    this.listenTo(wreqr.vent, 'resize', this.handleResize)
    $(window).on('resize.histogram', this.handleResize.bind(this))
    return this
  },
  removeResizeHandler: function() {
    $(window).off('resize.histogram')
    return this
  },
  onDestroy: function() {
    this.removeResizeHandler()
  },
  setupListeners: function() {
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
  listenToHistogram: function() {
    this.el
      .querySelector('.histogram-container')
      .on('plotly_click', this.plotlyClickHandler.bind(this))
  },
  plotlyClickHandler: function(data) {
    var indexClicked = getIndexClicked(data)
    var alreadySelected = this.pointsSelected.indexOf(indexClicked) >= 0
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
  handleControlClick: function(data, alreadySelected) {
    var attributeToCheck = this.histogramAttribute.currentView.model.getValue()[0]
    var categories = this.retrieveCategoriesFromPlotly()
    if (alreadySelected) {
      this.options.selectionInterface.removeSelectedResult(
        findMatchesForAttributeValues(
          this.options.selectionInterface.getCompleteActiveSearchResults(),
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
          this.options.selectionInterface.getCompleteActiveSearchResults(),
          attributeToCheck,
          getValueFromClick(data, categories)
        )
      )
      this.pointsSelected.push(getIndexClicked(data))
    }
  },
  handleShiftClick: function(data, alreadySelected) {
    var indexClicked = getIndexClicked(data)
    var firstIndex =
      this.pointsSelected.length === 0
        ? -1
        : this.pointsSelected.reduce(function(currentMin, point) {
            return Math.min(currentMin, point)
          }, this.pointsSelected[0])
    var lastIndex =
      this.pointsSelected.length === 0
        ? -1
        : this.pointsSelected.reduce(function(currentMin, point) {
            return Math.max(currentMin, point)
          }, this.pointsSelected[0])
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
  selectBetween: function(firstIndex, lastIndex) {
    for (var i = firstIndex; i <= lastIndex; i++) {
      if (this.pointsSelected.indexOf(i) === -1) {
        this.pointsSelected.push(i)
      }
    }
    var attributeToCheck = this.histogramAttribute.currentView.model.getValue()[0]
    var categories = this.retrieveCategoriesFromPlotly()
    var validCategories = categories.slice(firstIndex, lastIndex)
    var activeSearchResults = this.options.selectionInterface.getCompleteActiveSearchResults()
    this.options.selectionInterface.addSelectedResult(
      validCategories.reduce(function(results, category) {
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
  retrieveCategoriesFromPlotly: function() {
    var histogramElement = this.el.querySelector('.histogram-container')
    var xaxis = histogramElement._fullLayout.xaxis
    switch (xaxis.type) {
      case 'category':
        return xaxis._categories
      case 'date':
        return this.retrieveCategoriesFromPlotlyForDates()
      default:
        var xbins = histogramElement._fullData[0].xbins
        var min = xbins.start
        var max = xbins.end
        var binSize = xbins.size
        var categories = []
        var start = min
        while (start < max) {
          categories.push([start, start + binSize])
          start += binSize
        }
        return categories
    }
  },
  retrieveCategoriesFromPlotlyForDates: function() {
    var histogramElement = this.el.querySelector('.histogram-container')
    var categories = []
    var xbins = histogramElement._fullData[0].xbins
    var min = xbins.start
    var max = xbins.end
    var start = min
    var inMonths = xbins.size.constructor === String
    var binSize = inMonths ? parseInt(xbins.size.substring(1)) : xbins.size
    while (start < max) {
      var startDate = moment(start).format(plotlyDateFormat)
      var endDate = inMonths
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
  resetKeyTracking: function() {
    this.shiftKey = false
    this.metaKey = false
    this.ctrlKey = false
  },
  resetPointSelection: function() {
    this.pointsSelected = []
  },
  shiftKey: false,
  metaKey: false,
  ctrlKey: false,
  pointsSelected: [],
})
