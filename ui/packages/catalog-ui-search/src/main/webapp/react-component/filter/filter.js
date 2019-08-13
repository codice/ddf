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

const metacardDefinitions = require('../../component/singletons/metacard-definitions.js')
const Common = require('../../js/Common.js')

import * as React from 'react'
import {
  geometryComparators,
  dateComparators,
  stringComparators,
  numberComparators,
  booleanComparators,
} from '../../component/filter/comparators'

import { generatePropertyJSON } from './filterHelper'

import ExtensionPoints from '../../extension-points'
import withListenTo from '../../react-component/backbone-container'
import styled from 'styled-components'
import { GrabCursor } from '../styles/mixins'
import { Button, buttonTypeEnum } from '../presentation/button'
import FilterAttribute from './filter-attribute'
import FilterComparator from './filter-comparator'
import FilterInput from './filter-input'

const FilterRearrange = styled.div`
  ${GrabCursor};
  display: inline-block;
  width: ${({ theme }) => `calc(0.75 * ${theme.minimumButtonSize})`};
  opacity: 0.25;
  :hover {
    opacity: 0.5;
    transition: opacity 0.3s ease-in-out;
  }
`

const FilterRemove = styled(Button)`
  display: inline-block;
  vertical-align: middle;
  margin-right: ${({ theme }) => theme.minimumSpacing};
  width: ${({ theme }) => theme.minimumButtonSize};
  height: ${({ theme }) => theme.minimumButtonSize};
  line-height: ${({ theme }) => theme.minimumButtonSize};
  display: ${({ editing }) => (editing ? 'inline-block' : 'none')};
`

const Filter = withListenTo(
  class Filter extends React.Component {
    constructor(props) {
      super(props)
      const comparator = props.model.get('comparator') || 'CONTAINS'
      let attribute = props.model.get('type') || 'anyText'
      if (
        props.includedAttributes &&
        !props.includedAttributes.includes(attribute)
      ) {
        attribute = props.includedAttributes[0]
      }

      this.state = {
        comparator,
        attribute,
        editing: props.editing,
        suggestions: [],
      }
      props.model.set('type', attribute)
      props.model.set('comparator', comparator)
    }

    componentDidMount = () => {
      this.updateComparator()
      this.updateSuggestions()
    }

    componentWillReceiveProps = ({ editing }) => {
      this.setState({ editing })
    }

    render() {
      const type = metacardDefinitions.metacardTypes[this.state.attribute].type
      return (
        <React.Fragment>
          <FilterRearrange className="filter-rearrange">
            <span className="cf cf-sort-grabber" />
          </FilterRearrange>
          <FilterRemove
            buttonType={buttonTypeEnum.negative}
            editing={this.state.editing}
            onClick={this.props.onRemove}
            icon="fa fa-minus"
          />
          <FilterAttribute
            attribute={this.state.attribute}
            includedAttributes={this.props.includedAttributes}
            editing={this.state.editing}
            onChange={this.updateAttribute}
          />
          <FilterComparator
            comparator={this.state.comparator}
            editing={this.state.editing}
            type={type}
            attribute={this.state.attribute}
            onChange={comparator => {
              this.setState({ comparator })
              this.props.model.set('comparator', comparator)
            }}
          />

          <FilterInput
            suggestions={this.state.suggestions}
            attribute={this.state.attribute}
            comparator={this.state.comparator}
            editing={this.state.editing}
            model={this.props.model}
            type={type}
          />
          <ExtensionPoints.filterActions
            model={this.props.model}
            metacardDefinitions={metacardDefinitions}
            options={this.props}
          />
        </React.Fragment>
      )
    }

    updateComparator = (attribute = this.state.attribute) => {
      const currentComparator = this.props.model.get('comparator')
      const value = Common.duplicate(this.props.model.get('value'))
      const propertyJSON = generatePropertyJSON(
        value,
        attribute,
        currentComparator
      )

      let newComparator = currentComparator
      switch (propertyJSON.type) {
        case 'LOCATION':
          if (
            geometryComparators.indexOf(currentComparator) === -1 ||
            attribute === 'anyGeo'
          ) {
            newComparator = 'INTERSECTS'
          }
          break
        case 'DATE':
          if (dateComparators.indexOf(currentComparator) === -1) {
            newComparator = 'BEFORE'
          }
          break
        case 'BOOLEAN':
          if (booleanComparators.indexOf(currentComparator) === -1) {
            newComparator = '='
          }
          break
        case 'LONG':
        case 'DOUBLE':
        case 'FLOAT':
        case 'INTEGER':
        case 'SHORT':
          if (numberComparators.indexOf(currentComparator) === -1) {
            newComparator = '>'
          }
          break
        default:
          if (
            stringComparators.indexOf(currentComparator) === -1 ||
            (attribute === 'anyText' && currentComparator === 'IS EMPTY')
          ) {
            newComparator = 'CONTAINS'
          }
          break
      }
      if (currentComparator !== newComparator) {
        this.props.model.set({ comparator: newComparator })
      }
      this.setState(
        {
          comparator: newComparator,
          attribute,
        },
        this.updateSuggestions
      )
    }

    updateSuggestions = async () => {
      const propertyJSON = generatePropertyJSON(
        this.props.model.get('value'),
        this.state.attribute,
        this.state.comparator
      )
      if (this.props.suggester) {
        const suggestions = (await this.props.suggester(propertyJSON)).map(
          suggestion => ({
            label: suggestion,
            value: suggestion,
          })
        )
        this.setState({ suggestions })
      }
    }

    updateAttribute = attribute => {
      const previousAttributeType =
        metacardDefinitions.metacardTypes[this.state.attribute].type
      this.props.model.set('type', attribute)
      const newAttributeType = metacardDefinitions.metacardTypes[attribute].type
      let value = this.props.model.get('value')
      if (newAttributeType !== previousAttributeType) {
        value = ['']
        this.props.model.set('value', value)
      }
      this.updateComparator(attribute)
    }
  }
)

export default Filter
