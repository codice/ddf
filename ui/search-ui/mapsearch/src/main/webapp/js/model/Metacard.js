var Metacard = Backbone.Model.extend({

});

var MetacardResult = Backbone.Model.extend({
    initialize: function(options) {
        if(!_.isUndefined(options) && !_.isUndefined(options.metacard))
        {
            this.metacard = new Metacard(options.metacard);
        }
    }
});

var MetacardList = Backbone.Collection.extend({
    model: MetacardResult
});

var SearchResult = Backbone.Model.extend({
    initialize: function(options) {
        if(!_.isUndefined(options) && !_.isUndefined(options.results))
        {
            this.results = new MetacardList(options.results);
        }
    }
});