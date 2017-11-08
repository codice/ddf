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
var _get = require('lodash.get');
var properties = require('properties');

var spacingVariables = ['minimumButtonSize', 'minimumLineSize', 'minimumSpacing'];
var colorVariables = ['baseColor', 'primary-color', 'positive-color', 'negative-color', 'warning-color', 'favorite-color', 'baseGrey', 'text-color', 'button-text-color', 'links-visited-color', 'links-color'];
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
        'favorite-color': 'orange',
    },
    custom: {}
};

module.exports = Backbone.Model.extend({
    defaults: function() {
        return {
            spacingMode: _get(properties, 'spacingMode', 'comfortable'),
            colorMode: 'dark',
            backgroundColor: _get(properties, 'backgroundColor', colorModes.dark['baseGrey']),
            textColor: _get(properties, 'textColor', colorModes.dark['text-color']),
            buttonTextColor: _get(properties, 'buttonTextColor', colorModes.dark['button-text-color']),
            primaryColor: _get(properties, 'primaryColor', colorModes.dark['primary-color']),
            positiveColor: _get(properties, 'positiveColor', colorModes.dark['positive-color']),
            negativeColor: _get(properties, 'negativeColor', colorModes.dark['negative-color']),
            warningColor: _get(properties, 'warningColor', colorModes.dark['warning-color']),
            favoriteColor: _get(properties, 'favoriteColor', colorModes.dark['favorite-color']),
            linksVisitedColor: _get(properties, 'linksVisitedColor', colorModes.dark['links-visited-color']),
            linksColor: _get(properties, 'linksColor', colorModes.dark['links-color'])
        };
    },
    initialize: function() {
    },
    getTheme: function(){
        var theme = this.toJSON();

        colorModes.dark['baseGrey'] = theme.backgroundColor;
        colorModes.dark['text-color'] = theme.textColor;
        colorModes.dark['button-text-color'] = theme.buttonTextColor;
        colorModes.dark['primary-color'] = theme.primaryColor;
        colorModes.dark['positive-color'] = theme.positiveColor;
        colorModes.dark['negative-color'] = theme.negativeColor;
        colorModes.dark['warning-color'] = theme.warningColor;
        colorModes.dark['favorite-color'] = theme.favoriteColor;
        colorModes.dark['links-visited-color'] = theme.linksVisitedColor;
        colorModes.dark['links-color'] = theme.linksColor;

        return _.defaults(theme, spacingModes[theme.spacingMode], colorModes[theme.colorMode]);
    },
    getColorMode: function(){
        return this.get('colorMode');
    },
    getSpacingMode: function(){
        return this.get('spacingMode');
    },
    getBackgroundColor: function(){
        return this.get('backgroundColor');
    },
    getTextColor: function(){
        return this.get('textColor');
    },
    getButtonTextColor: function(){
        return this.get('buttonTextColor');
    },
    getPrimaryColor: function(){
        return this.get('primaryColor');
    },
    getPositiveColor: function(){
        return this.get('positiveColor');
    },
    getNegativeColor: function(){
        return this.get('negativeColor');
    },
    getWarningColor: function(){
        return this.get('warningColor');
    },
    getFavoriteColor: function(){
        return this.get('favoriteColor');
    },
    getLinksVisitedColor: function(){
        return this.get('linksVisitedColor');
    },
    getLinksColor: function(){
        return this.get('linksColor');
    }

});

if (module.hot) {
    module.hot.accept('js/uncompiled-less.unless', function() {
        lessStyles = require("js/uncompiled-less.unless");
    });
}