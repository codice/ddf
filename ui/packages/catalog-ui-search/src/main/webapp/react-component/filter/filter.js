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
import metacardDefinitions from '../../component/singletons/metacard-definitions.js'

import * as React from 'react'

import ExtensionPoints from '../../extension-points'
import styled from 'styled-components'
import { GrabCursor } from '../styles/mixins'
import { Button, buttonTypeEnum } from '../presentation/button'
import FilterAttribute from './filter-attribute'
import FilterComparator from './filter-comparator'
import FilterInput from './filter-input'
import { getAttributeType } from './filterHelper'

const Root = styled.div`
  width: auto;
  height: 100%;
  display: block;
  white-space: nowrap;
  margin: ${({ theme }) => theme.minimumSpacing};
  padding: ${({ theme }) => theme.minimumSpacing};
`

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

class Filter extends React.Component {
  constructor(props) {
    super(props)
    const comparator = props.comparator || 'CONTAINS'
    let attribute = props.attribute || 'anyText'
    if (
      props.includedAttributes &&
      !props.includedAttributes.includes(attribute)
    ) {
      attribute = props.includedAttributes[0]
    }

    this.state = {
      comparator,
      attribute,
      suggestions: props.suggestions || [],
      value: props.value !== undefined ? props.value : '',
      isValid: props.isValid,
    }
    props.onChange(this.state)
  }

  componentDidMount() {
    this.updateSuggestions()
  }

  render() {
    return (
      <Root>
        <FilterRearrange className="filter-rearrange">
          <span className="cf cf-sort-grabber" />
        </FilterRearrange>
        <FilterRemove
          buttonType={buttonTypeEnum.negative}
          editing={this.props.editing}
          onClick={this.props.onRemove}
          icon="fa fa-minus"
        />
        <FilterAttribute
          value={this.state.attribute}
          includedAttributes={this.props.includedAttributes}
          editing={this.props.editing}
          onChange={this.updateAttribute}
        />
        <FilterComparator
          comparator={this.state.comparator}
          editing={this.props.editing}
          attribute={this.state.attribute}
          onChange={comparator => this.setState({ comparator }, this.onChange)}
        />
        <FilterInput
          suggestions={this.state.suggestions}
          attribute={this.state.attribute}
          comparator={this.state.comparator}
          editing={this.props.editing}
          onChange={value => {
            this.setState({ value }, () => this.props.onChange(this.state))
          }}
          isValid={this.state.isValid}
          value={this.state.value}
        />
        <ExtensionPoints.filterActions
          model={this.props.model}
          metacardDefinitions={metacardDefinitions}
          options={this.props}
        />
      </Root>
    )
  }

  onChange = () => {
    this.updateSuggestions()
    this.props.onChange(this.state)
  }

  updateSuggestions = async () => {
    const { attribute } = this.state
    let suggestions = []
    if (metacardDefinitions.enums[attribute]) {
      suggestions = metacardDefinitions.enums[attribute].map(suggestion => {
        return { label: suggestion, value: suggestion }
      })
    } else if (this.props.suggester) {
      suggestions = (await this.props.suggester(
        metacardDefinitions.metacardTypes[attribute]
      )).map(suggestion => ({
        label: suggestion,
        value: suggestion,
      }))
    }
    this.setState({ suggestions })
  }

  updateAttribute = attribute => {
    const prevType = getAttributeType(this.state.attribute)
    const newType = getAttributeType(attribute)
    let value = this.state.value
    if (prevType !== newType) {
      value = ''
    }
    this.setState({ attribute, value }, this.onChange)
  }
}

export default Filter
