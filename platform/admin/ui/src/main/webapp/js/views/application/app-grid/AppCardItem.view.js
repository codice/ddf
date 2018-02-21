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
    'js/wreqr.js',
    'text!applicationInfo'
    ],function (Marionette, ich, _, wreqr, applicationInfo) {
    "use strict";

    if(!ich.applicationInfo) {
        ich.addTemplate('applicationInfo', applicationInfo);
    }

    // List of apps that cannot have any actions performed on them through
    // the applications module
    var disableList = [
        'platform-app',
        'admin-app'
    ];

    // Itemview for each individual application
    var AppInfoView = Marionette.Layout.extend({
        template: 'applicationInfo',
        className: 'grid-cell',
        regions: {
            modalRegion: '.modal-region'
        },
        events: {
            'click': 'selectApplication',
        },

        // Will disable functionality for certain applications
        serializeData: function () {
            var that = this;
            var disable = false;
            disableList.forEach(function(child) {
                if(that.model.get('appId') === child) {
                    disable = true;
                }
            });

            return _.extend(this.model.toJSON(), {isDisabled: disable});
        },

        selectApplication: function(){
            wreqr.vent.trigger('application:reqestSelection',this.model);
        }
    });

    return AppInfoView;
});