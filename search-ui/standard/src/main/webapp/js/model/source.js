/*global define*/
define(function (require) {
    "use strict";
    var Backbone = require('backbone');
    return  {
        Collection: Backbone.Collection.extend({
            url: "/services/catalog/sources",
            useAjaxSync: true
        })
    };
});