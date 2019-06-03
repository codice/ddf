/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

const Backbone = require('backbone')
const _ = require('underscore')
let lessStyles = require('../../js/uncompiled-less.unless')
const lessToJs = require('less-vars-to-js')
const _get = require('lodash.get')
const properties = require('../properties.js')
const $ = require('jquery')
require('spectrum-colorpicker')
const $spectrumInput = $(document.createElement('input')).spectrum()

const spacingVariables = [
  'minimumButtonSize',
  'minimumLineSize',
  'minimumSpacing',
]
const colorVariables = [
  'customPrimaryColor',
  'customPositiveColor',
  'customNegativeColor',
  'customWarningColor',
  'customFavoriteColor',
  'customBackgroundNavigation',
  'customBackgroundAccentContent',
  'customBackgroundDropdown',
  'customBackgroundContent',
  'customBackgroundModal',
  'customBackgroundSlideout',
]
const themeableVariables = spacingVariables.concat(colorVariables)

function trimVariables(variables) {
  const newVariableMap = {}
  _.forEach(variables, (value, key) => {
    const trimmedKey = key.substring(1)
    if (themeableVariables.indexOf(trimmedKey) !== -1) {
      newVariableMap[trimmedKey] = value
    }
  })
  return newVariableMap
}

function removeAlpha(color) {
  const hexString = $spectrumInput
    .spectrum('set', color)
    .spectrum('get')
    .toHexString()
  return hexString
}

function validTextColour(stringToTest) {
  if (
    [null, undefined, '', 'inherit', 'transparent'].indexOf(stringToTest) !== -1
  ) {
    return false
  }

  const image = document.createElement('img')
  image.style.color = 'rgb(0, 0, 0)'
  image.style.color = stringToTest
  if (image.style.color !== 'rgb(0, 0, 0)') {
    return true
  }
  image.style.color = 'rgb(255, 255, 255)'
  image.style.color = stringToTest
  return image.style.color !== 'rgb(255, 255, 255)'
}

const baseVariables = trimVariables(lessToJs(lessStyles))
const comfortableVariables = _.pick(baseVariables, spacingVariables)
const compactVariables = {
  minimumButtonSize: '1.8rem',
  minimumLineSize: '1.5rem',
  minimumSpacing: '0.3rem',
}

// spacing is set in stone as three choices
// coloring provides some themes, and allows infinite customization

const spacingModes = {
  compact: compactVariables,
  cozy: _.reduce(
    comfortableVariables,
    (result, value, key) => {
      result[key] =
        (parseFloat(value) + parseFloat(compactVariables[key])) / 2 + 'rem'
      return result
    },
    {}
  ),
  comfortable: comfortableVariables,
}

const colorModes = {
  dark: _.pick(baseVariables, colorVariables),
  light: {
    baseColor: 'white',
    'primary-color': 'blue',
    'positive-color': 'blue',
    'negative-color': 'red',
    'warning-color': 'yellow',
    'favorite-color': 'orange',
  },
  custom: {},
}

function sanitizeColors(theme) {
  colorVariables.forEach(color => {
    if (!validTextColour(theme[color])) {
      theme[color] = 'white' // default color
    } else {
      theme[color] = removeAlpha(theme[color]) // remove transparency
    }
  })
}

module.exports = Backbone.Model.extend({
  defaults() {
    const blob = {
      spacingMode: _get(properties, 'spacingMode', 'comfortable'),
      theme: _get(properties, 'theme', 'dark'),
    }
    colorVariables.forEach(color => {
      blob[color] = _get(properties, color, 'white')
    })
    return blob
  },
  initialize() {},
  getCustomColorNames() {
    return colorVariables
  },
  getTheme() {
    const theme = this.toJSON()
    sanitizeColors(theme)

    return _.defaults(
      theme,
      spacingModes[theme.spacingMode] /* , colorModes[theme.colorMode] */
    )
  },
  getColorMode() {
    return this.get('theme')
  },
  getSpacingMode() {
    return this.get('spacingMode')
  },
})

if (module.hot) {
  module.hot.accept('js/uncompiled-less.unless', () => {
    lessStyles = require('js/uncompiled-less.unless')
  })
}
