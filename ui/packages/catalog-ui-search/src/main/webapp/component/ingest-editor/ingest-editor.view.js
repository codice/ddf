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
    'js/store',
    './ingest-editor.hbs',
    'js/CustomElements',
    'component/property/property.collection.view',
    'properties'
], function (Marionette, _, $, store, template, CustomElements, PropertyCollectionView, properties) {

    return Marionette.LayoutView.extend({
        setDefaultModel: function() {
            //override
        },
        template: template,
        tagName: CustomElements.register('ingest-editor'),
        modelEvents: {
        },
        events: {
            'click .ingest-editor-clear': 'clear'
        },
        regions: {
            editorProperties: '.ingest-editor-properties'
        },
        initialize: function () {
            //override
        },
        onBeforeShow: function() {
            this.editorProperties.show(PropertyCollectionView.generateFilteredPropertyCollectionView(
                properties.editorAttributes,
                [],
            ));
            this.editorProperties.currentView.$el.addClass("is-list");
            this.$el.addClass('is-editing');
            this.editorProperties.currentView.turnOnEditing();
        },
        clear: function() {
            this.editorProperties.currentView.revert();
        },
        /*
            Return a map of attributes to their corresponding value arrays. Empty values are
            filtered, and only attributes with at least one non-empty value are returned.
         */
        toJSON: function() {
            const attributeMap = this.editorProperties.currentView.toPropertyJSON().properties;
            return Object.keys(attributeMap).reduce(function(filteredMap, attribute) {
                const values = attributeMap[attribute].filter((a) => a.trim().length > 0);
                return (values.length > 0)
                    ? _.extend(filteredMap, { [attribute]: values })
                    : filteredMap;
            }, {});
        }
    });
});
