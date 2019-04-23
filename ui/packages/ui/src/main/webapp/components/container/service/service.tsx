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
import { hot } from 'react-hot-loader'
import ServiceElement, { ServiceType } from '../../presentation/service'
const wreqr = require('js/wreqr.js')
const ServiceModel = require('js/models/Service')

type Props = ServiceType
type State = {}

class Service extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {}
  }
  getViewOptions = () => {
    const { model } = this.props
    var configuration
    var hasFactory = model.get('factory')
    var existingConfigurations = model.get('configurations')

    if (hasFactory || existingConfigurations.isEmpty()) {
      wreqr.vent.trigger('poller:stop')
      configuration = new ServiceModel.Configuration().initializeFromModel(
        model
      )
    } else {
      configuration = existingConfigurations.at(0)
    }
    return {
      model: configuration,
      service: model,
    }
  }
  render() {
    return (
      <ServiceElement getViewOptions={this.getViewOptions} {...this.props} />
    )
  }
}

export default hot(module)(Service)
