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
var template = require('./search-type.hbs');
var _ = require('underscore');
var Marionette = require('marionette');
var CustomElements = require('js/CustomElements');
var PropertyView = require('component/property/property.view');
var Property = require('component/property/property');
var user = require('component/singletons/user-instance');

module.exports = Marionette.LayoutView.extend({
    tagName: CustomElements.register('search-type'),
    template: template,
    regions: {
        typeSelector: '> .type-selector'
    },
    updateQueryType: function(){
        let type = this.typeSelector.currentView.model.getValue()[0];
        this.model.set('type', type);
        user.getQuerySettings().set('type', type);
        user.savePreferences();
    },
    onBeforeShow: function () {
        this.typeSelector.show(new PropertyView({
            model: new Property({
                enum: [
                    {
                        label: 'Text',
                        value: 'text'
                    },
                    {
                        label: 'Basic',
                        value: 'basic'
                    },
                    {
                        label: 'Advanced',
                        value: 'advanced'
                    }
                ],
                id: 'Search Type',
                value: [
                    this.model.get('type')
                ],
                enumFiltering: true
            })
        }));
        this.typeSelector.currentView.turnOnEditing();
        this.typeSelector.currentView.turnOnLimitedWidth();
        this.listenTo(this.typeSelector.currentView.model, 'change:value', this.updateQueryType);
    }
});