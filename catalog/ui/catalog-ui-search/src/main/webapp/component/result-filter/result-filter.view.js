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
    'text!./result-filter.hbs',
    'js/CustomElements',
    'component/singletons/user-instance',
    'component/filter-builder/filter-builder.view',
    'component/filter-builder/filter-builder',
    'js/cql'
], function (Marionette, _, $, template, CustomElements, user, FilterBuilderView, FilterBuilderModel, cql) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('result-filter'),
        modelEvents: {
            'change': 'render'
        },
        events: {
            'click > .editor-footer .footer-remove': 'removeFilter',
            'click > .editor-footer .footer-save': 'saveFilter'
        },
        ui: {
        },
        regions: {
            editorProperties: '.editor-properties'
        },
        initialize: function(){
        },
        onRender: function(){
            this.editorProperties.show(new FilterBuilderView({
                model: new FilterBuilderModel()
            }));
            this.editorProperties.currentView.turnOnEditing();
            this.editorProperties.currentView.turnOffNesting();
            var resultFilter = user.get('user').get('preferences').get('resultFilter');
            if (resultFilter){
                this.editorProperties.currentView.deserialize(cql.simplify(cql.read(resultFilter)));
            } else {
                this.editorProperties.currentView.deserialize({
                    property: 'anyText',
                    value: '',
                    type: 'ILIKE'
                });
            }
            this.handleFilter();
        },
        removeFilter: function(){
            user.get('user').get('preferences').set('resultFilter', undefined);
            user.get('user').get('preferences').savePreferences();
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        },
        saveFilter: function(){
            user.get('user').get('preferences').set('resultFilter', this.editorProperties.currentView.transformToCql());
            user.get('user').get('preferences').savePreferences();
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        },
        handleFilter: function(){
            var resultFilter = user.get('user').get('preferences').get('resultFilter');
            this.$el.toggleClass('has-filter', Boolean(resultFilter));
        }
    });
});
