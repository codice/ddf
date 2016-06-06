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
    'text!./metacard-associations.hbs',
    'js/CustomElements',
    'js/store',
    'js/Common',
    'component/loading-companion/loading-companion.view'
], function (wreqr, Marionette, _, $, template, CustomElements, store, Common, LoadingCompanionView) {

    return Marionette.ItemView.extend({
        setDefaultModel: function(){
            this.model = store.getSelectedResults().first();
        },
        template: template,
        tagName: CustomElements.register('metacard-associations'),
        modelEvents: {
        },
        events: {
            'click .associations-edit': 'turnOnEditing',
            'click .associations-cancel': 'turnOffEditing',
            'click .associations-addNew': 'addNewAssociation',
            'click .associations-save': 'save',
            'click .association-delete': 'delete'
        },
        ui: {
        },
        initialize: function(options){
            if (!options.model){
                this.setDefaultModel();
            }
            LoadingCompanionView.beginLoading(this);
            var self = this;
            this.determinePossibleAssociations();
            $.get('/search/catalog/internal/associations/'+this.model.get('metacard').get('id')).then(function(response){
                self._originalAssociations = JSON.parse(JSON.stringify(response));
                self._associations = response;
            }).always(function(){
                LoadingCompanionView.endLoading(self);
                self.render();
            });
        },
        serializeData: function(){
            return {
                associations: this._associations,
                possibleAssociations: this._possibleAssociations
            };
        },
        turnOnEditing: function(){
            this.$el.addClass('is-editing');
        },
        turnOffEditing: function(){
            this._associations = JSON.parse(JSON.stringify(this._originalAssociations));;
            this.render();
            this.$el.removeClass('is-editing');
        },
        addNewAssociation: function(){
            var self = this;
            var associationType = this.$el.find('select').first().val();
            var associationIds = this.$el.find('select').last().val();
            if (associationIds !== null){
                this._associations.forEach(function(associationCollection){
                    if (associationCollection.type === associationType){
                        associationIds.forEach(function(associationId){
                            var association = {
                                id: associationId,
                                title: self._possibleAssociations[associationId].title
                            };
                            var dupes = associationCollection.metacards.filter(function(existingAssociation){
                               return existingAssociation.id === association.id;
                            });
                            if (dupes.length === 0){
                                associationCollection.metacards.push(association);
                            }
                        });
                    }
                });
            }
            this.render();
            this.turnOnEditing();
        },
        determinePossibleAssociations: function(){
            var possibleAssociations = {};
            var currentWorkspace = store.getCurrentWorkspace();
            if (currentWorkspace){
                store.getCurrentQueries().filter(function(query) {
                    return query.get('result');
                }).map(function(query){
                    return query.get('result').toJSON().results;
                }).forEach(function(resultList){
                    resultList.forEach(function(metacard){
                        possibleAssociations[metacard.metacard.id] = {
                            id: metacard.metacard.id,
                            title: metacard.metacard.properties.title
                        }
                    });
                });
            }
            this._possibleAssociations = possibleAssociations;
        },
        save: function(){
            LoadingCompanionView.beginLoading(this);
            var self = this;
            $.ajax({
               url: '/search/catalog/internal/associations/'+this.model.get('metacard').get('id'),
                data: JSON.stringify(this._associations),
                method: 'PUT',
                contentType: 'application/json'
            }).always(function(response){
                LoadingCompanionView.endLoading(self);
                self._originalAssociations = JSON.parse(JSON.stringify(response));
                self._associations = response;
                self.render();
                self.turnOffEditing();
            });
        },
        delete: function(event){
            var metacardId = $(event.currentTarget).attr('data-metacardId');
            var associationType = $(event.currentTarget).attr('data-associationType');
            this._associations.forEach(function(associationCollection){
                 if (associationCollection.type === associationType){
                     associationCollection.metacards = associationCollection.metacards.filter(function(association){
                            return association.id !== metacardId;
                     });
                 }
            });
            this.render();
        }
    });
});
