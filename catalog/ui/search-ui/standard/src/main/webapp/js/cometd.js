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
/*global location*/
define(['jquery', 'jquerycometd'], function ($) {
    'use strict';

    var Cometd = {};

    Cometd.Comet = $.cometd;
    Cometd.Comet.websocketEnabled = false;
    var path = location.protocol + '//' + location.hostname+(location.port ? ':' + location.port : '') + '/cometd';
    Cometd.Comet.configure({
        url: path,
        maxNetworkDelay: 30000
//        logLevel: 'debug'
    });

    Cometd.Comet.onListenerException = function(exception, subscriptionHandle, isListener, message) {
        if (typeof console !== 'undefined') {
            console.error("Cometd listener threw an exception", exception, message, subscriptionHandle, isListener);
        }
    };

    //TODO: we need some way to unsub/disconnect when we know it is finished
    Cometd.Comet.handshake({});

    return Cometd;
});