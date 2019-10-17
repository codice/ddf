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
const PropertyView = require('../../component/property/property.view.js')
const PropertyModel = require('../../component/property/property.js')

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
  propertyModel: any
}

export default hot(module)(
  withListenTo(
    class EnumComponent extends React.Component<Props, State> {
      constructor(props: Props) {
        super(props)

        const propertyModel = new PropertyModel({
          label: this.props.label,
          enum: this.props.options,
          value: [this.props.value],
          isEditing: true,
        })

        this.state = { propertyModel: propertyModel }
      }
      componentDidMount() {
        if (this.props.onChange !== undefined) {
          this.props.listenTo(
            this.state.propertyModel,
            'change:value',
            this.onChange.bind(this)
          )
        }
      }
      componentWillReceiveProps = (nextProps: Props) => {
        if (nextProps.value !== this.props.value) {
          this.state.propertyModel &&
            this.state.propertyModel.setValue([nextProps.value])
        }
      }
      onChange() {
        if (this.props.onChange !== undefined) {
          this.props.onChange(this.state.propertyModel.getValue()[0])
        }
      }
      render() {
        //Forces re-render otherwise the region doesn't get replaced because the view doesn't change
        const Container = class extends MarionetteRegionContainer {}

        return (
          <Container
            view={PropertyView}
            viewOptions={() => ({
              model: this.state.propertyModel,
            })}
            replaceElement
          />
        )
      }
    }
  )
)
