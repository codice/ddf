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
import Router from '../../presentation/router'

import Navigation from '../navigation-container'
import ExtensionPoints from '../../../extension-points'

type Props = {
  navigation: React.ReactNode
  routeDefinitions: object
}

const Providers = ExtensionPoints.providers

class RouterContainer extends React.Component<Props, {}> {
  constructor(props: Props) {
    super(props)
  }
  render() {
    const navigation = <Navigation {...this.props} />
    return (
      <Providers>
        <Router
          nav={navigation}
          routeDefinitions={this.props.routeDefinitions}
          {...this.props}
        />
      </Providers>
    )
  }
}

export default RouterContainer
