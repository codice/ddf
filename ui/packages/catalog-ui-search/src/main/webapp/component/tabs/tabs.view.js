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
const _ = require('underscore')
const $ = require('jquery')
const TabsTemplate = require('./tabs.hbs')
const CustomElements = require('../../js/CustomElements.js')
const wreqr = require('../../js/wreqr.js')

function namespacedEvent(event, view) {
  return event + '.' + view.cid
}

/** This is an abstract view.  It should not be used directly.  It should be extended,
 *  and determineContent should be overwritten.
 */

const TabsView = Marionette.LayoutView.extend({
  template: TabsTemplate,
  tagName: CustomElements.register('tabs'),
  modelEvents: {
    'change:activeTab': 'handleTabChange',
  },
  events: {
    'click > .tabs-list .tabs-tab': 'changeTab',
    'click > .tabs-list .tabs-collapsed button': 'changeTab',
  },
  regions: {
    tabsContent: '.tabs-content',
  },
  initialize() {
    const view = this
    this._resizeHandler = _.throttle(this._resizeHandler, 200)
    $(window).on(namespacedEvent('resize', view), () => {
      view._resizeHandler()
    })
    this.listenTo(wreqr.vent, 'resize', this._resizeHandler)
  },
  onRender() {
    this.showTab(true)
    this.determineContent()
    this._clickHandler()
  },
  onAttach() {
    this._resizeHandler()
  },
  onBeforeDestroy() {
    $(window).off(namespacedEvent('resize', this))
  },
  handleTabChange() {
    this.showTab(true)
    this.determineContent()
  },
  showTab(shouldResize) {
    const currentTab = this.model.getActiveTab()
    this.$el.find('.is-active').removeClass('is-active')
    this.$el.find('[data-id="' + currentTab + '"]').addClass('is-active')
    this.showActiveDropdownTab()
    if (shouldResize) {
      this._resizeHandler()
    }
  },
  serializeData() {
    return _.extend(this.model.toJSON(), {
      tabTitles: Object.keys(this.model.get('tabs')),
    })
  },
  determineContent() {
    const activeTab = this.model.getActiveView()
    const activeTabOptions = this.model.getActiveViewOptions()
    if (activeTabOptions !== undefined) {
      this.tabsContent.show(new activeTab(activeTabOptions))
    } else {
      this.tabsContent.show(new activeTab())
    }
  },
  showActiveDropdownTab() {
    const hasActiveTab =
      this.$el.find(
        '> .tabs-list .tabs-dropdown .tabs-title.is-active.is-merged'
      ).length !== 0
    if (hasActiveTab) {
      this.$el.find('> .tabs-list .tabs-dropdown').addClass('has-activeTab')
    } else {
      this.$el.find('> .tabs-list .tabs-dropdown').removeClass('has-activeTab')
    }
  },
  changeTab(event) {
    const tab = event.currentTarget.getAttribute('data-id')
    this.model.setActiveTab(tab)
  },
  _clickHandler() {
    const view = this
    const tabList = view.$el.find('> .tabs-list')
    const menu = tabList.find('.tabs-dropdown')
    menu.off('click').on('click', () => {
      tabList.toggleClass('is-open')
      if (tabList.hasClass('is-open')) {
        $('body').on(namespacedEvent('click', view), e => {
          if (e.target !== menu[0] && menu.find(e.target).length === 0) {
            $('body').off(namespacedEvent('click', view))
            tabList.removeClass('is-open')
          }
        })
      } else {
        $('body').off(namespacedEvent('click', view))
      }
    })
  },
  _widthWhenCollapsed() {
    const widthWhenCollaspsed = []
    this._widthWhenCollapsed = function() {
      return widthWhenCollaspsed
    }
    return widthWhenCollaspsed
  },
  _resizeHandler() {
    const view = this
    const menu = view.$el.find('> .tabs-list')[0]
    if (!menu) {
      return
    }
    const expandedList = menu.querySelector('.tabs-expanded')
    if (
      view._hasMergeableTabs() &&
      expandedList.scrollWidth > expandedList.clientWidth
    ) {
      view._widthWhenCollapsed().push(expandedList.scrollWidth)
      view._mergeTab()
      view._resizeHandler()
    } else {
      if (
        view._widthWhenCollapsed().length !== 0 &&
        expandedList.clientWidth >
          view._widthWhenCollapsed()[view._widthWhenCollapsed().length - 1]
      ) {
        view._widthWhenCollapsed().pop()
        view._unmergeTab()
        view._resizeHandler()
      }
    }
    if (menu.querySelectorAll('.is-merged').length > 0) {
      const alreadyCollapsed = menu.classList.contains('is-collapsed')
      if (!alreadyCollapsed) {
        menu.classList.add('is-collapsed')
        view._resizeHandler()
      }
    } else {
      menu.classList.remove('is-collapsed')
    }
    view.showTab(false)
  },
  _hasMergeableTabs() {
    return (
      this.$el.find('> .tabs-list .tabs-expanded > .tabs-tab:not(.is-merged)')
        .length !== 0
    )
  },
  _mergeTab() {
    const id = this.$el
      .find('> .tabs-list .tabs-expanded > .tabs-tab:not(.is-merged)')
      .last()
      .attr('data-id')
    this.$el.find('> .tabs-list [data-id="' + id + '"]').addClass('is-merged')
  },
  _unmergeTab() {
    const id = this.$el
      .find('> .tabs-list .tabs-expanded > .tabs-tab.is-merged')
      .first()
      .attr('data-id')
    this.$el
      .find('> .tabs-list [data-id="' + id + '"]')
      .removeClass('is-merged')
  },
})

module.exports = TabsView
