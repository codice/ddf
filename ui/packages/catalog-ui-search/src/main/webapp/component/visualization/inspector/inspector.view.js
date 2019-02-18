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
/*global require*/
const template = require('./inspector.hbs')
const Marionette = require('marionette')
const CustomElements = require('../../../js/CustomElements.js')
const MetacardView = require('../../tabs/metacard/tabs-metacard.view.js')
const MetacardsView = require('../../tabs/metacards/tabs-metacards.view.js')
const MetacardTitleView = require('../../metacard-title/metacard-title.view.js')

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('inspector'),
  template: template,
  events: {},
  regions: {
    inspector: {
      selector: '.inspector-content',
    },
    inspectorTitle: {
      selector: '.inspector-title',
    },
  },
  initialize: function(options) {
    if (!options.selectionInterface) {
      throw 'Selection interface has not been provided'
    }
    this.setupListeners()
  },
  handleEmpty: function() {
    this.$el.toggleClass(
      'is-empty',
      this.options.selectionInterface.getSelectedResults().length === 0
    )
  },
  onRender: function() {
    this.handleEmpty()
    this.showTitle()
    this.showContent()
  },
  showTitle: function() {
    this.inspectorTitle.show(
      new MetacardTitleView({
        model: this.options.selectionInterface.getSelectedResults(),
      })
    )
  },
  showContent: function() {
    var selectedResults = this.options.selectionInterface.getSelectedResults()
    if (selectedResults.length === 1) {
      this.showMetacard()
    } else if (selectedResults.length > 1) {
      this.showMetacards()
    }
  },
  showMetacards: function() {
    if (
      !this.inspector.currentView ||
      this.inspector.currentView.constructor !== MetacardsView
    ) {
      this.inspector.show(
        new MetacardsView({
          selectionInterface: this.options.selectionInterface,
        })
      )
    }
  },
  showMetacard: function() {
    if (
      !this.inspector.currentView ||
      this.inspector.currentView.constructor !== MetacardView
    ) {
      this.inspector.show(
        new MetacardView({
          selectionInterface: this.options.selectionInterface,
        })
      )
    }
  },
  setupListeners: function() {
    this.listenTo(
      this.options.selectionInterface,
      'reset:activeSearchResults add:activeSearchResults',
      this.onRender
    )
    this.listenTo(
      this.options.selectionInterface.getSelectedResults(),
      'update add remove reset',
      this.onRender
    )
  },
})
