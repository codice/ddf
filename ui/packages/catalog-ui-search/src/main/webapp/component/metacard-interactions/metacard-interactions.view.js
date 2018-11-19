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
/*global define, window*/
const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')
const plugin = require('plugins/metacard-interactions')

import React from 'react'
import InteractionsView from '../../react-component/container/metacard-interactions'

const MetacardInteractionsView = Marionette.LayoutView.extend({
  template() {
    const props = {
      model: this.model,
      el: this.$el,
      extensions: this.getExtensions(),
      categories: this.getCategories(),
    }
    return <InteractionsView {...props} />
  },
  tagName: CustomElements.register('metacard-interactions'),
  className: 'composed-menu',
  modelEvents: {
    change: 'render',
  },
  ui: {},
  getExtensions() {},
  handleShare() {},

  /**
   * Should return a list of items comprising a 'category' of action links that will be rendered in the drop-down.
   * Ex.
   * {`category-name`: [{
   *  parent: `parent-css-class`,
   *  dataHelp: `Something helpful here`,
   *  icon: `icon class`, //should be consistent throughout the category
   *  linkText: `Text to be rendered for the link`,
   *  actionHandler: () => `Perform on-click`
   * }]}
   */
  getCategories() {},
})

module.exports = plugin(MetacardInteractionsView)
