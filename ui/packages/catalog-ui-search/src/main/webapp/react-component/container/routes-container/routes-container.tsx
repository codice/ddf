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
import RouteContainer from '../route-container'

const router = require('../../../component/router/router.js')

import styled from '../../styles/styled-components'
import { CustomElement } from '../../styles/mixins'
import withListenTo, { WithBackboneProps } from '../backbone-container'

type RouteWrapperProps = {
  isCurrentRoute: boolean
}

type Props = {
  isMenu: boolean
  routeDefinitions: object
} & WithBackboneProps

const RouteWrapper = styled.div`
  ${CustomElement} ${(props: RouteWrapperProps) => {
    if (props.isCurrentRoute) {
      return `display: block;`
    } else {
      return `display: none;`
    }
  }};
`

class RoutesContainer extends React.Component<
  Props,
  { currentRoute: string; routeDefinitions: any; isMenu: boolean }
> {
  constructor(props: Props) {
    super(props)
    this.state = {
      routeDefinitions: props.routeDefinitions,
      currentRoute: router.toJSON().name,
      isMenu: props.isMenu,
    }
  }
  componentDidMount() {
    this.props.listenTo(router, 'change', this.handleRouterChange.bind(this))
  }
  handleRouterChange() {
    this.setState({
      currentRoute: router.toJSON().name,
    })
  }
  /*
        We want to keep things around to handle reconcilation.
    */
  shouldShowRouteComponent(routeName: string) {
    if (this.state.currentRoute === routeName) {
      return true
    }
    if (this.state.isMenu) {
      return this.state.routeDefinitions[routeName].menu.component !== undefined
    } else {
      return this.state.routeDefinitions[routeName].component !== undefined
    }
  }
  isCurrentRoute(routeName: string) {
    return this.state.currentRoute === routeName
  }
  render() {
    return (
      <React.Fragment>
        {Object.keys(this.state.routeDefinitions)
          .filter(this.shouldShowRouteComponent.bind(this))
          .map(routeName => {
            return (
              <RouteWrapper
                key={routeName}
                isCurrentRoute={this.isCurrentRoute(routeName)}
              >
                <RouteContainer
                  routeDefinition={this.state.routeDefinitions[routeName]}
                  isMenu={this.state.isMenu}
                />
              </RouteWrapper>
            )
          })}
      </React.Fragment>
    )
  }
}

export default withListenTo(RoutesContainer)
