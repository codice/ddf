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
const sources = require('../../../component/singletons/sources-instance');


const TextWrapper = styled.div`
  padding: ${({ theme }) => theme.minimumSpacing};
`
const EnumMenuItem = props => (
  <MenuItemDisabled {...props} style={{ paddingLeft: '1.5rem'}}/>
)


const ImageIcon = styled.i`
color: #000;
text-align:center
:hover .tooltiptext {
  visibility: visible;
}
`

const UnsupportedAttribute = styled.div`
border-style: solid
border-color: red
`

const UnsupportedToolTip = styled.div`
visibility: hidden;
width: 120px;
background-color: black;
color: #fff;
text-align: center;
border-radius: 6px;
padding: 5px 0;

/* Position the tooltip */
position: absolute;
z-index: 1;
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

const isAttributeUnsupportedHelper = (settingsModel,suggestion) => {


  return settingsModel && isAttributeUnsupported(suggestion.value,settingsModel.attributes.src); 


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
  console.log(selected.value);
  const filteredSuggestions = getFilteredSuggestions(
    input,
    suggestions,
    matchCase
  )
  console.log(sources)
  console.log(settingsModel);
  console.log(filteredSuggestions)
  console.log(sources.models);

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
                title={isAttributeUnsupportedHelper(settingsModel,suggestion) ? 'Attribute is unsupported by the content store selected' : ''}  
                key={suggestion.value} 
                value={suggestion.value} 
                disabled={isAttributeUnsupportedHelper(settingsModel,suggestion)}
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
   {isAttributeUnsupportedHelper(settingsModel,selected) ? (
    <UnsupportedAttribute title = "Attribute is unsupported by the content store">
      {attributeDropdown}
    </UnsupportedAttribute>) : (<div>{attributeDropdown}</div>)
   }
   </div>
  )
}
/*










with pointer -event disabled and using buttons to disable 

 <button disabled={settingsModel && isAttributeUnsupported(suggestion.value,settingsModel.attributes.src)}>     
                        {suggestion.label}  
                       </button>     

                        {settingsModel && isAttributeUnsupported(suggestion.value,settingsModel.attributes.src) &&
                            
                            <Tooltip title="Attribute is unsupported by the current content store selected" placement="right">
                              <span> 
                                  <button disabled>         
                                      <ImageIcon  className="fa fa-info-circle"></ImageIcon>
                                  </button>
                              </span>
                            </Tooltip>                 
                        }




<div>
                  <EnumMenuItem          
                    key={suggestion.value} 
                    value={suggestion.value} 
                    disabled={settingsModel && isAttributeUnsupported(suggestion.value,settingsModel.attributes.src)}
                    >
                    {suggestion.label}
                    
                    </EnumMenuItem>
                    <div style = {{display : 'inline-block'}}>
                    {settingsModel && isAttributeUnsupported(suggestion.value,settingsModel.attributes.src) &&
                        <Tooltip title="Attribute is unsupported by the current content store selected">
                          <span>
                          <button disabled>
                              <ImageIcon  className="fa fa-info-circle"></ImageIcon>
                          </button>
                          </span>
                        </Tooltip>
                    }
                    </div>
              </div> 


 <span>
              
 
                  <ImageIcon 
                    className="fa fa-info-circle"
                    aria-owns={open ? 'mouse-over-popover' : undefined}
                    aria-haspopup="true"
                    onMouseEnter={handlePopoverOpen}
                    onMouseLeave={handlePopoverClose}
                    onMouseOver={handlePopoverOpen}
                  >
                  </ImageIcon>
                  <Popover
                  id="mouse-over-popover"
                  className={classes.popover}
                  classes={{
                    paper: classes.paper,
                  }}
                  open={open}
                  anchorEl={anchorEl}
                  anchorOrigin={{
                    vertical: 'bottom',
                    horizontal: 'left',
                  }}
                  transformOrigin={{
                    vertical: 'top',
                    horizontal: 'left',
                  }}
                  onClose={handlePopoverClose}
                  >
                  <h3>Unsupported Attribute due to content store selection</h3>
                </Popover>
                <UnsupportedToolTip className="tooltiptext">Unsupported attribute</UnsupportedToolTip>
              </span>     




              <Tooltip title="Attribute is unsupported by the current content store selected">
                <span>
                    <ImageIcon className="fa fa-info-circle"></ImageIcon>
                </span>
              </Tooltip>

        onMouseEnter={handlePopoverOpen}
        onMouseLeave={handlePopoverClose}


{settingsModel && isAttributeUnsupported(suggestion.value,settingsModel.attributes.src) &&
<span>    
  <ImageIcon className="fa fa-info-circle" ></ImageIcon>
  <UnsupportedToolTip className = "tooltiptext">Attribute is Usupported bu the Content Store selected</UnsupportedToolTip>
</span>
}
*/
//<ImageIcon className="fa fa-warning" ></ImageIcon>
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
