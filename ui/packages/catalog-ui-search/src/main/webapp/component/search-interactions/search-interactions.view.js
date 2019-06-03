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
const template = require('./search-interactions.hbs')
const CustomElements = require('../../js/CustomElements.js')
const DropdownModel = require('../dropdown/dropdown.js')
const SearchFormSelectorDropdownView = require('../dropdown/search-form-selector/dropdown.search-form-selector.view.js')
const ConfirmationView = require('../confirmation/confirmation.view.js')
const user = require('../singletons/user-instance.js')
const properties = require('../../js/properties.js')

module.exports = Marionette.LayoutView.extend({
  template,
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
      confirmation => {
        if (confirmation.get('choice')) {
          const defaults =
            this.model.get('type') === 'custom'
              ? this.model.toJSON()
              : undefined
          this.model.resetToDefaults(defaults)
          this.triggerCloseDropdown()
        }
      }
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
