/*global define*/

define(function (require) {
    "use strict";
    var $ = require('jquery'),
        Backbone = require('backbone'),
        _ = require('underscore'),
        ich = require('icanhaz'),
        MetaCardListView = {};

    ich.addTemplate('resultListItem', require('text!templates/resultListItem.handlebars'));
    ich.addTemplate('resultListTemplate', require('text!templates/resultList.handlebars'));


    MetaCardListView.MetacardRow = Backbone.View.extend({
        tagName: "tr",
        events: {
            'click .metacardLink': 'viewMetacard'
        },

        initialize: function (options) {
            this.mapView = options.mapView;
        },
        render: function () {
            this.$el.html(ich.resultListItem(this.model.toJSON()));
            return this;
        },
        viewMetacard: function () {
            //do something to view the metacard, worry about that later
            var geometry = this.model.get("metacard").get("geometry");
            this.mapView.flyToLocation(geometry);
        },
        close: function() {
            this.remove();
            this.stopListening();
            this.unbind();
            this.model.unbind();
            this.model.destroy();
        }
    });

    MetaCardListView.MetacardTable = Backbone.View.extend({

        initialize: function (options) {
            _.bindAll(this, 'appendCard', 'render', 'removeCard', 'changeCard');
            this.collection.bind("add", this.appendCard);
            this.collection.bind("remove", this.removeCard);
            this.collection.bind("change", this.changeCard);
            this.metacardRows = [];
            this.mapView = options.mapView;
        },
        render: function () {
            var m = null,
                newRow = null,
                mapView = this.mapView,
                metaCardRows = this.metacardRows,
                view = this;
            this.collection.each(function (model) {
                newRow = new MetaCardListView.MetacardRow({model: model, mapView : mapView});
                metaCardRows.push(newRow);
                view.$el.append(newRow.render().el);
            });
            return this;
        },
        appendCard: function (card) {
            var newRow = new MetaCardListView.MetacardRow({model: card.metacard, mapView : this.mapView});
            this.metacardRows.push(newRow);
            this.$el.append(newRow.render().el);
        },
        removeCard: function (card) {
            var i = null;
            for (i in this.metacardRows) {
                if (this.metacardRows[i].model.id === card.id) {
                    this.metacardRows[i].remove();
                    this.metacardRows.splice(i, 1);
                    break;
                }
            }
        },
        changeCard: function (change) {
            this.removeCard(change);
            this.appendCard(new MetaCardListView.Metacard(change.attributes));
        },
        close: function() {
            var i;
            this.remove();
            this.stopListening();
            this.unbind();
            this.collection.unbind();
            _.invoke(this.metacardRows, 'close');

        }
        
    });

    MetaCardListView.MetacardListView = Backbone.View.extend({
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
            var metacardTable = new MetaCardListView.MetacardTable({
                collection: this.model.get("results"),
                el: this.$(".resultTable").children("tbody"),
                mapView : this.mapView
            });
            metacardTable.render();
            this.metacardTable = metacardTable;
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
    return MetaCardListView;
});