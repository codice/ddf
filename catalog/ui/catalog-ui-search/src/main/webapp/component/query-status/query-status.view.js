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
    'text!./query-status.hbs',
    'js/CustomElements',
    'component/loading/loading.view',
    'js/store'
], function (Marionette, _, $, template, CustomElements, LoadingView, store) {
    
    return Marionette.ItemView.extend({
        setDefaultModel: function(){
        },
        template: template,
        tagName: CustomElements.register('query-status'),
        modelEvents: {
        },
        events: {
        },
        ui: {
        },
        initialize: function(options){
            if (store.getQueryById(this.model.id)) {
                this.listenTo(store.getQueryById(this.model.id), 'nested-change', this.render);
            }
        },
        loadData: function(){
        },
        onRender: function(){
        },
        serializeData: function(){
            var json = [];
            var results = store.getQueryById(this.model.id);
            if (results) {
                var status = results.get('result>status');
                var initiated = results.get('result>initiated');
                if (status) {
                    status = _.filter(status.toJSON(), function (status) {
                        return status.id !== 'cache';
                    });
                    status.forEach(function (status) {
                        switch (status.successful) {
                            case true:
                                status.state = status.count + " of " + status.hits;
                                break;
                            case false:
                                status.state = "failed";
                                break;
                            case undefined:
                                status.state = "pending";
                                status.pending = true;
                                break;
                        }
                    });
                    json = {
                        status: status,
                        initiated: initiated
                    }
                }
            }
            return json;
        }
    });
});
