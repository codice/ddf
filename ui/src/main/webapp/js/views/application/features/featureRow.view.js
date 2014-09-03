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
        'jquery',
        'marionette',
        'underscore',
        'text!featureRowTemplate',
        'icanhaz'
], function($,Marionette, _, FeatureRowTemplate, ich){
        "use strict";

        if(!ich.featureRowTemplate) {
            ich.addTemplate('featureRowTemplate', FeatureRowTemplate);
        }

        var FeatureRow = Marionette.ItemView.extend({
            template: 'featureRowTemplate',
            tagName: 'tr',

            events: {
                'click .fa-stack': 'onSelect'
            },

            onSelect: function() {
                var view = this;
                view.trigger('selected', view.model);
                view.$('.fa-stack').toggleClass('active', true);
            },

            onBeforeClose: function() {
                this.$el.off('click', this.onSelect);
            }

        });

        return FeatureRow;

    }
);