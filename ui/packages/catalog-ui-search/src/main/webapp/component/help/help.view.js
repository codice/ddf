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
const template = require('./help.hbs')
const CustomElements = require('../../js/CustomElements.js')
const Dropdown = require('../dropdown/dropdown.js')
const DropdownHintView = require('../dropdown/hint/dropdown.hint.view.js')
const Hint = require('../hint/hint.js')

var zeroScale = 'matrix(0, 0, 0, 0, 0, 0)'
var zeroOpacity = '0'

// zeroScale to specifically to account for IE Edge Bug, see http://codepen.io/andrewkfiedler/pen/apBbxq
// zeroOpacity to account for how browsers work
function isEffectivelyHidden(element) {
  if (element === document) {
    return false
  } else {
    var computedStyle = window.getComputedStyle(element)
    if (
      computedStyle.transform === zeroScale ||
      computedStyle.opacity === zeroOpacity
    ) {
      return true
    } else {
      return isEffectivelyHidden(element.parentNode)
    }
  }
}

// it'd be nice if we can use offsetParent directly, but that would require devs to be aware of how help.view works
function isOffsetParent(element) {
  return window.getComputedStyle(element).overflow !== 'visible'
}

function traverseAncestors(element, compareValue, extractValue) {
  var value = extractValue(element)
  element = element.parentNode
  while (element !== null && element !== document) {
    if (isOffsetParent(element)) {
      value = compareValue(value, extractValue(element))
    }
    element = element.parentNode
  }
  return value
}

function findHighestAncestorTop(element) {
  return traverseAncestors(
    element,
    function(currentTop, proposedTop) {
      return Math.max(currentTop, proposedTop)
    },
    function(element) {
      return element.getBoundingClientRect().top
    }
  )
}

function findHighestAncestorLeft(element) {
  return traverseAncestors(
    element,
    function(currentLeft, proposedLeft) {
      return Math.max(currentLeft, proposedLeft)
    },
    function(element) {
      return element.getBoundingClientRect().left
    }
  )
}

function findLowestAncestorBottom(element) {
  return traverseAncestors(
    element,
    function(currentBottom, proposedBottom) {
      return Math.min(currentBottom, proposedBottom)
    },
    function(element) {
      return element.getBoundingClientRect().bottom
    }
  )
}

function findLowestAncestorRight(element) {
  return traverseAncestors(
    element,
    function(currentRight, proposedRight) {
      return Math.min(currentRight, proposedRight)
    },
    function(element) {
      return element.getBoundingClientRect().right
    }
  )
}

function findBlockers() {
  var blockingElements = $(
    CustomElements.getNamespace() + 'dropdown-companion.is-open'
  )
    .add(CustomElements.getNamespace() + 'menu-vertical.is-open')
    .add('.is-blocker')
  return _.map(blockingElements, function(blockingElement) {
    return {
      boundingRect: blockingElement.getBoundingClientRect(),
      element: blockingElement,
    }
  })
}

function hasNotScrolledPastVertically(element, boundingRect) {
  return boundingRect.top + 1 >= findHighestAncestorTop(element)
}

function hasScrolledToVertically(element, boundingRect) {
  return boundingRect.bottom - 1 <= findLowestAncestorBottom(element)
}

function hasNotScrolledPastHorizontally(element, boundingRect) {
  return boundingRect.left + 1 >= findHighestAncestorLeft(element)
}

function hasScrolledToHorizontally(element, boundingRect) {
  return boundingRect.right - 1 <= findLowestAncestorRight(element)
}

function withinScrollViewport(element, boundingRect) {
  return (
    hasNotScrolledPastVertically(element, boundingRect) &&
    hasScrolledToVertically(element, boundingRect) &&
    hasNotScrolledPastHorizontally(element, boundingRect) &&
    hasScrolledToHorizontally(element, boundingRect)
  )
}

function isBlocked(element, boundingRect) {
  return _.some(findBlockers(), function(blocker) {
    if (
      blocker.element !== element &&
      $(blocker.element).find(element).length === 0
    ) {
      var top = Math.max(blocker.boundingRect.top, boundingRect.top)
      var bottom = Math.min(blocker.boundingRect.bottom, boundingRect.bottom)
      var left = Math.max(blocker.boundingRect.left, boundingRect.left)
      var right = Math.min(blocker.boundingRect.right, boundingRect.right)
      var height = bottom - top
      var width = right - left
      if (height > 0 && width > 0) {
        return true
      }
    }
  })
}

module.exports = new (Marionette.LayoutView.extend({
  template: template,
  tagName: CustomElements.register('help'),
  events: {
    mousedown: 'preventPropagation',
  },
  regions: {
    hints: '.help-hints',
  },
  initialize: function() {
    $('body').append(this.el)
  },
  onRender: function() {},
  hintOn: false,
  animationFrameId: undefined,
  toggleHints: function() {
    if (this.hintOn) {
      this.hideHints()
      this.stopListeningForResize()
      this.stopListeningForTyping()
      this.stopListeningForClick()
    } else {
      this.showHints()
      this.listenForResize()
      this.listenForTyping()
      this.listenForClick()
    }
  },
  removeOldHints: function() {
    this.stopPaintingHints()
    this.el.innerHTML = ''
    $(CustomElements.getNamespace() + 'dropdown-companion.is-hint').remove()
  },
  stopPaintingHints: function() {
    window.cancelAnimationFrame(this.animationFrameId)
  },
  paintHint: function(element) {
    if (isEffectivelyHidden(element)) {
      return
    }
    var boundingRect = element.getBoundingClientRect()
    var top = Math.max(findHighestAncestorTop(element), boundingRect.top)
    var bottom = Math.min(
      findLowestAncestorBottom(element),
      boundingRect.bottom
    )
    var left = Math.max(findHighestAncestorLeft(element), boundingRect.left)
    var right = Math.min(findLowestAncestorRight(element), boundingRect.right)
    var height = bottom - top
    var width = right - left
    if (
      boundingRect.width > 0 &&
      height > 0 &&
      width > 0 &&
      !isBlocked(element, {
        top: top,
        bottom: bottom,
        left: left,
        right: right,
      })
    ) {
      var dropdownHintView = new DropdownHintView({
        model: new Dropdown(),
        modelForComponent: new Hint({
          hint: element.getAttribute('data-help'),
        }),
      })
      dropdownHintView.render()
      this.$el.append(dropdownHintView.$el)
      dropdownHintView.$el
        .css('height', height)
        .css('width', width)
        .css('top', top)
        .css('left', left)
    }
  },
  paintHints: function($elementsWithHints) {
    this.animationFrameId = window.requestAnimationFrame(
      function() {
        var elements = $elementsWithHints.splice(0, 4)
        if (elements.length > 0) {
          elements.forEach(
            function(element) {
              this.paintHint(element)
            }.bind(this)
          )
          this.paintHints($elementsWithHints)
        }
      }.bind(this)
    )
  },
  showHints: function() {
    this.removeOldHints()
    this.hintOn = true
    this.$el.addClass('is-shown')
    var $elementsWithHints = $('[data-help]').not('.is-hidden [data-help]')
    $elementsWithHints = _.shuffle($elementsWithHints)
    this.addUntoggleElement()
    this.paintHints($elementsWithHints)
  },

  hideHints: function() {
    this.stopPaintingHints()
    this.hintOn = false
    this.$el.removeClass('is-shown')
  },
  addUntoggleElement: function() {
    var $untoggleElement = $('.navigation-item.item-help')
    _.forEach(
      $untoggleElement,
      function(element) {
        var $untoggleElementClone = $(element).clone(true)
        this.$el.append($untoggleElementClone)
        var boundingRect = element.getBoundingClientRect()
        $untoggleElementClone
          .css('height', boundingRect.height)
          .css('width', boundingRect.width)
          .css('top', boundingRect.top)
          .css('left', boundingRect.left)
          .css('position', 'absolute')
          .css('text-align', 'center')
          .css('font-size', '1.4rem')
          .css('overflow', 'hidden')
          .on('click', this.toggleHints.bind(this))
      }.bind(this)
    )
  },
  preventPropagation: function(e) {
    e.stopPropagation()
  },
  listenForResize: function() {
    $(window).on(
      'resize.' + this.cid,
      _.debounce(
        function(event) {
          this.showHints()
        }.bind(this),
        50
      )
    )
  },
  stopListeningForResize: function() {
    $(window).off('resize.' + this.cid)
  },
  listenForTyping: function() {
    $(window).on(
      'keydown.' + this.cid,
      function(event) {
        var code = event.keyCode
        if (event.charCode && code == 0) code = event.charCode
        switch (code) {
          case 27:
            // Escape
            this.toggleHints()
            break
          default:
            break
        }
      }.bind(this)
    )
  },
  stopListeningForTyping: function() {
    $(window).off('keydown.' + this.cid)
  },
  listenForClick: function() {
    this.$el.on(
      'click.' + this.cid,
      function() {
        this.toggleHints()
      }.bind(this)
    )
  },
  stopListeningForClick: function() {
    this.$el.off('click.' + this.cid)
  },
}))()
