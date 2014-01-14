/*global define*/
/*global location*/
define(function (require) {
    'use strict';

    // Load attached libs and application modules
    var $ = require('jquery'),
        ddf = require('ddf'),
        Cometd = ddf.module();

    Cometd.Comet = $.cometd;
    var path = location.protocol + '//' + location.hostname+(location.port ? ':' + location.port : '') + '/cometd';
    Cometd.Comet.configure({
        url: path,
        logLevel: 'debug'
    });
    //TODO: we need some way to unsub/disconnect when we know it is finished
    Cometd.Comet.handshake({});

    return Cometd;
});