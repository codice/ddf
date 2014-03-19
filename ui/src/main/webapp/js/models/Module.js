/*global define*/
define(function (require) {
    var Backbone = require('backbone');
    require('backbonerelational');

    var Module = {};
    var module = Backbone.RelationalModel.extend({});
    var moduleCollection = Backbone.Collection.extend({model:module});
    Module.Model = Backbone.RelationalModel.extend({
        url: "/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listModules",
        relations: [
            {
                type: Backbone.HasMany,
                key: 'value',
                relatedModel: module,
                collectionType: moduleCollection,
                includeInJSON: false,
            }
        ]
    });

    return Module;
});