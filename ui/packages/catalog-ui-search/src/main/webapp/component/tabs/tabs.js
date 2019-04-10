/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

const _ = require('underscore')
const Backbone = require('backbone')
var Tabs = Backbone.Model.extend({
  defaults: {
    tabs: {},
    tabsOptions: {},
    activeTab: undefined,
  },
  initialize: function() {
    this.setDefaultActiveTab()
  },
  setDefaultActiveTab: function() {
    var tabs = this.get('tabs')
    if (Object.keys(tabs).length > 0 && !this.getActiveTab()) {
      this.set('activeTab', Object.keys(tabs)[0])
    }
  },
  setActiveTab: function(tab) {
    this.set('activeTab', tab)
  },
  getActiveTab: function() {
    return this.get('activeTab')
  },
  getActiveView: function() {
    return this.get('tabs')[this.getActiveTab()]
  },
  getActiveViewOptions: function() {
    if (this.get('tabsOptions')) {
      return this.get('tabsOptions')[this.getActiveTab()]
    }
  },
})

module.exports = Tabs
