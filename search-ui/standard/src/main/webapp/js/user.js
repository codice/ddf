/*global define*/

define([
    'backbone'
    ], function (Backbone) {
    'use strict';

    var UserModel = Backbone.Model.extend({
        url: '/search/standard/user',
        useAjaxSync: true
    });

    return UserModel;
});
