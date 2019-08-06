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
import {NewItem} from '../../newitem/newitem.view'
const user = require('../../../component/singletons/user-instance')


class NewItemManager extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      currentView: 'new item',
      selectedMetacardType: undefined,
      files: []
    }

    const uploads = user
      .get('user')
      .get('preferences')
      .get('uploads')
    
  

    // this.listenTo(uploads, 'add', this.add)
    // this.listenTo(uploads, 'remove', this.remove)

  }

  uploadEvent() {
    console.log("nice");
  }

  add() {
    console.log("file added");
  }

  remove() {
    console.log("file removed");
  }

  tempClick() {
    console.log("clicked");
  }

  handleViewUpdate(newView) {
    this.setState({
      currentView: newView
    });
  }

  setSelectedMetacardType(card) {
    this.setState({
      selectedMetacardType: card
    })
  }

  getCurrentView() {
    const uploads = user
      .get('user')
      .get('preferences')
      .get('uploads')
    
    console.log(uploads)
    return (<NewItem/>)
  }

  render() {
    
    return( 
      this.getCurrentView() 
    )
  }
}

export {NewItemManager}

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
