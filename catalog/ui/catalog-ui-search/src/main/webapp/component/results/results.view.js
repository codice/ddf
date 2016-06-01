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
    'marionette',
    'underscore',
    'jquery',
    'text!./results.hbs',
    'js/CustomElements',
    'component/dropdown/query-select/dropdown.query-select.view',
    'component/dropdown/dropdown',
    'js/store',
    'component/result-selector/result-selector.view'
], function (Marionette, _, $, resultsTemplate, CustomElements, QuerySelectDropdown, DropdownModel, store,
            ResultSelectorView) {

    var ResultsView = Marionette.LayoutView.extend({
        setDefaultModel: function(){
            this.model = store.getCurrentQueries();
        },
        template: resultsTemplate,
        tagName: CustomElements.register('results'),
        modelEvents: {
        },
        events: {
        },
        ui: {
        },
        regions: {
            resultsSelect: '.results-select',
            resultsList: '.results-list'
        },
        initialize: function(options){
            if (options.model === undefined){
                this.setDefaultModel();
            }
            this._resultsSelectDropdownModel = new DropdownModel({
                value: undefined
            });
            this.listenTo(this._resultsSelectDropdownModel, 'change:value', this.updateResultsList);
        },
        onBeforeShow: function(){
            this.resultsSelect.show(new QuerySelectDropdown({
                model: this._resultsSelectDropdownModel
            }));
        },
        updateResultsList: function(){
            var queryId = this._resultsSelectDropdownModel.get('value');
            if (queryId){
                this.resultsList.show(new ResultSelectorView({
                    model: store.getCurrentQueries().get(queryId)
                }));
            } else {

            }
        },
        onRender: function(){
        }
    });

    return ResultsView;
});
