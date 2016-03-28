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
    'jquery'
], function ($) {


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
        }
    };
});