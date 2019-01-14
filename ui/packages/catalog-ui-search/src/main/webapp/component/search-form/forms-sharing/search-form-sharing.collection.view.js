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
const Marionette = require('marionette')
const SearchFormView = require('../search-form.view')
const CustomElements = require('../../../js/CustomElements')

module.exports = Marionette.CollectionView.extend({
  childView: SearchFormView,
  tagName: CustomElements.register('my-shared-search-forms'),
  className: 'is-list is-inline has-list-highlighting',
  initialize: function(options) {},
  childViewOptions: function() {
    return {
      queryModel: this.options.model,
      sharingLightboxTitle: 'Search Form Sharing',
    }
  },
})
