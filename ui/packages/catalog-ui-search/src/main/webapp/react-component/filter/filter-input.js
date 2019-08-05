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

const BaseRoot = styled.div`
  display: inline-block;
  vertical-align: middle;
  margin-right: 0px;
  height: ${({ theme }) => theme.minimumButtonSize};
  line-height: ${({ theme }) => theme.minimumButtonSize};
  min-width: ${({ theme }) => `calc(13 * ${theme.minimumFontSize})`};
  intrigue-input.is-with-param {
    min-width: ${({ theme }) => `calc(20 * ${theme.minimumFontSize})`};
  }
`
const LocationRoot = styled(BaseRoot)`
  padding: ${({ theme }) =>
    `${theme.minimumSpacing}
      1.5rem 0px calc(${theme.minimumSpacing} + 0.75*${theme.minimumButtonSize} + ${theme.minimumButtonSize})`};

  min-width: ${({ theme }) => `calc(19*${theme.minimumFontSize})`};
  margin: 0px !important;
  display: block !important;
  height: auto;
`

const DateRoot = styled(BaseRoot)`
  min-width: ${({ theme }) => `calc(24*${theme.minimumFontSize})`};
  height: auto;
`

const EmptyRoot = styled(BaseRoot)`
  display: none;
`

const Roots = {
  'LOCATION': LocationRoot,
  'GEOMETRY': LocationRoot,
  'DATE': DateRoot,
  'EMPTY': EmptyRoot
}

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
      const Root = Roots[this.state.comparator === 'IS EMPTY' ? 'EMPTY' : this.props.type] || BaseRoot
      return (
        <Root
          data-help="The value for the property to use during comparison."
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
