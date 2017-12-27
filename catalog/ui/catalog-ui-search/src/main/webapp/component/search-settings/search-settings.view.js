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
/*global define, setTimeout*/
define([
    'marionette',
    'underscore',
    'properties',
    'jquery',
    './search-settings.hbs',
    'js/CustomElements',
    'component/singletons/user-instance',
    'component/property/property.view',
    'component/property/property',
    'component/query-settings/query-settings.view',
    'js/model/Query'
], function (Marionette, _, properties, $, template, CustomElements, user, PropertyView, Property, QuerySettingsView, QueryModel) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('search-settings'),
        regions: {
            propertyResultCount: '.property-result-count',
            propertySearchSettings: '.property-search-settings'
        },
        onBeforeShow: function () {
            this.setupResultCount();
            this.setupSearchSettings();
            this.listenToOnce(this.regionManager, 'before:remove:region', this.save);
        },
        setupSearchSettings: function() {
            this.propertySearchSettings.show(new QuerySettingsView({
                model: new QueryModel.Model()
            }));
        },
        setupResultCount: function () {
            var userResultCount = user.get('user').get('preferences').get('resultCount');

            this.propertyResultCount.show(new PropertyView({
                model: new Property({
                    label: "Number of Search Results",
                    value: [userResultCount],
                    min: 1,
                    max: properties.resultCount,
                    type: 'RANGE'
                })
            }));

            this.propertyResultCount.currentView.turnOnLimitedWidth();
            this.propertyResultCount.currentView.turnOnEditing();
        },
        updateSearchSettings: function() {
            user.getPreferences().get('querySettings').set(this.propertySearchSettings.currentView.toJSON());
        },
        updateResultCountSettings: function () {
            user.getPreferences().get({
                resultCount: this.propertyResultCount.currentView.model.getValue()[0]
            });
        },
        save: function() {
            this.updateResultCountSettings();
            this.updateSearchSettings();
            user.savePreferences();
        }
    });
});
