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
    'backbone',
    'marionette',
    'underscore',
    'jquery',
    './result-group.hbs',
    'js/CustomElements',
    'js/store',
    'js/Common',
    'js/model/Metacard'
], function (Backbone, Marionette, _, $, template, CustomElements, store, Common, Metacard) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('result-group'),
        modelEvents: {
        },
        events: {
        },
        regions: {
            groupResults: '.group-results'
        },
        selectionInterface: store,
        initialize: function(options){
            this.selectionInterface = options.selectionInterface || store;
            this.resultItemCollectionView = require('component/result-item/result-item.collection.view');
        },
        onBeforeShow: function(){
            var resultCollection = new Metacard.Results();
            resultCollection.add(this.model);
            resultCollection.add(this.model.duplicates);
            this.groupResults.show(new this.resultItemCollectionView({
                collection: resultCollection,
                selectionInterface: this.selectionInterface,
                group: true
            }));
        },
        serializeData: function(){
            return {
                amount: this.model.duplicates.length + 1
            };
        }
    });
});
