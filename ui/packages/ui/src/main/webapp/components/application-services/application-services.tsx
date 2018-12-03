/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
import * as React from 'react'
const Service = require('js/models/Service')
import ServiceElement, { ServiceType } from './service'
import { ConfigurationType } from './configuration'
import styled from '../../styles/styled-components'

import WithBackbone, { WithBackboneProps } from '../hocs/backbone'
import { hot } from 'react-hot-loader'
const wreqr = require('js/wreqr.js')

type Props = {
  url: string
} & WithBackboneProps
type State = {
  collection: any[]
}
const Header = styled.h4`
  text-align: center;
`

const mapModelToState = (model: any) => {
  const collection = model.get('value')
  const jsonCollection = collection.toJSON()
  jsonCollection.forEach((service: ServiceType) => {
    service.model = collection.get(service.id)
    service.configurations.forEach((configuration: ConfigurationType) => {
      configuration.model = service.model
        .get('configurations')
        .get(configuration.id)
      configuration.serviceModel = service.model
    })
  })
  return {
    collection: jsonCollection,
  }
}

class ApplicationServices extends React.Component<Props, State> {
  model = new Service.Response({ url: this.props.url })
  constructor(props: Props) {
    super(props)
    this.state = mapModelToState(this.model)
    this.model.fetch()
    this.props.listenTo(wreqr.vent, 'refreshConfigurations', () => {
      this.model.fetch()
    })
    this.props.listenTo(this.model, 'sync', this.updateState)
  }
  updateState = () => {
    this.setState(mapModelToState(this.model))
  }
  render() {
    return (
      <>
        {this.state.collection.length === 0 ? (
          <Header>
            <span className="fa fa-refresh fa-spin fa-5x" />
            <div>Loading Configurations</div>
          </Header>
        ) : (
          this.state.collection.map((service: ServiceType) => {
            return <ServiceElement key={service.id} {...service} />
          })
        )}
      </>
    )
  }
}

export default hot(module)(WithBackbone(ApplicationServices))
