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
import withListenTo from '../../react-component/container/backbone-container'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'
import styled from 'styled-components'
const PropertyModel = require('../../component/property/property.js')
const ValueModel = require('../../component/value/value.js')
import {
  generatePropertyJSON,
  determineView,
  transformValue,
} from './filterHelper'
const Common = require('../../js/Common.js')

const Root = styled.div`
  vertical-align: middle;
  margin-right: ${({ theme }) => theme.minimumSpacing};
  display: ${({ type }) => (type === 'EMPTY' ? 'none' : 'inline-block')};
  min-width: ${({ type, theme }) =>
    type === 'DATE'
      ? `calc(19 * ${theme.minimumFontSize})`
      : `calc(12.5* ${theme.minimumFontSize})`};
  ${({ type, theme }) =>
    type === 'LOCATION' || type === 'GEOMETRY'
      ? `margin-top: ${
          theme.minimumSpacing
        } !important; display: block !important;`
      : null} intrigue-input.is-with-param {
    min-width: ${({ theme }) => `calc(20* ${theme.minimumFontSize})`};
  }
`

const FilterInput = withListenTo(
  class FilterInput extends React.Component {
    component
    constructor(props) {
      super(props)

      this.state = this.propsToState(props)
    }

    propsToState = ({ editing, comparator, attribute }) => {
      return { editing, comparator, attribute }
    }

    componentWillReceiveProps = props => {
      this.setState(this.propsToState(props))
    }

    render() {
      this.determineInput()
      if (this.state.editing) {
        const property =
          this.component.model instanceof ValueModel
            ? this.component.model.get('property')
            : this.component.model
        property.set('isEditing', true)
      } else {
        const property =
          this.component.model instanceof ValueModel
            ? this.component.model.get('property')
            : this.component.model
        property.set(
          'isEditing',
          this.props.isForm === true || this.props.isFormBuilder === true
        )
      }

      return (
        <Root
          data-help="The value for the property to use during comparison."
          type={
            this.state.comparator === 'IS EMPTY' ? 'EMPTY' : this.props.type
          }
          className="filter-input"
        >
          <MarionetteRegionContainer view={this.component} />
        </Root>
      )
    }

    determineInput = () => {
      let value = Common.duplicate(this.props.model.get('value'))
      const comparator = Common.duplicate(this.state.comparator)
      value = transformValue(value, comparator)
      const attribute = Common.duplicate(this.state.attribute)
      const propertyJSON = generatePropertyJSON(value, attribute, comparator)
      const ViewToUse = determineView(comparator)
      if (
        this.props.suggestions.length > 0 &&
        propertyJSON.enum === undefined
      ) {
        propertyJSON.enum = this.props.suggestions
      }
      const model = new PropertyModel(propertyJSON)

      this.props.listenTo(model, 'change:value', this.updateValueFromInput)

      this.component = new ViewToUse({
        model,
      })
      this.updateValueFromInput()
    }

    updateValueFromInput = () => {
      const value = Common.duplicate(this.component.model.getValue())
      const isValid = this.component.isValid()
      this.props.model.set({ value, isValid })
    }
  }
)

export default FilterInput
