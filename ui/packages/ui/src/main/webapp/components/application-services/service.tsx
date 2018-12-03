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
import { hot } from 'react-hot-loader'
import styled from '../../styles/styled-components'
import MarionetteRegionContainer from '../container/marionette-region-container'
const ConfigurationEditView = require('components/configuration-edit/configuration-edit.view')
  .View
import Configuration, { ConfigurationType } from './configuration'
const ServiceModel = require('js/models/Service')
const wreqr = require('js/wreqr.js')
const $ = require('jquery')
import getId from '../uuid'
const Root = styled.div`
  padding: ${props => props.theme.minimumSpacing};
`
const Link = styled.a`
  text-decoration: none !important;
  width: 100%;
  font-weight: bold;
`
const ServiceDivider = styled.hr`
  border-bottom: solid 3px #eee;
  margin: 0;
`
export type ServiceType = {
  factory: boolean
  name: string
  description: string
  id: string
  configurations: any[]
  metatype: any[]
  uuid: string
  model: any
}
type Props = {} & ServiceType
type State = {
  isEditing: boolean
}

class Service extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = {
      isEditing: false,
    }
  }
  startEditing = () => {
    this.setState({
      isEditing: true,
    })
  }
  stopEditing = () => {
    this.setState({
      isEditing: false,
    })
    if (this.modalRef.current) {
      $(this.modalRef.current).modal('hide')
    }
  }
  id = getId()
  modalRef = React.createRef() as any
  render() {
    const { name, configurations, model } = this.props
    return (
      <Root>
        <Link
          href={`#${this.id}`}
          className="newLink"
          data-toggle="modal"
          data-backdrop="static"
          data-keyboard="false"
          onClick={this.startEditing}
        >
          {name}
        </Link>
        <ServiceDivider />
        {configurations.map((configuration: ConfigurationType) => {
          return <Configuration key={configuration.id} {...configuration} />
        })}
        <div
          id={this.id}
          className="service-modal modal"
          tabIndex={-1}
          role="dialog"
          aria-hidden="true"
          ref={this.modalRef as any}
        >
          {this.state.isEditing ? (
            <MarionetteRegionContainer
              view={ConfigurationEditView}
              viewOptions={(() => {
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
                  stopEditing: this.stopEditing,
                }
              })()}
            />
          ) : null}
        </div>
      </Root>
    )
  }
}

export default hot(module)(Service)
