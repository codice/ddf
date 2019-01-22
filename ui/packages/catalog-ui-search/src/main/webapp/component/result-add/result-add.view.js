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
/*global define, require, module*/
var Marionette = require('marionette')
var $ = require('jquery')
var template = require('./result-add.hbs')
var CustomElements = require('../../js/CustomElements.js')
var store = require('../../js/store.js')
var ListCreateView = require('../list-create/list-create.view.js')
var lightboxInstance = require('../lightbox/lightbox.view.instance.js')
var List = require('../../js/model/List.js')
var PopoutView = require('../dropdown/popout/dropdown.popout.view.js')
var filter = require('../../js/filter.js')
var cql = require('../../js/cql.js')
var _ = require('lodash')
var properties = require('../../js/properties.js')
var announcement = require('../announcement/index.jsx')

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('result-add'),
  template: template,
  events: {
    'click .is-existing-list.matches-filter:not(.already-contains)':
      'addToList',
    'click .is-existing-list.already-contains': 'removeFromList',
  },
  regions: {
    newList: '.create-new-list',
  },
  removeFromList: function(e) {
    var listId = $(e.currentTarget).data('id')
    store
      .getCurrentWorkspace()
      .get('lists')
      .get(listId)
      .removeBookmarks(
        this.model.map(function(result) {
          return result.get('metacard').id
        })
      )
  },
  addToList: function(e) {
    var listId = $(e.currentTarget).data('id')
    const list = store
      .getCurrentWorkspace()
      .get('lists')
      .get(listId)
    const max = properties.listItemLimit;
    const atMax = list.get('list.bookmarks').length >= max
    if(atMax){
      announcement.announce({
        title: 'Error',
        message: 'List Item Limit Reached (' + max + ' items)',
        type: 'error',
      })
    }else{
      list.addBookmarks(
        this.model.map(function(result) {
          return result.get('metacard').id
        })
      )
    }
  },
  onRender: function() {
    this.setupCreateList()
  },
  safeRender: function() {
    if (!this.isDestroyed) {
      this.render()
    }
  },
  setupCreateList: function() {
    this.newList.show(
      PopoutView.createSimpleDropdown({
        componentToShow: ListCreateView,
        modelForComponent: this.model,
        leftIcon: 'fa fa-plus',
        label: 'Create New List',
        options: {
          withBookmarks: true,
        },
      })
    )
  },
  initialize: function() {
    this.listenTo(
      store.getCurrentWorkspace().get('lists'),
      'add remove update change',
      this.safeRender
    )
  },
  serializeData: function() {
    var listJSON = store
      .getCurrentWorkspace()
      .get('lists')
      .toJSON()
    listJSON = listJSON.map(list => {
      list.matchesFilter = true
      if (list['list.cql'] !== '') {
        list.matchesFilter = this.model.every(function(result) {
          return result.matchesCql(list['list.cql'])
        })
      }
      list.alreadyContains = false
      if (
        _.intersection(
          list['list.bookmarks'],
          this.model.map(function(result) {
            return result.get('metacard').id
          })
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
