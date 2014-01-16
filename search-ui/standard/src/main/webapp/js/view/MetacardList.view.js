/*global define*/

define(function (require) {
    "use strict";

    var $ = require('jquery'),
        Backbone = require('backbone'),
        Marionette = require('marionette'),
        _ = require('underscore'),
        ich = require('icanhaz'),
        Spinner = require('spin'),
        spinnerConfig = require('spinnerConfig'),
        dir = require('direction'),
        List = {};

    ich.addTemplate('resultListItem', require('text!templates/resultListItem.handlebars'));
    ich.addTemplate('resultListTemplate', require('text!templates/resultList.handlebars'));


    List.MetacardRow = Marionette.ItemView.extend({
        tagName: "tr",
        template : 'resultListItem',
        events: {
            'click .metacard-link': 'viewMetacard'
        },

        initialize: function (options) {
            _.bindAll(this);
            this.searchControlView = options.searchControlView;
        },

        serializeData: function(){
            //we are overriding this serializeData function to change marionette's behavior
            //previously it was performing a .toJSON on the model which is normally what you want
            //but our model is pretty deep and this was causing some big performance issues
            //so with this change we simply need up adapt our templates to work with backbone
            //objects instead of flat json records
            var data = {};

            if (this.model) {
                data = this.model;
            }

            return data;
        },

        onRender : function(){
            if(this.model.get('context')){
                this.$el.addClass('selected');
            }
        },

        viewMetacard: function () {
            this.model.set('direction', dir.forward);
            if(this.model.get('context')){
                this.searchControlView.showMetacardDetail(this.model);
            }
            this.model.set('context', true);
        }

    });

    List.MetacardTable = Marionette.CollectionView.extend({
        itemView : List.MetacardRow,
        initialize: function (options) {
            this.searchControlView = options.searchControlView;
        },
        itemViewOptions : function(model){
            return {
                model : model.get('metacard'),
                searchControlView : this.searchControlView
            };
        },
        appendHtml: function (collectionView, itemView, index) {
            var childAtIndex;

            // could just quickly
            // use prepend
            if (index === 0) {
                return collectionView.$el.prepend(itemView.el);
            } else {
                // see if there is already
                // a child at the index
                childAtIndex = collectionView.$el.children().eq(index);
                if (childAtIndex.length) {
                    return childAtIndex.before(itemView.el);
                } else {
                    return collectionView.$el.append(itemView.el);
                }
            }
        }
    });

    List.MetacardListView = Backbone.View.extend({
        className : 'slide-animate',
        events: {
            'click .load-more-link': 'loadMoreResults'
        },
        initialize: function (options) {
            _.bindAll(this);
            //options should be -> { results: results, mapView: mapView }
            this.model = options.result;
            this.searchControlView = options.searchControlView;
        },
        render: function () {
            this.$el.html(ich.resultListTemplate(this.model));
            var metacardTable = new List.MetacardTable({
                collection: this.model.get("results"),
                el: this.$(".resultTable").children("tbody"),
                searchControlView: this.searchControlView
            });
            metacardTable.render();
            this.metacardTable = metacardTable;
            this.showHideLoadMore();

            this.delegateEvents();
            this.trigger("render");
            return this;
        },
        close: function () {
            this.remove();
            this.stopListening();
            //apparently unbind is sort of a sledgehammer approach to dealing with zombie views
            //I tried it with this commented out and it appears to clean up the view correctly without it
            //I'm going to leave it here as a note however
//            this.unbind();
            this.metacardTable.close();
        },
        loadMoreResults: function () {
            var view = this;
            var spinner = new Spinner(spinnerConfig).spin(this.searchControlView.el);
            $('.load-more-link', this.$el).hide();
            this.model.loadMoreResults().complete(function() {
                spinner.stop();
                view.showHideLoadMore();
            });
        },
        showHideLoadMore: function() {
            if (this.model.get("results").length >= this.model.get("hits") || this.model.get("hits") === 0) {
                $(".load-more-link", this.$el).hide();
            }
            else {
                $(".load-more-link", this.$el).css( "display", "block").show();
            }
        }
    });

    return List;

});
