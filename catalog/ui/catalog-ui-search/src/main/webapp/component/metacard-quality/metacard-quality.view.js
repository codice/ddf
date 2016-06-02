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
/*global define, setTimeout*/
define([
    'marionette',
    'underscore',
    'jquery',
    'text!./metacard-quality.hbs',
    'js/CustomElements',
    'component/loading-companion/loading-companion.view',
    'js/store',
    'js/Common'
], function (Marionette, _, $, template, CustomElements, LoadingCompanionView, store, Common) {

    var selectedVersion;

    return Marionette.ItemView.extend({
        setDefaultModel: function(){
            this.model = store.getSelectedResults().first();
        },
        template: template,
        tagName: CustomElements.register('metacard-quality'),
        modelEvents: {
            'all': 'render'
        },
        events: {
        },
        ui: {
        },
        initialize: function(options){
            if (!options.model){
                this.setDefaultModel();
            }
            this.loadData();
        },
        loadData: function(){
            LoadingCompanionView.beginLoading(this);
            var self = this;
            setTimeout(function(){
                $.get('/services/search/catalog/metacard/'+self.model.get('metacard').id+'/validation').then(function(response){
                    self._validation = response;
                }).always(function(){
                    LoadingCompanionView.endLoading(self);
                    if (!self.isDestroyed){
                        self.render();
                    }
                });
            }, 1000);
        },
        onRender: function(){
        },
        serializeData: function(){
            var self = this;
            var hasValidation = false;
            if (this._validation){
                hasValidation = this._validation.length > 0;
            }
            return {
                validation: this._validation,
                hasValidation: hasValidation
            };
        }
    });
});
