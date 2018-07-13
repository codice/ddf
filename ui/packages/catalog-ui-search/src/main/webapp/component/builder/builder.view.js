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

const Backbone = require('backbone');
const Marionette = require('marionette');
const _ = require('underscore');
const $ = require('jquery');
const template = require('./builder.hbs');
const CustomElements = require('js/CustomElements');
const PropertyCollectionView = require('component/property/property.collection.view');
const LoadingCompanionView = require('component/loading-companion/loading-companion.view');
const DropdownView = require('component/dropdown/dropdown.view');
const metacardDefinitions = require('component/singletons/metacard-definitions');
const PropertyView = require('component/property/property.view');
const Property = require('component/property/property');

let availableTypes;

const ajaxCall = $.get({
    url: './internal/builder/availabletypes'
}).then((response) => {
    availableTypes = response;
});

module.exports = Marionette.LayoutView.extend({
    template,
    tagName: CustomElements.register('builder'),
    modelEvents: {
        'change:metacard': 'handleMetacard'
    },
    events: {
        'click .builder-edit': 'edit',
        'click .builder-save': 'save',
        'click .builder-cancel': 'cancel'
    },
    regions: {
        builderProperties: '> .builder-properties',
        builderAvailableType: '> .builder-select-available-type > .builder-select-available-type-dropdown'
    },
    initialize(options) {

        if (!availableTypes) {
            const loadingview = new LoadingCompanionView();
            ajaxCall.then(() => {
                loadingview.remove();
                this.model.set('availableTypes', availableTypes);
                this.handleAvailableTypes();
            });
        } else {
            this.model.set('availableTypes', availableTypes);
        }

    },
    isSingleAvailableType() {
        const availableTypes = this.model.get('availableTypes');
        return availableTypes && availableTypes.availabletypes && availableTypes.availabletypes.length === 1;
    },
    isMultipleAvailableTypes() {
        const availableTypes = this.model.get('availableTypes');
        return availableTypes && availableTypes.availabletypes && availableTypes.availabletypes.length > 1;
    },
    showMetacardTypeSelection() {
        const enums = this.model.get('availableTypes').availabletypes.map((availableType) => ({ label: availableType.metacardType, value: availableType.metacardType }));

        const availableTypesModel = new Property({
            label: "Select An Available Metacard Type",
            value: [this.model.get('availableTypes').availabletypes[0].metacardType],
            enum: enums,
            id: "Select Metacard Type"
        });

        this.builderAvailableType.show(new PropertyView({
            model: availableTypesModel
        }));

        this.builderAvailableType.currentView.turnOnEditing();

        this.listenTo(availableTypesModel, 'change:value', this.handleSelectedAvailableType);

        this.$el.addClass('is-selecting-available-types');
    },
    handleSystemTypes() {

        const mds = metacardDefinitions.metacardDefinitions;

        const allTypes = Object.keys(mds)
            .sort()
            .reduce((accumulator, currentValue) => {
                const visibleAttributes = Object.keys(mds[currentValue]);
                accumulator.availabletypes.push({ metacardType: currentValue, visibleAttributes: visibleAttributes});
                return accumulator;
            }, { availabletypes: [] });

        this.model.set('availableTypes', allTypes);

        this.showMetacardTypeSelection();

    },
    handleAvailableTypes() {

        const availableTypes = this.model.get('availableTypes');

        if(this.isSingleAvailableType()) {
            this.model.set('selectedAvailableType', this.model.get('availableTypes').availabletypes[0]);
            this.showMetacardBuilder();
        } else if(this.isMultipleAvailableTypes()) {
            this.showMetacardTypeSelection();
        } else {
            this.handleSystemTypes();
        }
    },
    handleSelectedAvailableType() {
        this.$el.removeClass('is-selecting-available-types');

        const selectedAvailableType = this.builderAvailableType.currentView.model.getValue()[0];

        const availableTypes = this.model.get('availableTypes').availabletypes;

        this.model.set('selectedAvailableType', availableTypes.filter((availableType) => availableType.metacardType === selectedAvailableType)[0]);

        this.showMetacardBuilder();
    },
    showMetacardBuilder() {

        const availableTypes = this.model.get('availableTypes').availabletypes;

        const selectedAvailableType = this.model.get('selectedAvailableType');

        const metacardDefinition = metacardDefinitions.metacardDefinitions[selectedAvailableType.metacardType];

        const propertyCollection = {
            'metacard-type': selectedAvailableType.metacardType
        };

        selectedAvailableType.visibleAttributes
            .filter((attribute) => !metacardDefinitions.isHiddenType(attribute))
            .filter((attribute) => !metacardDefinition[attribute].readOnly)
            .filter((attribute) => attribute !== "id")
            .forEach((attribute) => {
            if (metacardDefinition[attribute].multivalued) {
              propertyCollection[attribute] = [];
            } else if (metacardDefinitions.enums[attribute]) {
              propertyCollection[attribute] = metacardDefinitions.enums[attribute][0];
            } else {
              propertyCollection[attribute] = undefined;
            }
        });

        this.model.set('metacard', propertyCollection);
        this.handleMetacard();
        this.$el.addClass('is-building');

    },
    handleMetacard() {
        const metacard = this.model.get('metacard');
        this.builderProperties.show(PropertyCollectionView.generatePropertyCollectionView([metacard]));
        this.builderProperties.currentView.$el.addClass("is-list");

    },
    onBeforeShow() {
        this.handleAvailableTypes();
    },
    edit() {
        this.$el.addClass('is-editing');
        this.builderProperties.currentView.turnOnEditing();
        this.builderProperties.currentView.focus();
    },
    cancel() {
        this.$el.removeClass('is-editing');
        this.builderProperties.currentView.revert();
        this.builderProperties.currentView.turnOffEditing();
    },
    save() {
        this.$el.removeClass('is-editing');

        const metacardType = this.model.get('selectedAvailableType').metacardType;

        const metacardDefinition = metacardDefinitions.metacardDefinitions[metacardType];

        const editedMetacard = this.builderProperties.currentView.toPropertyJSON();

        const props = editedMetacard.properties;
        editedMetacard.properties = Object.keys(editedMetacard.properties)
            .filter((attributeName) => props[attributeName].length >= 1)
            .filter((attributeName) => props[attributeName][0] !== "")
            .reduce((accummulator, currentValue) => _.extend(accummulator, {
                [currentValue]:  metacardDefinition[currentValue].multivalued ? props[currentValue] : props[currentValue][0]
            }), {});

        editedMetacard.properties['metacard-type'] = metacardType;
        editedMetacard.type = 'Feature';

        $.ajax({
            type: 'POST',
            url: './internal/catalog/?transform=geojson',
            data: JSON.stringify(editedMetacard),
            contentType: 'application/json'
        }).then((response, status, xhr) => {
            const id = xhr.getResponseHeader('id');
            if (id) {
                this.options.handleNewMetacard(id);
                this.options.close();
            }
        });

        this.builderProperties.currentView.turnOffEditing();
    }
});
