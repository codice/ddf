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
    'text!./result-item.hbs',
    'js/CustomElements',
    'js/store',
    'js/Common',
    'component/dropdown/dropdown',
    'component/dropdown/metacard-interactions/dropdown.metacard-interactions.view',
    'component/result-indicator/result-indicator.view',
    'properties'
], function (Backbone, Marionette, _, $, template, CustomElements, store, Common, DropdownModel, MetacardInteractionsDropdownView, ResultIndicatorView, properties) {

    return Marionette.LayoutView.extend({
        template: template,
        attributes: function(){
            return {
                'data-resultid': this.model.id
            };
        },
        tagName: CustomElements.register('result-item'),
        modelEvents: {
        },
        events: {
            'click .result-save': 'handleSave',
            'click .result-unsave': 'handleUnsave'
        },
        regions: {
            resultActions: '.result-actions',
            resultIndicator: '.result-indicator'
        },
        initialize: function(){
            this.checkDisplayType();
            this.checkIfSaved();
            var currentWorkspace = store.getCurrentWorkspace();
            if (currentWorkspace) {
                this.listenTo(currentWorkspace, 'change:metacards', this.checkIfSaved);
            }
            this.listenTo(this.model.get('metacard').get('properties'), 'change', this.handleMetacardUpdate);
            this.listenTo(store.get('user').get('user').get('preferences'), 'change:resultDisplay', this.checkDisplayType);
        },
        handleMetacardUpdate: function(){
            this.render();
            this.onBeforeShow();
            this.checkDisplayType();
            this.checkIfSaved();
        },
        onBeforeShow: function(){
            this._resultActions = new DropdownModel();
            this.resultActions.show(new MetacardInteractionsDropdownView({
                model: this._resultActions,
                modelForComponent: new Backbone.Collection([this.model])
            }));
            this.resultIndicator.show(new ResultIndicatorView({
                model: this.model
            }));
        },
        addConfiguredResultProperties: function(result){
            result.customDetail = [];
            properties.summaryShow.forEach(function(additionProperty){
                result.customDetail.push(result.metacard.properties[additionProperty]);
            });
            return result;
        },
        massageResult: function(result){
            //make a nice date
            result.local = Boolean(result.metacard.properties['source-id'] === 'ddf.distribution');
            var dateModified = new Date(result.metacard.properties.modified);
            result.niceDiff = Common.getMomentDate(dateModified);
            //check validation errors
            var validationErrors = result.metacard.properties['validation-errors'];
            var validationWarnings = result.metacard.properties['validation-warnings'];
            if (validationErrors){
                result.hasError = true;
                result.error = validationErrors;
            }
            if (validationWarnings){
                result.hasWarning = true;
                result.warning = validationWarnings;
            }

            return result;
        },
        serializeData: function(){
            return this.addConfiguredResultProperties(this.massageResult(this.model.toJSON()));
        },
        checkIfSaved: function(){
            var currentWorkspace = store.getCurrentWorkspace();
            if (currentWorkspace){
                var isSaved = true;
                if (currentWorkspace.get('metacards').indexOf(this.model.get('metacard').get('properties').get('id')) === -1) {
                    isSaved = false;
                }
                this.$el.toggleClass('is-saved', isSaved);
            }
        },
        checkDisplayType: function() {
            var displayType = store.get('user').get('user').get('preferences').get('resultDisplay');
            switch(displayType){
                case 'List':
                    this.$el.removeClass('is-grid').addClass('is-list');
                    break;
                case 'Grid':
                    this.$el.addClass('is-grid').removeClass('is-list');
                    break;
            }
        },
        handleSave: function(e){
            e.preventDefault();
            e.stopPropagation();
            var currentWorkspace = store.getCurrentWorkspace();
            if (currentWorkspace){
                currentWorkspace.set('metacards', _.union(currentWorkspace.get('metacards'), [this.model.get('metacard').get('properties').get('id')]));
            } else {
                //bring up modal to select workspace(s) to save to
            }
            this.checkIfSaved();
        },
        handleUnsave: function(e){
            e.preventDefault();
            e.stopPropagation();
            var currentWorkspace = store.getCurrentWorkspace();
            if (currentWorkspace){
                currentWorkspace.set('metacards', _.difference(currentWorkspace.get('metacards'), [this.model.get('metacard').get('properties').get('id')]));
            }
            this.checkIfSaved();
        }
    });
});
