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
/*global require*/
var Marionette = require('marionette');
var template = require('./sources.hbs');
var CustomElements = require('js/CustomElements');
var SourceItemCollectionView = require('component/source-item/source-item.collection.view');
var sources = require('component/singletons/sources-instance');
var SourcesSummaryView = require('component/sources-summary/sources-summary.view');

module.exports = Marionette.LayoutView.extend({
    template: template,
    tagName: CustomElements.register('sources'),
    regions: {
        sourcesSummary: '.sources-summary',
        sourcesDetails: '.sources-details'
    },
    onBeforeShow: function() {
        this.sourcesSummary.show(new SourcesSummaryView());
        this.sourcesDetails.show(new SourceItemCollectionView({
            collection: sources
        }));
    }
});