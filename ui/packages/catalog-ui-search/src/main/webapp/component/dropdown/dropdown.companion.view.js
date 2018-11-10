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
/*global define, window, setTimeout*/
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const CustomElements = require('../../js/CustomElements.js')
const template = require('./dropdown.companion.hbs')
const Common = require('../../js/Common.js')
const store = require('../../js/store.js')
const DropdownBehaviorUtility = require('../../behaviors/dropdown.behavior.utility.js')
const router = require('../router/router.js')
require('../../behaviors/navigation.behavior.js')

module.exports = Marionette.LayoutView.extend(
  {
    template: template,
    tagName: CustomElements.register('dropdown-companion'),
    regions: {
      componentToShow: '.dropdown-companion-component',
    },
    events: {
      keydown: 'handleSpecialKeys',
      'keyup .dropdown-companion-filter': 'handleFilterUpdate',
      'click > button': 'triggerToggleAll',
    },
    attributes: {
      tabindex: 0,
    },
    behaviors: function() {
      return this.options.linkedView.options.dropdownCompanionBehaviors
    },
    initialize: function() {
      this.listenTo(
        this.options.linkedView.model,
        'change:isOpen',
        this.handleOpenChange
      )
      this.listenForRoute()
      this.listenForClose()
    },
    hasFiltering: function() {
      return Boolean(
        this.options.linkedView.hasFiltering ||
          this.options.linkedView.options.hasFiltering
      )
    },
    isMultiSelect: function() {
      return Boolean(
        this.options.linkedView.isMultiSelect ||
          this.options.linkedView.options.isMultiSelect
      )
    },
    updateWidth: function() {
      var clientRect = this.options.linkedView
        .getCenteringElement()
        .getBoundingClientRect()
      if (this.options.linkedView.hasLimitedWidth) {
        this.$el.css(
          'width',
          Math.min(clientRect.width, window.innerWidth - 20)
        )
      } else {
        this.$el.css(
          'min-width',
          Math.min(clientRect.width, window.innerWidth - 20)
        )
      }
    },
    updateFilterMaxHeight: function(bottomRoom) {
      var extraRoom = '0rem'
      if (this.isMultiSelect()) {
        extraRoom = '2.75rem'
      }
      if (this.hasFiltering()) {
        this.componentToShow.$el.css(
          'max-height',
          'calc(' +
            bottomRoom +
            'px - 1.875rem - 2.75rem - 1.25rem - ' +
            extraRoom +
            ')'
        )
      }
    },
    updatePosition: function() {
      const bottomRoom = DropdownBehaviorUtility.updatePosition(
        this.$el,
        this.options.linkedView.getCenteringElement()
      )
      this.updateFilterMaxHeight(bottomRoom)
    },
    handleTail: function() {
      this.$el.toggleClass('has-tail', this.options.linkedView.hasTail)
    },
    handleOpenChange: function() {
      var isOpen = this.options.linkedView.model.get('isOpen')
      if (isOpen) {
        this.onOpen()
      } else {
        this.onClose()
      }
    },
    onOpen: function() {
      if (!this.el.parentElement) {
        $('body').append(this.el)
        this.render()
        this.handleTail()
        var componentToShow =
          this.options.linkedView.componentToShow ||
          this.options.linkedView.options.componentToShow
        this.componentToShow.show(
          new componentToShow(
            _.extend(
              this.options.linkedView.options,
              this.options.linkedView.options.options,
              {
                model: this.options.linkedView.modelForComponent,
              }
            )
          )
        )
        this.listenForReposition()
      }
      this.updateWidth()
      this.updatePosition()
      this.$el.addClass('is-open')
      this.listenForOutsideClick()
      this.listenForResize()
      this.focusOnFilter()
      this.handleFiltering()
      this.handleToggleAll()
    },
    focusOnFilter: function() {
      if (this.hasFiltering()) {
        Common.queueExecution(() => {
          this.$el.children('input').focus()
        })
      } else {
        if (this.componentToShow.currentView.focus) {
          Common.queueExecution(() => {
            this.componentToShow.currentView.focus()
          })
        } else {
          this.$el.focus()
        }
      }
    },
    handleFiltering: function() {
      this.$el.toggleClass('has-filtering', this.hasFiltering())
    },
    handleToggleAll: function() {
      this.$el.toggleClass('is-multiselect', this.isMultiSelect())
    },
    triggerToggleAll: function(event) {
      this.componentToShow.currentView.handleToggleAll()
    },
    handleFilterUpdate: function(event) {
      if (this.isDestroyed) {
        return
      }
      var code = event.keyCode
      if (event.charCode && code == 0) code = event.charCode
      switch (code) {
        case 13:
        // Enter
        case 27:
        // Escape
        case 37:
        // Key left.
        case 39:
        // Key right.
        case 38:
        // Key up.
        case 40:
          // Key down
          break
        default:
          var filterValue = this.$el.children('input').val()
          this.options.linkedView.model.set('filterValue', filterValue)
          this.updateWidth()
          this.updatePosition()
          break
      }
    },
    handleSpecialKeys: function(event) {
      if (this.isDestroyed) {
        return
      }
      var code = event.keyCode
      if (event.charCode && code == 0) code = event.charCode
      switch (code) {
        case 13:
          // Enter
          if (this.componentToShow.currentView.handleEnter) {
            this.componentToShow.currentView.handleEnter()
          }
          break
        case 27:
          // Escape
          event.preventDefault()
          event.stopPropagation()
          this.handleEscape()
          break
        case 38:
          // Key up.
          if (this.componentToShow.currentView.handleUpArrow) {
            event.preventDefault()
            this.componentToShow.currentView.handleUpArrow()
          }
          break
        case 40:
          // Key down.
          if (this.componentToShow.currentView.handleDownArrow) {
            event.preventDefault()
            this.componentToShow.currentView.handleDownArrow()
          }
          break
        default:
          //anything else
          var hasFiltering = Boolean(
            this.options.linkedView.hasFiltering ||
              this.options.linkedView.options.hasFiltering
          )
          if (hasFiltering) {
            Common.queueExecution(() => {
              this.$el.children('input').focus()
            })
          }
      }
    },
    onClose: function() {
      if (this.el.parentElement) {
        this.$el.removeClass('is-open')
      }
      this.stopListeningForOutsideClick()
      this.stopListeningForResize()
    },
    close: function() {
      this.options.linkedView.model.close()
    },
    handleEscape: function() {
      this.close()
      this.options.linkedView.$el.focus()
    },
    listenForReposition: function() {
      this.$el.on(
        'repositionDropdown.' + CustomElements.getNamespace(),
        function(e) {
          this.updateWidth()
          this.updatePosition()
        }.bind(this)
      )
    },
    stopListeningForReposition: function() {
      this.$el.off('repositionDropdown.' + CustomElements.getNamespace())
    },
    listenForClose: function() {
      this.$el.on(
        'closeDropdown.' + CustomElements.getNamespace(),
        function(e) {
          // stop from closing dropdowns higher in the dom
          e.stopPropagation()
          // close
          this.close()
          this.options.linkedView.$el.focus()
        }.bind(this)
      )
    },
    stopListeningForClose: function() {
      this.$el.off('closeDropdown.' + CustomElements.getNamespace())
    },
    listenForOutsideClick: function() {
      $('body').on(
        'mousedown.' + this.cid,
        function(event) {
          if (!DropdownBehaviorUtility.drawing(event)) {
            if (!DropdownBehaviorUtility.withinAnyDropdown(event.target)) {
              this.close()
            }
            if (
              DropdownBehaviorUtility.withinParentDropdown(
                this.$el,
                event.target
              )
            ) {
              this.close()
            }
          }
        }.bind(this)
      )
    },
    stopListeningForOutsideClick: function() {
      $('body').off('mousedown.' + this.cid)
    },
    listenForRoute: function() {
      this.listenTo(router, 'change', this.handleRouteChange)
    },
    handleRouteChange: function() {
      this.close()
    },
    listenForResize: function() {
      $(window).on(
        'resize.' + this.cid,
        _.throttle(
          function(event) {
            this.updatePosition()
            this.updateWidth()
          }.bind(this),
          16
        )
      )
    },
    stopListeningForResize: function() {
      $(window).off('resize.' + this.cid)
    },
    onDestroy: function() {
      this.stopListeningForClose()
      this.stopListeningForOutsideClick()
      this.stopListeningForResize()
      this.stopListeningForReposition()
    },
  },
  {
    getNewCompanionView: function(linkedView) {
      return new this({
        linkedView: linkedView,
      })
    },
  }
)
