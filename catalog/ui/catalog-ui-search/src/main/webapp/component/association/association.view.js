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
/*global require*/
var template = require('./association.hbs');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var DropdownView = require('component/dropdown/dropdown.view');
var _ = require('underscore');

function getModelUpdateMethod(modelToUpdate, property, relatedModel) {
    return function() {
        modelToUpdate.set(property, relatedModel.get('value')[0]);
    };
}

function getDropdownUpdateMethod(dropdownModel, property, relatedModel) {
    return function() {
        dropdownModel.set('value', [relatedModel.get(property)]);
    }
}

function determineChoices(view) {
    var currentMetacard = view.options.currentMetacard;
    var choices = view.options.selectionInterface.getActiveSearchResults().filter(function(result) {
        return result.get('metacard').id !== currentMetacard.get('metacard').id
    }).reduce(function(options, result) {
        options.push({
            label: result.get('metacard').get('properties').get('title'),
            value: result.get('metacard').id
        })
        return options;
    }, [{
        label: 'Current Metacard',
        value: currentMetacard.get('metacard').id
    }].concat(view.options.knownMetacards.filter(function(metacard) {
        return metacard.id !== currentMetacard.get('metacard').id
    }).map(function(metacard) {
        return {
            label: metacard.get('title'),
            value: metacard.id
        };
    })));
    choices = _.uniq(choices, false, function(choice) {
        return choice.value
    });
    return choices;
}

module.exports = Marionette.LayoutView.extend({
    tagName: CustomElements.register('association'),
    template: template,
    regions: {
        associationParent: '> .association-content > .association-parent',
        associationRelationship: '> .association-content > .association-relationship',
        associationChild: '> .association-content > .association-child'
    },
    events: {
        'click > .association-remove': 'removeAssociation'
    },
    initialize: function() {
        var currentMetacardId = this.options.currentMetacard.get('metacard').id;
        if (!this.model.get('parent')) {
            this.model.set('parent', currentMetacardId);
        }
        if (!this.model.get('child')) {
            this.model.set('child', currentMetacardId);
        }
    },
    removeAssociation: function() {
        this.model.collection.remove(this.model);
    },
    choices: undefined,
    onBeforeShow: function() {
        this.choices = determineChoices(this);
        this.showAssociationParent();
        this.showAssociationRelationship();
        this.showAssociationChild();
        this.setupListeners();
        this.setupReadOnlyLabelListeners();
        this.updateReadOnlyLabels();
        this.checkHeritage();
    },
    turnOnEditing: function() {
        this.$el.toggleClass('is-editing', true);
        this.regionManager.forEach(function(region) {
            if (region.currentView && region.currentView.turnOnEditing) {
                region.currentView.turnOnEditing();
            }
        });
    },
    turnOffEditing: function() {
        this.$el.toggleClass('is-editing', false);
        this.regionManager.forEach(function(region) {
            if (region.currentView && region.currentView.turnOffEditing) {
                region.currentView.turnOffEditing();
            }
        });
    },
    setupListeners: function() {
        this.listenTo(this.model, 'change:parent change:child', this.ensureAtLeastOneCurrent);
    },
    ensureAtLeastOneCurrent: function(model, options) {
        var currentMetacard = this.options.currentMetacard;
        var value = model.hasChanged('parent') ? model.get('parent') : model.get('child');
        if (value !== currentMetacard.get('metacard').id) {
            model.set(model.hasChanged('parent') ? 'child' : 'parent', currentMetacard.get('metacard').id);
        }
        this.rerenderDropdowns();
        this.checkHeritage();
    },
    checkHeritage: function() {
        var currentMetacard = this.options.currentMetacard;
        this.$el.toggleClass('is-parent', this.model.get('parent') === currentMetacard.get('metacard').id);
        this.$el.toggleClass('is-child', this.model.get('child') === currentMetacard.get('metacard').id);
    },
    rerenderDropdowns: function() {
        var childDropdown = this.associationChild.currentView.dropdownCompanion.componentToShow.currentView;
        var parentDropdown = this.associationParent.currentView.dropdownCompanion.componentToShow.currentView;
        if (childDropdown) {
            childDropdown.render();
        }
        if (parentDropdown) {
            parentDropdown.render();
        }
    },
    updateAssociationParent: function() {
        this.model.set('parent', this.associationParent.currentView.model.get('value'));
    },
    showAssociationParent: function() {
        this.associationParent.show(DropdownView.createSimpleDropdown({
            list: this.choices,
            defaultSelection: [this.model.get('parent') || this.choices[0].value],
            hasFiltering: true
        }));
        var relatedModel = this.associationParent.currentView.model;
        this.listenTo(relatedModel, 'change:value', getModelUpdateMethod(this.model, 'parent', relatedModel));
        this.listenTo(this.model, 'change:parent', getDropdownUpdateMethod(relatedModel, 'parent', this.model));
    },
    showAssociationRelationship: function() {
        this.associationRelationship.show(DropdownView.createSimpleDropdown({
            list: [{
                label: 'related to',
                value: 'related'
            }, {
                label: 'derived from',
                value: 'derived'
            }],
            defaultSelection: [this.model.get('relationship') || 'related']
        }));
        var relatedModel = this.associationRelationship.currentView.model;
        this.listenTo(relatedModel, 'change:value', getModelUpdateMethod(this.model, 'relationship', relatedModel));
        this.listenTo(this.model, 'change:relationship', getDropdownUpdateMethod(relatedModel, 'relationship', this.model));
    },
    showAssociationChild: function() {
        this.associationChild.show(DropdownView.createSimpleDropdown({
            list: this.choices,
            defaultSelection: [this.model.get('child') || this.choices[0].value],
            hasFiltering: true
        }));
        var relatedModel = this.associationChild.currentView.model;
        this.listenTo(relatedModel, 'change:value', getModelUpdateMethod(this.model, 'child', relatedModel));
        this.listenTo(this.model, 'change:child', getDropdownUpdateMethod(relatedModel, 'child', this.model));
    },
    setupReadOnlyLabelListeners: function() {
        this.listenTo(this.model, 'change:child', this.updateChildReadOnly);
        this.listenTo(this.model, 'change:relationship', this.updateRelationshipReadOnly);
        this.listenTo(this.model, 'change:parent', this.updateParentReadOnly);
    },
    updateChildReadOnly: function() {
        var currentMetacard = this.options.currentMetacard;
        var currentId = this.model.get('child');
        var label = currentMetacard.get('metacard').id === currentId ?
            'Current Metacard' : this.getChoiceById(currentId).label;
        this.$el.find('.association-child').attr('data-label', label);
        this.$el.find('.association-child-link a').attr('href', '#metacards/'+currentId).html(label);
    },
    updateParentReadOnly: function() {
        var currentMetacard = this.options.currentMetacard;
        var currentId = this.model.get('parent');
        var label = currentMetacard.get('metacard').id === currentId ?
            'Current Metacard' : this.getChoiceById(currentId).label;
        this.$el.find('.association-parent').attr('data-label', label);
        this.$el.find('.association-parent-link a').attr('href', '#metacards/'+currentId).html(label);
    },
    updateRelationshipReadOnly: function() {
        var currentMetacard = this.options.currentMetacard;
        var currentRelation = this.model.get('relationship') === 'related' ? 'related to' : 'derived from';
        this.$el.find('.association-relationship').attr('data-label', currentRelation);
    },
    updateReadOnlyLabels: function() {
        this.updateChildReadOnly();
        this.updateParentReadOnly();
        this.updateRelationshipReadOnly();
    },
    getChoiceById: function(id){
        return this.choices.filter(function(choice){
            return choice.value === id;
        })[0];
    }
});