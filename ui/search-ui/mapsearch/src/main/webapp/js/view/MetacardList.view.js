/*global define*/

define(function(require){
    "use strict";

    var $ = require('jquery'),
        Backbone = require('backbone'),
        _ = require('underscore'),
        ich = require('icanhaz'),
        List = {};

    ich.addTemplate('resultListItem', require('text!templates/resultListItem.handlebars'));
    ich.addTemplate('resultListTemplate', require('text!templates/resultList.handlebars'));


    List.MetacardRow = Backbone.View.extend({
        tagName: "tr",
        events: {
            'click .metacardLink' : 'viewMetacard'
        },

        render: function() {
            this.$el.html(ich.resultListItem(this.model.toJSON()));
            return this;
        },
        viewMetacard: function() {

        },
        close: function() {
            this.remove();
            this.stopListening();
            this.unbind();
            this.model.unbind();
            this.model.destroy();
        }
    });

    List.MetacardTable = Backbone.View.extend({
        metacardRows: [],
        initialize: function(){
            _.bindAll(this, 'render');
            this.metacardRows = [];
        },
        render: function() {
            var view = this,
                newRow = null;
            this.collection.each(function(model){
                newRow = new List.MetacardRow({
                    model: model
                });
                view.metacardRows.push(newRow);
                view.$el.append(newRow.render().el);
            });
            return this;
        },
        close: function() {
            this.remove();
            this.stopListening();
            this.unbind();
            this.collection.unbind();
            _.invoke(this.metacardRows, 'close');

        }
    });

    List.MetacardListView = Backbone.View.extend({
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
            this.listenTo(this.model, 'change', this.render);
        },
        render: function() {
            this.$el.html(ich.resultListTemplate(this.model.toJSON()));
            var metacardTable = new List.MetacardTable({
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

    return List;

});
