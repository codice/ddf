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