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

const Marionette = require('marionette')
import Dropdown from '../../dropdown'
import { Menu, MenuItem,MenuItemDisabled } from '../../menu'
import TextField from '../../text-field'
import styled from 'styled-components'
import { getFilteredSuggestions, inputMatchesSuggestions } from './enumHelper'
import PropTypes from 'prop-types'
import IconButton from 'material-ui/IconButton'
import getMuiTheme from 'material-ui/styles/getMuiTheme'
import MuiThemeProvider from 'material-ui/styles/MuiThemeProvider'
import muiThemeable from 'material-ui/styles/muiThemeable'
import IconMenu from 'material-ui/IconMenu'
const sources = require('../../../component/singletons/sources-instance');


const TextWrapper = styled.div`
  padding: ${({ theme }) => theme.minimumSpacing};
`
const EnumMenuItem = props => (
  <MenuItemDisabled {...props} style={{ paddingLeft: '1.5rem' }}/>
)
/*
<IconButton style={{ color: muiTheme.palette.warningColor }} alignItems="center" tooltip="Attribute Unsupported">
</IconButton> 
                      
<i className="fa fa-warning"style={{ color: muiTheme.palette.warningColor }} alignItems="center" />
*/
const ImageIcon = styled.i.attrs(props => ({
  className: "fa fa-info-circle"
}))`
color: #000;
:hover {
  color: #ed1212;
  cursor: pointer;
}
:hover.tooltiptext {
  visibility : visible
}

`

const isIconDisplayed = (AllSupportedAttributes,currValue) => {

  if(AllSupportedAttributes.length == 0){
    return false;
  }
  if(AllSupportedAttributes.indexOf(currValue) >= 0){
    return false;
  }

  return true;

}

const isAttributeUnsupported = (currValue,settingsModel) => {
  // if no source is selected gather all supportedAttributes from all available sources
  if(settingsModel.length == 0){
      let AllSupportedAttributes = sources.models.map(source => {

        //NDL EAST is only supported in NCL-Search not in advanced search
        if(!(source.id == "NDL-East")){

          return source.attributes.supportedAttributes;

        }
        return [];
      });
      AllSupportedAttributes = AllSupportedAttributes.flat()
      AllSupportedAttributes.push("ext.acquisition-status");
      return isIconDisplayed(AllSupportedAttributes,currValue);

  }
  else{

    let sourceModelsSelected = sources.models.filter(source => settingsModel.includes(source.id));
    
    

    let AllSupportedAttributes = sourceModelsSelected.map(sourceSelected => {

     
      if(sourceSelected.id == 'GIMS_GIN'){
        return ["ext.alternate-identifier-qualifier"];
      }

      if(!(sourceSelected.id == "NDL-East")){

        return sourceSelected.attributes.supportedAttributes;

      }
      return [];
      
    });
    
    AllSupportedAttributes = AllSupportedAttributes.flat()
    return isIconDisplayed(AllSupportedAttributes,currValue);

  }
}

const EnumInput = ({
  allowCustom,
  matchCase,
  onChange,
  suggestions,
  value,
  settingsModel
}) => {
  const [input, setInput] = useState('')
  const selected = suggestions.find(suggestion => suggestion.value === value)
  console.log(selected);
  const filteredSuggestions = getFilteredSuggestions(
    input,
    suggestions,
    matchCase
  )
  console.log(settingsModel);
  console.log(filteredSuggestions)
  console.log(sources.models);

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
          

            //need to figire ou hw to render disbaled without prnting object object
            <EnumMenuItem key={suggestion.value} value={suggestion.value} disabled={isAttributeUnsupported(suggestion.value,settingsModel.attributes.src)}>
              {suggestion.label}
              {settingsModel && isAttributeUnsupported(suggestion.value,settingsModel.attributes.src) &&
                    <span>
                        <ImageIcon className="fa fa-info-circle" ></ImageIcon>
                    </span>
              }
            </EnumMenuItem>
        
          )
        })}
      </Menu>
    </Dropdown>
  )
}
//<ImageIcon className="fa fa-info-circle" ></ImageIcon>
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
