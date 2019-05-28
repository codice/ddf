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
const template = require('./metacard-preview.hbs')
const CustomElements = require('../../js/CustomElements.js')
const LoadingCompanionView = require('../loading-companion/loading-companion.view.js')
const store = require('../../js/store.js')
const user = require('../singletons/user-instance.js')
const preferences = user.get('user').get('preferences')
const wreqr = require('../../js/wreqr.js')

function getSrc(previewHtml, textColor) {
  return (
    '<html class="is-iframe is-preview" style="font-size: ' +
    preferences.get('fontSize') +
    'px; color: ' +
    textColor +
    ';">' +
    '<link href="css/styles.' +
    document
      .querySelector('link[href*="css/styles."]')
      .href.split('css/styles.')[1] +
    '" rel="stylesheet">' +
    previewHtml +
    document.querySelector('[data-theme]').cloneNode(true).outerHTML +
    '</html>'
  )
}

module.exports = Marionette.ItemView.extend({
  setDefaultModel() {
    this.model = this.selectionInterface.getSelectedResults().first()
  },
  template,
  tagName: CustomElements.register('metacard-preview'),
  selectionInterface: store,
  initialize(options) {
    this.selectionInterface =
      options.selectionInterface || this.selectionInterface
    if (!options.model) {
      this.setDefaultModel()
    }
    LoadingCompanionView.beginLoading(this)
    this.previewRequest = $.get({
      url: this.model.getPreview(),
      dataType: 'html',
      customErrorHandling: true,
    })
      .then(
        function(previewHtml) {
          this.previewHtml = previewHtml
        }.bind(this)
      )
      .always(
        function() {
          LoadingCompanionView.endLoading(this)
        }.bind(this)
      )
  },
  onAttach() {
    this.textColor = window.getComputedStyle(this.el).color
    this.previewRequest.then(
      function() {
        if (!this.isDestroyed) {
          this.populateIframe()
          this.listenTo(
            user.get('user').get('preferences'),
            'change:fontSize',
            this.populateIframe
          )
          this.listenTo(wreqr.vent, 'resize', this.populateIframeIfNecessary)
        }
      }.bind(this)
    )
  },
  // golden layout destroys and recreates elements in such a way as to empty iframes: https://github.com/deepstreamIO/golden-layout/issues/154
  populateIframeIfNecessary() {
    if (
      this.$el
        .find('iframe')
        .contents()[0]
        .children[0].getAttribute('class') === null
    ) {
      this.populateIframe()
    }
  },
  populateIframe() {
    const $iframe = this.$el.find('iframe')
    $iframe.ready(
      function() {
        $iframe.contents()[0].open()
        $iframe.contents()[0].write(getSrc(this.previewHtml, this.textColor))
        $iframe.contents()[0].close()
      }.bind(this)
    )
  },
  onDestroy() {},
})
