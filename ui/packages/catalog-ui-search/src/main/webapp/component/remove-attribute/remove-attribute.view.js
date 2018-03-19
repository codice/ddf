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
var template = require('./remove-attribute.hbs');
var _ = require('underscore');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var PropertyView = require('component/property/property.view');
var Property = require('component/property/property');
var properties = require('properties');
var metacardDefinitions = require('component/singletons/metacard-definitions');

function determineAttributes(selectionInterface) {
    var types = _.union.apply(this, selectionInterface.getSelectedResults().map((result) => {
        return [result.get('metacardType')];
    }));
    var possibleAttributes = _.intersection.apply(this, types.map((type) => {
        return Object.keys(metacardDefinitions.metacardDefinitions[type]);
    }));
    var attributes = _.union.apply(this, selectionInterface.getSelectedResults().map((result) => {
        return Object.keys(result.get('metacard').get('properties').toJSON());
    })).filter((attribute) => possibleAttributes.indexOf(attribute) >= 0);
    return metacardDefinitions.sortMetacardTypes(attributes.map(attribute => metacardDefinitions.metacardTypes[attribute])
            .filter((definition) => (!definition.hidden && !definition.readOnly)))
        .map((definition) => ({
            label: definition.alias || definition.id,
            value: definition.id
        }));
}

module.exports = Marionette.LayoutView.extend({
    tagName: CustomElements.register('remove-attribute'),
    template: template,
    regions: {
        attributeSelector: '> .attribute-selector'
    },
    removeAttribute: function(){
        this.model.set('value', this.attributeSelector.currentView.model.get('value'));
    },
    onBeforeShow: function () {
        var attributes = determineAttributes(this.options.selectionInterface);
        this.attributeSelector.show(new PropertyView({
            model: new Property({
                enum: attributes,
                id: 'Attributes To Remove',
                value: [
                    []
                ],
                showValidationIssues: false,
                enumFiltering: true,
                enumMulti: true,
                onlyEditing: true
            })
        }));
        this.attributeSelector.currentView.turnOnEditing();
        this.attributeSelector.currentView.turnOnLimitedWidth();
        this.listenTo(this.attributeSelector.currentView.model, 'change:value', this.removeAttribute);
    }
});