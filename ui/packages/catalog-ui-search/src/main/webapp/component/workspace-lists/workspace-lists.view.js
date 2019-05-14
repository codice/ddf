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
const CustomElements = require('../../js/CustomElements.js')
const template = require('./workspace-lists.hbs')
const store = require('../../js/store.js')
const ListSelectorView = require('../dropdown/list-select/dropdown.list-select.view.js')
const DropdownModel = require('../dropdown/dropdown.js')
const ResultSelectorView = require('../result-selector/result-selector.view.js')
const $ = require('jquery')
const PopoutView = require('../dropdown/popout/dropdown.popout.view.js')

let selectedListId

module.exports = Marionette.LayoutView.extend({
  setDefaultModel: function() {
    this.model = store.getCurrentWorkspace().get('lists')
  },
  template: template,
  tagName: CustomElements.register('workspace-lists'),
  regions: {
    listSelect: '> .list-select',
    listEmpty: '.list-empty',
    listResults: '.list-results',
    listQuickCreate: '> .lists-empty .quick-create',
  },
  events: {
    'click > .list-empty .quick-search': 'triggerSearch',
    'click > .lists-empty .quick-search': 'triggerSearch',
    'click > .list-empty .quick-delete': 'triggerDelete',
  },
  initialize: function(options) {
    if (options.model === undefined) {
      this.setDefaultModel()
    }
  },
  onBeforeShow: function() {
    if (store.getCurrentWorkspace()) {
      this.setupWorkspaceListSelect()
    }
    this.setupQuickCreateList()
  },
  setupQuickCreateList: function() {
    this.listQuickCreate.show(
      PopoutView.createSimpleDropdown({
        componentToShow: ListCreate,
        modelForComponent: this.model,
        label: 'new list',
        options: {
          withBookmarks: false,
        },
      })
    )
  },
  getPreselectedList: function() {
    if (this.model.length === 1) {
      return this.model.first().id
    } else if (this.model.get(selectedListId)) {
      return selectedListId
    } else {
      return undefined
    }
  },
  setupWorkspaceListSelect: function() {
    this.listSelect.show(
      new ListSelectorView({
        model: new DropdownModel({
          value: this.getPreselectedList(),
        }),
        workspaceLists: this.model,
        dropdownCompanionBehaviors: {
          navigation: {},
        },
      })
    )
    this.listenTo(
      this.listSelect.currentView.model,
      'change:value',
      this.handleUpdates
    )
    this.listenTo(
      this.model,
      'remove update change:list.bookmarks add',
      this.handleUpdates
    )
    this.listenTo(this.model, 'add', this.handleAdd)
    this.handleUpdates()
  },
  handleAdd: function(newList, lists, options) {
    if (options.preventSwitch !== true) {
      this.listSelect.currentView.model.set('value', newList.id)
      this.listSelect.currentView.model.close()
    }
  },
  handleUpdates(newList, lists, options) {
    this.updateResultsList()
    this.handleEmptyLists()
    this.handleEmptyList()
    this.handleSelection()
  },
  handleSelection: function() {
    this.$el.toggleClass(
      'has-selection',
      this.model.get(this.listSelect.currentView.model.get('value')) !==
        undefined
    )
  },
  handleEmptyLists: function() {
    this.$el.toggleClass('is-empty-lists', this.model.isEmpty())
    if (this.model.length === 1) {
      this.listSelect.currentView.model.set('value', this.model.first().id)
    }
  },
  handleEmptyList: function() {
    if (
      this.model.get(selectedListId) &&
      this.model.get(selectedListId).isEmpty()
    ) {
      this.$el.addClass('is-empty-list')
    } else {
      this.$el.removeClass('is-empty-list')
    }
  },
  updateResultsList: function() {
    const listId = this.listSelect.currentView.model.get('value')
    if (listId) {
      selectedListId = listId
      if (
        !this.model.get(selectedListId).isEmpty() &&
        (this.listResults.currentView === undefined ||
          this.listResults.currentView.model.id !== selectedListId)
      ) {
        this.listResults.show(
          new ResultSelectorView({
            model: this.model.get(selectedListId).get('query'),
            selectionInterface: this.options.selectionInterface,
            tieredSearchIds: this.model
              .get(selectedListId)
              .get('list.bookmarks'),
          })
        )
      }
    } else {
      this.listResults.empty()
    }
    this.handleEmptyList()
  },
  triggerDelete: function() {
    this.model.remove(this.model.get(selectedListId))
  },
  triggerSearch: function() {
    $('.content-adhoc')
      .mousedown()
      .click()
  },
})
