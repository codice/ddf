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
/*global define, window*/

define([
    'backbone',
    'backboneassociations'
    ], function (Backbone) {
    'use strict';

    var User = {};

    User.Preferences = Backbone.AssociatedModel.extend({
        defaults: {
            pointColor: '#FFA467',
            multiPointColor: '#FFA467',
            lineColor: '#5B93FF',
            multiLineColor: '#5B93FF',
            polygonColor: '#FF6776',
            multiPolygonColor: '#FF6776',
            geometryCollectionColor: '#FFFF67'
        },
        //this URL is not used for fetching, but for saving the preferences back to the user service
        url: '/service/user',
        initialize: function () {
            if (this.parents.length === 0 || this.parents[0].isGuestUser()) {
                var jsonString = window.localStorage.getItem('org.codice.ddf.search.preferences');
                if (jsonString && jsonString !== '') {
                    this.set(JSON.parse(jsonString));
                }
            }
        },
        savePreferences: function () {
            if (this.parents[0].isGuestUser()) {
                window.localStorage.setItem('org.codice.ddf.search.preferences', JSON.stringify(this.toJSON()));
            } else {
                //this call will use the above URL through the cometd backbone library
                this.save();
            }
        }
    });

    User.Model = Backbone.AssociatedModel.extend({
        relations: [
            {
                type: Backbone.One,
                key: 'preferences',
                relatedModel: User.Preferences
            }
        ],
        defaults: function() {
            return {
                isAnonymous: true,
                preferences: new User.Preferences()
            };
        },
        isGuestUser: function() {
            return this.get('isAnonymous') === 'true' || this.get('isAnonymous') === true;
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
        defaults: function() {
            return {
                user: new User.Model()
            };
        },
        parse: function (resp) {
            if (resp.data) {
                return resp.data;
            }
            return resp;
        }
    });

    return User;
});
