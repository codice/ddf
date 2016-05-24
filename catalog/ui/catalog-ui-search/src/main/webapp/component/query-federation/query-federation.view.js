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
    'text!./query-federation.hbs',
    'js/CustomElements',
    'js/store'
], function (Marionette, _, $, template, CustomElements, store) {

    var federationToDescription = {
        enterprise: 'All Sources',
        selected: 'Specific Sources',
        local: 'None'
    };

    var descriptionToFederation = _.invert(federationToDescription);

    return Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('query-federation'),
        modelEvents: {
            'change': 'render'
        },
        events: {
            'click .choice': 'handleChoice'
        },
        ui: {
        },
        initialize: function(){
        },
        onRender: function(){
            this.handleValue();
        },
        handleValue: function(){
            var value = this.model.get('value');
            this.$el.find('[data-value]').removeClass('is-selected');
            this.$el.find('[data-value="'+federationToDescription[value]+'"]').addClass('is-selected');
        },
        handleChoice: function(e){
            var value = $(e.currentTarget).attr('data-value');
            this.model.set({
                value: descriptionToFederation[value]
            });
            this.$el.trigger('closeDropdown.'+CustomElements.getNamespace());
        },
        serializeData: function(){
            return descriptionToFederation;
        }
    });
});
