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
/*global define, window, performance, setTimeout*/
/*jshint bitwise: false*/
define([
    'jquery',
    'moment',
    'js/requestAnimationFramePolyfill'
], function ($, moment) {

    var format = 'DD MMM YYYY HH:mm:ss.SSS';

    return {
        //randomly generated guid guaranteed to be unique ;)
        undefined: '2686dcb5-7578-4957-974d-aaa9289cd2f0',
        coreTransitionTime: 250,
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
            return moment(date).format(this.getDateFormat());
        },
        getDateFormat: function(){
            return format;
        },
        getMomentDate: function(date){
           return moment(date).fromNow();
        },
        getImageSrc: function(img){
            if (img === "" || img.substring(0, 4) === 'http') {
                return img;
            } else {
                return "data:image/png;base64," + img;
            }
        },
        cancelRepaintForTimeframe: function(requestDetails){
            if (requestDetails) {
                window.cancelAnimationFrame(requestDetails.requestId);
            }
        },
        repaintForTimeframe: function(time, callback){
            var requestDetails = {
                requestId: undefined
            };
            var timeEnd = Date.now() + time;
            var repaint = function(){
                callback();
                if (Date.now() < timeEnd){
                    requestDetails.requestId = window.requestAnimationFrame(function(){
                        repaint();
                    });
                }
            };
            requestDetails.requestId = window.requestAnimationFrame(function(){
                repaint();
            });
            return requestDetails;
        },
        executeAfterRepaint: function(callback){
            return window.requestAnimationFrame(function(){
                window.requestAnimationFrame(callback);
            });
        },
        queueExecution: function(callback){
            return setTimeout(callback, 0);
        },
        escapeHTML: function(value){
            return $("<div>").text(value).html();
        },
        duplicate: function(reference){
            return JSON.parse(JSON.stringify(reference));
        }
    };
});