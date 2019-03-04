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
import styled from '../../react-component/styles/styled-components'
var Marionette = require('marionette')
var CustomElements = require('../../js/CustomElements.js')
var _ = require('underscore')
var user = require('../singletons/user-instance.js')

const CustomElement = styled.div`
  height: 100%;
  width: 100%;
  display: block;
`
const Visualization = styled.div`
  .visualization-choice {
    white-space: nowrap;
    padding: ${props => props.theme.largeSpacing}
    cursor: move;
    cursor: grab;
    cursor: -moz-grab;
    cursor: -webkit-grab;
  }
  .visualization-icon {
    text-align: center;
    width: ${props => props.theme.minimumButtonSize}
  }

  .visualization-icon,
  .visualization-text {
    display: inline-block;
    vertical-align: middle;
  }

  .visualization-text {
    width: ~'calc(100% - ${props => props.theme.minimumButtonSize})';
    font-size: ${props => props.theme.mediumFontSize};
    overflow: hidden;
    text-overflow: ellipsis;
  }
`

var configs = {
  openlayers: {
    title: '2D Map',
    type: 'component',
    componentName: 'openlayers',
    componentState: {},
  },
  cesium: {
    title: '3D Map',
    type: 'component',
    componentName: 'cesium',
    componentState: {},
  },
  inspector: {
    title: 'Inspector',
    type: 'component',
    componentName: 'inspector',
    componentState: {},
  },
  table: {
    title: 'Table',
    type: 'component',
    componentName: 'table',
    componentState: {},
  },
  histogram: {
    title: 'Histogram',
    type: 'component',
    componentName: 'histogram',
    componentState: {},
  },
}

function unMaximize(contentItem) {
  if (contentItem.isMaximised) {
    contentItem.toggleMaximise()
    return true
  } else if (contentItem.contentItems.length === 0) {
    return false
  } else {
    return _.some(contentItem.contentItems, subContentItem => {
      return unMaximize(subContentItem)
    })
  }
}

module.exports = Marionette.ItemView.extend({
  template() {
    return (
      <CustomElement onMouseDown={this.handleMouseDown.bind(this)} onMouseUp={this.handleMouseUp.bind(this)} onClick={this.handleChoice.bind(this)}>
        <Visualization className="choice-2dmap is-button" data-choice="openlayers">
          <div className="visualization-icon fa fa-map" />
          <div className="visualization-text">2D Map</div>
        </Visualization>
        <Visualization className="choice-3dmap is-button" data-choice="cesium">
          <div className="visualization-icon fa fa-globe" />
          <div className="visualization-text">3D Map</div>
        </Visualization>
        <Visualization className="choice-inspector is-button" data-choice="inspector">
          <div className="visualization-icon fa fa-info" />
          <div className="visualization-text">Inspector</div>
        </Visualization>
        <Visualization className="choice-histogram is-button" data-choice="histogram">
          <div className="visualization-icon fa fa-bar-chart" />
          <div className="visualization-text">Histogram</div> 
        </Visualization>
        <Visualization className="choice-table is-button" data-choice="table">
          <div className="visualization-icon fa fa-table" />
          <div className="visualization-text">Table</div>
        </Visualization>
      </CustomElement>
    )
  } ,
  dragSources: [],
  onRender: function() {
    this.dragSources = []
    this.dragSources.push(
      this.options.goldenLayout.createDragSource(
        this.el.querySelector('.choice-2dmap'),
        configs.openlayers
      )
    )
    this.dragSources.push(
      this.options.goldenLayout.createDragSource(
        this.el.querySelector('.choice-3dmap'),
        configs.cesium
      )
    )
    this.dragSources.push(
      this.options.goldenLayout.createDragSource(
        this.el.querySelector('.choice-histogram'),
        configs.histogram
      )
    )
    this.dragSources.push(
      this.options.goldenLayout.createDragSource(
        this.el.querySelector('.choice-table'),
        configs.table
      )
    )
    this.dragSources.push(
      this.options.goldenLayout.createDragSource(
        this.el.querySelector('.choice-inspector'),
        configs.inspector
      )
    )
    this.listenToDragSources()
  },
  listenToDragStart: function(dragSource) {
    dragSource._dragListener.on('dragStart', () => {
      this.interimState = false
    })
  },
  listenToDragStop: function(dragSource) {
    dragSource._dragListener.on('dragStop', () => {
      this.listenToDragStart(dragSource)
      this.listenToDragStop(dragSource)
    })
  },
  listenToDragSources: function() {
    this.dragSources.forEach(dragSource => {
      this.listenToDragStart(dragSource)
      this.listenToDragStop(dragSource)
    })
  },
  handleChoice: function() {
    this.$el.trigger('closeSlideout.' + CustomElements.getNamespace())
  },
  handleMouseDown: function(event) {
    unMaximize(this.options.goldenLayout.root)
    this.interimState = true
    this.interimChoice = event.currentTarget.getAttribute('data-choice')
  },
  handleMouseUp: function() {
    if (this.interimState) {
      if (this.options.goldenLayout.root.contentItems.length === 0) {
        this.options.goldenLayout.root.addChild({
          type: 'column',
          content: [configs[this.interimChoice]],
        })
      } else {
        this.options.goldenLayout.root.contentItems[0].addChild(
          configs[this.interimChoice]
        )
      }
    }
    this.interimState = false
  },
})
