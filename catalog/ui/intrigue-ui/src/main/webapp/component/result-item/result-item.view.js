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
    './result-item.hbs',
    'js/CustomElements',
    'js/store',
    'js/Common',
    'component/dropdown/dropdown',
    'component/dropdown/metacard-interactions/dropdown.metacard-interactions.view',
    'component/result-indicator/result-indicator.view',
    'properties',
    'component/router/router',
    'component/singletons/user-instance',
    'component/singletons/metacard-definitions',
    'moment',
    'component/singletons/sources-instance',
    'behaviors/button.behavior'
], function (Backbone, Marionette, _, $, template, CustomElements, store, Common, DropdownModel,
             MetacardInteractionsDropdownView, ResultIndicatorView, properties, router, user,
             metacardDefinitions, moment, sources) {

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
        behaviors: {
            button: {}
        },
        initialize: function(options){
            if (!options.selectionInterface) {
                throw 'Selection interface has not been provided';
            }
            this.checkDisplayType();
            this.checkTags();
            this.checkIfSaved();
            this.checkIsInWorkspace();
            this.checkIfBlacklisted();
            var currentWorkspace = store.getCurrentWorkspace();
            if (currentWorkspace) {
                this.listenTo(currentWorkspace, 'change:metacards', this.checkIfSaved);
            }
            this.listenTo(this.model.get('metacard').get('properties'), 'change', this.handleMetacardUpdate);
            this.listenTo(user.get('user').get('preferences'), 'change:resultDisplay', this.checkDisplayType);
            this.listenTo(router, 'change', this.handleMetacardUpdate);
            this.listenTo(user.get('user').get('preferences').get('resultBlacklist'), 
                'add remove update reset', this.checkIfBlacklisted);
            this.listenTo(this.options.selectionInterface.getSelectedResults(), 'update add remove reset', this.handleSelectionChange);
            this.handleSelectionChange();
        },
        handleSelectionChange: function() {
            var selectedResults = this.options.selectionInterface.getSelectedResults();
            var isSelected = selectedResults.get(this.model.id);
            this.$el.toggleClass('is-selected', Boolean(isSelected));
        },
        handleMetacardUpdate: function(){
            var currentWorkspace = store.getCurrentWorkspace();
            if (currentWorkspace) {
                this.stopListening(currentWorkspace);
                this.listenTo(currentWorkspace, 'change:metacards', this.handleMetacardUpdate);
            }
            this.render();
            this.onBeforeShow();
            this.checkDisplayType();
            this.checkIfSaved();
            this.checkTags();
            this.checkIsInWorkspace();
            this.checkIfBlacklisted();
        },
        onBeforeShow: function(){
            this.resultActions.show(new MetacardInteractionsDropdownView({
                model: new DropdownModel(),
                modelForComponent: new Backbone.Collection([this.model])
            }));
            this.resultIndicator.show(new ResultIndicatorView({
                model: this.model
            }));
        },
        addConfiguredResultProperties: function(result){
            result.customDetail = [];
            if (properties.resultShow) {
                properties.resultShow.forEach(function (additionProperty) {
                    var value = result.metacard.properties[additionProperty];
                    if (value && metacardDefinitions.metacardTypes[additionProperty]) {
                        switch (metacardDefinitions.metacardTypes[additionProperty].type) {
                            case 'DATE':
                                if (value.constructor === Array) {
                                    value = value.map(function (val) {
                                        return Common.getMomentDate(val);
                                    });
                                } else {
                                    value = Common.getMomentDate(value);
                                }
                                break;
                        }
                        result.customDetail.push({
                            label: additionProperty,
                            value: value
                        });
                    }
                });
            }
            return result;
        },
        massageResult: function(result){
            //make a nice date
            result.local = Boolean(result.metacard.properties['source-id'] === sources.localCatalog);
            var dateModified = moment(result.metacard.properties.modified);
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
        checkIfBlacklisted: function(){
            var pref = user.get('user').get('preferences');
            var blacklist = pref.get('resultBlacklist');
            var id = this.model.get('metacard').get('properties').get('id');
            var isBlacklisted = blacklist.get(id) !== undefined;
            this.$el.toggleClass('is-blacklisted', isBlacklisted);
        },
        checkIsInWorkspace: function(){
            var currentWorkspace = store.getCurrentWorkspace();
            this.$el.toggleClass('in-workspace', Boolean(currentWorkspace));
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
            var displayType = user.get('user').get('preferences').get('resultDisplay');
            switch(displayType){
                case 'List':
                    this.$el.removeClass('is-grid').addClass('is-list');
                    break;
                case 'Grid':
                    this.$el.addClass('is-grid').removeClass('is-list');
                    break;
            }
        },
        checkTags: function(){
            this.$el.toggleClass('is-workspace', this.model.isWorkspace());
            this.$el.toggleClass('is-resource', this.model.isResource());
            this.$el.toggleClass('is-revision', this.model.isRevision());
            this.$el.toggleClass('is-deleted', this.model.isDeleted());
            this.$el.toggleClass('is-remote', this.model.isRemote());
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
