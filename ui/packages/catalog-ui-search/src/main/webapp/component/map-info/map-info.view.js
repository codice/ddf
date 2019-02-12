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
/*global require*/
import React from 'react'

const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance.js')
const properties = require('properties')
const metacardDefinitions = require('component/singletons/metacard-definitions')
const Common = require('js/Common')
const hbHelper = require('js/HandlebarsHelpers')

const mtgeo = require('mt-geo')
const usngs = require('usng.js')

const converter = new usngs.Converter()
const usngPrecision = 6

function getCoordinateFormat() {
  return user
    .get('user')
    .get('preferences')
    .get('coordinateFormat')
}

function leftPad(numToPad, size) {
  var sign = Math.sign(numToPad) === -1 ? '-' : ''
  var numNoDecimal = Math.floor(Math.abs(numToPad))
  return new Array(sign === '-' ? size - 1 : size)
    .concat([Math.sign(numToPad) * numNoDecimal])
    .join(' ')
    .slice(-size)
}

function formatAttribute(attributeName, attributeValue) {
  if (metacardDefinitions.metacardTypes[attributeName].type === 'DATE') {
    attributeValue = Common.getHumanReadableDateTime(attributeValue)
  }
  return attributeName.toUpperCase() + ': ' + attributeValue
}

module.exports = Marionette.LayoutView.extend({
  template() {
    return (
      <React.Fragment>
        {this.getAttributes()}
        <div className="info-feature">{this.target}</div>
        <div className="info-coordinates">{this.getDisplayComponent()}</div>
      </React.Fragment>
    )
  },
  tagName: CustomElements.register('map-info'),
  modelEvents: {},
  events: {
    'click > .user-settings-navigation .navigation-choice': 'handleNavigate',
  },
  regions: {},
  ui: {},
  initialize: function() {
    this.listenTo(
      this.model,
      'change:mouseLat change:mouseLon change:target',
      this.render
    )
    this.listenTo(this.model, 'change:target', this.handleTarget)
    this.listenTo(
      user.get('user').get('preferences'),
      'change:coordinateFormat',
      this.render
    )
  },
  onBeforeShow: function() {},
  handleBack: function() {},
  handleTarget: function() {
    this.$el.toggleClass('has-feature', this.model.get('target') !== undefined)
  },
  getDisplayComponent: function() {
    const coordinateFormat = getCoordinateFormat()
    const lat = this.model.get('mouseLat')
    const lon = this.model.get('mouseLon')
    if (typeof lat === 'undefined' || typeof lon === 'undefined') {
      return null
    }
    switch (coordinateFormat) {
      case 'degrees':
        return <span>{mtgeo.toLat(lat) + ' ' + mtgeo.toLon(lon)}</span>
      case 'decimal':
        return this.decimalComponent(lat, lon)
      case 'mgrs':
        // TODO: Move leaking defensive check knowledge to usng library (DDF-4335)
        return lat > 84 || lat < -80 ? (
          'In UPS Space'
        ) : (
          <span>{converter.LLtoUSNG(lat, lon, usngPrecision)}</span>
        )
      case 'utm':
        return <span>{converter.LLtoUTMUPS(lat, lon)}</span>
    }
    throw 'Unrecognized coordinate format value [' + coordinateFormat + ']'
  },
  decimalComponent(lat, lon) {
    return (
      <span>
        {`${leftPad(lat, 3, ' ')}.${Math.abs(lat % 1)
          .toFixed(6)
          .toString()
          .slice(2)} ${leftPad(lon, 4, ' ')}.${Math.abs(lon % 1)
          .toFixed(6)
          .toString()
          .slice(2)}`}
      </span>
    )
  },
  getAttributes() {
    if (this.model.get('targetMetacard') === undefined) {
      return []
    }
    const jsx = []
    properties.summaryShow.forEach(attribute => {
      const attributeName = hbHelper.getAlias(attribute)
      const attributeValue = this.model
        .get('targetMetacard')
        .get('metacard')
        .get('properties')
        .get(attributeName)
      if (attributeValue !== undefined)
        jsx.push(
          <div className="info-feature">
            {formatAttribute(attributeName, attributeValue)}
          </div>
        )
    })
    return jsx
  },
  onRender: function() {
    this.$el.toggleClass(
      'is-off-map',
      typeof this.model.get('mouseLat') === 'undefined'
    )
  },
})
