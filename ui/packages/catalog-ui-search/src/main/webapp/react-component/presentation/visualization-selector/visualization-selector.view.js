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
import React from 'react'
import styled from '../../styles/styled-components'
const _ = require('underscore')

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

class VisualizationSelector extends React.Component {
  dragSources = []
  constructor(props) {
    super(props)
    this.el = React.createRef()
  }
  render() {
    return (
      <div ref={this.el}>
        <CustomElement onClick={this.handleChoice.bind(this)}>
          <Visualization
            className="choice-2dmap is-button"
            onMouseDown={this.handleMouseDown.bind(this, 'openlayers')}
            onMouseUp={this.handleMouseUp.bind(this, 'openlayers')}
          >
            <div className="visualization-icon fa fa-map" />
            <div className="visualization-text">2D Map</div>
          </Visualization>
          <Visualization
            className="choice-3dmap is-button"
            onMouseDown={this.handleMouseDown.bind(this, 'cesium')}
            onMouseUp={this.handleMouseUp.bind(this, 'cesium')}
          >
            <div className="visualization-icon fa fa-globe" />
            <div className="visualization-text">3D Map</div>
          </Visualization>
          <Visualization
            className="choice-inspector is-button"
            onMouseDown={this.handleMouseDown.bind(this, 'inspector')}
            onMouseUp={this.handleMouseUp.bind(this, 'inspector')}
          >
            <div className="visualization-icon fa fa-info" />
            <div className="visualization-text">Inspector</div>
          </Visualization>
          <Visualization
            className="choice-histogram is-button"
            onMouseDown={this.handleMouseDown.bind(this, 'histogram')}
            onMouseUp={this.handleMouseUp.bind(this, 'histogram')}
          >
            <div className="visualization-icon fa fa-bar-chart" />
            <div className="visualization-text">Histogram</div>
          </Visualization>
          <Visualization
            className="choice-table is-button"
            onMouseDown={this.handleMouseDown.bind(this, 'table')}
            onMouseUp={this.handleMouseUp.bind(this, 'table')}
          >
            <div className="visualization-icon fa fa-table" />
            <div className="visualization-text">Table</div>
          </Visualization>
        </CustomElement>
      </div>
    )
  }

  componentDidMount() {
    this.dragSources = []
    this.dragSources.push(
      this.props.goldenLayout.createDragSource(
        this.el.current.querySelector('.choice-2dmap'),
        configs.openlayers
      )
    )
    this.dragSources.push(
      this.props.goldenLayout.createDragSource(
        this.el.current.querySelector('.choice-3dmap'),
        configs.cesium
      )
    )
    this.dragSources.push(
      this.props.goldenLayout.createDragSource(
        this.el.current.querySelector('.choice-histogram'),
        configs.histogram
      )
    )
    this.dragSources.push(
      this.props.goldenLayout.createDragSource(
        this.el.current.querySelector('.choice-table'),
        configs.table
      )
    )
    this.dragSources.push(
      this.props.goldenLayout.createDragSource(
        this.el.current.querySelector('.choice-inspector'),
        configs.inspector
      )
    )
    this.listenToDragSources()
  }
  listenToDragStart(dragSource) {
    dragSource._dragListener.on('dragStart', () => {
      this.interimState = false
    })
  }
  listenToDragStop(dragSource) {
    dragSource._dragListener.on('dragStop', () => {
      this.listenToDragStart(dragSource)
      this.listenToDragStop(dragSource)
    })
  }
  listenToDragSources() {
    this.dragSources.forEach(dragSource => {
      this.listenToDragStart(dragSource)
      this.listenToDragStop(dragSource)
    })
  }
  handleChoice() {
    this.props.onClose()
  }
  handleMouseDown(event, choice) {
    unMaximize(this.props.goldenLayout.root)
    this.interimState = true
    this.interimChoice = choice
  }
  handleMouseUp(choice) {
    if (this.interimState) {
      if (this.props.goldenLayout.root.contentItems.length === 0) {
        this.props.goldenLayout.root.addChild({
          type: 'column',
          content: [configs[choice]],
        })
      } else {
        this.props.goldenLayout.root.contentItems[0].addChild(configs[choice])
      }
    }
    this.interimState = false
  }
}
export default VisualizationSelector
