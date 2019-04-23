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
import withListenTo, { WithBackboneProps } from '../backbone-container'
import { hot } from 'react-hot-loader'
const PropertyView = require('../../../component/property/property.view.js')
const PropertyModel = require('../../../component/property/property.js')

type Option = {
  label: string
  value: any
}

type Props = {
  label: string
  options: Option[]
  value: any
  onChange?: (value: any) => void
} & WithBackboneProps

type State = {
  value: any
}

export default hot(module)(
  withListenTo(
    class EnumComponent extends React.Component<Props, State> {
      constructor(props: Props) {
        super(props)
        this.state = {
          value: this.props.value,
        }
        this.propertyModel = new PropertyModel({
          label: this.props.label,
          enum: this.props.options,
          value: [this.state.value],
          isEditing: true,
        })
      }
      propertyModel: any
      componentDidMount() {
        if (this.props.onChange !== undefined) {
          this.props.listenTo(
            this.propertyModel,
            'change:value',
            this.onChange.bind(this)
          )
        }
      }
      onChange() {
        if (this.props.onChange !== undefined) {
          this.props.onChange(this.propertyModel.getValue()[0])
        }
      }
      render() {
        return (
          <MarionetteRegionContainer
            view={PropertyView}
            viewOptions={() => ({
              model: this.propertyModel,
            })}
            replaceElement
          />
        )
      }
    }
  )
)
