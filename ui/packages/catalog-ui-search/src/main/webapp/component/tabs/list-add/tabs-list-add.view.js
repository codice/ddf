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
const userNotifications = require('../../../component/singletons/user-notifications.js')

class NewItemManager extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      currentView: 'new item',
      selectedMetacardType: undefined,
      files: []
    }

    this.tempClick = this.tempClick.bind(this);
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

  render() {
    return( 
      <NewItem onClick={this.tempClick}/>
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
