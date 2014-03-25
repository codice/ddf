/*global define*/
define(function (require) {
    var Backbone = require('backbone');

    var AppModel = Backbone.Model.extend({
        url: "/services/admin/config"
    });

    return AppModel;
});