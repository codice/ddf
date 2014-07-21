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
/*global define*/

define([
    'backbone',
    'backboneassociations'
    ], function (Backbone) {
    'use strict';

    var User = {};

    User.Model = Backbone.AssociatedModel.extend({
        isGuestUser: function() {
            return this.get('username') === this.guestUser;
        }
    });

    User.Response = Backbone.AssociatedModel.extend({
        relations: [
            {
                type: Backbone.One,
                key: 'user',
                relatedModel: User.Model
            }
        ],
        url: '/service/user',
        syncUrl: "/search/standard/user",
        useAjaxSync: false,
        guestUser: 'guest',
        guestPass: 'guest',
        parse: function (resp) {
            if (resp.data) {
                return resp.data;
            }
            return resp;
        }

    });

    return User;
});
