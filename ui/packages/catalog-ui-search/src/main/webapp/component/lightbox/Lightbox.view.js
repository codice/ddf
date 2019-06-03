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
const $ = require('jquery')
const CustomElements = require('../../js/CustomElements.js')
const store = require('../../js/store.js')
const Lightbox = require('./Lightbox.js')
const router = require('../router/router.js')
const componentName = 'lightbox'
import React from 'react'
import MarionetteRegionContainer from '../../react-component/container/marionette-region-container'

module.exports = Marionette.LayoutView.extend(
  {
    showContent(contentComponent) {
      this.contentComponent = contentComponent
      this.render()
    },
    template() {
      let component = null
      if (
        this.contentComponent !== undefined &&
        this.contentComponent._isMarionetteView
      ) {
        component = (
          <MarionetteRegionContainer
            view={this.contentComponent}
            replaceElement
          />
        )
      } else if (this.contentComponent !== undefined) {
        component = this.contentComponent
      }
      return (
        <React.Fragment>
          <div className="lightbox-container">
            <div className="lightbox-title-bar clearfix">
              <button title="close" className="lightbox-close is-button">
                <span className="fa fa-times" />
              </button>
              <span className="lightbox-title">{this.model.get('title')}</span>
            </div>
            <div className="lightbox-content">{component}</div>
          </div>
        </React.Fragment>
      )
    },
    tagName: CustomElements.register(componentName),
    modelEvents: {
      all: 'render',
      'change:open': 'handleOpen',
    },
    events: {
      click: 'handleOutsideClick',
      'click .lightbox-close': 'close',
    },
    initialize() {
      $('body').append(this.el)
      this.listenTo(
        store.get('workspaces'),
        'change:currentWorkspace',
        this.close
      )
      this.listenForClose()
      this.listenForRoute()
      this.listenForEscape()
    },
    listenForEscape() {
      $(window).on(
        'keydown.' + CustomElements.getNamespace() + componentName,
        this.handleSpecialKeys.bind(this)
      )
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
    listenForRoute() {
      this.listenTo(router, 'change', this.handleRouteChange)
    },
    listenForClose() {
      this.$el.on(
        CustomElements.getNamespace() + 'close-' + componentName,
        () => {
          this.close()
        }
      )
    },
    handleOpen() {
      this.$el.toggleClass('is-open', this.model.isOpen())
      $('html').toggleClass('open-lightbox', true)
    },
    handleRouteChange() {
      this.close()
    },
    handleEscape() {
      this.close()
    },
    handleOutsideClick(event) {
      if (event.target === this.el) {
        this.close()
      }
    },
    close() {
      this.model.close()
      $('html').toggleClass('open-lightbox', false)
    },
  },
  {
    generateNewLightbox() {
      return new this({
        model: new Lightbox(),
      })
    },
  }
)
