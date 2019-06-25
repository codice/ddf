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
import MarionetteRegionContainer from '@connexta/atlas/atoms/marionette'
const ConfigurationEditView = require('components/configuration-edit/configuration-edit.view')
  .View
import styled from '@connexta/atlas/styled'
const $ = require('jquery')
import getId from '../../uuid'
import Button from '@connexta/atlas/atoms/button'

const ConfigurationElement = styled.div`
  display: block;
  white-space: nowrap;
  padding: ${props => props.theme.minimumSpacing};
  margin-left: ${props => props.theme.largeSpacing};
  > div {
    padding-left: ${props => props.theme.minimumSpacing};
    display: inline-block;
    white-space: normal;
    word-wrap: break-word;
    width: 45%;
    vertical-align: top;
  }

  > div:last-of-type {
    text-align: right;
    width: 10%;
  }
`

export type ConfigurationType = {
  id: string
  fpid: string
  enabled: boolean
  name: string
  uuid: string
  properties: any
  displayName: string
  bundle_name: string
  hasFactory: boolean
  model: any
  serviceModel: any
}
type Props = {
  destroy: () => void
  onBeforeEdit: () => void
} & ConfigurationType
type State = {
  isEditing: boolean
}

class Configuration extends React.Component<Props, State> {
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
    const {
      displayName,
      bundle_name,
      fpid,
      properties,
      model,
      serviceModel,
      onBeforeEdit,
      destroy,
    } = this.props
    const { isEditing } = this.state
    return (
      <ConfigurationElement>
        <div>
          <a
            href={`#${this.id}`}
            className="editLink"
            data-toggle="modal"
            data-backdrop="static"
            data-keyboard="false"
            onClick={() => {
              onBeforeEdit()
              this.startEditing()
            }}
          >
            {displayName}
          </a>
          <div className="config-modal-container">
            <div
              id={this.id}
              className="config-modal modal"
              tabIndex={-1}
              role="dialog"
              aria-hidden="true"
              ref={this.modalRef as any}
            >
              {isEditing ? (
                <MarionetteRegionContainer
                  view={ConfigurationEditView}
                  viewOptions={{
                    model,
                    service: serviceModel,
                    stopEditing: this.stopEditing,
                  }}
                />
              ) : null}
            </div>
          </div>
        </div>
        <div>{bundle_name}</div>
        <div>
          {fpid ? (
            <Button
              color="secondary"
              onClick={(e: { preventDefault: () => void }) => {
                e.preventDefault()
                var question =
                  'Are you sure you want to remove the configuration: ' +
                  properties['service.pid'] +
                  '?'
                var confirmation = window.confirm(question)
                if (confirmation) {
                  destroy()
                }
              }}
            >
              <span className="glyphicon glyphicon-remove" />
            </Button>
          ) : null}
        </div>
      </ConfigurationElement>
    )
  }
}

export default hot(module)(Configuration)
