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

/*
    This is only here until we fully deprecate dropdown.companion.view and dropdown.view.
    It will help ensure we keep some of the functionality between the two dropdown methods in sync.
*/
const $ = require('jquery')
const CustomElements = require('../js/CustomElements.js')
const store = require('../js/store.js')

module.exports = {
  drawing(event) {
    return (
      event.target.constructor === HTMLCanvasElement &&
      store.get('content').get('drawing')
    )
  },
  hasRightRoom(left, element) {
    return left + element.clientWidth < window.innerWidth
  },
  hasLeftRoom(left) {
    return left > 0
  },
  getBottomRoom(top, element) {
    return window.innerHeight - top
  },
  withinAnyDropdownCompanion(clickedElement) {
    return (
      $(`${CustomElements.getNamespace()}dropdown-companion`)
        .find(clickedElement)
        .addBack(clickedElement).length > 0
    )
  },
  withinAnyDropdownBehavior(clickedElement) {
    return (
      $('[data-behavior-dropdown]')
        .find(clickedElement)
        .addBack(clickedElement).length > 0
    )
  },
  withinAnyReactPortal(clickedElement) {
    return (
      $('react-portal')
        .find(clickedElement)
        .addBack(clickedElement).length > 0
    )
  },
  withinAnyDropdown(clickedElement) {
    return (
      this.withinAnyDropdownBehavior(clickedElement) ||
      this.withinAnyDropdownCompanion(clickedElement) ||
      this.withinAnyReactPortal(clickedElement)
    )
  },
  withinParentDropdownCompanion($dropdownEl, clickedElement) {
    return (
      $dropdownEl
        .prevAll(`${CustomElements.getNamespace()}dropdown-companion`)
        .find(clickedElement)
        .addBack(clickedElement).length > 0 ||
      $dropdownEl
        .closest('react-portal')
        .prevAll(`${CustomElements.getNamespace()}dropdown-companion`)
        .find(clickedElement)
        .addBack(clickedElement).length > 0
    )
  },
  withinParentDropdownBehavior($dropdownEl, clickedElement) {
    return (
      $dropdownEl
        .prevAll('[data-behavior-dropdown]')
        .find(clickedElement)
        .addBack(clickedElement).length > 0 ||
      $dropdownEl
        .closest('react-portal')
        .prevAll('[data-behavior-dropdown]')
        .find(clickedElement)
        .addBack(clickedElement).length > 0
    )
  },
  withinParentReactPortal($dropdownEl, clickedElement) {
    return (
      $dropdownEl
        .closest('react-portal')
        .prevAll('react-portal')
        .find(clickedElement)
        .addBack(clickedElement).length > 0 ||
      $dropdownEl
        .prevAll('react-portal')
        .find(clickedElement)
        .addBack(clickedElement).length > 0
    )
  },
  withinParentDropdown($dropdownEl, clickedElement) {
    return (
      this.withinParentDropdownBehavior($dropdownEl, clickedElement) ||
      this.withinParentDropdownCompanion($dropdownEl, clickedElement) ||
      this.withinParentReactPortal($dropdownEl, clickedElement)
    )
  },
  withinDOM(clickedElement) {
    return (
      $('body')
        .find(clickedElement)
        .addBack(clickedElement).length > 0
    )
  },
  updatePosition($dropdownEl, sourceEl) {
    const clientRect = sourceEl.getBoundingClientRect()
    const menuWidth = $dropdownEl[0].clientWidth
    const necessaryLeft = Math.floor(
      clientRect.left + clientRect.width / 2 - menuWidth / 2
    )
    const necessaryTop = Math.floor(clientRect.top + clientRect.height)
    const bottomRoom = this.getBottomRoom(necessaryTop, $dropdownEl[0])
    const topRoom = clientRect.top
    if (!this.hasRightRoom(necessaryLeft, $dropdownEl[0])) {
      $dropdownEl.css('left', window.innerWidth - menuWidth - 20)
    }
    if (!this.hasLeftRoom(necessaryLeft)) {
      $dropdownEl.css('left', 10)
    }
    if (bottomRoom > topRoom) {
      $dropdownEl.addClass('is-bottom').removeClass('is-top')
      $dropdownEl.css('left', necessaryLeft).css('top', necessaryTop)
      $dropdownEl.css('max-height', bottomRoom - 10)
      return bottomRoom
    } else {
      $dropdownEl.addClass('is-top').removeClass('is-bottom')
      $dropdownEl.css('left', necessaryLeft).css('top', topRoom)
      $dropdownEl.css('max-height', topRoom - 10)
      return topRoom
    }
  },
}
