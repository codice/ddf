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
const user = require('../../../component/singletons/user-instance')
const Upload = require('../../../js/model/Upload.js')
import withListenTo from '../../../react-component/backbone-container'
import {BottomBar} from './bottom-bar'


const AttributeEditorView = styled.div`
  display: flex;
  width: 50%;
  overflow-y: scroll;
  height: calc(90% - 1.8rem);
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
      informalBottomText: 'Starting'
    }

    this.change = this.change.bind(this)
    this.add = this.add.bind(this)
    this.cancelUpload = this.cancelUpload.bind(this)
    this.initializeUploadListeners = this.initializeUploadListeners.bind(this)
    this.setSelectedMetacardType = this.setSelectedMetacardType.bind(this)
    this.goToFile = this.goToFile.bind(this)
    this.initializeUploadListeners()
  }

  initializeUploadListeners() {
    const uploads = user
      .get('user')
      .get('preferences')
      .get('uploads')
    //TODO subscribe to single payload from add
    this.props.listenTo(uploads, 'add', this.add)
  }

  getFileModels(uploadPayload) {
    try {
      //TODO detect upload issues
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
    //TODO implement redirect to file
    console.log(file)
  }

  componentDidMount() {}

  cancelUpload() {}

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

    if (uploadPayload.attributes.percentage === 100) {
      //TODO make sure security access attribute is set to only the uploader

      console.log('all files finished')
    }
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

  setSelectedMetacardType(card) {
    this.setState({
      selectedMetacardType: card,
    })
  }

  getCurrentView() {
    switch(this.props.currentView){
      case 'new item':
        return (
          <NewItem
            files={this.state.files}
            metacardType={this.state.selectedMetacardType}
            onManualSubmit={this.props.setManualCreateAsView}
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
                                 handleNewMetacard={this.props.handleNewMetacard}/>
              </AttributeEditorView>
              <BottomBar bottomBarText={this.state.bottomBarText}
                         rightButtonText={'View Items'}
                         leftButtonText={'Done'}/>
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
                       leftButtonText={'Done'}/>
          </ViewWithBottomBar>
        )
    }
  }

  render() {
      return this.getCurrentView()
  }
}

export default withListenTo(NewItemManager)

// module.exports = TabsView.extend({
//   className: 'is-list-add',
//   setDefaultModel(options) {
//     this.model = new ListAddTabsModel()
//   },
//   initialize(options) {
//     this.setDefaultModel(options)

//     TabsView.prototype.initialize.call(this)
//     this.model.set('activeTab', 'Import')
//   },
//   determineContent() {
//     const ActiveTab = this.model.getActiveView()
//     if (this.model.attributes.activeTab === 'Import') {
//       this.tabsContent.show(
//         new ActiveTab({
//           isList: true,
//           extraHeaders: this.options.extraHeaders,
//           url: this.options.url,
//           handleUploadSuccess: this.options.handleUploadSuccess,
//         })
//       )
//     } else {
//       this.tabsContent.show(
//         new ActiveTab({
//           handleNewMetacard: this.options.handleNewMetacard,
//           close: this.options.close,
//           model: this.model,
//         })
//       )
//     }
//   },
// })
