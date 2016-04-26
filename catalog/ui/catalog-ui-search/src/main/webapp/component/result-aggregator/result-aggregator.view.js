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
/*global define*/
define([
    'wreqr',
    'marionette',
    'underscore',
    'jquery',
    'text!./result-aggregator.hbs',
    'js/CustomElements',
    'js/store'
], function (wreqr, Marionette, _, $, resultAggregatorTemplate, CustomElements, store) {

    var resultAggregator = Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = store.getCurrentWorkspace();
        },
        template: resultAggregatorTemplate,
        tagName: CustomElements.register('result-aggregator'),
        modelEvents: {
        },
        events: {
        },
        ui: {
        },
        regions: {
        },
        initialize: function(options){
            if (options.model === undefined){
                this.setDefaultModel();
            }
            //this.listenTo(this.model.get('queries'), 'nested-change', _.debounce(this.handleUpdate,200));
            //this.listenTo(store.get('content').get('results'), 'all', this.rerender);
            //this.handleUpdate();
        },
        handleUpdate: function(){
            var results = store.get('content').get('results');
            wreqr.vent.trigger('map:clear');
            this.model.get('queries').forEach(function(search){
                var searchResult = search.get('result');
                if (searchResult){
                    var searchResults = searchResult.get('results');
                    if (searchResults){
                        searchResults.forEach(function(metacardResult){
                            var metacard = metacardResult.get('metacard');
                            results.add(metacard);
                        });
                    }
                }
                if (searchResult){
                    wreqr.vent.trigger('map:results', searchResult, false);
                }
            });
        },
        serializeData: function(){
            return store.get('content').get('results').toJSON();
        },
        render: function(){
            console.log('overriding render');
        }
    });

    return resultAggregator;
});
