/*global define*/

define(function (require) {
    "use strict";
    var $ = require('jquery'),
        Backbone = require('backbone'),
        _ = require('underscore'),
        MetaCard = {};

    require('backbonerelational');
    MetaCard.Geometry = Backbone.RelationalModel.extend({

});

    MetaCard.Properties = Backbone.RelationalModel.extend({

});

    MetaCard.Metacard = Backbone.RelationalModel.extend({
    relations: [
        {
            type: Backbone.HasOne,
            key: 'geometry',
            relatedModel: MetaCard.Geometry
        },
        {
            type: Backbone.HasOne,
            key: 'properties',
            relatedModel: MetaCard.Properties
        }
    ]
});

    MetaCard.MetacardResult = Backbone.RelationalModel.extend({
    relations: [{
        type: Backbone.HasOne,
        key: 'metacard',
        relatedModel: MetaCard.Metacard
    }]
});

    MetaCard.MetacardList = Backbone.Collection.extend({
    model: MetaCard.MetacardResult
});

    MetaCard.SearchResult = Backbone.RelationalModel.extend({
    relations: [{
        type: Backbone.HasMany,
        key: 'results',
        relatedModel: MetaCard.MetacardResult,
        collectionType: MetaCard.MetacardList
    }],
    url: "/services/catalog/query"
});

    return MetaCard;

});