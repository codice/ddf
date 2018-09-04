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
var template = require('./hide-attribute.hbs');
var _ = require('underscore');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var PropertyView = require('component/property/property.view');
var Property = require('component/property/property');
var properties = require('properties');
var metacardDefinitions = require('component/singletons/metacard-definitions');
var user = require('component/singletons/user-instance');

function filterAndSort(attributes){
    return attributes.filter(function (property) {
        if (metacardDefinitions.metacardTypes[property]) {
            return !metacardDefinitions.metacardTypes[property].hidden;
        } else {
            announcement.announce({
                title: 'Missing Attribute Definition',
                message: 'Could not find information for ' + property + ' in definitions.  If this problem persists, contact your Administrator.',
                type: 'warn'
            });
            return false;
        }
    }).sort((a, b) => metacardDefinitions.attributeComparator(a, b));
}

function calculateAvailableAttributesFromSelection(selectionInterface) {
    var types = _.union.apply(this, selectionInterface.getSelectedResults().map((result) => {
        return [result.get('metacardType')];
    }));
    var possibleAttributes = _.intersection.apply(this, types.map((type) => {
        return Object.keys(metacardDefinitions.metacardDefinitions[type]);
    }));
    return selectionInterface.getSelectedResults().reduce(function (currentAvailable, result) {
        currentAvailable = _.union(currentAvailable, Object.keys(result.get('metacard').get('properties').toJSON()));
        return currentAvailable;
    }, []).filter((attribute) => possibleAttributes.indexOf(attribute) >= 0);
}

function calculateDetailsAttributes() {
    var userPropertyArray = user.get('user').get('preferences').get('inspector-detailsHidden');
    return userPropertyArray;
}

module.exports = Marionette.LayoutView.extend({
    tagName: CustomElements.register('hide-attribute'),
    template: template,
    regions: {
        attributeSelector: '> .attribute-selector'
    },
    onBeforeShow: function () {
        var attributes = calculateAvailableAttributesFromSelection(this.options.selectionInterface);
        var detailsAttributes = calculateDetailsAttributes();
        var totalAttributes = filterAndSort(_.union(attributes, detailsAttributes));
        var detailsHidden = user.get('user').get('preferences').get('inspector-detailsHidden');
        this.attributeSelector.show(new PropertyView({
            model: new Property({
                enum: totalAttributes.map((attr) => {
                    return {
                        label: metacardDefinitions.getLabel(attr),
                        value: attr
                    };
                }),
                id: 'Attributes To Hide',
                value: [
                    detailsHidden
                ],
                showValidationIssues: false,
                enumFiltering: true,
                enumMulti: true
            })
        }));
        this.attributeSelector.currentView.turnOnEditing();
        this.listenTo(this.attributeSelector.currentView.model, 'change:value', this.handleSave);
    },
    handleSave: function () {
        var prefs = user.get('user').get('preferences');
        prefs.set('inspector-detailsHidden', this.attributeSelector.currentView.model.get('value')[0]);
        prefs.savePreferences();
    }
});