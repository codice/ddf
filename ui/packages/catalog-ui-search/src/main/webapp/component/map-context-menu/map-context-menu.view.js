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
const InspectorView = require('../visualization/inspector/inspector.view.js')
const HistogramView = require('../visualization/histogram/histogram.view.js')
const SelectionInterfaceModel = require('../selection-interface/selection-interface.model.js')
const lightboxInstance = require('../lightbox/lightbox.view.instance.js')

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
  selectMenu(value) {
    switch (value) {
      case 'Histogram':
        this.triggerHistogram()
        break
      case 'HistogramSelection':
        this.triggerHistogramSelection()
        break
      case 'Inspector':
        this.triggerViewDetails()
        break
      case 'InspectorSelection':
        this.triggerViewDetailsSelection()
        break
      default:
        break
    }
  },
  initialize() {
    this.debounceUpdateSelectionInterface()
    this.selectionInterface = new SelectionInterfaceModel()
    this.listenTo(
      this.options.mapModel,
      'change:clickLat change:clickLon',
      this.render
    )
    this.listenTo(
      this.options.selectionInterface.getSelectedResults(),
      'update add remove reset',
      this.handleSelectionChange
    )
    this.listenTo(
      this.options.selectionInterface.getActiveSearchResults(),
      'update add remove reset',
      this.handleResultsChange
    )
    this.listenTo(
      this.selectionInterface.getSelectedResults(),
      'update add remove reset',
      this.updateSelectionInterface
    )
  },
  debounceUpdateSelectionInterface() {
    this.updateSelectionInterface = _.debounce(
      this.updateSelectionInterface,
      200,
      { leading: false, trailing: true }
    )
  },
  updateSelectionInterface() {
    this.options.selectionInterface.clearSelectedResults()
    this.options.selectionInterface.addSelectedResult(
      this.selectionInterface.getSelectedResults().models
    )
  },
  handleResultsChange() {
    this.$el.toggleClass(
      'has-results',
      this.options.selectionInterface.getActiveSearchResults().length > 0
    )
  },
  handleSelectionChange() {
    this.$el.toggleClass(
      'has-selection',
      this.options.selectionInterface.getSelectedResults().length > 0
    )
  },
  triggerClick() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  triggerHistogram() {
    this.triggerClick()
    lightboxInstance.model.updateTitle('Histogram')
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      new HistogramView({
        selectionInterface: this.options.selectionInterface,
      })
    )
  },
  triggerHistogramSelection() {
    this.triggerClick()
    this.stopListening(
      this.selectionInterface.getSelectedResults(),
      'update add remove reset',
      this.updateSelectionInterface
    )
    this.selectionInterface.addSelectedResult(
      this.options.selectionInterface.getSelectedResults().models
    )
    lightboxInstance.model.updateTitle('Histogram')
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      new HistogramView({
        selectionInterface: this.selectionInterface,
      })
    )
    this.listenTo(
      this.selectionInterface.getSelectedResults(),
      'update add remove reset',
      this.updateSelectionInterface
    )
  },
  triggerViewDetailsSelection() {
    this.triggerClick()
    lightboxInstance.model.updateTitle('Inspector')
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      new InspectorView({
        selectionInterface: this.options.selectionInterface,
      })
    )
  },
  triggerViewDetails() {
    this.triggerClick()
    this.stopListening(
      this.selectionInterface.getSelectedResults(),
      'update add remove reset',
      this.updateSelectionInterface
    )
    this.selectionInterface.clearSelectedResults()
    this.selectionInterface.addSelectedResult(this.previousHoverModel)
    this.selectionInterface.setCurrentQuery(
      this.options.selectionInterface.getCurrentQuery()
    )
    lightboxInstance.model.updateTitle('Inspector')
    lightboxInstance.model.open()
    lightboxInstance.showContent(
      new InspectorView({
        selectionInterface: this.selectionInterface,
      })
    )
  },
  onRender() {
    this.repositionDropdown()
    this.handleTarget()
    this.handleSelectionChange()
    this.handleResultsChange()
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
