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
var Backbone = require('backbone');
var properties = require('properties');
require('backboneassociations');

module.exports = Backbone.AssociatedModel.extend({
    defaults: {
        count: 0,
        elapsed: 0,
        hits: 0,
        id: 'undefined',
        successful: undefined,
        top: 0,
        fromcache: 0,
        cacheHasReturned: properties.isCacheDisabled,
        cacheSuccessful: true,
        cacheMessages: [],
        hasReturned: false,
        messages: []
    },
    initialize: function () {
        this.listenToOnce(this, 'change:successful', this.setHasReturned);
    },
    setHasReturned: function () {
        this.set('hasReturned', true);
    },
    setCacheHasReturned: function () {
        this.set('cacheHasReturned', true);
    },
    updateMessages: function (messages, id, status) {
        if (this.id === id) {
            this.set('messages', messages);
        }
        if (id === 'cache') {
            this.set({
                cacheHasReturned: true,
                cacheSuccessful: status ? status.successful : false,
                cacheMessages: messages
            });
        }
    },
    updateStatus: function (results) {
        var top = 0;
        var fromcache = 0;
        results.forEach(function (result) {
            if (result.get('metacard').get('properties').get('source-id') === this.id) {
                top++;
                if (!result.get('uncached')) {
                    fromcache++;
                }
            }
        }.bind(this));
        this.set({
            top: top,
            fromcache: fromcache
        });
    }
});