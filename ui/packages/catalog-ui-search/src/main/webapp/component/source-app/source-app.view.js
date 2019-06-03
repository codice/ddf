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
const template = require('./source-app.hbs')
const CustomElements = require('../../js/CustomElements.js')
const LoadingCompanionView = require('../loading-companion/loading-companion.view.js')

module.exports = Marionette.LayoutView.extend({
  template,
  tagName: CustomElements.register('source-app'),
  events: {
    'click button': 'handleClick',
  },
  serializeData() {
    return {
      url: this.options['url'],
    }
  },
  handleClick() {
    this.$el.trigger(CustomElements.getNamespace() + 'close-lightbox')
  },
  onRender() {
    LoadingCompanionView.beginLoading(this)

    this.$el.find('iframe').on('load', () => {
      LoadingCompanionView.endLoading(this)
    })
  },
})
