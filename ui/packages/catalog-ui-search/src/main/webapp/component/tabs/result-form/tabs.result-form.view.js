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
/* global require */
const TabsView = require('../tabs.view')
const ResultFormModel = require('./tabs.result-form')
const store = require('../../../js/store.js')

module.exports = TabsView.extend({
  selectionInterface: store,
  setDefaultModel() {
    this.model = new ResultFormModel()
  },
  initialize() {
    this.setDefaultModel()
    TabsView.prototype.initialize.call(this)
  },
  determineContent() {
    const ActiveTab = this.model.getActiveView()
    this.tabsContent.show(
      new ActiveTab({
        model: this.options.model,
      })
    )
  },
})
