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

const template = require('./table.hbs')
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')

function moveHeaders(elementToUpdate, elementToMatch) {
  this.$el
    .find('th')
    .css('transform', 'translate3d(0, ' + this.el.scrollTop + 'px, 0)')
}

module.exports = Marionette.LayoutView.extend({
  tagName: CustomElements.register('table'),
  template,
  regions: {
    bodyThead: {
      selector: 'thead',
      replaceElement: true,
    },
    bodyTbody: {
      selector: 'tbody',
      replaceElement: true,
    },
  },
  headerAnimationFrameId: undefined,
  getHeaderView() {
    console.log(
      'You need to overwrite this function and provide the constructed HeaderView'
    )
  },
  getBodyView() {
    console.log(
      'You need to overwrite this function and provide the constructed BodyView'
    )
  },
  onRender() {
    this.bodyTbody.show(this.getBodyView(), {
      replaceElement: true,
    })
    this.bodyThead.show(this.getHeaderView(), {
      replaceElement: true,
    })
    this.onDestroy()
    this.startUpdatingHeaders()
  },
  startUpdatingHeaders() {
    window.requestAnimationFrame(() => {
      moveHeaders.call(this)
      this.startUpdatingHeaders()
    })
  },
  onDestroy() {
    window.cancelAnimationFrame(this.headerAnimationFrameId)
  },
})
