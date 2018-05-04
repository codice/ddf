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
require('backbone-associations');
const URITemplate = require('urijs/src/URITemplate');

module.exports = Backbone.AssociatedModel.extend({
    defaults: function() {
        return {
            url: undefined,
            title: undefined,
            description: undefined,
            id: undefined,
            queryId: undefined
        };
    },
    getExportType: function() {
        return this.get('title').replace(/^Export( as)?\s+\b/, '');
    },
    initialize: function() {
        this.handleQueryId();
        this.listenTo(this, 'change:queryId', this.handleQueryId);
    },
    handleQueryId: function() {
        if (this.get('queryId') !== undefined) {
            const decodedUrl = decodeURIComponent(this.get('url'));
            const expandedUrl = URITemplate(decodedUrl).expand({
                queryId: this.get('queryId')
            });
            this.set('url', expandedUrl);
        }
    }
});