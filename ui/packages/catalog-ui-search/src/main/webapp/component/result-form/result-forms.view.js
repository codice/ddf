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
/* global setTimeout */
const SearchFormViews = require('component/search-form/search-form.view')

module.exports = SearchFormViews.extend({
  initialize: function () {
    SearchFormViews.prototype.initialize.call(this)
  },
  changeView: function () {
    let oldType = this.options.queryModel.get('type')
    switch (this.model.get('type')) {
      case 'new-result':
        this.options.queryModel.set({
          type: 'new-result',
          resultTitle: '',
          formId: this.model.get('id'),
          accessGroups: [],
          accessIndividuals: [],
          descriptors: [],
          description: ''
        })
        break
      case 'result':
        this.options.queryModel.set({
          type: 'result',
          resultTitle: this.model.get('name'),
          formId: this.model.get('id'),
          accessGroups: this.model.get('accessGroups'),
          accessIndividuals: this.model.get('accessIndividuals'),
          descriptors: this.model.get('descriptors'),
          description: this.model.get('description')
        })
        break
    }
    if (oldType === this.model.get('type')) {
      this.options.queryModel.trigger('change:type')
    }
    this.triggerCloseDropdown()
  }
})
