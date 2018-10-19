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
/*global define*/
const Marionette = require('marionette')
const _ = require('underscore')
const $ = require('jquery')
const DropdownView = require('../dropdown.view')
const template = require('./dropdown.hover-preview.hbs')
const ComponentView = require('component/hover-preview/hover-preview.view')
const properties = require('properties')
const _merge = require('lodash/merge')
const Common = require('js/Common')
const user = require('component/singletons/user-instance')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-hover-preview',
  componentToShow: ComponentView,
  events: {
    'mouseenter button': 'handleMouseEnter',
    'mouseleave button': 'handleMouseLeave',
    'click button': 'handleClick',
  },
  handleMouseEnter: function() {
    if (user.getHoverPreview()) {
      this.model.open()
    }
  },
  handleMouseLeave: function() {
    this.model.close()
  },
  handleClick: function() {
    window.open(
      Common.getImageSrc(
        this.options.modelForComponent
          .get('metacard')
          .get('properties')
          .get('thumbnail')
      )
    )
  },
  initialize: function() {
    DropdownView.prototype.initialize.call(this)
  },
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = this.options.modelForComponent
  },
  listenToComponent: function() {
    //override if you need more functionality
  },
  serializeData: function() {
    return this.options.modelForComponent.toJSON()
  },
})
