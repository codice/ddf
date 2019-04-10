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
var Marionette = require('marionette')
var _ = require('underscore')
var $ = require('jquery')
var CustomElements = require('../../js/CustomElements.js')
var ListItemCollectionView = require('../list-item/list-item.collection.view.js')
var template = require('./list-select.hbs')
var PopoutView = require('../dropdown/popout/dropdown.popout.view.js')

var eventsHash = {
  click: 'handleClick',
}

var namespace = CustomElements.getNamespace()
var listItemClickEvent = 'click ' + namespace + 'list-item'
eventsHash[listItemClickEvent] = 'handleListItemClick'

let ListSelectingView = ListItemCollectionView.extend({
  className: 'is-list-select composed-menu',
  events: eventsHash,
  onBeforeShow: function() {
    this.handleValue()
  },
  handleListItemClick: function(event) {
    this.model.set('value', $(event.currentTarget).attr('data-listid'))
    this.handleValue()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  handleValue: function() {
    var listId = this.model.get('value')
    this.$el.find(namespace + 'list-item').removeClass('is-selected')
    if (listId) {
      this.$el
        .find(namespace + 'list-item[data-listid="' + listId + '"]')
        .addClass('is-selected')
    }
  },
})

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('list-select'),
  template: template,
  className: 'composed-menu',
  regions: {
    listCollection: '> .list-collection',
    listCreate: '> .list-create',
  },
  initialize: function() {},
  onBeforeShow: function() {
    this.showListCollection()
    this.setupCreateList()
  },
  setupCreateList: function() {
    this.listCreate.show(
      PopoutView.createSimpleDropdown({
        componentToShow: ListCreate,
        modelForComponent: this.model,
        leftIcon: 'fa fa-plus',
        label: 'Create New List',
        options: {
          withBookmarks: false,
        },
      })
    )
  },
  showListCollection: function() {
    this.listCollection.show(
      new ListSelectingView({
        model: this.model,
        workspaceLists: this.options.workspaceLists,
      })
    )
  },
})
