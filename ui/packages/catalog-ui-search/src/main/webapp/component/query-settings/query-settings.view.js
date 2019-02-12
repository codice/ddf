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
const Backbone = require('backbone')
const _ = require('underscore')
const $ = require('jquery')
const template = require('./query-settings.hbs')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const DropdownModel = require('../dropdown/dropdown.js')
const QuerySrcView = require('../dropdown/query-src/dropdown.query-src.view.js')
const PropertyView = require('../property/property.view.js')
const Property = require('../property/property.js')
const SortItemCollectionView = require('../sort/sort.view.js')
const Common = require('../../js/Common.js')
const properties = require('../../js/properties.js')
const plugin = require('plugins/query-settings')
const announcement = require('../announcement/index.jsx')
import { InvalidSearchFormMessage } from 'component/announcement/CommonMessages'
const ResultForm = require('../result-form/result-form.js')

module.exports = plugin(
  Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('query-settings'),
    modelEvents: {},
    events: {
      'click .editor-edit': 'turnOnEditing',
      'click .editor-cancel': 'cancel',
      'click .editor-save': 'save',
      'click .editor-saveRun': 'run',
    },
    regions: {
      settingsSortField: '.settings-sorting-field',
      settingsSrc: '.settings-src',
      resultForm: '.result-form',
      extensions: '.query-extensions',
    },
    ui: {},
    focus: function() {},
    initialize: function() {
      this.model = this.model._cloneOf
        ? store.getQueryById(this.model._cloneOf)
        : this.model
      this.listenTo(
        this.model,
        'change:sortField change:sortOrder change:src change:federation',
        Common.safeCallback(this.onBeforeShow)
      )
      this.resultFormCollection = ResultForm.getResultCollection()
      this.listenTo(
        this.resultFormCollection,
        'change:added',
        this.handleFormUpdate
      )
    },
    handleFormUpdate: function(newForm) {
      this.renderResultForms(this.resultFormCollection.filteredList)
    },
    onBeforeShow: function() {
      this.setupSortFieldDropdown()
      this.setupSrcDropdown()
      this.turnOnEditing()
      this.renderResultForms(this.resultFormCollection.filteredList)
      this.setupExtensions()
    },
    renderResultForms: function(resultTemplates) {
      resultTemplates = resultTemplates ? resultTemplates : []
      resultTemplates.push({
        label: 'All Fields',
        value: 'allFields',
        id: 'All Fields',
        descriptors: [],
        description: 'All Fields',
      })
      resultTemplates = _.uniq(resultTemplates, 'id')
      let lastIndex = resultTemplates.length - 1
      let detailLevelProperty = new Property({
        label: 'Result Form',
        enum: resultTemplates,
        value: [
          this.model.get('detail-level') ||
            (resultTemplates &&
              resultTemplates[lastIndex] &&
              resultTemplates[lastIndex].value),
        ],
        showValidationIssues: false,
        id: 'Result Form',
      })
      this.listenTo(
        detailLevelProperty,
        'change:value',
        this.handleChangeDetailLevel
      )
      this.resultForm.show(
        new PropertyView({
          model: detailLevelProperty,
        })
      )
      this.resultForm.currentView.turnOnEditing()
    },
    getExtensions: function() {},
    setupExtensions: function() {
      const extensions = this.getExtensions()
      if (extensions !== undefined) {
        this.extensions.show(extensions)
      } else {
        this.extensions.empty()
      }
    },
    handleChangeDetailLevel: function(model, values) {
      $.each(
        model.get('enum'),
        function(index, value) {
          if (values[0] === value.value) {
            this.model.set('detail-level', value)
          }
        }.bind(this)
      )
    },
    onRender: function() {
      this.setupSrcDropdown()
    },
    setupSortFieldDropdown: function() {
      this.settingsSortField.show(
        new SortItemCollectionView({
          collection: new Backbone.Collection(this.model.get('sorts')),
          showBestTextOption: true,
        })
      )
    },
    setupSrcDropdown: function() {
      var sources = this.model.get('src')
      this._srcDropdownModel = new DropdownModel({
        value: sources ? sources : [],
        federation: this.model.get('federation'),
      })
      if (this.getExtensions() !== undefined) {
        return
      }
      this.settingsSrc.show(
        new QuerySrcView({
          model: this._srcDropdownModel,
        })
      )
      this.settingsSrc.currentView.turnOffEditing()
    },
    turnOffEditing: function() {
      this.$el.removeClass('is-editing')
      this.regionManager.forEach(function(region) {
        if (region.currentView && region.currentView.turnOffEditing) {
          region.currentView.turnOffEditing()
        }
      })
    },
    turnOnEditing: function() {
      this.$el.addClass('is-editing')
      this.regionManager.forEach(function(region) {
        if (region.currentView && region.currentView.turnOnEditing) {
          region.currentView.turnOnEditing()
        }
      })
      this.focus()
    },
    cancel: function() {
      this.$el.removeClass('is-editing')
      this.onBeforeShow()
      this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    },
    toJSON: function() {
      var federation = this._srcDropdownModel.get('federation')
      var src
      if (federation === 'selected') {
        src = this._srcDropdownModel.get('value')
        if (src === undefined || src.length === 0) {
          federation = 'local'
        }
      }
      var sorts = this.settingsSortField.currentView.collection.toJSON()
      let detailLevel =
        this.resultForm.currentView &&
        this.resultForm.currentView.model.get('value')[0]
      if (detailLevel && detailLevel === 'allFields') {
        detailLevel = undefined
      }
      return {
        src: src,
        federation: federation,
        sorts: sorts,
        'detail-level': detailLevel,
      }
    },
    saveToModel: function() {
      this.model.set(this.toJSON())
    },
    isValid: function() {
      return this.settingsSortField.currentView.collection.models.length !== 0
    },
    save: function() {
      if (!this.isValid()) {
        announcement.announce(InvalidSearchFormMessage)
        return
      }
      this.saveToModel()
      this.cancel()
      this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    },
    run: function() {
      if (!this.isValid()) {
        announcement.announce(InvalidSearchFormMessage)
        return
      }
      this.saveToModel()
      this.cancel()
      this.model.startSearch()
      store.setCurrentQuery(this.model)
      this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    },
  })
)
