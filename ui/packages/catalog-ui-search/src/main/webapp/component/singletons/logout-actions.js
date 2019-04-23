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

//meant to be used for just in time feature detection
const Backbone = require('backbone');
require('backbone-associations')

const LogoutAction = Backbone.AssociatedModel.extend({
  defaults: {
    auth: '',
    description: '',
    realm: '',
    title: '',
    url: '',
  },
});

const LogoutActions = Backbone.Collection.extend({
  model: LogoutAction,
  url: './internal/logout/actions',
});

const logoutModel = new (Backbone.AssociatedModel.extend({
  defaults: {
    actions: [],
    fetched: false,
  },
  relations: [
    {
      type: Backbone.Many,
      key: 'actions',
      collectionType: LogoutActions,
    },
  ],
  url: './internal/logout/actions',
  initialize: function() {
    this.listenTo(this, 'sync:actions', this.setFetched)
    this.get('actions').fetch()
  },
  setFetched: function() {
    this.set('fetched', true)
  },
  isIdp: function() {
    return this.get('actions').where({ realm: 'idp' }).length > 0
  },
}))();

module.exports = logoutModel
