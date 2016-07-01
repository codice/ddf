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
    'text!./query-src.hbs',
    'js/CustomElements',
    'component/singletons/sources-instance'
], function (Marionette, _, $, template, CustomElements, sources) {

    return Marionette.ItemView.extend({
        template: template,
        tagName: CustomElements.register('query-src'),
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
            var srcs = this.model.get('value');
            this.$el.find('[data-value]').removeClass('is-selected');
            srcs.forEach(function(src){
                this.$el.find('[data-value="'+src+'"]').addClass('is-selected');
            }.bind(this));
        },
        handleChoice: function(e){
            $(e.currentTarget).toggleClass('is-selected');
            this.updateValue();
        },
        updateValue: function(){
            var srcs = _.map(this.$el.find('.is-selected'), function(choice){
                 return $(choice).attr('data-value');
            });
            this.model.set({
                value: srcs
            });
        },
        serializeData: function(){
            return sources.toJSON();
        }
    });
});
