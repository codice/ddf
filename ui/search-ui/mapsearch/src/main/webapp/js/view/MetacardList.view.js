var MetacardRow = Backbone.View.extend({
    tagName: "tr",
    events: {
        'click .metacardLink' : 'viewMetacard'
    },
    render: function() {
        this.$el.html(ich.metacardRow(this.model.toJSON()));
        return this;
    },
    viewMetacard: function() {
        //do something to view the metacard, worry about that later
        var geometry = this.model.get("metacard").get("geometry");
        mapView.flyToLocation(geometry);
    }
});

var MetacardTable = Backbone.View.extend({
    metacardRows: [],
    initialize: function(){
        _.bindAll(this, 'appendCard', 'render', 'removeCard', 'changeCard');
        this.collection.bind("add", this.appendCard);
        this.collection.bind("remove", this.removeCard);
        this.collection.bind("change", this.changeCard);
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
    appendCard: function(card) {
        var newRow = new MetacardRow({model: card.metacard});
        this.metacardRows.push(newRow);
        this.$el.append(newRow.render().el);
    },
    removeCard: function(card) {
        var i = null;
        for(i in this.metacardRows) {
            if(this.metacardRows[i].model.id === card.id) {
                this.metacardRows[i].remove();
                this.metacardRows.splice(i,1);
                break;
            }
        }
    },
    changeCard: function(change) {
        this.removeCard(change);
        this.appendCard(new Metacard(change.attributes));
    }
});

var MetacardListView = Backbone.View.extend({
    initialize: function(options) {
        _.bindAll(this, "render");
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
    },
    render: function() {
        this.$el.html(ich.resultListTemplate(this.model.toJSON()));
        var metacardTable = new MetacardTable({
            collection: this.model.get("results"),
            el: this.$(".resultTable").children("tbody")
        });
        metacardTable.render();
        return this;
    }
});
