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
const template = require('./search-interactions.hbs')
const CustomElements = require('../../js/CustomElements.js')
const lightboxInstance = require('../lightbox/lightbox.view.instance.js')
const SearchSettingsDropdownView = require('../dropdown/search-settings/dropdown.search-settings.view.js')
const DropdownModel = require('../dropdown/dropdown.js')
const SearchFormSelectorDropdownView = require('../dropdown/search-form-selector/dropdown.search-form-selector.view.js')
const _merge = require('lodash/merge')
const ConfirmationView = require('../confirmation/confirmation.view.js')
const user = require('../singletons/user-instance.js')
const properties = require('../../js/properties.js')

module.exports = Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('search-interactions'),
  className: 'composed-menu',
  regions: {
    searchType: '.interaction-type',
    resultType: '.interaction-result-type',
    searchAdvanced: '.interaction-type-advanced',
    searchSettings: '.interaction-settings',
  },
  events: {
    'click > .interaction-reset': 'triggerReset',
    'click > .interaction-type-advanced': 'triggerTypeAdvanced',
    'click > .interaction-type-basic': 'triggerTypeBasic',
    'click > .interaction-type-text': 'triggerTypeText',
  },
  onRender() {
    this.listenTo(
      this.model,
      'change:type closeDropdown',
      this.triggerCloseDropdown
    )
    this.generateSearchFormSelector()
    this.generateSearchSettings()
  },
  generateSearchFormSelector() {
    this.searchType.show(
      new SearchFormSelectorDropdownView({
        model: new DropdownModel(),
        modelForComponent: this.model,
        selectionInterface: this.options.selectionInterface,
      }),
      {
        replaceElement: true,
      }
    )
  },
  generateSearchSettings() {
    this.searchSettings.show(
      new SearchSettingsDropdownView({
        model: new DropdownModel(),
        modelForComponent: this.model,
        selectionInterface: this.options.selectionInterface,
        showFooter: true,
      }),
      {
        replaceElement: true,
      }
    )
  },
  triggerCloseDropdown() {
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
  },
  triggerReset() {
    this.listenTo(
      ConfirmationView.generateConfirmation({
        prompt: 'Are you sure you want to reset the search?',
        no: 'Cancel',
        yes: 'Reset',
      }),
      'change:choice',
      function(confirmation) {
        if (confirmation.get('choice')) {
          const defaults =
            this.model.get('type') === 'custom'
              ? this.model.toJSON()
              : undefined
          this.model.resetToDefaults(defaults)
          this.triggerCloseDropdown()
        }
      }.bind(this)
    )
  },
  triggerTypeAdvanced() {
    this.model.set('type', 'advanced')
    user.getQuerySettings().set('type', 'advanced')
    user.savePreferences()
    this.triggerCloseDropdown()
  },
  triggerTypeBasic() {
    this.model.set('type', 'basic')
    user.getQuerySettings().set('type', 'basic')
    user.savePreferences()
    this.triggerCloseDropdown()
  },
  triggerTypeText() {
    this.model.set('type', 'text')
    user.getQuerySettings().set('type', 'text')
    user.savePreferences()
    this.triggerCloseDropdown()
  },
  serializeData() {
    return {
      experimental: properties.hasExperimentalEnabled(),
    }
  },
})
