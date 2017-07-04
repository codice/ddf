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
/*global require*/
var Backbone = require('backbone');
var _ = require('underscore');
var lessStyles = require('js/uncompiled-less.unless');
var lessToJs = require('less-vars-to-js');
var _ = require('underscore');

var spacingVariables = ['minimumButtonSize', 'minimumLineSize', 'minimumSpacing'];
var colorVariables = ['baseColor', 'primary-color', 'positive-color', 'negative-color', 'warning-color', 'favorite-color'];
var themeableVariables = spacingVariables.concat(colorVariables);

function trimVariables(variables){
    var newVariableMap = {};
    _.forEach(variables, (value, key) => {
        var trimmedKey = key.substring(1);
        if (themeableVariables.indexOf(trimmedKey) !== -1) {
            newVariableMap[trimmedKey] = value;
        }
    });
    return newVariableMap;
}

var baseVariables = trimVariables(lessToJs(lessStyles));
var comfortableVariables = _.pick(baseVariables, spacingVariables);
var compactVariables = {
    minimumButtonSize: '1.8rem',
    minimumLineSize: '1.5rem',
    minimumSpacing: '0.3rem'
};

// spacing is set in stone as three choices
// coloring provides some themes, and allows infinite customization

var spacingModes = {
    compact: compactVariables, 
    cozy: _.reduce(comfortableVariables, (result, value, key) => {
        result[key] = ((parseFloat(value)+parseFloat(compactVariables[key]))/2) + 'rem';
        return result;
    }, {}),
    comfortable: comfortableVariables
};

var colorModes = {
    dark: _.pick(baseVariables, colorVariables),
    light: {
        baseColor: 'white',
        'primary-color': 'blue',
        'positive-color': 'blue',
        'negative-color': 'red',
        'warning-color': 'yellow',
        'favorite-color': 'orange'
    },
    custom: {}
};

module.exports = Backbone.Model.extend({
    defaults: function() {
        return {
            spacingMode: 'comfortable',
            colorMode: 'dark'
        };
    },
    initialize: function() {
    },
    getTheme: function(){
        var theme = this.toJSON();
        return _.defaults(theme, spacingModes[theme.spacingMode], colorModes[theme.colorMode]);
    },
    getColorMode: function(){
        return this.get('colorMode');
    },
    getSpacingMode: function(){
        return this.get('spacingMode');
    }
});

if (module.hot) {
    module.hot.accept('js/uncompiled-less.unless', function() {
        lessStyles = require("js/uncompiled-less.unless");
    });
}