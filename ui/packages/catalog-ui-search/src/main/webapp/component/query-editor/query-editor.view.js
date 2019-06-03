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
const template = require('./query-editor.hbs')
const CustomElements = require('../../js/CustomElements.js')
const QueryBasic = require('../query-basic/query-basic.view.js')
const QueryAdvanced = require('../query-advanced/query-advanced.view.js')
const QueryTitle = require('../query-title/query-title.view.js')
const QueryAdhoc = require('../query-adhoc/query-adhoc.view.js')
const cql = require('../../js/cql.js')
const CQLUtils = require('../../js/CQLUtils.js')
const store = require('../../js/store.js')
const announcement = require('../announcement/index.jsx')
import { InvalidSearchFormMessage } from 'component/announcement/CommonMessages'

function isNested(filter) {
  let nested = false
  filter.filters.forEach(subfilter => {
    nested = nested || subfilter.filters
  })
  return nested
}

function isTypeLimiter(filter) {
  let typesFound = {}
  filter.filters.forEach(subfilter => {
    typesFound[CQLUtils.getProperty(subfilter)] = true
  })
  typesFound = Object.keys(typesFound)
  return (
    typesFound.length === 2 &&
    typesFound.indexOf('metadata-content-type') >= 0 &&
    typesFound.indexOf('datatype') >= 0
  )
}

function isAnyDate(filter) {
  const propertiesToCheck = [
    'created',
    'modified',
    'effective',
    'metacard.created',
    'metacard.modified',
  ]
  const typesFound = {}
  const valuesFound = {}
  if (filter.filters.length === propertiesToCheck.length) {
    filter.filters.forEach(subfilter => {
      typesFound[subfilter.type] = true
      valuesFound[subfilter.value] = true
      const indexOfType = propertiesToCheck.indexOf(
        CQLUtils.getProperty(subfilter)
      )
      if (indexOfType >= 0) {
        propertiesToCheck.splice(indexOfType, 1)
      }
    })
    return (
      propertiesToCheck.length === 0 &&
      Object.keys(typesFound).length === 1 &&
      Object.keys(valuesFound).length === 1
    )
  }
  return false
}

function translateFilterToBasicMap(filter) {
  const propertyValueMap = {}
  let downConversion = false
  if (filter.filters) {
    filter.filters.forEach(filter => {
      if (!filter.filters) {
        propertyValueMap[CQLUtils.getProperty(filter)] =
          propertyValueMap[CQLUtils.getProperty(filter)] || []
        if (
          propertyValueMap[CQLUtils.getProperty(filter)].filter(
            existingFilter => existingFilter.type === filter.type
          ).length === 0
        ) {
          propertyValueMap[CQLUtils.getProperty(filter)].push(filter)
        }
      } else if (!isNested(filter) && isAnyDate(filter)) {
        propertyValueMap['anyDate'] = propertyValueMap['anyDate'] || []
        if (
          propertyValueMap['anyDate'].filter(
            existingFilter => existingFilter.type === filter.filters[0].type
          ).length === 0
        ) {
          propertyValueMap['anyDate'].push(filter.filters[0])
        }
      } else if (!isNested(filter) && isTypeLimiter(filter)) {
        propertyValueMap[CQLUtils.getProperty(filter.filters[0])] =
          propertyValueMap[CQLUtils.getProperty(filter.filters[0])] || []
        filter.filters.forEach(subfilter => {
          propertyValueMap[CQLUtils.getProperty(filter.filters[0])].push(
            subfilter
          )
        })
      } else {
        downConversion = true
      }
    })
  } else {
    propertyValueMap[CQLUtils.getProperty(filter)] =
      propertyValueMap[CQLUtils.getProperty(filter)] || []
    propertyValueMap[CQLUtils.getProperty(filter)].push(filter)
  }
  return {
    propertyValueMap,
    downConversion,
  }
}

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('query-editor'),
  regions: {
    queryContent: '> .editor-content > .content-form',
    queryTitle: '> .editor-content > .content-title',
  },
  originalType: '',
  events: {
    'click .editor-edit': 'edit',
    'click .editor-cancel': 'cancel',
    'click .editor-save': 'save',
    'click .editor-saveRun': 'saveRun',
  },
  initialize() {
    this.model = this.model._cloneOf
      ? store.getQueryById(this.model._cloneOf)
      : this.model
    this.listenTo(this.model, 'resetToDefaults change:type', this.reshow)
    this.listenTo(this.model, 'revert', this.revert)
    this.originalType = this.model.get('type')
  },
  revert() {
    if (this.model.get('type') !== this.originalType) {
      this.model.set('type', this.originalType)
    } else {
      this.reshow()
    }
  },
  reshow() {
    switch (this.model.get('type')) {
      case 'text':
        this.showText()
        break
      case 'basic':
        this.showBasic()
        break
      case 'advanced':
        this.showAdvanced()
        break
      case 'custom':
        this.showCustom()
        break
    }
    this.edit()
  },
  onBeforeShow() {
    this.reshow()
    this.showTitle()
  },
  showTitle() {
    this.queryTitle.show(
      new QueryTitle({
        model: this.model,
      })
    )
  },
  showText() {
    const translationToBasicMap = translateFilterToBasicMap(
      cql.simplify(cql.read(this.model.get('cql')))
    )
    this.queryContent.show(
      new QueryAdhoc({
        model: this.model,
        text: translationToBasicMap.propertyValueMap.anyText
          ? translationToBasicMap.propertyValueMap.anyText[0].value
          : '',
      })
    )
  },
  showBasic() {
    this.queryContent.show(
      new QueryBasic({
        model: this.model,
      })
    )
  },
  showCustom() {
    this.queryContent.show(
      new QueryAdvanced({
        model: this.model,
        isForm: true,
        isFormBuilder: false,
      })
    )
  },
  handleEditOnShow() {
    if (this.$el.hasClass('is-editing')) {
      this.edit()
    }
  },
  showAdvanced() {
    this.queryContent.show(
      new QueryAdvanced({
        model: this.model,
      })
    )
  },
  edit() {
    this.$el.addClass('is-editing')
    this.queryContent.currentView.edit()
  },
  cancel() {
    this.$el.removeClass('is-editing')
    this.onBeforeShow()
  },
  save() {
    if (!this.queryContent.currentView.isValid()) {
      announcement.announce(InvalidSearchFormMessage)
      return
    }
    this.queryContent.currentView.save()
    this.queryTitle.currentView.save()
    if (store.getCurrentQueries().get(this.model) === undefined) {
      store.getCurrentQueries().add(this.model)
    }
    this.cancel()
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    this.originalType = this.model.get('type')
  },
  saveRun() {
    if (!this.queryContent.currentView.isValid()) {
      announcement.announce(InvalidSearchFormMessage)
      return
    }
    this.queryContent.currentView.save()
    this.queryTitle.currentView.save()
    if (store.getCurrentQueries().get(this.model) === undefined) {
      store.getCurrentQueries().add(this.model)
    }
    this.cancel()
    this.model.startSearch()
    store.setCurrentQuery(this.model)
    this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    this.originalType = this.model.get('type')
  },
})
