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
import MarionetteRegionContainer from '../marionette-region-container'

const $ = require('jquery')
const wreqr = require('../../../js/wreqr.js')
import LoadingCompanion from '../loading-companion'

interface Props {
  isMenu: boolean
  routeDefinition: any
}

// needed for golden-layout
const triggerResize = () => {
  setTimeout(() => {
    wreqr.vent.trigger('resize')
    $(window).trigger('resize')
  }, 100)
}

const isFetched = (props: Props) => {
  if (props.isMenu) {
    return props.routeDefinition.menu.component !== undefined
  } else {
    return props.routeDefinition.component !== undefined
  }
}

interface Extension {
  deferred: any
}

class RouteContainer
  extends React.Component<
    Props,
    { routeDefinition: any; isMenu: boolean; isFetched: boolean }
  >
  implements Extension {
  constructor(props: Props) {
    super(props)
    this.state = {
      routeDefinition: props.routeDefinition,
      isMenu: props.isMenu,
      isFetched: isFetched(props),
    }
  }
  deferred: any
  getComponent() {
    if (this.state.isMenu) {
      return this.state.routeDefinition.menu.component
    } else {
      return this.state.routeDefinition.component
    }
  }
  fetchComponent() {
    if (this.state.isMenu) {
      return this.state.routeDefinition.menu.getComponent()
    } else {
      return this.state.routeDefinition.getComponent()
    }
  }
  componentDidUpdate() {
    if (this.state.isFetched !== false) {
      triggerResize()
      this.componentDidUpdate = () => {}
    }
  }
  componentDidMount() {
    this.deferred = this.fetchComponent()
    this.deferred.then(() => {
      this.setState({
        isFetched: true,
      })
    })
  }
  componentWillUnmount() {
    this.deferred.reject()
  }
  determineComponent() {
    const Component = this.getComponent()
    if (Component._isMarionetteView) {
      return <MarionetteRegionContainer view={Component} />
    } else {
      return <Component />
    }
  }
  render() {
    return (
      <LoadingCompanion loading={!this.state.isFetched}>
        {this.state.isFetched ? this.determineComponent() : ''}
      </LoadingCompanion>
    )
  }
}

export default RouteContainer
