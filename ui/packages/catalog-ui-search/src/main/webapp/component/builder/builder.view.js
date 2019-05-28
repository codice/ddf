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

import fetch from '../../react-component/utils/fetch'

const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./builder.hbs')
const CustomElements = require('../../js/CustomElements.js')
const PropertyCollectionView = require('../property/property.collection.view.js')
const LoadingCompanionView = require('../loading-companion/loading-companion.view.js')
const metacardDefinitions = require('../singletons/metacard-definitions.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const announcement = require('../announcement/index.jsx')

const AVAILABLE_TYPES_ATTRIBUTE = 'availableTypes'
const SELECTED_AVAILABLE_TYPE_ATTRIBUTE = 'selectedAvailableType'

let retrievedAvailableTypes

const announceMissingDefinition = definition =>
  announcement.announce({
    title: 'Missing Type Definition',
    message: `Could not find type definition "${definition}". Please contact the system administrator to add this type to the whitelist.`,
    type: 'warn',
  })

const retrieveAvailableTypes = async () => {
  const response = await fetch('./internal/builder/availabletypes')
  return await response.json()
}

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('builder'),
  modelEvents: {
    'change:metacard': 'handleMetacard',
  },
  events: {
    'click .builder-edit': 'edit',
    'click .builder-save': 'save',
    'click .builder-cancel': 'cancel',
  },
  regions: {
    builderProperties: '> .builder-properties',
    builderAvailableType:
      '> .builder-select-available-type > .builder-select-available-type-dropdown',
  },
  async initialize(options) {
    if (!retrievedAvailableTypes) {
      LoadingCompanionView.beginLoading(this)
      retrievedAvailableTypes = await retrieveAvailableTypes()
      LoadingCompanionView.endLoading(this)

      this.model.set(AVAILABLE_TYPES_ATTRIBUTE, retrievedAvailableTypes)
      this.handleAvailableTypes()
    } else {
      this.model.set(AVAILABLE_TYPES_ATTRIBUTE, retrievedAvailableTypes)
    }
  },
  isSingleAvailableType() {
    const availableTypes = this.model.get(AVAILABLE_TYPES_ATTRIBUTE)
    return (
      availableTypes &&
      availableTypes.availabletypes &&
      availableTypes.availabletypes.length === 1
    )
  },
  isMultipleAvailableTypes() {
    const availableTypes = this.model.get(AVAILABLE_TYPES_ATTRIBUTE)
    return (
      availableTypes &&
      availableTypes.availabletypes &&
      availableTypes.availabletypes.length > 1
    )
  },
  showMetacardTypeSelection(availableTypes) {
    const enums = availableTypes.map(availableType => ({
      label: availableType.metacardType,
      value: availableType.metacardType,
    }))

    const availableTypesModel = new Property({
      label: 'Select An Available Metacard Type',
      value: [availableTypes[0].metacardType],
      enum: enums,
      id: 'Select Metacard Type',
    })

    this.builderAvailableType.show(
      new PropertyView({
        model: availableTypesModel,
      })
    )

    this.builderAvailableType.currentView.turnOnEditing()

    this.listenTo(
      availableTypesModel,
      'change:value',
      this.handleSelectedAvailableType
    )

    //This selects the first element in the drop-down
    this.handleSelectedAvailableType(this, Object.values(availableTypes)[0])

    this.$el.addClass('is-selecting-available-types')
  },
  handleSystemTypes() {
    const mds = metacardDefinitions.metacardDefinitions

    const allTypes = Object.keys(mds)
      .sort()
      .reduce(
        (accumulator, currentValue) => {
          const visibleAttributes = Object.keys(mds[currentValue])
          accumulator.availabletypes.push({
            metacardType: currentValue,
            visibleAttributes,
          })
          return accumulator
        },
        { availabletypes: [] }
      )

    this.model.set(AVAILABLE_TYPES_ATTRIBUTE, allTypes)

    this.showMetacardTypeSelection(allTypes.availabletypes)
  },
  handleAvailableTypes() {
    if (this.isSingleAvailableType()) {
      const selectedAvailableType = this.model.get(AVAILABLE_TYPES_ATTRIBUTE)
        .availabletypes[0]
      this.model.set(SELECTED_AVAILABLE_TYPE_ATTRIBUTE, selectedAvailableType)
      this.showMetacardBuilder(selectedAvailableType)
    } else if (this.isMultipleAvailableTypes()) {
      this.showMetacardTypeSelection(
        this.model.get(AVAILABLE_TYPES_ATTRIBUTE).availabletypes
      )
    } else {
      this.handleSystemTypes()
    }
  },
  handleSelectedAvailableType(reference, selectedAvailableTypeList) {
    const selectedAvailableType =
      selectedAvailableTypeList && Array.isArray(selectedAvailableTypeList)
        ? selectedAvailableTypeList[0]
        : selectedAvailableTypeList

    const availableTypes = this.model.get(AVAILABLE_TYPES_ATTRIBUTE)
      .availabletypes

    const selection = availableTypes.find(type => {
      const selectedType =
        (selectedAvailableType && selectedAvailableType.metacardType) ||
        selectedAvailableType
      return type.metacardType === selectedType
    })

    if (!selection) {
      return
    }
    this.model.set(SELECTED_AVAILABLE_TYPE_ATTRIBUTE, selection)

    this.showMetacardBuilder(selection)
  },
  showMetacardBuilder(selectedAvailableType) {
    const metacardDefinition =
      metacardDefinitions.metacardDefinitions[
        selectedAvailableType.metacardType
      ]

    const propertyCollection = selectedAvailableType.visibleAttributes
      .filter(attribute => !metacardDefinitions.isHiddenType(attribute))
      .filter(
        attribute =>
          metacardDefinition && !metacardDefinition[attribute].readOnly
      )
      .filter(attribute => attribute !== 'id')
      .reduce((obj, attribute) => {
        if (metacardDefinition[attribute].multivalued)
          return { ...obj, [attribute]: [] }

        if (metacardDefinitions.enums[attribute])
          return {
            ...obj,
            [attribute]: metacardDefinitions.enums[attribute][0],
          }

        return { ...obj, [attribute]: undefined }
      }, {})

    this.model.set('metacard', {
      'metacard-type': selectedAvailableType.metacardType,
      ...propertyCollection,
    })

    if (Object.keys(propertyCollection).length === 0) {
      announceMissingDefinition(selectedAvailableType.metacardType)
      return
    }

    this.handleMetacard()
    this.$el.addClass('is-building')
  },
  handleMetacard() {
    const metacard = this.model.get('metacard')
    this.builderProperties.show(
      PropertyCollectionView.generatePropertyCollectionView([metacard])
    )
    this.builderProperties.currentView.$el.addClass('is-list')
  },
  onBeforeShow() {
    this.handleAvailableTypes()
  },
  edit() {
    this.$el.addClass('is-editing')
    this.builderProperties.currentView.turnOnEditing()
    this.builderProperties.currentView.focus()
  },
  cancel() {
    this.$el.removeClass('is-editing')
    this.builderProperties.currentView.revert()
    this.builderProperties.currentView.turnOffEditing()
  },
  save() {
    this.$el.removeClass('is-editing')

    const metacardType = this.model.get(SELECTED_AVAILABLE_TYPE_ATTRIBUTE)
      .metacardType

    const metacardDefinition =
      metacardDefinitions.metacardDefinitions[metacardType]

    const editedMetacard = this.builderProperties.currentView.toPropertyJSON()

    const props = editedMetacard.properties
    editedMetacard.properties = Object.keys(editedMetacard.properties)
      .filter(attributeName => props[attributeName].length >= 1)
      .filter(attributeName => props[attributeName][0] !== '')
      .reduce(
        (accummulator, currentValue) =>
          _.extend(accummulator, {
            [currentValue]: metacardDefinition[currentValue].multivalued
              ? props[currentValue]
              : props[currentValue][0],
          }),
        {}
      )

    editedMetacard.properties['metacard-type'] = metacardType
    editedMetacard.type = 'Feature'

    $.ajax({
      type: 'POST',
      url: './internal/catalog/?transform=geojson',
      data: JSON.stringify(editedMetacard),
      dataType: 'text',
      contentType: 'application/json',
    }).then((response, status, xhr) => {
      const id = xhr.getResponseHeader('id')
      if (id) {
        this.options.handleNewMetacard(id)
        this.options.close()
      }
    })

    this.builderProperties.currentView.turnOffEditing()
  },
})
