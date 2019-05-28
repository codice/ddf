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
const _ = require('underscore')
const $ = require('jquery')
const template = require('./list-editor.hbs')
const CustomElements = require('../../js/CustomElements.js')
require('../../behaviors/button.behavior.js')
var DropdownView = require('../dropdown/dropdown.view.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const List = require('../../js/model/List.js')
var DropdownView = require('../dropdown/popout/dropdown.popout.view.js')
const ListFilterView = require('../result-filter/list/result-filter.list.view.js')
const properties = require('../../js/properties.js')

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('list-editor'),
  template,
  events: {
    'click .editor-cancel': 'cancel',
    'click .editor-save': 'save',
  },
  regions: {
    listTitle: '.list-title',
    listTemplate: '.list-template',
    listCQLSwitch: '.list-limiting-switch',
    listCQL: '.list-limiting',
    listIcon: '.list-icon',
  },
  listTemplateId: 'custom',
  initialize(options) {
    this.model.set('showFooter', this.options.showFooter)
  },
  onBeforeShow() {
    this.showListTitle()
    this.showListTemplate()
    this.showCQLSwitch()
    this.showCQL()
    this.showIcon()
    this.edit()
  },
  showListTitle() {
    this.listTitle.show(
      PropertyView.getPropertyView({
        label: 'Title',
        value: [this.model.get('title')],
        type: 'TEXT',
      })
    )
  },
  handleListTemplate() {
    this.$el.toggleClass('is-template', this.listTemplateId !== 'custom')
  },
  showListTemplate() {
    if (this.options.showListTemplates === true) {
      const propertyModel = new Property({
        label: 'Template',
        value: [this.listTemplateId],
        enum: [
          {
            label: 'Custom',
            value: 'custom',
          },
        ].concat(
          properties.listTemplates.map(template => ({
            label: template.id,
            value: template.id,
            class: List.getIconMapping()[template['list.icon']],
          }))
        ),
        id: 'Template',
      })
      this.listTemplate.show(
        new PropertyView({
          model: propertyModel,
        })
      )
      this.listTemplate.currentView.turnOnEditing()
      this.listenTo(propertyModel, 'change:value', () => {
        this.listTemplateId = propertyModel.getValue()[0]
        this.handleListTemplate()
      })
    }
  },
  showCQLSwitch() {
    this.listCQLSwitch.show(
      PropertyView.getPropertyView({
        label: 'Limit based on filter',
        value: [this.model.get('list.cql') !== ''],
        radio: [
          {
            label: 'Yes',
            value: true,
          },
          {
            label: 'No',
            value: false,
          },
        ],
      })
    )
    this.listenTo(
      this.listCQLSwitch.currentView.model,
      'change:value',
      this.handleCQLSwitch
    )
    this.handleCQLSwitch()
  },
  handleCQLSwitch() {
    const shouldLimit = this.listCQLSwitch.currentView.model.getValue()[0]
    this.$el.toggleClass('is-limited', shouldLimit)
  },
  showCQL() {
    this.listCQL.show(
      DropdownView.createSimpleDropdown({
        componentToShow: ListFilterView,
        defaultSelection: this.model.get('list.cql'),
        leftIcon: 'fa fa-pencil',
        label: 'Edit Filter',
      })
    )
  },
  showIcon() {
    this.listIcon.show(
      PropertyView.getPropertyView({
        label: 'Icon',
        value: [this.model.get('list.icon')],
        enum: List.getIconMappingForSelect(),
      })
    )
  },
  edit() {
    this.$el.addClass('is-editing')
    this.regionManager.forEach(region => {
      if (region.currentView && region.currentView.turnOnEditing) {
        region.currentView.turnOnEditing()
      }
    })
    const tabbable = _.filter(
      this.$el.find('[tabindex], input, button'),
      element => element.offsetParent !== null
    )
    if (tabbable.length > 0) {
      $(tabbable[0]).focus()
    }
  },
  cancel() {
    this.$el.removeClass('is-editing')
    this.onBeforeShow()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  saveIcon() {
    const icon =
      this.listTemplateId === 'custom'
        ? this.listIcon.currentView.model.getValue()[0]
        : properties.listTemplates.filter(
            template => template.id === this.listTemplateId
          )[0]['list.icon']
    this.model.set('list.icon', icon)
  },
  saveTitle() {
    this.model.set('title', this.listTitle.currentView.model.getValue()[0])
  },
  saveCQL() {
    const shouldLimit = this.listCQLSwitch.currentView.model.getValue()[0]
    let cql = ''
    if (this.listTemplateId !== 'custom') {
      cql = properties.listTemplates.filter(
        template => template.id === this.listTemplateId
      )[0]['list.cql']
    } else if (shouldLimit === true) {
      cql = this.listCQL.currentView.model.getValue()
    }
    this.model.set('list.cql', cql)
  },
  save() {
    this.saveTitle()
    this.saveIcon()
    this.saveCQL()
    this.cancel()
  },
  serializeData() {
    return this.model.toJSON({
      additionalProperties: ['cid', 'color'],
    })
  },
})
