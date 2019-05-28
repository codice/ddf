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

import React from 'react'
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const $ = require('jquery')
const router = require('../router/router.js')
const Common = require('../../js/Common.js')

const componentName = 'slideout'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'

module.exports = Marionette.ItemView.extend({
  template() {
    let ContentView = () => null
    if (this.contentView) {
      ContentView = this.contentView.prototype._isMarionetteView
        ? () => <MarionetteRegionContainer view={this.contentView} />
        : this.contentView
    }
    return (
      <React.Fragment>
        <div className="slideout-cover" />
        <div className="slideout-content">
          <ContentView closeSlideout={this.close.bind(this)} />
        </div>
      </React.Fragment>
    )
  },
  contentView: undefined,
  tagName: CustomElements.register(componentName),
  events: {
    click: 'handleOutsideClick',
    keydown: 'handleSpecialKeys',
  },
  initialize() {
    $('body').append(this.el)
    this.listenForClose()
    this.listenForEscape()
    this.listenTo(router, 'change', this.close)
  },
  listenForEscape() {
    $(window).on(
      'keydown.' + CustomElements.getNamespace() + componentName,
      this.handleSpecialKeys.bind(this)
    )
  },
  listenForClose() {
    this.$el.on('closeSlideout.' + CustomElements.getNamespace(), () => {
      this.close()
    })
  },
  open() {
    this.$el.toggleClass('is-open', true)
  },
  handleOutsideClick(event) {
    if (event.target === this.el.children[0]) {
      this.close()
    }
  },
  close() {
    this.$el.toggleClass('is-open', false)
    this.emptyContent()
  },
  emptyContent() {
    setTimeout(() => {
      this.updateContent()
    }, Common.coreTransitionTime * 1.1)
  },
  updateContent(view) {
    this.contentView = view
    this.render()
  },
  handleSpecialKeys(event) {
    let code = event.keyCode
    if (event.charCode && code == 0) code = event.charCode
    switch (code) {
      case 27:
        // Escape
        event.preventDefault()
        this.handleEscape()
        break
    }
  },
  handleEscape() {
    this.close()
  },
})
