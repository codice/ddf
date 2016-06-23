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
        className: 'is-metacards-basic',
        setDefaultModel: function(){
            this.model = this.selectionInterface.getSelectedResults();
        },
        selectionInterface: store,
        initialize: function(options){
            this.selectionInterface = options.selectionInterface || this.selectionInterface;
            EditorView.prototype.initialize.call(this, options);
        },
        onBeforeShow: function() {
            var results = this.selectionInterface.getSelectedResults();
            var types = results.map(function (result) {
                return result.get('propertyTypes');
            });
            var metacards = results.map(function (result) {
                return result.get('metacard>properties').toJSON();
            });
            this.editorProperties.show(PropertyCollectionView.generatePropertyCollectionView(types, metacards));
            this.editorProperties.currentView.turnOnLimitedWidth();
            this.editorProperties.currentView.$el.addClass("is-list");
            //this.getValidation();
        },
        getValidation: function(){
            var self = this;
            $.get('/search/catalog/internal/metacard/'+this.model.get('metacard').id+'/validation').then(function(response){
                if (validationResponse && !_.isEmpty(validationResponse.length) && !self.isDestroyed){
                    self.editorProperties.currentView.updateValidation(response);
                }
            }).always(function(){
                if (!self.isDestroyed){

                }
            });
        },
        afterCancel: function(){
            //this.getValidation();
        },
        afterSave: function(editorJSON){
            if (editorJSON.length > 0){
                var payload = [
                    {
                        ids: this.model.map(function(metacard){
                            return metacard.get('metacard').get('id');
                        }),
                        attributes: editorJSON
                    }
                ];
                LoadingCompanionView.beginLoading(this);
                var self = this;
                setTimeout(function(){
                    $.ajax({
                        url: '/search/catalog/internal/metacards',
                        type: 'PATCH',
                        data: JSON.stringify(payload),
                        contentType: 'application/json'
                    }).always(function(response){
                        var attributeMap = response.reduce(function(attributeMap, changes){
                            return changes.attributes.reduce(function(attrMap, chnges){
                                attrMap[chnges.attribute] = chnges.values[0];
                                return attrMap;
                            }, attributeMap);
                        }, {});
                        self.model.forEach(function(metacard){
                           metacard.get('metacard').get('properties').set(attributeMap);
                        });
                        setTimeout(function(){  //let solr flush
                            LoadingCompanionView.endLoading(self);
                            self.onBeforeShow();
                        }, 1000);
                    });
                }, 1000);
            }
        }
    });
});
