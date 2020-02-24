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
import React, { useState } from 'react'

import Dropdown from '../../dropdown'
import { Menu, MenuItem } from '../../menu'
import TextField from '../../text-field'
import styled from 'styled-components'
import { getFilteredSuggestions, inputMatchesSuggestions } from './enumHelper'
import PropTypes from 'prop-types'
const properties = require('catalog-ui-search/src/main/webapp/js/properties.js')
const TextWrapper = styled.div`
  padding: ${({ theme }) => theme.minimumSpacing};
`
const SOURCES = properties.i18n['sources'] || 'source(s)'
const EnumMenuItem = props => (
  <span
    title={
      isAttributeDisabled(props.supportedAttributes, props.value)
        ? `Attribute is unsupported by the ${SOURCES} selected`
        : ''
    }
  >
    <MenuItem {...props} style={{ paddingLeft: '1.5rem' }} />
  </span>
)

const UnsupportedAttribute = styled.div`
border-style: solid
border-color: red
`

const isAttributeDisabled = (allSupportedAttributes, currValue) => {
  //if the parent component does not pass down the attributes then return false
  if (!allSupportedAttributes) {
    return false
  }
  return (
    allSupportedAttributes.length > 0 &&
    allSupportedAttributes.indexOf(currValue.value) == -1
  )
}

const addDisabledPropToSuggestions = (
  allSupportedAttributes,
  filteredSuggestions
) => {
  return filteredSuggestions.map(suggestion => {
    let disabledStatus = isAttributeDisabled(allSupportedAttributes, suggestion)
    suggestion.disabled = disabledStatus
    return suggestion
  }, allSupportedAttributes)
}

const EnumInput = ({
  allowCustom,
  matchCase,
  onChange,
  suggestions,
  value,
  supportedAttributes,
}) => {
  const [input, setInput] = useState('')
  const selected = suggestions.find(suggestion => suggestion.value === value)
  const filteredSuggestions = getFilteredSuggestions(
    input,
    suggestions,
    matchCase
  )
  const displayInput = !inputMatchesSuggestions(input, suggestions, matchCase)
  addDisabledPropToSuggestions(supportedAttributes, filteredSuggestions)
  const attributeDropdown = (
    <Dropdown label={(selected && selected.label) || value}>
      <TextWrapper>
        <TextField
          autoFocus
          value={input}
          placeholder={'Type to Filter'}
          onChange={setInput}
        />
      </TextWrapper>
      <Menu value={value} onChange={onChange}>
        {allowCustom &&
          displayInput && (
            <EnumMenuItem value={input}>{input} (custom)</EnumMenuItem>
          )}
        {filteredSuggestions.map(suggestion => {
          return (
            <EnumMenuItem
              key={suggestion.value}
              value={suggestion.value}
              supportedAttributes={supportedAttributes}
              disabled={suggestion.disabled}
            >
              {suggestion.label}
            </EnumMenuItem>
          )
        })}
      </Menu>
    </Dropdown>
  )
  return (
    <div>
      {isAttributeDisabled(supportedAttributes, selected) ? (
        <div>
          <UnsupportedAttribute title="Attribute is unsupported by the content store(s) selected">
            {attributeDropdown}
          </UnsupportedAttribute>
          <div style={{ color: 'red' }}>
            {' '}
            This selection does not work with the {SOURCES} selected{' '}
          </div>
        </div>
      ) : (
        <div>{attributeDropdown}</div>
      )}
    </div>
  )
}
EnumInput.propTypes = {
  /** The current selected value. */
  value: PropTypes.string,

  /** Value change handler. */
  onChange: PropTypes.func.isRequired,

  /** Array that represents the options. [{ label: string, value: string}] */
  suggestions: PropTypes.array.isRequired,

  /** Should filtering be case sensitive? */
  matchCase: PropTypes.bool,

  /** Should custom values be allowed? */
  allowCustom: PropTypes.bool,
}
export default EnumInput
