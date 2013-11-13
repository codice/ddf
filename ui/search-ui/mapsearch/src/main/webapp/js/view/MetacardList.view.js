var MetacardRow = Backbone.View.extend({
    tagName: "tr",
    events: {
        'click .metacardLink' : 'viewMetacard'
    },
    render: function() {
        this.$el.html(ich.resultListItem(this.model.toJSON()));
        return this;
    },
    viewMetacard: function() {
        //do something to view the metacard, worry about that later
        var geometry = this.model.get("metacard").get("geometry");
        mapView.flyToLocation(geometry);
    },
    close: function() {
        this.remove();
        this.stopListening();
        this.unbind();
        this.model.unbind();
        this.model.destroy();
    }
});

var MetacardTable = Backbone.View.extend({
    metacardRows: [],
    initialize: function(){
        _.bindAll(this, 'render');
    },
    render: function() {
        var m = null,
            newRow = null;
        for(m in this.collection.models){
            newRow = new MetacardRow({model: this.collection.models[m]});
            this.metacardRows.push(newRow);
            this.$el.append(newRow.render().el);
        }
        return this;
    },
    close: function() {
        var i;
        this.remove();
        this.stopListening();
        this.unbind();
        this.collection.unbind();
        for(i in this.metacardRows) {
            this.metacardRows[i].close();
        }
    }
});

var MetacardListView = Backbone.View.extend({
    tagName: "div id='resultPage' class='height-full'",
    events: {
        'click .load-more-link': 'loadMoreResults'
    },
    initialize: function(options) {
        _.bindAll(this, "render", "loadMoreResults");
        //options should be -> { results: results, mapView: mapView }
        if(options && options.result)
        {
            this.model = options.result;
        }
        if(options && options.mapView)
        {
            //we can control what results are displayed on the map as we page
            this.mapView = options.mapView;
        }
        this.listenTo(this.model, 'change', this.render);
    },
    render: function() {
        this.$el.html(ich.resultListTemplate(this.model.toJSON()));
        var metacardTable = new MetacardTable({
            collection: this.model.get("results"),
            el: this.$(".resultTable").children("tbody")
        });
        metacardTable.render();
        this.metacardTable = metacardTable;
        if(this.model.get("results").length >= this.model.get("hits") || this.model.get("hits") === 0) {
            $(".load-more-link").hide();
        }
        else {
            $(".load-more-link").show();
        }
        return this;
    },
    close: function() {
        this.remove();
        this.stopListening();
        this.unbind();
        this.model.unbind();
        this.model.destroy();
        this.metacardTable.close();
    },
    loadMoreResults: function() {
        this.model.loadMoreResults();
    }
});
