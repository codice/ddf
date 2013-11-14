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
    url: "/services/catalog/query",
    loadMoreResults: function() {
        var queryParams = this.get("queryParams");
        this.set("count", this.get("count") + this.get("itemsPerPage"));
        queryParams = queryParams.replace(new RegExp("&count=\\d*", "g"), "&count="+this.get("count"));
        this.fetch({
            url: this.url,
            data: queryParams,
            dataType: "jsonp",
            timeout: 300000
        });
    }
});
    return MetaCard;

});