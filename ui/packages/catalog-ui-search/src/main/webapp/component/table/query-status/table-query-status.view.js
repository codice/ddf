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

const TableView = require('../table.view');
const HeaderView = require('../../query-status/query-status-header.view.js');
const BodyView = require('../../query-status/query-status-body.view.js');

module.exports = TableView.extend({
  className: 'is-query-status',
  initialize: function() {
    const result = this.model.get('result');
    if (!result) {
      this.startListeningToSearch()
    }
  },
  startListeningToSearch: function() {
    this.listenToOnce(this.model, 'change:result', this.render)
  },
  getHeaderView: function() {
    return new HeaderView({
      model: this.model,
    })
  },
  getBodyView: function() {
    const result = this.model.get('result');
    const bodyView = new BodyView();
    if (result) {
      bodyView.collection = result.get('status')
    }
    return bodyView
  },
})
