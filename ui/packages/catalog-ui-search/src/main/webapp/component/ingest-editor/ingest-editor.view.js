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
/*global define*/
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const store = require('../../js/store.js')
const template = require('./ingest-editor.hbs')
const CustomElements = require('../../js/CustomElements.js')
const PropertyCollectionView = require('../property/property.collection.view.js')
const properties = require('../../js/properties.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('ingest-editor'),
  events: {
    'click .ingest-editor-clear': 'clear',
  },
  regions: {
    editorProperties: '.ingest-editor-properties',
  },
  onBeforeShow: function() {
    this.editorProperties.show(
      PropertyCollectionView.generateFilteredPropertyCollectionView(
        properties.editorAttributes,
        []
      )
    )
    this.editorProperties.currentView.$el.addClass('is-list')
    this.editorProperties.currentView.turnOnEditing()
  },
  clear: function() {
    this.editorProperties.currentView.revert()
    this.editorProperties.currentView.hideRequiredWarnings()
  },
  getPropertyCollectionView: function() {
    return this.editorProperties.currentView
  },
  /*
        Return a map of attributes to their corresponding value arrays. Blank values are
        filtered, and only attributes with at least one non-blank value are returned.
     */
  getAttributeOverrides: function() {
    return _.chain(
      this.editorProperties.currentView.toPropertyJSON().properties
    )
      .mapObject(function(values) {
        return values.filter(value => value.trim().length > 0)
      })
      .pick(function(values) {
        return values.length > 0
      })
      .value()
  },
})
