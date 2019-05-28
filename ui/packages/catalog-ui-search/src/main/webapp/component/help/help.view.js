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

const zeroScale = 'matrix(0, 0, 0, 0, 0, 0)'
const zeroOpacity = '0'

// zeroScale to specifically to account for IE Edge Bug, see http://codepen.io/andrewkfiedler/pen/apBbxq
// zeroOpacity to account for how browsers work
function isEffectivelyHidden(element) {
  if (element === document) {
    return false
  } else {
    const computedStyle = window.getComputedStyle(element)
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
  let value = extractValue(element)
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
    (currentTop, proposedTop) => Math.max(currentTop, proposedTop),
    element => element.getBoundingClientRect().top
  )
}

function findHighestAncestorLeft(element) {
  return traverseAncestors(
    element,
    (currentLeft, proposedLeft) => Math.max(currentLeft, proposedLeft),
    element => element.getBoundingClientRect().left
  )
}

function findLowestAncestorBottom(element) {
  return traverseAncestors(
    element,
    (currentBottom, proposedBottom) => Math.min(currentBottom, proposedBottom),
    element => element.getBoundingClientRect().bottom
  )
}

function findLowestAncestorRight(element) {
  return traverseAncestors(
    element,
    (currentRight, proposedRight) => Math.min(currentRight, proposedRight),
    element => element.getBoundingClientRect().right
  )
}

function findBlockers() {
  const blockingElements = $(
    CustomElements.getNamespace() + 'dropdown-companion.is-open'
  )
    .add(CustomElements.getNamespace() + 'menu-vertical.is-open')
    .add('.is-blocker')
  return _.map(blockingElements, blockingElement => ({
    boundingRect: blockingElement.getBoundingClientRect(),
    element: blockingElement,
  }))
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
  return _.some(findBlockers(), blocker => {
    if (
      blocker.element !== element &&
      $(blocker.element).find(element).length === 0
    ) {
      const top = Math.max(blocker.boundingRect.top, boundingRect.top)
      const bottom = Math.min(blocker.boundingRect.bottom, boundingRect.bottom)
      const left = Math.max(blocker.boundingRect.left, boundingRect.left)
      const right = Math.min(blocker.boundingRect.right, boundingRect.right)
      const height = bottom - top
      const width = right - left
      if (height > 0 && width > 0) {
        return true
      }
    }
  })
}

module.exports = new (Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('help'),
  events: {
    mousedown: 'preventPropagation',
  },
  regions: {
    hints: '.help-hints',
  },
  initialize() {
    $('body').append(this.el)
  },
  onRender() {},
  hintOn: false,
  animationFrameId: undefined,
  toggleHints() {
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
  removeOldHints() {
    this.stopPaintingHints()
    this.el.innerHTML = ''
    $(CustomElements.getNamespace() + 'dropdown-companion.is-hint').remove()
  },
  stopPaintingHints() {
    window.cancelAnimationFrame(this.animationFrameId)
  },
  paintHint(element) {
    if (isEffectivelyHidden(element)) {
      return
    }
    const boundingRect = element.getBoundingClientRect()
    const top = Math.max(findHighestAncestorTop(element), boundingRect.top)
    const bottom = Math.min(
      findLowestAncestorBottom(element),
      boundingRect.bottom
    )
    const left = Math.max(findHighestAncestorLeft(element), boundingRect.left)
    const right = Math.min(findLowestAncestorRight(element), boundingRect.right)
    const height = bottom - top
    const width = right - left
    if (
      boundingRect.width > 0 &&
      height > 0 &&
      width > 0 &&
      !isBlocked(element, {
        top,
        bottom,
        left,
        right,
      })
    ) {
      const dropdownHintView = new DropdownHintView({
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
  paintHints($elementsWithHints) {
    this.animationFrameId = window.requestAnimationFrame(() => {
      const elements = $elementsWithHints.splice(0, 4)
      if (elements.length > 0) {
        elements.forEach(element => {
          this.paintHint(element)
        })
        this.paintHints($elementsWithHints)
      }
    })
  },
  showHints() {
    this.removeOldHints()
    this.hintOn = true
    this.$el.addClass('is-shown')
    let $elementsWithHints = $('[data-help]').not('.is-hidden [data-help]')
    $elementsWithHints = _.shuffle($elementsWithHints)
    this.addUntoggleElement()
    this.paintHints($elementsWithHints)
  },

  hideHints() {
    this.stopPaintingHints()
    this.hintOn = false
    this.$el.removeClass('is-shown')
  },
  addUntoggleElement() {
    const $untoggleElement = $('.navigation-item.item-help')
    _.forEach($untoggleElement, element => {
      const $untoggleElementClone = $(element).clone(true)
      this.$el.append($untoggleElementClone)
      const boundingRect = element.getBoundingClientRect()
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
    })
  },
  preventPropagation(e) {
    e.stopPropagation()
  },
  listenForResize() {
    $(window).on(
      'resize.' + this.cid,
      _.debounce(event => {
        this.showHints()
      }, 50)
    )
  },
  stopListeningForResize() {
    $(window).off('resize.' + this.cid)
  },
  listenForTyping() {
    $(window).on('keydown.' + this.cid, event => {
      let code = event.keyCode
      if (event.charCode && code == 0) code = event.charCode
      switch (code) {
        case 27:
          // Escape
          this.toggleHints()
          break
        default:
          break
      }
    })
  },
  stopListeningForTyping() {
    $(window).off('keydown.' + this.cid)
  },
  listenForClick() {
    this.$el.on('click.' + this.cid, () => {
      this.toggleHints()
    })
  },
  stopListeningForClick() {
    this.$el.off('click.' + this.cid)
  },
}))()
