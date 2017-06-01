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
/*jshint latedef: nofunc*/
/*
    Chrome only lets us pull off this trick once per loaded iframe.  So we need to detach and reattach it after every submit.
    Luckily, we can put it on the load event, and keep the logic centralized here.
*/

var $ = require('jquery');

function waitForInitialAttachLoad($iframe){
    $iframe.on('load', function(){
        $iframe.off('load');
        attachSubmitListener($iframe);
    });
}

function attachSubmitListener($iframe){
    $iframe.on('load', function(){
        $iframe.off('load');
        $iframe.detach();
        waitForInitialAttachLoad($iframe);
        $('body').append($iframe);
    });
}

var $iframe = $('iframe[name=autocomplete]');
attachSubmitListener($iframe);
