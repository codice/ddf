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

var wreqr = require('../../../js/wreqr.js')
var $ = require('jquery')
var _ = require('underscore')
var Marionette = require('marionette')
var CustomElements = require('../../../js/CustomElements.js')
var Common = require('../../../js/Common.js')
var RowView = require('./row.view')
require('../../../behaviors/selection.behavior.js')

module.exports = Marionette.CollectionView.extend({
  tagName: CustomElements.register('result-tbody'),
  className: 'is-tbody is-list has-list-highlighting',
  behaviors: function() {
    return {
      selection: {
        selectionInterface: this.options.selectionInterface,
        selectionSelector: `> *`,
      },
    }
  },
  childView: RowView,
  childViewOptions: function() {
    return {
      selectionInterface: this.options.selectionInterface,
    }
  },
  initialize: function(options) {
    if (!options.selectionInterface) {
      throw 'Selection interface has not been provided'
    }
  },
})
