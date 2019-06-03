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

import ListCreate from '../list-create/list-create.js'

const Marionette = require('marionette')
const $ = require('jquery')
const template = require('./result-add.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const List = require('../../js/model/List.js')
const PopoutView = require('../dropdown/popout/dropdown.popout.view.js')
const _ = require('lodash')

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('result-add'),
  template,
  events: {
    'click .is-existing-list.matches-filter:not(.already-contains)':
      'addToList',
    'click .is-existing-list.already-contains': 'removeFromList',
  },
  regions: {
    newList: '.create-new-list',
  },
  removeFromList(e) {
    const listId = $(e.currentTarget).data('id')
    store
      .getCurrentWorkspace()
      .get('lists')
      .get(listId)
      .removeBookmarks(this.model.map(result => result.get('metacard').id))
  },
  addToList(e) {
    const listId = $(e.currentTarget).data('id')
    store
      .getCurrentWorkspace()
      .get('lists')
      .get(listId)
      .addBookmarks(this.model.map(result => result.get('metacard').id))
  },
  onRender() {
    this.setupCreateList()
  },
  safeRender() {
    if (!this.isDestroyed) {
      this.render()
    }
  },
  setupCreateList() {
    this.newList.show(
      PopoutView.createSimpleDropdown({
        componentToShow: ListCreate,
        modelForComponent: this.model,
        leftIcon: 'fa fa-plus',
        label: 'Create New List',
        options: {
          withBookmarks: true,
        },
      })
    )
  },
  initialize() {
    this.listenTo(
      store.getCurrentWorkspace().get('lists'),
      'add remove update change',
      this.safeRender
    )
  },
  serializeData() {
    let listJSON = store
      .getCurrentWorkspace()
      .get('lists')
      .toJSON()
    listJSON = listJSON.map(list => {
      list.matchesFilter = true
      if (list['list.cql'] !== '') {
        list.matchesFilter = this.model.every(result =>
          result.matchesCql(list['list.cql'])
        )
      }
      list.alreadyContains = false
      if (
        _.intersection(
          list['list.bookmarks'],
          this.model.map(result => result.get('metacard').id)
        ).length === this.model.length
      ) {
        list.alreadyContains = true
      }
      list.icon = List.getIconMapping()[list['list.icon']]
      return list
    })
    return listJSON
  },
})
