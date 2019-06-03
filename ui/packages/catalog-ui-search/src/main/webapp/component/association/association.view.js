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

const template = require('./association.hbs')
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const Common = require('../../js/Common.js')
const DropdownView = require('../dropdown/dropdown.view.js')
const _ = require('underscore')

function getModelUpdateMethod(modelToUpdate, property, relatedModel) {
  return function() {
    modelToUpdate.set(property, relatedModel.get('value')[0])
  }
}

function getDropdownUpdateMethod(dropdownModel, property, relatedModel) {
  return function() {
    dropdownModel.set('value', [relatedModel.get(property)])
  }
}

function determineChoices(view) {
  const currentMetacard = view.options.currentMetacard
  let choices = view.options.selectionInterface
    .getCurrentQuery()
    .get('result')
    .get('results')
    .filter(
      result => result.get('metacard').id !== currentMetacard.get('metacard').id
    )
    .filter(
      result =>
        !(
          result.isWorkspace() ||
          result.isRevision() ||
          result.isRemote() ||
          result.isDeleted()
        )
    )
    .reduce(
      (options, result) => {
        options.push({
          label: result
            .get('metacard')
            .get('properties')
            .get('title'),
          value: result.get('metacard').id,
        })
        return options
      },
      [
        {
          label: 'Current Metacard',
          value: currentMetacard.get('metacard').id,
        },
      ].concat(
        view.options.knownMetacards
          .filter(
            metacard => metacard.id !== currentMetacard.get('metacard').id
          )
          .map(metacard => ({
            label: metacard.get('title'),
            value: metacard.id,
          }))
      )
    )
  choices = _.uniq(choices, false, choice => choice.value)
  return choices
}

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('association'),
  template,
  regions: {
    associationParent: '> .association-content > .association-parent',
    associationRelationship:
      '> .association-content > .association-relationship',
    associationChild: '> .association-content > .association-child',
  },
  events: {
    'click > .association-remove': 'removeAssociation',
  },
  initialize() {
    const currentMetacardId = this.options.currentMetacard.get('metacard').id
    if (!this.model.get('parent')) {
      this.model.set('parent', currentMetacardId)
    }
    if (!this.model.get('child')) {
      this.model.set('child', currentMetacardId)
    }
  },
  removeAssociation() {
    this.model.collection.remove(this.model)
  },
  choices: undefined,
  onBeforeShow() {
    this.choices = determineChoices(this)
    this.showAssociationParent()
    this.showAssociationRelationship()
    this.showAssociationChild()
    this.setupListeners()
    this.setupReadOnlyLabelListeners()
    this.updateReadOnlyLabels()
    this.checkHeritage()
  },
  turnOnEditing() {
    this.$el.toggleClass('is-editing', true)
    this.regionManager.forEach(region => {
      if (region.currentView && region.currentView.turnOnEditing) {
        region.currentView.turnOnEditing()
      }
    })
  },
  turnOffEditing() {
    this.$el.toggleClass('is-editing', false)
    this.regionManager.forEach(region => {
      if (region.currentView && region.currentView.turnOffEditing) {
        region.currentView.turnOffEditing()
      }
    })
  },
  setupListeners() {
    this.listenTo(
      this.model,
      'change:parent change:child',
      this.ensureAtLeastOneCurrent
    )
  },
  ensureAtLeastOneCurrent(model, options) {
    const currentMetacard = this.options.currentMetacard
    const value = model.hasChanged('parent')
      ? model.get('parent')
      : model.get('child')
    if (value !== currentMetacard.get('metacard').id) {
      model.set(
        model.hasChanged('parent') ? 'child' : 'parent',
        currentMetacard.get('metacard').id
      )
    }
    this.rerenderDropdowns()
    this.checkHeritage()
  },
  checkHeritage() {
    const currentMetacard = this.options.currentMetacard
    this.$el.toggleClass(
      'is-parent',
      this.model.get('parent') === currentMetacard.get('metacard').id
    )
    this.$el.toggleClass(
      'is-child',
      this.model.get('child') === currentMetacard.get('metacard').id
    )
  },
  rerenderDropdowns() {
    const childDropdown = this.associationChild.currentView.dropdownCompanion
      .componentToShow.currentView
    const parentDropdown = this.associationParent.currentView.dropdownCompanion
      .componentToShow.currentView
    if (childDropdown) {
      childDropdown.render()
    }
    if (parentDropdown) {
      parentDropdown.render()
    }
  },
  updateAssociationParent() {
    this.model.set(
      'parent',
      this.associationParent.currentView.model.get('value')
    )
  },
  showAssociationParent() {
    this.associationParent.show(
      DropdownView.createSimpleDropdown({
        list: this.choices,
        defaultSelection: [this.model.get('parent') || this.choices[0].value],
        hasFiltering: true,
      })
    )
    const relatedModel = this.associationParent.currentView.model
    this.listenTo(
      relatedModel,
      'change:value',
      getModelUpdateMethod(this.model, 'parent', relatedModel)
    )
    this.listenTo(
      this.model,
      'change:parent',
      getDropdownUpdateMethod(relatedModel, 'parent', this.model)
    )
  },
  showAssociationRelationship() {
    this.associationRelationship.show(
      DropdownView.createSimpleDropdown({
        list: [
          {
            label: 'related to',
            value: 'related',
          },
          {
            label: 'derived from',
            value: 'derived',
          },
        ],
        defaultSelection: [this.model.get('relationship') || 'related'],
      })
    )
    const relatedModel = this.associationRelationship.currentView.model
    this.listenTo(
      relatedModel,
      'change:value',
      getModelUpdateMethod(this.model, 'relationship', relatedModel)
    )
    this.listenTo(
      this.model,
      'change:relationship',
      getDropdownUpdateMethod(relatedModel, 'relationship', this.model)
    )
  },
  showAssociationChild() {
    this.associationChild.show(
      DropdownView.createSimpleDropdown({
        list: this.choices,
        defaultSelection: [this.model.get('child') || this.choices[0].value],
        hasFiltering: true,
      })
    )
    const relatedModel = this.associationChild.currentView.model
    this.listenTo(
      relatedModel,
      'change:value',
      getModelUpdateMethod(this.model, 'child', relatedModel)
    )
    this.listenTo(
      this.model,
      'change:child',
      getDropdownUpdateMethod(relatedModel, 'child', this.model)
    )
  },
  setupReadOnlyLabelListeners() {
    this.listenTo(this.model, 'change:child', this.updateChildReadOnly)
    this.listenTo(
      this.model,
      'change:relationship',
      this.updateRelationshipReadOnly
    )
    this.listenTo(this.model, 'change:parent', this.updateParentReadOnly)
  },
  updateChildReadOnly() {
    const currentMetacard = this.options.currentMetacard
    const currentId = this.model.get('child')
    const label =
      currentMetacard.get('metacard').id === currentId
        ? 'Current Metacard'
        : this.getChoiceById(currentId).label
    this.$el.find('.association-child').attr('data-label', label)
    this.$el
      .find('.association-child-link a')
      .attr('href', '#metacards/' + currentId)
      .html(Common.escapeHTML(label))
  },
  updateParentReadOnly() {
    const currentMetacard = this.options.currentMetacard
    const currentId = this.model.get('parent')
    const label =
      currentMetacard.get('metacard').id === currentId
        ? 'Current Metacard'
        : this.getChoiceById(currentId).label
    this.$el.find('.association-parent').attr('data-label', label)
    this.$el
      .find('.association-parent-link a')
      .attr('href', '#metacards/' + currentId)
      .html(Common.escapeHTML(label))
  },
  updateRelationshipReadOnly() {
    const currentMetacard = this.options.currentMetacard
    const currentRelation =
      this.model.get('relationship') === 'related'
        ? 'related to'
        : 'derived from'
    this.$el
      .find('.association-relationship')
      .attr('data-label', currentRelation)
  },
  updateReadOnlyLabels() {
    this.updateChildReadOnly()
    this.updateParentReadOnly()
    this.updateRelationshipReadOnly()
  },
  getChoiceById(id) {
    return this.choices.filter(choice => choice.value === id)[0]
  },
})
