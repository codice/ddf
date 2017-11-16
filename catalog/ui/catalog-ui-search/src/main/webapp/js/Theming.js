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
var $ = require('jquery');
var wreqr = require('wreqr');
var _ = require('underscore');
var user = require('component/singletons/user-instance.js');
var preferences = user.get('user').get('preferences');
var Less = require('less');
var lessStyles = require('./uncompiled-less.unless');
var variableRegex = '/@(.*:[^;]*)/g';
var variableRegexPrefix = '@';
var variableRegexPostfix = '(.*:[^;]*)';
var Common = require('js/Common');

function updateTheme(css) {
    var existingUserStyles = $('[data-theme=user]');
    var userStyles = document.createElement('style');
    userStyles.setAttribute('data-theme', 'user');
    userStyles.innerHTML = css;
    document.body.appendChild(userStyles);
    existingUserStyles.remove();
}

function handleThemeChange(){
    var theme = preferences.get('theme').getTheme();
    var newLessStyles = lessStyles;
    _.forEach(theme, (value, key) => {
        newLessStyles = newLessStyles.replace(new RegExp(variableRegexPrefix + key + variableRegexPostfix), function () {
            return '@'+key+': '+value+';';
        });
    });
    Less.render(newLessStyles, function(e, data){
        if (data !== undefined) {
            updateTheme(data.css);
            wreqr.vent.trigger('resize');
            $(window).trigger('resize');
        }
    });
}

function handleFontSizeChange() {
    var fontSize = preferences.get('fontSize');
    $('html').css('fontSize', fontSize + 'px');
    Common.repaintForTimeframe(500, () => {
        wreqr.vent.trigger('resize');
        $(window).trigger('resize');
    });
}

function handleAnimationChange() {
    var animationMode = preferences.get('animation');
    $('html').toggleClass('no-animation', !animationMode);
}

function attemptToStart() {
    if (user.fetched) {
        handleFontSizeChange();
        handleThemeChange();
        handleAnimationChange();
        preferences.on('change:fontSize', handleFontSizeChange);
        preferences.on('change:theme', handleThemeChange);
        preferences.on('change:animation', handleAnimationChange);
    } else {
        user.once('sync', function () {
            attemptToStart();
        });
    }
}

attemptToStart();

if (module.hot) {
    module.hot.accept('./uncompiled-less.unless', function() {
        lessStyles = require("./uncompiled-less.unless");
        handleThemeChange();
    });
}