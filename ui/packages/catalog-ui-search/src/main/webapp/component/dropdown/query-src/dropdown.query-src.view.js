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
const template = require('./dropdown.query-src.hbs')
const ComponentView = require('../../query-src/query-src.view.js')
const sources = require('../../singletons/sources-instance.js')
const properties = require('../../../js/properties.js')

module.exports = DropdownView.extend({
  template: template,
  className: 'is-querySrc',
  componentToShow: ComponentView,
  initializeComponentModel: function() {
    //override if you need more functionality
    this.modelForComponent = this.model
    this.listenTo(this.model, 'change:federation', this.render)
  },
  isCentered: true,
  getCenteringElement: function() {
    return this.el.querySelector('.dropdown-container')
  },
  hasTail: true,
  hasLimitedWidth: true,
  serializeData: function() {
    var srcs = this.model.get('value')
    return {
      sources: sources.toJSON().filter(function(src) {
        return srcs.indexOf(src.id) !== -1
      }),
      enterprise: this.model.get('federation') === 'enterprise',
      localCatalog: sources.localCatalog,
      isLocalCatalogEnabled: !properties.isDisableLocalCatalog(),
    }
  },
})
