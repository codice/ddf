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
import withListenTo from '../../../react-component/backbone-container'
import { BottomBar } from './bottom-bar'
const $ = require('jquery')
const user = require('../../../component/singletons/user-instance')
const metacardDefinitions = require('../../singletons/metacard-definitions')


const AttributeEditorView = styled.div`
  display: flex;
  width: 50%;
  overflow-y: scroll;
  height: calc(90% - 1.8rem);
  background-color: ${props => props.theme.backgroundNavigation};
  margin-top: calc(1.5 * ${props => props.theme.minimumSpacing});
  margin-left: ${props => props.theme.minimumSpacing};
`

const ViewWithBottomBar = styled.div`
  height: 100%;
`

const ButtonStyle = styled.div`
  padding: 0px ${props => props.theme.minimumSpacing};
  min-width: ${props => props.theme.minimumButtonSize};
  background-color: ${props => props.theme.primaryColor};
  align-self: center;
`

class NewItemManager extends React.Component {
  constructor(props) {
    super(props)
    this.state = {
      currentView: 'new item',
      selectedMetacardType: 'common',
      currentBatch: undefined,
      addedUploads: undefined,
      files: [],
      informalBottomText: 'Starting',
      manualMetacard: undefined
    }

    this.initializeUploadListeners = this.initializeUploadListeners.bind(this)
    this.getInformalBottomText = this.getInformalBottomText.bind(this)
    this.createManualMetacard = this.createManualMetacard.bind(this)
    this.setCancelAction = this.setCancelAction.bind(this)
    this.onAttributeEdit = this.onAttributeEdit.bind(this)
    this.onManualSubmit = this.onManualSubmit.bind(this)
    this.change = this.change.bind(this)
    this.add = this.add.bind(this)
  }

  initializeUploadListeners() {
    const uploads = user
      .get('user')
      .get('preferences')
      .get('uploads')
      
    this.props.listenTo(uploads, 'add', this.add)
  }

  add(addedUploads) {    
    addedUploads.attributes.uploads.models.map(model => {
      const fileModel = model.attributes.file
      return fileModel
    })
    this.setState({
      addedUploads
    })
    this.props.listenTo(addedUploads, 'change', this.change)

    this.props.setInformalView()
  }

  change(uploadPayload) {
    this.setCancelAction(uploadPayload)
    this.setState({
      files: this.getFileModels(uploadPayload),
      informalBottomText: this.getInformalBottomText(uploadPayload)
    })
  }

  setCancelAction(uploadPayload) {
    uploadPayload.attributes.uploads.models
    .filter(model => model.attributes.file.status === 'uploading')
    .map(model => {
      
      model.attributes.file.status = 'stop'
      model.attributes.file.onClick = () => {
        model.cancel()
        this.props.stopListening(model)
      }
    })

  }

  getInformalBottomText(uploadPayload) {

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

    return progressText + ' ' + errorText + ' ' + issueText

  }

  componentDidMount(){
    this.initializeUploadListeners()
  }

  componentWillUnmount() {
    this.props.stopListening(this.state.addedUploads)
    this.state.addedUploads.cancel()
    user
      .get('user')
      .get('preferences')
      .save()
  }

  getFileModels(uploadPayload) {
    return uploadPayload.attributes.uploads.models.map(model => {
      const fileModel = model.attributes.file
      return fileModel
    })
  }

  handleViewUpdate(newView) {
    this.setState({
      currentView: newView,
    })
  }

  onAttributeEdit(editedMetacard) {
    this.setState({
      manualMetacard: editedMetacard,
    })
  }

  onManualSubmit(selectedMetacardType) {
    this.setState({
      selectedMetacardType,
    })
    this.props.setManualCreateAsView()
  }

  createManualMetacard() {
    const editedMetacard = this.state.manualMetacard
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

    switch (this.props.currentView) {
      case 'new item':
        return (
          <NewItem
            files={this.props.files}
            metacardType={this.state.selectedMetacardType}
            onManualSubmit={this.onManualSubmit}
            handleUploadSuccess={this.props.handleUploadSuccess}
            url={this.props.url}
            extraHeaders={this.props.extraHeaders}
          />
        )
      case 'manual upload':
        const addButton = (
          <ButtonStyle>
            <button onClick={this.createManualMetacard}>
              {'Add Item'}
            </button>
          </ButtonStyle>
        )
        return (
          <ViewWithBottomBar>
            <AttributeEditorView>
              <AttributeEditor
                metacardType={this.state.selectedMetacardType}
                onAttributeEdit={this.onAttributeEdit}
              />
            </AttributeEditorView>
            <BottomBar children={[addButton]}/>
          </ViewWithBottomBar>
        )
      case 'informal table':
        const viewItemsButton = (
          <ButtonStyle>
            <button onClick={this.props.closeModal}>
              {'View Items'}
            </button>
          </ButtonStyle>
        )
        
        return (
          <ViewWithBottomBar>
            <InformalProductsTable files={this.state.files} />
            <BottomBar bottomBarText={this.state.informalBottomText} children={[viewItemsButton]}/>
          </ViewWithBottomBar>
        )
    }
  }

  render() {
    return this.getCurrentView()
  }
}

export default withListenTo(NewItemManager)
