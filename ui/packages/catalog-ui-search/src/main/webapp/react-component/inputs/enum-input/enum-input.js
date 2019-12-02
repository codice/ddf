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
const sources = require('../../../component/singletons/sources-instance');

const TextWrapper = styled.div`
  padding: ${({ theme }) => theme.minimumSpacing};
`

const EnumMenuItem = props => (
  <MenuItem {...props} style={{ paddingLeft: '1.5rem' }}/>
)

const isMenuItemDisabled = (currValue) => {
  
  let AllSupportedAttributes = sources.models.map(source => {

    //NDL EASt is only supported in NCL-Search not in advanced search
    if(!(source.id == "NDL-East")){

      return source.attributes.supportedAttributes;

    }
    return [];
  });
  AllSupportedAttributes = AllSupportedAttributes.flat()
  //AllSupportedAttributes.push("ext.acquisition-status");
  if(AllSupportedAttributes.length == 0){
    return false;
  }
  if(AllSupportedAttributes.indexOf(currValue) >= 0){
    return false;
  }

  return true;

}

const EnumInput = ({
  allowCustom,
  matchCase,
  onChange,
  suggestions,
  value,
}) => {
  const [input, setInput] = useState('')
  const selected = suggestions.find(suggestion => suggestion.value === value)
  const filteredSuggestions = getFilteredSuggestions(
    input,
    suggestions,
    matchCase
  )
  
  console.log(filteredSuggestions)
  console.log(sources.models);
  console.log(filteredSuggestions);
  const displayInput = !inputMatchesSuggestions(input, suggestions, matchCase)
  return (
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
            <EnumMenuItem key={suggestion.value} value={suggestion.value}  disabled={isMenuItemDisabled(suggestion.value)}>
              {suggestion.label}
            </EnumMenuItem>
          )
        })}
      </Menu>
    </Dropdown>
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
