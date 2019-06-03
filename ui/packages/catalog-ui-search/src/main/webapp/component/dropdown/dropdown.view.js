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
const CustomElements = require('../../js/CustomElements.js')
const DropdownCompanionView = require('./dropdown.companion.view')
const DropdownModel = require('./dropdown')
const template = require('./dropdown.hbs')
const SelectView = require('../select/select.collection.view.js')
require('../../behaviors/button.behavior.js')

module.exports = Marionette.LayoutView.extend(
  {
    template,
    className: 'is-simpleDropdown',
    tagName: CustomElements.register('dropdown'),
    events: {
      click: 'handleClick',
      mousedown: 'handleMouseDown',
    },
    behaviors: {
      button: {},
    },
    wasOpen: false,
    handleMouseDown(e) {
      this.wasOpen = this.model.get('isOpen')
    },
    handleClick(e) {
      e.preventDefault()
      e.stopPropagation()
      if (this.model.get('isEditing') && !this.wasOpen) {
        this.model.toggleOpen()
      }
    },
    handleEditing() {
      this.$el.toggleClass('is-editing', this.model.get('isEditing'))
    },
    hasTail: false,
    componentToShow: undefined,
    modelForComponent: undefined,
    dropdownCompanion: undefined,
    initializeComponentModel() {
      //override if you need more functionality
      this.modelForComponent = this.options.modelForComponent || this.model
    },
    getTargetElement() {
      //override with where you want the dropdown to center
    },
    listenToComponent() {
      //override if you need more functionality
      this.listenTo(this.modelForComponent, 'change:value', () => {
        this.model.set('value', this.modelForComponent.get('value'))
      })
    },
    initialize() {
      this.initializeComponentModel()
      this.listenTo(this.model, 'change:value', this.render)
      this.listenTo(this.model, 'change:isEditing', this.handleEditing)
      this.listenToComponent()
      this.handleEditing()
    },
    initializeDropdown() {
      this.dropdownCompanion = DropdownCompanionView.getNewCompanionView(this)
    },
    firstRender: true,
    onRender() {
      if (this.firstRender) {
        this.firstRender = false
        this.initializeDropdown()
      }
    },
    turnOnEditing() {
      this.model.set('isEditing', true)
    },
    turnOffEditing() {
      this.model.set('isEditing', false)
    },
    onDestroy() {
      //ensure that if a dropdown goes away, it's companion does too
      if (!this.dropdownCompanion.isDestroyed) {
        this.dropdownCompanion.destroy()
      }
    },
    isCentered: true,
    getCenteringElement() {
      return this.el
    },
    determineSelections() {
      const values = this.model.get('value')
      if (
        this.options.isMultiSelect === undefined &&
        (values[0] === undefined || values[0] === null)
      ) {
        return values[0] // otherwise placeholder (click here to select) won't appear
      }
      return values.map(value => {
        const selection = this.options.list.filter(
          item => JSON.stringify(item.value) === JSON.stringify(value)
        )
        if (selection.length > 0) {
          return selection[0]
        } else {
          return {
            value,
            label: value,
          }
        }
      })
    },
    serializeData() {
      if (this.options.list) {
        const selections = this.determineSelections()
        return {
          value: selections,
          concatenatedLabel: selections
            ? selections
                .map(
                  selection => selection.label || selection.value || selection
                )
                .join(' | ')
            : selections,
        }
      } else {
        return this.model.toJSON()
      }
    },
  },
  {
    createSimpleDropdown(options) {
      return new this({
        model:
          options.model ||
          new DropdownModel({
            value: options.defaultSelection,
            leftIcon: options.leftIcon,
            rightIcon: options.rightIcon,
            label: options.label,
          }),
        list: options.list,
        dropdownCompanionBehaviors: options.dropdownCompanionBehaviors,
        hasFiltering: options.hasFiltering,
        componentToShow: options.componentToShow || SelectView,
        isMultiSelect: options.isMultiSelect,
        defaultSelection: options.defaultSelection,
        customChildView: options.customChildView,
        matchcase: options.matchcase,
        modelForComponent: options.modelForComponent,
        options: options.options,
      })
    },
  }
)
