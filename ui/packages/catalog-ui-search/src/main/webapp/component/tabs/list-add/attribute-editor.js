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
import styled from 'styled-components'
import { hot } from 'react-hot-loader'
import MarionetteRegionContainer from '../../../react-component/marionette-region-container'
const $ = require('jquery')
const metacardDefinitions = require('../../singletons/metacard-definitions')
const PropertyCollectionView = require('../../property/property.collection.view')
const Property = require('../../property/property')

const AttributeEditorContainer = styled.div`
  display: flex;
  flex-flow: column;
  background-color: ${props => props.theme.backgroundNavigation};
  border-radius: 3px;
  intrigue-property {
    padding: 0;
  }

  & .property-label{
    padding: 0px ${props => props.theme.minimumSpacing};
  }

  & .property-value{
    padding: 0px ${props => props.theme.minimumSpacing};
  }
`

const AttributeTitle = styled.div`
  padding: ${props => props.theme.minimumSpacing};
  font-size: ${props => props.theme.largeFontSize};
`

class AttributeEditor extends React.Component {
  constructor(props) {
    super(props)

    //TODO find out if pre validation is needed
    this.turnOnEditing = this.turnOnEditing.bind(this)
    this.stripValuesFromFields = this.stripValuesFromFields.bind(this)
    this.getEditableFields = this.getEditableFields.bind(this)
    this.submitMetacard = this.submitMetacard.bind(this)

    const attributes = this.stripValuesFromFields()
    attributes['metacard-type'] = this.props.metacardType
    this.state = {
      propertyView: PropertyCollectionView.generatePropertyCollectionView([attributes])
    }
  }

  getEditableFields() {
    const metacardAttributes = metacardDefinitions.metacardDefinitions[this.props.metacardType]
    const editableFields = []
    // remove hidden and readonly attributes
    Object.keys(metacardAttributes).forEach(key => {
        if((!metacardAttributes[key].hidden) && (!metacardAttributes[key].readOnly)){
          editableFields.push(key)
        }
    })
    return editableFields
  }

  stripValuesFromFields() {
    let stripedFields = {}
    this.getEditableFields().forEach(key => {
      stripedFields[key] = undefined
    })
    return stripedFields
  }

  componentDidMount() {
    this.turnOnEditing()
  }

  turnOnEditing() {
    this.state.propertyView.turnOnEditing()
  }

  submitMetacard() {
    //TODO make sure this works!
    const metacardType = this.props.metacardType

    const metacardDefinition =
      metacardDefinitions.metacardDefinitions[metacardType]

    const editedMetacard = this.state.propertyView.toPropertyJSON()

    const props = editedMetacard.properties
    editedMetacard.properties = Object.keys(editedMetacard.properties)
      .filter(attributeName => props[attributeName].length >= 1)
      .filter(attributeName => props[attributeName][0] !== '')
      .reduce(
        (accummulator, currentValue) =>
          _.extend(accummulator, {
            [currentValue]: metacardDefinition[currentValue].multivalued
              ? props[currentValue]
              : props[currentValue][0],
          }),
        {}
      )

    editedMetacard.properties['metacard-type'] = metacardType
    editedMetacard.type = 'Feature'

    $.ajax({
      type: 'POST',
      url: './internal/catalog/?transform=geojson',
      data: JSON.stringify(editedMetacard),
      dataType: 'text',
      contentType: 'application/json',
    }).then((response, status, xhr) => {
      const id = xhr.getResponseHeader('id')
      if (id) {
        this.props.handleNewMetacard(id)
      }
    })
  }

  render() {
    return (
      <AttributeEditorContainer>
        <AttributeTitle>{this.props.metacardType} Attributes</AttributeTitle>
        <MarionetteRegionContainer view={this.state.propertyView} 
                                   onClick={this.turnOnEditing} 
                                   onDoubleClick={this.submitMetacard}
                                   >
        </MarionetteRegionContainer>  
      </AttributeEditorContainer>
    )
  }
}

export default hot(module)(AttributeEditor)
