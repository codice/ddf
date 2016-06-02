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
    '../editor.view',
    'js/store',
    'component/property/property.collection.view',
    'component/loading-companion/loading-companion.view'
], function (Marionette, _, $, EditorView, store, PropertyCollectionView, LoadingCompanionView) {

    return EditorView.extend({
        className: 'is-metacard-advanced',
        setDefaultModel: function(){
            this.model = store.getSelectedResults().first();
        },
        initialize: function(options){
            EditorView.prototype.initialize.call(this, options);
            this.listenTo(this.model.get('metacard').get('properties'), 'change', this.onBeforeShow);
        },
        onBeforeShow: function() {
            this.editorProperties.show(PropertyCollectionView.generatePropertyCollectionView(
                [this.model.get('propertyTypes')],
                [this.model.get('metacard>properties').toJSON()]));
            this.editorProperties.currentView.turnOnLimitedWidth();
            this.editorProperties.currentView.$el.addClass("is-list");

            var self = this;
            $.when( $.get('/services/search/catalog/metacard/'+this.model.get('metacard').id +'/validation')).done(function(validationResponse){
                if (validationResponse && !_.isEmpty(validationResponse.length)) {
                    self.editorProperties.currentView.updateValidation(validationResponse[0]);
                }
            });
        },
        getValidation: function(){
            var self = this;
            $.get('/services/search/catalog/metacard/'+this.model.get('metacard').id+'/validation').then(function(response){
                if (!self.isDestroyed && self.editorProperties.currentView){
                    self.editorProperties.currentView.updateValidation(response);
                }
            }).always(function(){
                if (!self.isDestroyed){

                }
            });
        },
        afterCancel: function(){

        },
        afterSave: function(editorJSON){
            if (editorJSON.length > 0){
                var payload = [
                    {
                        ids: [this.model.get('metacard').get('id')],
                        attributes: editorJSON
                    }
                ];
                LoadingCompanionView.beginLoading(this);
                var self = this;
                setTimeout(function(){
                    $.ajax({
                        url: '/services/search/catalog/metacards',
                        type: 'PATCH',
                        data: JSON.stringify(payload),
                        contentType: 'application/json'
                    }).always(function(response){
                        var attributeMap = response.reduce(function(attributeMap, changes){
                            return changes.attributes.reduce(function(attrMap, chnges){
                                attrMap[chnges.attribute] = chnges.values;
                                return attrMap;
                            }, attributeMap);
                        }, {});
                        self.model.get('metacard').get('properties').set(attributeMap);
                        store.get('workspaces').forEach(function(workspace){
                            workspace.get('queries').forEach(function(query){
                                if (query.get('result')) {
                                    query.get('result').get('results').forEach(function(result){
                                        if (result.get('metacard').get('properties').get('id') ===  self.model.get('metacard').get('properties').get('id')){
                                            result.get('metacard').get('properties').set(attributeMap);
                                        }
                                    });
                                }
                            });
                        });
                        setTimeout(function(){  //let solr flush
                            LoadingCompanionView.endLoading(self);
                        }, 1000);
                    });
                }, 1000);
            }
        }
    });
});
