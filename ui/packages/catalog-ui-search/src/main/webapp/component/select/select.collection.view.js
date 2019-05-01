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

const Backbone = require('backbone')
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const childView = require('./select.view')
const CustomElements = require('../../js/CustomElements.js')
const Common = require('../../js/Common.js')
import { matchesFilter, getAppropriateString } from './filterHelper'

module.exports = Marionette.CollectionView.extend({
  emptyView: Marionette.ItemView.extend({
    className: 'select-collection-empty',
    template: 'Nothing Found',
  }),
  getChildView: function() {
    return this.options.customChildView || childView
  },
  tagName: CustomElements.register('select-collection'),
  className: 'is-action-list',
  modelEvents: {},
  events: {
    'click .choice': 'handleChoice',
    'mouseenter .choice': 'handleMouseEnter',
  },
  initialize: function() {
    this.collection = new Backbone.Collection(this.options.list)
    if (this.collection.first().get('filterChoice') === true) {
      this.collection
        .first()
        .listenTo(
          this.model,
          'change:filterValue',
          this.updateFilterChoice.bind(this)
        )
    }
    this.listenTo(this.model, 'change:filterValue', this.handleFilterUpdate)
  },
  updateFilterChoice: function() {
    var filterValue = this.model.get('filterValue')
    this.collection.first().set({
      label: filterValue !== '' ? filterValue : this.model.get('value')[0],
      value: filterValue !== '' ? filterValue : this.model.get('value')[0],
    })
  },
  onAddChild: function(childView) {
    if (!childView.isDestroyed && childView.model) {
      childView.$el.addClass('choice')
      childView.$el.attr(
        'data-value',
        JSON.stringify(childView.model.get('value'))
      )
      if (childView._index === 0) {
        childView.$el.addClass('is-active')
      }
      this.handleValueForChildView(childView)
    }
  },
  onRender: function() {
    this.handleActive()
  },
  handleValueForChildView: function(childView) {
    var values = this.model.get('value')
    values.forEach(function(value) {
      if (childView.$el.attr('data-value') === JSON.stringify(value)) {
        childView.$el.addClass('is-selected')
      }
    })
  },
  handleActive: function() {
    this.$el
      .children('.choice')
      .first()
      .addClass('is-active')
  },
  handleMouseEnter: function(e) {
    this.$el.children('.is-active').removeClass('is-active')
    $(e.currentTarget).toggleClass('is-active')
  },
  handleChoice: function(e) {
    if (!this.options.isMultiSelect) {
      this.$el.children('.is-selected').removeClass('is-selected')
    }
    $(e.currentTarget).toggleClass('is-selected')
    this.updateValue(e.currentTarget)
    if (!this.options.isMultiSelect) {
      this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
    }
  },
  filterValue: '',
  handleToggleAll: function() {
    var availableChoices = this.$el.children()
    var selectAll = availableChoices.filter(':not(.is-selected)').length > 0
    var values = availableChoices
      .map(function(index, element) {
        return JSON.parse(element.getAttribute('data-value'))
      })
      .get()
    if (selectAll) {
      availableChoices.addClass('is-selected')
      this.addValues(values)
    } else {
      availableChoices.removeClass('is-selected')
      this.removeValues(values)
    }
  },
  handleFilterUpdate: function() {
    this.render()
  },
  handleEnter: function(e) {
    if (!this.options.isMultiSelect) {
      this.$el.children('.is-selected').removeClass('is-selected')
    }
    var activeChoice = this.$el.children('.choice.is-active')
    if (activeChoice.length > 0) {
      var el = activeChoice.toggleClass('is-selected')[0]
      this.updateValue(el)
      if (!this.options.isMultiSelect) {
        this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
      }
    }
  },
  handleDownArrow: function() {
    var $currentActive = this.$el.children('.choice.is-active')
    var $nextActive = $currentActive.next()
    if ($nextActive.length !== 0) {
      $currentActive.removeClass('is-active')
      $nextActive.addClass('is-active')
      var diff =
        $nextActive[0].getBoundingClientRect().top +
        $nextActive[0].getBoundingClientRect().height -
        ($nextActive[0].parentNode.parentNode.parentNode.clientHeight +
          $nextActive[0].parentNode.parentNode.parentNode.getBoundingClientRect()
            .top)
      if (diff >= 0) {
        $nextActive[0].parentNode.parentNode.parentNode.scrollTop =
          $nextActive[0].parentNode.parentNode.parentNode.scrollTop + diff
      }
    }
  },
  handleUpArrow: function() {
    var $currentActive = this.$el.children('.choice.is-active')
    var $nextActive = $currentActive.prev()
    if ($nextActive.length !== 0) {
      $currentActive.removeClass('is-active')
      $nextActive.addClass('is-active')
      var diff =
        $nextActive[0].parentNode.parentNode.parentNode.getBoundingClientRect()
          .top - $nextActive[0].getBoundingClientRect().top
      if (diff >= 0) {
        $nextActive[0].parentNode.parentNode.parentNode.scrollTop =
          $nextActive[0].parentNode.parentNode.parentNode.scrollTop - diff
      }
    }
  },
  filter: function(child) {
    var filterValue = this.model.get('filterValue')
    filterValue = filterValue !== undefined ? filterValue : ''
    if (
      child.get('filterChoice') === true &&
      this.collection.filter(model => {
        return (
          getAppropriateString(model.get('value')) ===
          getAppropriateString(child.get('value'))
        )
      }).length > 1
    ) {
      return false
    }
    if (child.get('label') !== undefined) {
      return matchesFilter(
        filterValue,
        child.get('label'),
        this.options.matchcase
      )
    }
    if (child.get('value') !== undefined) {
      return matchesFilter(
        filterValue,
        child.get('value'),
        this.options.matchcase
      )
    }
    return true
  },
  addValues: function(values) {
    var currentValues = this.model.get('value').slice()
    values.forEach(function(value) {
      var index = currentValues.indexOf(value)
      if (index === -1) {
        currentValues.push(value)
      }
    })
    this.model.set({
      value: currentValues,
    })
  },
  removeValues: function(values) {
    var currentValues = this.model.get('value').slice()
    values.forEach(function(value) {
      var index = currentValues.indexOf(value)
      if (index >= 0) {
        currentValues.splice(index, 1)
      }
    })
    this.model.set({
      value: currentValues,
    })
  },
  updateValue: function(target) {
    var value = JSON.parse($(target).attr('data-value'))
    var values = this.model.get('value').slice()
    if (this.options.isMultiSelect) {
      var index = values.indexOf(value)
      if (index >= 0) {
        values.splice(index, 1)
      } else {
        values.push(value)
      }
    } else {
      values = [value]
    }
    this.model.set({
      value: values,
    })
  },
})
