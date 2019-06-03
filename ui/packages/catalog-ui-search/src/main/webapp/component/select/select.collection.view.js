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
const $ = require('jquery')
const childView = require('./select.view')
const CustomElements = require('../../js/CustomElements.js')
import { matchesFilter, getAppropriateString } from './filterHelper'

module.exports = Marionette.CollectionView.extend({
  emptyView: Marionette.ItemView.extend({
    className: 'select-collection-empty',
    template: 'Nothing Found',
  }),
  getChildView() {
    return this.options.customChildView || childView
  },
  tagName: CustomElements.register('select-collection'),
  className: 'is-action-list',
  modelEvents: {},
  events: {
    'click .choice': 'handleChoice',
    'mouseenter .choice': 'handleMouseEnter',
  },
  initialize() {
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
  updateFilterChoice() {
    const filterValue = this.model.get('filterValue')
    this.collection.first().set({
      label: filterValue !== '' ? filterValue : this.model.get('value')[0],
      value: filterValue !== '' ? filterValue : this.model.get('value')[0],
    })
  },
  onAddChild(childView) {
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
  onRender() {
    this.handleActive()
  },
  handleValueForChildView(childView) {
    const values = this.model.get('value')
    values.forEach(value => {
      if (childView.$el.attr('data-value') === JSON.stringify(value)) {
        childView.$el.addClass('is-selected')
      }
    })
  },
  handleActive() {
    this.$el
      .children('.choice')
      .first()
      .addClass('is-active')
  },
  handleMouseEnter(e) {
    this.$el.children('.is-active').removeClass('is-active')
    $(e.currentTarget).toggleClass('is-active')
  },
  handleChoice(e) {
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
  handleToggleAll() {
    const availableChoices = this.$el.children()
    const selectAll = availableChoices.filter(':not(.is-selected)').length > 0
    const values = availableChoices
      .map((index, element) => JSON.parse(element.getAttribute('data-value')))
      .get()
    if (selectAll) {
      availableChoices.addClass('is-selected')
      this.addValues(values)
    } else {
      availableChoices.removeClass('is-selected')
      this.removeValues(values)
    }
  },
  handleFilterUpdate() {
    this.render()
  },
  handleEnter(e) {
    if (!this.options.isMultiSelect) {
      this.$el.children('.is-selected').removeClass('is-selected')
    }
    const activeChoice = this.$el.children('.choice.is-active')
    if (activeChoice.length > 0) {
      const el = activeChoice.toggleClass('is-selected')[0]
      this.updateValue(el)
      if (!this.options.isMultiSelect) {
        this.$el.trigger('closeDropdown.' + CustomElements.getNamespace())
      }
    }
  },
  handleDownArrow() {
    const $currentActive = this.$el.children('.choice.is-active')
    const $nextActive = $currentActive.next()
    if ($nextActive.length !== 0) {
      $currentActive.removeClass('is-active')
      $nextActive.addClass('is-active')
      const diff =
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
  handleUpArrow() {
    const $currentActive = this.$el.children('.choice.is-active')
    const $nextActive = $currentActive.prev()
    if ($nextActive.length !== 0) {
      $currentActive.removeClass('is-active')
      $nextActive.addClass('is-active')
      const diff =
        $nextActive[0].parentNode.parentNode.parentNode.getBoundingClientRect()
          .top - $nextActive[0].getBoundingClientRect().top
      if (diff >= 0) {
        $nextActive[0].parentNode.parentNode.parentNode.scrollTop =
          $nextActive[0].parentNode.parentNode.parentNode.scrollTop - diff
      }
    }
  },
  filter(child) {
    let filterValue = this.model.get('filterValue')
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
  addValues(values) {
    const currentValues = this.model.get('value').slice()
    values.forEach(value => {
      const index = currentValues.indexOf(value)
      if (index === -1) {
        currentValues.push(value)
      }
    })
    this.model.set({
      value: currentValues,
    })
  },
  removeValues(values) {
    const currentValues = this.model.get('value').slice()
    values.forEach(value => {
      const index = currentValues.indexOf(value)
      if (index >= 0) {
        currentValues.splice(index, 1)
      }
    })
    this.model.set({
      value: currentValues,
    })
  },
  updateValue(target) {
    const value = JSON.parse($(target).attr('data-value'))
    let values = this.model.get('value').slice()
    if (this.options.isMultiSelect) {
      const index = values.indexOf(value)
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
