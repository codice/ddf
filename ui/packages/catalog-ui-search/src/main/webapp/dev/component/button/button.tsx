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
import styled from '../../../react-component/styles/styled-components'
import {
  Button,
  buttonTypeEnum,
} from '../../../react-component/presentation/button'
import { LiveProvider, LiveEditor, LiveError, LivePreview } from 'react-live'

import MarionetteRegionContainer from '../../../react-component/container/marionette-region-container'
const PropertyView = require('../../../component/property/property.view.js')
const PropertyModel = require('../../../component/property/property.js')

const Root = styled.div`
  height: 100%;
`

function getEnumTypes(enumObject: object) {
  return Object.keys(enumObject).slice(Object.keys(enumObject).length / 2)
}

class ButtonGuide extends React.Component<
  {},
  {
    fadeUntilHoverModel: any
    code: any
    buttonTypeModel: any
    inTextModel: any
    disabledModel: any
    textModel: any
    iconModel: any
  }
> {
  constructor(props: any) {
    super(props)
    const enumeration = getEnumTypes(buttonTypeEnum).map((type: any) => {
      return {
        label: type,
        value: buttonTypeEnum[type],
      }
    })
    this.state = {
      buttonTypeModel: new PropertyModel({
        isEditing: true,
        label: 'Button Type',
        value: [enumeration[0].value],
        enum: enumeration,
      }),
      disabledModel: new PropertyModel({
        isEditing: true,
        label: 'Disabled',
        value: [true],
        type: 'BOOLEAN',
      }),
      textModel: new PropertyModel({
        isEditing: true,
        label: 'Text',
        value: ['Buttons are fun'],
        type: 'STRING',
      }),
      iconModel: new PropertyModel({
        isEditing: true,
        label: 'Icon',
        value: ['fa fa-home'],
        enum: [
          {
            label: 'home',
            value: 'fa fa-home',
          },
          {
            label: 'question',
            value: 'fa fa-question',
          },
          {
            label: 'Text Only',
            value: '',
          },
        ],
      }),
      inTextModel: new PropertyModel({
        isEditing: true,
        label: 'In Text',
        value: [false],
        type: 'BOOLEAN',
      }),
      fadeUntilHoverModel: new PropertyModel({
        isEditing: true,
        label: 'Fade until hover',
        value: [false],
        type: 'BOOLEAN',
      }),
      code: '',
    }
    this.state.buttonTypeModel.on('change:value', () => {
      this.updateCode()
    })
    this.state.disabledModel.on('change:value', () => {
      this.updateCode()
    })
    this.state.textModel.on('change:value', () => {
      this.updateCode()
    })
    this.state.iconModel.on('change:value', () => {
      this.updateCode()
    })
    this.state.inTextModel.on('change:value', () => {
      this.updateCode()
    })
    this.state.fadeUntilHoverModel.on('change:value', () => {
      this.updateCode()
    })
  }
  updateCode() {
    this.setState({
      code: this.getCode(),
    })
  }
  componentDidMount() {
    this.setState({
      code: this.getCode(),
    })
  }
  getCode() {
    const inText = this.state.inTextModel.getValue()[0]
    return `
            ${inText ? `<div>in the flow of a` : ''}
            <Button 
                buttonType={buttonTypeEnum.${
                  getEnumTypes(buttonTypeEnum)[
                    this.state.buttonTypeModel.getValue()[0]
                  ]
                }}
                text="${this.state.textModel.getValue()[0]}"
                disabled={${this.state.disabledModel.getValue()[0]}}
                icon="${this.state.iconModel.getValue()[0]}"
                style={{padding: '0px 10px'}}
                inText={${inText}}
                fadeUntilHover={${this.state.fadeUntilHoverModel.getValue()[0]}}
            />
            ${
              inText
                ? ` sentence that me be very long who knows where it will end up and finish
                    <Button 
                    buttonType={buttonTypeEnum.${
                      getEnumTypes(buttonTypeEnum)[
                        this.state.buttonTypeModel.getValue()[0]
                      ]
                    }}
                    text="${this.state.textModel.getValue()[0]}"
                    disabled={${this.state.disabledModel.getValue()[0]}}
                    icon="${this.state.iconModel.getValue()[0]}"
                    style={{padding: '0px 10px'}}
                    inText={${inText}}
                    fadeUntilHover={${
                      this.state.fadeUntilHoverModel.getValue()[0]
                    }}
                />
            feel free to put it there </div>`
                : ''
            }
        `
  }
  render() {
    return (
      <Root {...this.props}>
        <div className="section">
          <div className="is-header">Examples</div>
          <div className="examples is-list has-list-highlighting">
            <div className="example">
              <div className="title">Controlled Example</div>
              <MarionetteRegionContainer
                view={PropertyView}
                viewOptions={() => {
                  return {
                    model: this.state.buttonTypeModel,
                  }
                }}
                replaceElement={false}
              />
              <MarionetteRegionContainer
                view={PropertyView}
                viewOptions={() => {
                  return {
                    model: this.state.disabledModel,
                  }
                }}
                replaceElement={false}
              />
              <MarionetteRegionContainer
                view={PropertyView}
                viewOptions={() => {
                  return {
                    model: this.state.textModel,
                  }
                }}
                replaceElement={false}
              />
              <MarionetteRegionContainer
                view={PropertyView}
                viewOptions={() => {
                  return {
                    model: this.state.iconModel,
                  }
                }}
                replaceElement={false}
              />
              <MarionetteRegionContainer
                view={PropertyView}
                viewOptions={() => {
                  return {
                    model: this.state.inTextModel,
                  }
                }}
                replaceElement={false}
              />
              <MarionetteRegionContainer
                view={PropertyView}
                viewOptions={() => {
                  return {
                    model: this.state.fadeUntilHoverModel,
                  }
                }}
                replaceElement={false}
              />
              <LiveProvider
                code={this.state.code}
                scope={{ Button, buttonTypeEnum }}
              >
                <LiveEditor contentEditable={false} />
                <LiveError />
                <LivePreview style={{ padding: '20px' }} />
              </LiveProvider>
            </div>
          </div>
        </div>
      </Root>
    )
  }
}

export default ButtonGuide
