/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define, window, performance*/
/*jshint bitwise: false*/
define([
    'jquery',
    'moment',
    'js/requestAnimationFramePolyfill'
], function ($, moment) {

    return {
        generateUUID: function(){
            var d = new Date().getTime();
            if(window.performance && typeof window.performance.now === "function"){
                d += performance.now(); //use high-precision timer if available
            }
            var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = (d + Math.random()*16)%16 | 0;
                d = Math.floor(d/16);
                return (c==='x' ? r : (r&0x3|0x8)).toString(16);
            });
            return uuid;
        },
        cqlToHumanReadable: function(cql){
            if (cql===undefined){
                return cql;
            }
            cql = cql.replace(new RegExp('anyText ILIKE ','g'),'~');
            cql = cql.replace(new RegExp('anyText LIKE ','g'),'');
            cql = cql.replace(new RegExp('AFTER','g'),'>');
            cql = cql.replace(new RegExp('DURING','g'),'BETWEEN');
            return cql;
        },
        setupPopOver: function ($component) {
            $component.find('[title]').each(function(){
                var $element = $(this);
                $element.popover({
                    delay: {
                        show: 1000,
                        hide: 0
                    },
                    trigger: 'hover'
                });
            });
        },
        getHumanReadableDate: function(date) {
            var format = 'DD MMM YYYY HH:mm:ss.SSS';
            return moment(date).format(format);
        },
        getNiceDate: function(date){
            var niceDiff;
            var dateModified = new Date(date);
            var diffMs = (new Date()) - dateModified;
            var diffDays = Math.round(diffMs / 86400000); // days
            var diffHrs = Math.round((diffMs % 86400000) / 3600000); // hours
            var diffMins = Math.round(((diffMs % 86400000) % 3600000) / 60000);
            if (diffDays > 2){
                niceDiff = dateModified.toDateString() + ', ' + dateModified.toLocaleTimeString();
            } else if (diffDays > 0) {
                niceDiff = 'Yesterday, ' + dateModified.toLocaleTimeString();
            } else if (diffHrs > 4) {
                niceDiff = 'Today, ' + dateModified.toLocaleTimeString();
            } else if (diffHrs > 1){
                niceDiff = diffHrs + ' hours ago';
            } else if (diffMins > 0){
                niceDiff = diffMins + ' minutes ago';
            } else {
                niceDiff = 'A few seconds ago';
            }
            return niceDiff;
        },
        getMomentDate: function(date){
           return moment(date).fromNow();
        },
        repaintForTimeframe: function(time, callback){
            var timeEnd = Date.now() + time;
            var repaint = function(){
                callback();
                if (Date.now() < timeEnd){
                    window.requestAnimationFrame(function(){
                        repaint();
                    });
                }
            };
            window.requestAnimationFrame(function(){
                repaint();
            });
        }
    };
});