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
        'icanhaz',
        'underscore',
        'jquery',
        './featureRow.view',
        'text!featureTemplate'
],
    function(Marionette, ich, _, $, FeatureRowView, FeaturesTemplate){
        'use strict';

        if(!ich.featureTemplate) {
            ich.addTemplate('featureTemplate', FeaturesTemplate);
        }

        var FeaturesView = Marionette.CompositeView.extend({
            template: 'featureTemplate',
            itemViewContainer: 'tbody',
            itemView: FeatureRowView,

            initialize: function(options) {
              this.showWarnings = options.showWarnings;
            },

            serializeData: function() {
                var returnValue = {
                    showWarnings: this.showWarnings
                };

                return returnValue;
            }
        });

        return FeaturesView;
    }
);