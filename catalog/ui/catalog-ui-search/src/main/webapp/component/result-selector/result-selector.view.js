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
    'text!./result-selector.hbs',
    'js/CustomElements',
    'js/store'
], function (wreqr, Marionette, _, $, resultSelectorTemplate, CustomElements, store) {

    var ResultSelector = Marionette.LayoutView.extend({
        template: resultSelectorTemplate,
        tagName: CustomElements.register('result-selector'),
        modelEvents: {
        },
        events: {
        },
        ui: {
        },
        regions: {
        },
        initialize: function(options){
            this.listenTo(this.model, 'nested-change', _.debounce(this.handleUpdate,200));
            this.handleUpdate();
        },
        handleUpdate: function(){
            wreqr.vent.trigger('map:clear');
            this.updateMap();
            this.render();
        },
        serializeData: function(){
            return this.model.get('result').get('results').toJSON();
        },
        updateMap: function(){
            var searchResult = this.model.get('result');
            if (searchResult){
                wreqr.vent.trigger('map:results', searchResult, false);
            }
        }
    });

    return ResultSelector;
});
