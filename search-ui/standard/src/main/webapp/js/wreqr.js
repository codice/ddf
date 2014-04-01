/*global define*/

define(['backbone', 'marionette'], function (Backbone) {
    'use strict';
    var wreqr = {};

    wreqr.vent = new Backbone.Wreqr.EventAggregator();
    wreqr.commands = new Backbone.Wreqr.Commands();
    wreqr.reqres = new Backbone.Wreqr.RequestResponse();

    return wreqr;
});