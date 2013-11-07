var Geometry = Backbone.RelationalModel.extend({

});

var Properties = Backbone.RelationalModel.extend({

});

var Metacard = Backbone.RelationalModel.extend({
    relations: [
        {
            type: Backbone.HasOne,
            key: 'geometry',
            relatedModel: Geometry
        },
        {
            type: Backbone.HasOne,
            key: 'properties',
            relatedModel: Properties
        }
    ]
});

var MetacardResult = Backbone.RelationalModel.extend({
    relations: [{
        type: Backbone.HasOne,
        key: 'metacard',
        relatedModel: Metacard
    }]
});

var MetacardList = Backbone.Collection.extend({
    model: MetacardResult
});

var SearchResult = Backbone.RelationalModel.extend({
    relations: [{
        type: Backbone.HasMany,
        key: 'results',
        relatedModel: MetacardResult,
        collectionType: MetacardList
    }],
    url: "/services/catalog/query"
});