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
import { Menu, MenuItemDisabled } from '../../menu'
import TextField from '../../text-field'
import styled from 'styled-components'
import { getFilteredSuggestions, inputMatchesSuggestions } from './enumHelper'
import PropTypes from 'prop-types'
const sources = require('../../../component/singletons/sources-instance')
const TextWrapper = styled.div`
  padding: ${({ theme }) => theme.minimumSpacing};
`
const EnumMenuItem = props => (
  <MenuItemDisabled {...props} style={{ paddingLeft: '1.5rem' }} />
)

const UnsupportedAttribute = styled.div`
border-style: solid
border-color: red
`

const isAttributeDisabled = (allSupportedAttributes, currValue) => {
  //All attributes are supported
  if (AllSupportedAttributes.length == 0) {
    return false
  }
  //If attribute is supported  dont disable the option
  if (AllSupportedAttributes.indexOf(currValue) >= 0) {
    return false
  }
  //attribute was not found in the supported list therefore disable the option
  return true
}
const isAttributeUnsupported = (currValue, settingsModel) => {
  // if no source is selected and settingsModel is present from parent component we want to present all attributes as available
  if (settingsModel != undefined && settingsModel.length == 0) {
        return false;
  } else {
    // if settingsModel is not available treat it as all attributes are supported
    if (settingsModel == undefined) {
      return false;
    }

    let sourceModelsSelected = sources.models.filter(source =>
      settingsModel.includes(source.id)
    )

    let AllSupportedAttributes = sourceModelsSelected.map(sourceSelected => {
      return sourceSelected.attributes.supportedAttributes
    })

    AllSupportedAttributes = AllSupportedAttributes.flat()
    return isAttributeDisabled(AllSupportedAttributes, currValue)
  }
}

const isAttributeUnsupportedHelper = (settingsModel, suggestion) => {
  //if settingsModel is passed down from a parent component , proceed to check if the attribute is unsupported
  return (
    settingsModel &&
    isAttributeUnsupported(suggestion.value, settingsModel.attributes.src)
  )
}

const EnumInput = ({
  allowCustom,
  matchCase,
  onChange,
  suggestions,
  value,
  settingsModel,
}) => {
  const [input, setInput] = useState('')
  const selected = suggestions.find(suggestion => suggestion.value === value)

  const filteredSuggestions = getFilteredSuggestions(
    input,
    suggestions,
    matchCase
  )

  const displayInput = !inputMatchesSuggestions(input, suggestions, matchCase)

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
      <Menu value={value} onChange={onChange} class="fa">
        {allowCustom &&
          displayInput && (
            <EnumMenuItem value={input}>{input} (custom)</EnumMenuItem>
          )}
        {filteredSuggestions.map(suggestion => {
          return (
            <EnumMenuItem
              title={
                isAttributeUnsupportedHelper(settingsModel, suggestion)
                  ? 'Attribute is unsupported by the content store(s) selected'
                  : ''
              }
              key={suggestion.value}
              value={suggestion.value}
              disabled={isAttributeUnsupportedHelper(settingsModel, suggestion)}
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
      {isAttributeUnsupportedHelper(settingsModel, selected) ? (
        <div>
          <UnsupportedAttribute title="Attribute is unsupported by the content store(s) selected">
            {attributeDropdown}
          </UnsupportedAttribute>
          <div style={{ color: 'red' }}>
            {' '}
            This selection does not work with the content store selected{' '}
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
