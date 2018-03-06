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
/*global define*/
define([
], function () {

    function getRandomColor() {
        var letters = '789ABCD';
        var color = '#';
        for (var i = 0; i < 6; i++) {
            color += letters[Math.floor(Math.random() * 6)];
        }
        return color;
    }

    return {
        getNewGenerator: function(){
            var colors = [
                // http://colorbrewer2.org/?type=qualitative&scheme=Paired&n=10
                '#cab2d6',
                '#6a3d9a',
                '#fdbf6f',
                '#063640',
                '#dfc27d',
                '#8c510a',
                '#b2df8a',
                '#33a02c',
                '#a6cee3',
                '#1f78b4'
            ];
            var idToColor = {};

            return {
                getColor: function(id){
                    if (idToColor[id] === undefined){
                        if (colors.length === 0){
                            idToColor[id] = undefined;
                            //throw "Generator is out of colors to assign.";
                        }
                        idToColor[id] = colors.pop();
                    }
                    return idToColor[id];
                },
                removeColor: function(id){
                    var color = idToColor[id];
                    if (color !== undefined){
                        colors.push(color);
                        delete idToColor[id];
                    }
                }
            };
        }
    };
});