/*global define*/
/*global location*/
define(['jquery', 'jquerycometd'], function ($) {
    'use strict';

    var Cometd = {};

    Cometd.Comet = $.cometd;
    Cometd.Comet.websocketEnabled = false;
    var path = location.protocol + '//' + location.hostname+(location.port ? ':' + location.port : '') + '/cometd';
    Cometd.Comet.configure({
        url: path
//        logLevel: 'debug'
    });
    //TODO: we need some way to unsub/disconnect when we know it is finished
    Cometd.Comet.handshake({});

    return Cometd;
});