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

const CustomElement = styled.div`
  height: 100%;
  width: 100%;
  display: block;
`
const Visualization = styled.div`
  cursor: pointer;
  opacity: ${props => props.theme.minimumOpacity}
  :hover {
    opacity: 1
  }
`
const VisualizationChoice = styled.div`
    white-space: nowrap;
    padding: ${props => props.theme.largeSpacing}
    cursor: move;
    cursor: grab;
    cursor: -moz-grab;
    cursor: -webkit-grab;
`

const VisualizationIcon = styled.div`
    text-align: center;
    width: ${props => props.theme.minimumButtonSize}
    display: inline-block;
    vertical-align: middle;
`
const VisualizationText = styled.div`
  width: calc(100% - ${props => props.theme.minimumButtonSize});
  font-size: ${props => props.theme.mediumFontSize};
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  vertical-align: middle;
`
const configs = {
  openlayers: {
    title: '2D Map',
    type: 'component',
    componentName: 'openlayers',
    icon: 'fa fa-map',
    componentState: {},
  },
  cesium: {
    title: '3D Map',
    type: 'component',
    componentName: 'cesium',
    icon: 'fa fa-globe',
    componentState: {},
  },
  inspector: {
    title: 'Inspector',
    type: 'component',
    componentName: 'inspector',
    icon: 'fa fa-info',
    componentState: {},
  },
  table: {
    title: 'Table',
    type: 'component',
    componentName: 'table',
    icon: 'fa fa-table',
    componentState: {},
  },
  histogram: {
    title: 'Histogram',
    type: 'component',
    componentName: 'histogram',
    icon: 'fa fa-bar-chart',
    componentState: {},
  },
}

const unMaximize = contentItem => {
  if (contentItem.isMaximised) {
    contentItem.toggleMaximise()
    return true
  } else if (contentItem.contentItems.length === 0) {
    return false
  } else {
    return Array.some(contentItem.contentItems, subContentItem => {
      return unMaximize(subContentItem)
    })
  }
}

class VisualizationSelector extends React.Component {
  dragSources = []
  constructor(props) {
    super(props)
    this.openlayers = React.createRef()
    this.cesium = React.createRef()
    this.inspector = React.createRef()
    this.histogram = React.createRef()
    this.table = React.createRef()
  }
  render() {
    return (
      <CustomElement onClick={this.handleChoice.bind(this)}>
        {Object.values(configs).map(
          ({ title, icon, componentName }, index) => (
            <Visualization
              key={index.toString()}
              innerRef={x => {
                this[componentName] = x
              }}
              onMouseDown={this.handleMouseDown.bind(this, componentName)}
              onMouseUp={this.handleMouseUp.bind(this, componentName)}
            >
              <VisualizationIcon className={icon} />
              <VisualizationText>{title}</VisualizationText>
            </Visualization>
          ),
          this
        )}
      </CustomElement>
    )
  }

  componentDidMount() {
    this.dragSources = []
    this.dragSources = Object.keys(configs).map(key =>
      this.props.goldenLayout.createDragSource(this[key], configs[key])
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
