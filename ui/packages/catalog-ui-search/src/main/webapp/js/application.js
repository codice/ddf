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

// #Main Application
const $ = require('jquery')
const Marionette = require('marionette')
const Backbone = require('backbone')
const properties = require('./properties.js')
const banner = require('../templates/banner.layout.handlebars')
require('bootstrap/dist/js/bootstrap.min')
require('backbone-associations')
require('backbone.modelbinder')
require('backbone.modelbinder/Backbone.CollectionBinder')
require('jquery-ui-multiselect-widget/src/jquery.multiselect')

const Application = {}
Application.App = new Marionette.Application()
Application.App.addRegions({
  router: '#router',
  header: 'header',
  footer: 'footer',
})
Application.AppModel = new Backbone.Model(properties)
const $faviconElement = $('#favicon')
const $loadingElement = $('#loading')

const BannerView = Marionette.ItemView.extend({
  tagName() {
    return this.options.tagName
  },
  template: banner,
  model: Application.AppModel,
  events: {
    'click .fa-times': 'triggerHide',
  },
  initialize() {
    const message = this.model.get('ui')[this.tagName]
    if (message && message !== '') {
      this.$el.addClass('is-not-blank')
    }
  },
  getMessage() {
    return this.model.get('ui')[this.tagName]
  },
  serializeData() {
    const modelJSON = this.model.toJSON()
    modelJSON.message = this.getMessage()
    return modelJSON
  },
  triggerHide() {
    $('body').removeClass('has-' + this.tagName)
  },
})

//setup the header
Application.App.addInitializer(() => {
  Application.App.header.show(new BannerView({ tagName: 'header' }), {
    replaceElement: true,
  })
  if (
    process.env.NODE_ENV !== 'production' ||
    (Application.AppModel.get('ui').header &&
      Application.AppModel.get('ui').header !== '')
  ) {
    $('body').addClass('has-header')
  }
})
//setup the footer
Application.App.addInitializer(() => {
  Application.App.footer.show(new BannerView({ tagName: 'footer' }), {
    replaceElement: true,
  })
  if (
    process.env.NODE_ENV !== 'production' ||
    (Application.AppModel.get('ui').footer &&
      Application.AppModel.get('ui').footer !== '')
  ) {
    $('body').addClass('has-footer')
  }
})

$faviconElement.attr('href', 'data:image/png;base64,' + properties.ui.favIcon)
$loadingElement.find('.welcome-branding').text(properties.branding)
$loadingElement.find('.welcome-branding-name').text(properties.product)
$loadingElement.addClass('show-welcome')

//load all modules
Application.App.addInitializer(() => {
  require(['../js/router'], () => {
    setTimeout(() => {
      $loadingElement.removeClass('is-open')
    }, 0)
  })
})

// Once the application has been initialized (i.e. all initializers have completed), start up
// Backbone.history.
Application.App.on('start', () => {
  Backbone.history.start()
})

module.exports = Application
