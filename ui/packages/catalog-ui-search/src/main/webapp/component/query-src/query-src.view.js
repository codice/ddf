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
    './query-src.hbs',
    'js/CustomElements',
    'component/singletons/sources-instance'
], function (Marionette, _, $, template, CustomElements, sources) {

    return Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('query-src'),
        className: 'is-action-list',
        modelEvents: {
            'change': 'render'
        },
        events: {
            'click .choice:not(.is-all)': 'handleChoice',
            'click .choice.is-all': 'handleChoiceAll'
        },
        ui: {
        },
        initialize: function(){
        },
        onRender: function(){
            this.handleValue();
        },
        handleValue: function(){
            switch(this.model.get('federation')){
                case 'enterprise':
                    this.$el.find('.choice.is-all').addClass('is-selected');
                    break;
                case 'selected':
                    var srcs = this.model.get('value');
                    srcs.forEach(function(src){
                        this.$el.find('[data-value="'+src+'"]').addClass('is-selected');
                    }.bind(this));
                    break;
                default: 
                    break;
            }
        },
        handleChoiceAll: function(e){
            $(e.currentTarget).toggleClass('is-selected');
            this.model.set({
                federation: this.model.get('federation') === 'enterprise' ? 'selected' : 'enterprise'
            });
        },
        handleChoice: function(e){
            $(e.currentTarget).toggleClass('is-selected');
            this.updateValue();
        },
        updateValue: function(){
            var srcs = _.map(this.$el.find('.is-specific.is-selected'), function(choice){
                 return $(choice).attr('data-value');
            });
            this.model.set({
                value: srcs,
                federation: 'selected'
            });
        },
        serializeData: function(){
            return sources.toJSON();
        }
    });
});
