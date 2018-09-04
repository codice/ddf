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
 var TabsView = require('../tabs.view')
 var ResultFormModel = require('./tabs.result-form')
 var store = require('js/store')

 module.exports = TabsView.extend({
   selectionInterface: store,
   setDefaultModel: function () {
     this.model = new ResultFormModel()
   },
   initialize: function () {
     this.setDefaultModel()
     TabsView.prototype.initialize.call(this)
   },
   determineContent: function () {
     var ActiveTab = this.model.getActiveView()
     this.tabsContent.show(new ActiveTab({
       model: this.options.model
     }))
   }
 })
