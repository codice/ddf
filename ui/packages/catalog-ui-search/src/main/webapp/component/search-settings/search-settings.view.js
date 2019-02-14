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
/*global define, setTimeout*/
const Marionette = require('marionette')
const _ = require('underscore')
const properties = require('../../js/properties.js')
const $ = require('jquery')
const template = require('./search-settings.hbs')
const CustomElements = require('../../js/CustomElements.js')
const user = require('../singletons/user-instance.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const QuerySettingsView = require('../query-settings/query-settings.view.js')
const QueryModel = require('../../js/model/Query.js')
const ConfirmationView = require('../confirmation/confirmation.view.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('search-settings'),
  regions: {
    propertyResultCount: '.property-result-count',
    propertySearchSettings: '.property-search-settings',
  },
  events: {
    'click > .editor-footer .editor-save': 'triggerSave',
    'click > .editor-footer .editor-cancel': 'triggerCancel',
  },
  initialize: function() {
    this.showFooter()
  },
  showFooter: function() {
    this.$el.toggleClass('show-footer', this.options.showFooter === true)
  },
  onBeforeShow: function() {
    this.setupResultCount()
    this.setupSearchSettings()
    if (this.options.showFooter !== true) {
      this.listenToOnce(this.regionManager, 'before:remove:region', this.save)
    }
  },
  setupSearchSettings: function() {
    this.propertySearchSettings.show(
      new QuerySettingsView({
        model: new QueryModel.Model(),
        inSearchSettings: true,
      })
    )
  },
  setupResultCount: function() {
    var userResultCount = user
      .get('user')
      .get('preferences')
      .get('resultCount')

    const model = new Property({
      label: 'Number of Search Results',
      value: [userResultCount],
      min: 1,
      max: properties.resultCount,
      type: 'RANGE',
    })

    this.propertyResultCount.show(new PropertyView({ model }))

    this.propertyResultCount.currentView.turnOnEditing()
    this.listenTo(model, 'change:value', this.updateResultCountSettings)
  },
  updateSearchSettings: function() {
    user
      .getPreferences()
      .get('querySettings')
      .set(this.propertySearchSettings.currentView.toJSON())
  },
  updateResultCountSettings: function() {
    user.getPreferences().set({
      resultCount: this.propertyResultCount.currentView.model.getValue()[0],
    })
  },
  triggerSave: function() {
    this.save()
    this.listenTo(
      ConfirmationView.generateConfirmation({
        prompt: 'Do you want to apply the new defaults to this search?',
        no: 'No',
        yes: 'Apply',
      }),
      'change:choice',
      function(confirmation) {
        if (confirmation.get('choice')) {
          this.model.applyDefaults()
        }
      }.bind(this)
    )
    this.onBeforeShow()
  },
  save: function() {
    this.updateResultCountSettings()
    this.updateSearchSettings()
    user.savePreferences()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  triggerCancel: function() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    this.onBeforeShow()
  },
})
