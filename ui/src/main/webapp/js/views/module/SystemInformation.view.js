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
        'moment',
        'text!systemInformationTemplate',
        'js/util/TimeUtil',
        'js/util/UnitsUtil',
        'icanhaz'
    ],
    function(Marionette, _, $, moment, SystemInformationTemplate, TimeUtil, UnitsUtil, ich){
        'use strict';

        if(!ich.systemInformationTemplate) {
            ich.addTemplate('systemInformationTemplate', SystemInformationTemplate);
        }


        var FeaturesView = Marionette.ItemView.extend({
            template: 'systemInformationTemplate',

            serializeData: function() {
                var systemData = this.options.systemInformation.toJSON();
                var operatingSystemData = this.options.operatingSystem.toJSON();
                var uptime = TimeUtil.convertUptimeToString(systemData.Uptime);
                var usedMemory =  UnitsUtil.convertBytesToDisplay(operatingSystemData.TotalPhysicalMemorySize - operatingSystemData.FreePhysicalMemorySize);
                var totalMemory = UnitsUtil.convertBytesToDisplay(operatingSystemData.TotalPhysicalMemorySize);
                var freeMemory = UnitsUtil.convertBytesToDisplay(operatingSystemData.FreePhysicalMemorySize);
                var startTime = moment(systemData.StartTime).toDate();

                var returnValue = {
                    systemInformation: systemData,
                    operatingSystem: operatingSystemData,
                    startTime: startTime,
                    uptime: uptime,
                    usedMemory: usedMemory,
                    totalMemory: totalMemory,
                    freeMemory : freeMemory,
                    runtime: systemData.SystemProperties['java.runtime.name'],
                    runtimeVersion: systemData.SystemProperties['java.runtime.version']
                };

                return returnValue;
            }
        });

        return FeaturesView;
    }
);