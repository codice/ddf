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
// const TabsView = require('../tabs.view')
// const ListAddTabsModel = require('./tabs-list-add')
import * as React from 'react'
import styled from 'styled-components'
import { NewItem } from '../../newitem/new-item'
import AttributeEditor from '../../tabs/list-add/attribute-editor'
import { InformalProductsTable } from '../../../react-component/informal-products/informal-upload-table'
const $ = require('jquery')
const user = require('../../../component/singletons/user-instance')
const Upload = require('../../../js/model/Upload.js')
const metacardDefinitions = require('../../singletons/metacard-definitions')
const PropertyCollectionView = require('../../property/property.collection.view')
import withListenTo from '../../../react-component/backbone-container'
import {BottomBar} from './bottom-bar'


const AttributeEditorView = styled.div`
  display: flex;
  width: 50%;
  overflow-y: scroll;
  height: calc(80% - 1.8rem);
  background-color: ${props => props.theme.backgroundNavigation};
  margin-top: ${props => props.theme.minimumSpacing};
  margin-left: ${props => props.theme.minimumSpacing};
`

const ViewWithBottomBar = styled.div`
  height: 100%;
`

class NewItemManager extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      currentView: 'new item',
      selectedMetacardType: 'common',
      currentBatch: undefined,
      files: [],
      uploads: [],
      informalBottomText: 'Starting',
      manualMetacard: undefined
    }

    this.initializeUploadListeners = this.initializeUploadListeners.bind(this)
    this.createManualMetacard = this.createManualMetacard.bind(this)
    this.onAttributeEdit = this.onAttributeEdit.bind(this)
    this.onManualSubmit = this.onManualSubmit.bind(this)
    this.goToFile = this.goToFile.bind(this)
    this.change = this.change.bind(this)
    this.add = this.add.bind(this)
    this.initializeUploadListeners()
  }

  initializeUploadListeners() {
    const uploads = user
      .get('user')
      .get('preferences')
      .get('uploads')
      
    this.props.listenTo(uploads, 'add', this.add)
  }

  getFileModels(uploadPayload) {
    try {
      return uploadPayload.attributes.uploads.models.map(model => {
        const fileModel = model.attributes.file
        if (fileModel.status === 'uploading') {
          fileModel.status = 'stop'
        }
        else if(model.attributes.progress === 100) {
          fileModel.onClick = this.goToFile
        }
        return fileModel
      })
    } catch (err) {
      console.error(err)
      return
    }
  }

  goToFile(file) {
    // TODO implement redirect to file
    // close modal and highlight selected metacard
    console.log(file)
  }

  componentDidMount() {}

  change(uploadPayload) {
    uploadPayload.attributes.uploads.models
    .filter( model => model.attributes.file.status === 'success' 
                    || model.attributes.file.status === 'error')
      .forEach( el => {
        // TODO not sure how I want to handle error's yet. Want a hover to explain
        // But it does not look like that info is available
        el.attributes.file.onClick = this.goToFile
      })

    const progressText = `${uploadPayload.attributes.complete} 
                          of ${uploadPayload.attributes.amount} items uploaded.`
    
    let errorText = ''
    if(uploadPayload.attributes.errors > 0){
      errorText = `${uploadPayload.attributes.errors} errors.`
    }

    let issueText = ''
    if(uploadPayload.attributes.issues > 0){
      issueText = `${uploadPayload.attributes.issues} issues.`
    }
    
    this.setState({
      files: this.getFileModels(uploadPayload),
      uploads: uploadPayload,
      informalBottomText: progressText + ' ' + errorText + ' ' + issueText
    })
  }

  add(addedUploads) {    
    this.props.listenTo(addedUploads, 'change', this.change)
    // TODO the message may need to be set 
    // THis initializes the file details that only need to be done once
    addedUploads.attributes.uploads.models.map(model => {
      const fileModel = model.attributes.file
      fileModel.onClick = model.cancel.bind(model)
      return fileModel
    })
    this.props.setInformalView()
  }

  handleViewUpdate(newView) {
    this.setState({
      currentView: newView,
    })
  }

  onAttributeEdit(editedMetacard) {
    this.setState({
      manualMetacard: editedMetacard
    })
  }

  onManualSubmit(selectedMetacardType) {
    this.setState({
      selectedMetacardType
    })
    this.props.setManualCreateAsView()
  }

  createManualMetacard() {
    const editedMetacard = this.state.manualMetacard
    console.log(editedMetacard)
    const metacardType = this.state.selectedMetacardType

    const metacardDefinition =
      metacardDefinitions.metacardDefinitions[metacardType]

    const properties = editedMetacard.properties
    editedMetacard.properties = Object.keys(editedMetacard.properties)
      .filter(attributeName => properties[attributeName].length >= 1)
      .filter(attributeName => properties[attributeName][0] !== '')
      .reduce(
        (accummulator, currentValue) =>
          _.extend(accummulator, {
            [currentValue]: metacardDefinition[currentValue].multivalued
              ? properties[currentValue]
              : properties[currentValue][0],
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
  
  getCurrentView() {
    switch(this.props.currentView){
      case 'new item':
        return (
          <NewItem
            files={this.state.files}
            metacardType={this.state.selectedMetacardType}
            onManualSubmit={this.onManualSubmit}
            handleUploadSuccess={this.props.handleUploadSuccess}
            url={this.props.url}
            extraHeaders={this.props.extraHeaders}
          />
        )
      case 'manual upload':
          return(
            <ViewWithBottomBar>
              <AttributeEditorView>
                <AttributeEditor metacardType={this.state.selectedMetacardType}
                                 onAttributeEdit={this.onAttributeEdit}/>
              </AttributeEditorView>
              <BottomBar bottomBarText={this.state.bottomBarText}
                         rightButtonText={'Add Item'}
                         onRightButtonClick={this.createManualMetacard}
                         />
            </ViewWithBottomBar>
          )
      case 'informal table':
        return (
          <ViewWithBottomBar>
            <InformalProductsTable 
                    files={this.state.files}
            /> 
            <BottomBar bottomBarText={this.state.informalBottomText}
                       rightButtonText={'View Items'}
                       onRightButtonClick={this.props.closeModal}
                       />
          </ViewWithBottomBar>
        )
    }
  }

  render() {
      return this.getCurrentView()
  }
}

export default withListenTo(NewItemManager)