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

let filter = ''

function convertArrayToModels(array) {
  return array.map(key => {
    return {
      id: key,
    }
  })
}

function getDifference(collection, array) {
  return collection.filter(model => array.indexOf(model.id) === -1)
}

function intersect(collection, array) {
  const difference = getDifference(collection, array)
  collection.remove(difference)
  return difference
}

function sync(collection, array) {
  const difference = getDifference(collection, array)
  collection.remove(difference)
  collection.add(convertArrayToModels(array))
  return difference
}

const Backbone = require('backbone')
const Marionette = require('marionette')
const _ = require('underscore')
const template = require('./editor.hbs')
const CustomElements = require('../../js/CustomElements.js')
const DetailsFilterView = require('../dropdown/details-filter/dropdown.details-filter.view.js')
const DropdownModel = require('../dropdown/dropdown.js')
const DetailsInteractionsView = require('../details-interactions/details-interactions.view.js')
const PopoutView = require('../dropdown/popout/dropdown.popout.view.js')
const user = require('../singletons/user-instance.js')
const properties = require('../../js/properties.js')

module.exports = Marionette.LayoutView.extend({
  setDefaultModel() {
    //override
  },
  template,
  tagName: CustomElements.register('editor'),
  modelEvents: {},
  events: {
    'click .editor-edit': 'edit',
    'click .editor-save': 'save',
    'click .editor-cancel': 'cancel',
  },
  regions: {
    editorProperties: '> .editor-properties',
    editorFilter: '> .editor-header > .header-filter',
    editorActions: '> .editor-header > .header-actions',
  },
  attributesAdded: undefined,
  attributesRemoved: undefined,
  attributesMocked: undefined,
  attributesToKeep: undefined,
  initialize(options) {
    if (options.model === undefined) {
      this.setDefaultModel()
    }
    this.handleTypes()
    this.attributesAdded = new Backbone.Collection([])
    this.attributesRemoved = new Backbone.Collection([])
    this.attributesMocked = new Backbone.Collection([])
    this.attributesToKeep = new Backbone.Collection([])
    this.listenTo(this.attributesAdded, 'reset', this.handleEphemeralReset)
    this.listenTo(
      this.attributesRemoved,
      'reset',
      this.handleAttributesToRemoveReset
    )
    //this.listenTo(user.get('user').get('preferences'), 'change:columnOrder', this.render);
    this.listenTo(
      user.get('user').get('preferences'),
      'change:inspector-summaryShown',
      this.handleFilterValue
    )
    this.listenTo(
      user.get('user').get('preferences'),
      'change:inspector-detailsHidden',
      this.handleFilterValue
    )
  },
  handleTypes() {
    const username = user.get('user').get('userid')
    let isOwner = true
    const types = {}
    this.model.forEach(result => {
      const tags = result
        .get('metacard')
        .get('properties')
        .get('metacard-tags')
      if (result.isWorkspace()) {
        types.workspace = true
      } else if (result.isResource()) {
        types.resource = true
      } else if (result.isRevision()) {
        types.revision = true
      } else if (result.isDeleted()) {
        types.deleted = true
      }
      if (result.isRemote()) {
        types.remote = true
      }
      const metacardOwner = result
        .get('metacard')
        .get('properties')
        .get('metacard.owner')

      isOwner = isOwner && username === metacardOwner
    })
    this.$el.toggleClass('is-mixed', Object.keys(types).length > 1)
    this.$el.toggleClass('is-workspace', types.workspace !== undefined)
    this.$el.toggleClass('is-resource', types.resource !== undefined)
    this.$el.toggleClass('is-revision', types.revision !== undefined)
    this.$el.toggleClass('is-deleted', types.deleted !== undefined)
    this.$el.toggleClass('is-remote', types.remote !== undefined)
    this.$el.toggleClass('is-owner', isOwner)
  },
  getEditorActionsOptions() {
    return {
      summary: true,
    }
  },
  generateEditorActions() {
    this.editorActions.show(
      PopoutView.createSimpleDropdown(
        _.extend({
          componentToShow: DetailsInteractionsView,
          dropdownCompanionBehaviors: {
            navigation: {},
          },
          label: 'Actions',
          rightIcon: 'fa fa-ellipsis-v',
          selectionInterface: this.selectionInterface,
          options: _.extend(
            {
              selectionInterface: this.selectionInterface,
            },
            this.getEditorActionsOptions()
          ),
        })
      )
    )
    this.listenTo(
      this.editorActions.currentView.model,
      'change:attributesToAdd',
      this.handleAttributeAdd
    )
    this.listenTo(
      this.editorActions.currentView.model,
      'change:attributesToRemove',
      this.handleAttributeRemove
    )
  },
  onBeforeShow() {
    this.editorFilter.show(
      new DetailsFilterView({
        model: new DropdownModel({
          value: filter,
        }),
      })
    )
    this.listenTo(
      this.editorFilter.currentView.model,
      'change:value',
      this.handleFilterValue
    )
    this.handleFilterValue()
    this.generateEditorActions()
  },
  handleAttributesToRemoveReset(collection, options) {
    this.handleAttributesToRemove()
    const ephemeralAttributesToUnRemove = this.attributesMocked
      .map(model => model.id)
      .filter(id => this.attributesRemoved.get(id) === undefined)
    this.editorProperties.currentView.removeProperties(
      ephemeralAttributesToUnRemove
    )
    this.generateEditorActions()
  },
  handleEphemeralReset(collection, options) {
    this.attributesToKeep.add(options.previousModels)
    const ephemeralAttributes = options.previousModels.map(model => model.id)
    this.editorProperties.currentView.removeProperties(ephemeralAttributes)
    this.generateEditorActions()
  },
  handleAttributeRemove() {
    sync(
      this.attributesRemoved,
      this.editorActions.currentView.model.get('attributesToRemove')[0]
    )
    const newAttributes = this.editorProperties.currentView.addProperties(
      this.attributesRemoved.pluck('id')
    )
    this.attributesMocked.add(convertArrayToModels(newAttributes))
    this.editorProperties.currentView.removeProperties(
      intersect(this.attributesMocked, this.attributesRemoved.pluck('id'))
    )
    this.handleNewProperties()
    this.handleAttributesToRemove()
  },
  handleAttributesToRemove() {
    this.editorProperties.currentView.children.forEach(propertyView => {
      const id = propertyView.model.id
      propertyView.$el.toggleClass(
        'scheduled-for-removal',
        this.attributesRemoved.get(id) !== undefined
      )
    })
    this.handleFilterValue()
  },
  handleAttributesToAdd() {
    this.editorProperties.currentView.children.forEach(propertyView => {
      const id = propertyView.model.id
      propertyView.$el.toggleClass(
        'scheduled-for-add',
        this.attributesAdded.get(id) !== undefined
      )
    })
  },
  handleAttributeAdd() {
    const difference = sync(
      this.attributesAdded,
      this.editorActions.currentView.model.get('attributesToAdd')[0]
    )
    const newAttributes = this.editorProperties.currentView.addProperties(
      this.attributesAdded.pluck('id')
    )
    this.editorProperties.currentView.removeProperties(difference)
    this.handleNewProperties()
    this.handleAttributesToAdd()
    this.handleFilterValue()
  },
  isSupposedToBeShown(attribute) {
    const ephemeralAttributes = this.attributesAdded.map(model => model.id)
    const attributesToRemove = this.attributesRemoved.map(model => model.id)
    const attributesToKeep = this.attributesToKeep.map(model => model.id)
    if (
      attributesToKeep.indexOf(attribute) >= 0 ||
      ephemeralAttributes.indexOf(attribute) >= 0 ||
      attributesToRemove.indexOf(attribute) >= 0
    ) {
      return true
    }
    if (this.getEditorActionsOptions().summary) {
      const userSummaryChoice = user
        .get('user')
        .get('preferences')
        .get('inspector-summaryShown')
      if (userSummaryChoice.length > 0) {
        return userSummaryChoice.indexOf(attribute) >= 0
      } else {
        return properties.summaryShow.indexOf(attribute) >= 0
      }
    } else {
      return (
        user
          .get('user')
          .get('preferences')
          .get('inspector-detailsHidden')
          .indexOf(attribute) === -1
      )
    }
  },
  handleFilterValue() {
    filter = this.editorFilter.currentView.model.get('value')
    this.editorProperties.currentView.children.forEach(propertyView => {
      const identifier =
        propertyView.model.get('label') || propertyView.model.id
      if (
        identifier.toLowerCase().indexOf(filter.toLowerCase()) >= 0 &&
        this.isSupposedToBeShown(propertyView.model.id)
      ) {
        propertyView.show()
      } else {
        propertyView.hide()
      }
    })
  },
  handleNewProperties() {
    this.$el.addClass('is-editing')
    this.editorProperties.currentView.turnOnEditing()
  },
  edit() {
    this.$el.addClass('is-editing')
    this.editorProperties.currentView.turnOnEditing()
    this.editorProperties.currentView.focus()
  },
  cancel() {
    this.$el.removeClass('is-editing')
    this.attributesAdded.reset()
    this.attributesRemoved.reset()
    this.editorProperties.currentView.revert()
    this.editorProperties.currentView.turnOffEditing()
    this.afterCancel()
  },
  save() {
    this.$el.removeClass('is-editing')
    const ephemeralAttributes = this.attributesAdded.map(model => model.id)
    const attributesToRemove = this.attributesRemoved.map(model => model.id)
    this.afterSave(
      this.editorProperties.currentView.toPatchJSON(
        ephemeralAttributes,
        attributesToRemove
      )
    )
    this.attributesAdded.reset()
    this.attributesRemoved.reset()
    this.editorProperties.currentView.revert()
    this.editorProperties.currentView.turnOffEditing()
  },
  afterCancel() {
    //override
  },
  afterSave() {
    //override
  },
  toJSON() {
    return this.editorProperties.currentView.toJSON()
  },
})
