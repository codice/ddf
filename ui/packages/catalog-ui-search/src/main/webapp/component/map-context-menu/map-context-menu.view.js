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
import { MapContextMenu } from '../../react-component/map-context-menu'

const _ = require('underscore')
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')

module.exports = Marionette.LayoutView.extend({
  template({ mouseLat, mouseLon, target, coordinateValues, selectionCount }) {
    return (
      <MapContextMenu
        onChange={value => this.selectMenu(value)}
        mouseLat={mouseLat}
        mouseLon={mouseLon}
        target={target}
        coordinateValues={coordinateValues}
        selectionCount={selectionCount}
        closeMenu={this.triggerClick.bind(this)}
      />
    )
  },
  tagName: CustomElements.register('map-context-menu'),
  className: 'composed-menu',
  modelEvents: {
    change: 'render',
  },
  initialize() {
    this.listenTo(
      this.options.mapModel,
      'change:clickLat change:clickLon',
      this.render
    )
  },
  triggerClick() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },

  onRender() {
    this.repositionDropdown()
    this.handleTarget()
    this.keepHoverMetacardAround()
    this.handleOffMap()
  },
  handleOffMap() {
    this.$el.toggleClass('is-off-map', this.options.mapModel.isOffMap())
  },
  keepHoverMetacardAround() {
    this.previousHoverModel =
      this.options.mapModel.get('targetMetacard') || this.previousHoverModel // save in case they hover elsewhere and it's lost, then the user clicks view details
  },
  handleTarget() {
    this.$el.toggleClass(
      'has-target',
      this.options.mapModel.get('target') !== undefined
    )
  },
  serializeData() {
    const mapModelJSON = this.options.mapModel.toJSON()
    mapModelJSON.selectionCount = this.options.selectionInterface.getSelectedResults().length
    return mapModelJSON
  },
  repositionDropdown() {
    this.$el.trigger('repositionDropdown.' + CustomElements.getNamespace())
  },
})
