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

const Marionette = require('marionette')
const template = require('./details-interactions.hbs')
const CustomElements = require('../../js/CustomElements.js')
const DropdownModel = require('../dropdown/dropdown.js')
const AddAttributeView = require('../dropdown/add-attribute/dropdown.add-attribute.view.js')
const RemoveAttributeView = require('../dropdown/remove-attribute/dropdown.remove-attribute.view.js')
const AttributesRearrangeView = require('../dropdown/attributes-rearrange/dropdown.attributes-rearrange.view.js')
const ShowAttributeView = require('../dropdown/show-attribute/dropdown.show-attribute.view.js')
const HideAttributeView = require('../dropdown/hide-attribute/dropdown.hide-attribute.view.js')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('details-interactions'),
  className: 'composed-menu',
  regions: {
    detailsAdd: '.interaction-add',
    detailsRemove: '.interaction-remove',
    detailsRearrange: '.interaction-rearrange',
    detailsShow: '.interaction-show',
    detailsHide: '.interaction-hide',
  },
  initialize() {
    this.handleTypes()
    this.handleSummary()
  },
  handleSummary() {
    this.$el.toggleClass('is-summary', this.options.summary)
  },
  handleTypes() {
    const types = {}
    this.options.selectionInterface.getSelectedResults().forEach(result => {
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
    })
    this.$el.toggleClass('is-mixed', Object.keys(types).length > 1)
    this.$el.toggleClass('is-workspace', types.workspace !== undefined)
    this.$el.toggleClass('is-resource', types.resource !== undefined)
    this.$el.toggleClass('is-revision', types.revision !== undefined)
    this.$el.toggleClass('is-deleted', types.deleted !== undefined)
    this.$el.toggleClass('is-remote', types.remote !== undefined)
  },
  generateDetailsAdd() {
    this.detailsAdd.show(
      new AddAttributeView({
        model: new DropdownModel(),
        selectionInterface: this.options.selectionInterface,
      }),
      {
        replaceElement: true,
      }
    )
    this.listenTo(
      this.detailsAdd.currentView.model,
      'change:value',
      this.handleAddAttribute
    )
  },
  generateDetailsRemove() {
    this.detailsRemove.show(
      new RemoveAttributeView({
        model: new DropdownModel(),
        selectionInterface: this.options.selectionInterface,
      }),
      {
        replaceElement: true,
      }
    )
    this.listenTo(
      this.detailsRemove.currentView.model,
      'change:value',
      this.handleRemoveAttribute
    )
  },
  generateDetailsShow() {
    this.detailsShow.show(
      new ShowAttributeView({
        model: new DropdownModel(),
        selectionInterface: this.options.selectionInterface,
      }),
      {
        replaceElement: true,
      }
    )
    this.listenTo(
      this.detailsShow.currentView.model,
      'change:value',
      this.handleShowAttribute
    )
  },
  generateDetailsHide() {
    this.detailsHide.show(
      new HideAttributeView({
        model: new DropdownModel(),
        selectionInterface: this.options.selectionInterface,
      }),
      {
        replaceElement: true,
      }
    )
    this.listenTo(
      this.detailsHide.currentView.model,
      'change:value',
      this.handleHideAttribute
    )
  },
  generateDetailsRearrange() {
    this.detailsRearrange.show(
      new AttributesRearrangeView({
        model: new DropdownModel(),
        selectionInterface: this.options.selectionInterface,
        summary: this.options.summary,
      }),
      {
        replaceElement: true,
      }
    )
  },
  onRender() {
    this.generateDetailsAdd()
    this.generateDetailsRemove()
    this.generateDetailsRearrange()
    this.generateDetailsShow()
    this.generateDetailsHide()
  },
  handleRemoveAttribute() {
    this.model.set(
      'attributesToRemove',
      this.detailsRemove.currentView.model.get('value')
    )
  },
  handleAddAttribute() {
    this.model.set(
      'attributesToAdd',
      this.detailsAdd.currentView.model.get('value')
    )
  },
})
