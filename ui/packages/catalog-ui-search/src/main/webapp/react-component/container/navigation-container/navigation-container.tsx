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
import Navigation from '../../presentation/navigation'
import withListenTo, { WithBackboneProps } from '../backbone-container'

const store = require('../../../js/store.js')
const wreqr = require('../../../js/wreqr.js')
const sources = require('../../../component/singletons/sources-instance.js')
const properties = require('../../../js/properties.js')

const hasLogo = () => {
  return properties.showLogo && properties.ui.vendorImage !== ''
}

const hasUnavailable = () => {
  return sources.some(function(source: Backbone.Model) {
    return !source.get('available')
  })
}

const hasUnsaved = () => {
  return store.get('workspaces').some(function(workspace: any) {
    return !workspace.isSaved()
  })
}

const isDrawing = () => {
  return store.get('content').get('drawing')
}

const turnOffDrawing = () => {
  wreqr.vent.trigger('search:drawend', store.get('content').get('drawingModel'))
}

type Props = {
  routeDefinitions: object
} & WithBackboneProps

type State = {
  hasLogo: boolean
  hasUnavailable: boolean
  hasUnsaved: boolean
  isDrawing: boolean
  logo: string
}

class NavigationContainer extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      hasLogo: hasLogo(),
      hasUnavailable: hasUnavailable(),
      hasUnsaved: hasUnsaved(),
      isDrawing: isDrawing(),
      logo: properties.ui.vendorImage,
    }
  }
  componentDidMount() {
    this.props.listenTo(
      store.get('workspaces'),
      'change:saved update add remove',
      this.handleSaved.bind(this)
    )
    this.props.listenTo(sources, 'all', this.handleSources.bind(this))
    this.props.listenTo(
      store.get('content'),
      'change:drawing',
      this.handleDrawing.bind(this)
    )
  }
  handleSaved() {
    this.setState({
      hasUnsaved: hasUnsaved(),
    })
  }
  handleSources() {
    this.setState({
      hasUnavailable: hasUnavailable(),
    })
  }
  handleDrawing() {
    this.setState({
      isDrawing: isDrawing(),
    })
  }
  render() {
    return (
      <Navigation
        isDrawing={this.state.isDrawing}
        hasUnavailable={this.state.hasUnavailable}
        hasUnsaved={this.state.hasUnsaved}
        hasLogo={this.state.hasLogo}
        logo={this.state.logo}
        turnOffDrawing={() => {
          turnOffDrawing()
        }}
        {...this.props}
      />
    )
  }
}

export default withListenTo(NavigationContainer)
