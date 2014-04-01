/*global define*/

define(function (require) {
    "use strict";

    var $ = require('jquery'),
        Backbone = require('backbone'),
        Marionette = require('marionette'),
        _ = require('underscore'),
        ich = require('icanhaz'),
        dir = require('direction'),
        Spinner = require('spin'),
        spinnerConfig = require('spinnerConfig'),
        List = {};
    
    function throwError(message, name) {
      var error = new Error(message);
      error.name = name || 'Error';
      throw error;
    }

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
    
    List.LoadingView = Marionette.ItemView.extend({
        spinner: new Spinner(spinnerConfig),
        onRender: function(){
            var view = $.find('#searchControls').pop();
            this.spinner.spin(view);
        },
        onClose: function(){
            this.spinner.stop();
        }
    });

    List.MetacardTable = Marionette.CollectionView.extend({
        emptyView: List.LoadingView,
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

            if (collectionView.isBuffering) {
                // could just quickly
                // use prepend
                if (index === 0) {
                    collectionView._bufferedChildren.reverse();
                    collectionView._bufferedChildren.push(itemView);
                    collectionView._bufferedChildren.reverse();
                    if(collectionView.elBuffer.firstChild) {
                        return collectionView.elBuffer.insertBefore(itemView.el, collectionView.elBuffer.firstChild);
                    } else {
                        return collectionView.elBuffer.appendChild(itemView.el);
                    }
                } else {
                    // see if there is already
                    // a child at the index
                    childAtIndex = collectionView.$el.children().eq(index);
                    if (childAtIndex.length) {
                        return childAtIndex.before(itemView.el);
                    } else {
                        return collectionView.elBuffer.appendChild(itemView.el);
                    }
                }
            } else {
                // If we've already rendered the main collection, just
                // append the new items directly into the element.
                var $container = this.getItemViewContainer(collectionView);
                if (index === 0) {
                    $container.empty();
                    return $container.prepend(itemView.el);
                } else {
                    // see if there is already
                    // a child at the index
                    childAtIndex = $container.children().eq(index);
                    if (childAtIndex.length) {
                        return childAtIndex.before(itemView.el);
                    } else {
                        return $container.append(itemView.el);
                    }
                }
            }
        },
        getItemViewContainer: function(containerView){
            if ("$itemViewContainer" in containerView){
                return containerView.$itemViewContainer;
            }
        
            var container;
            var itemViewContainer = Marionette.getOption(containerView, "itemViewContainer");
            if (itemViewContainer) {
                var selector = _.isFunction(itemViewContainer) ? itemViewContainer.call(this) : itemViewContainer;
                container = containerView.$(selector);
                if (container.length <= 0) {
                    throwError("The specified `itemViewContainer` was not found: " + containerView.itemViewContainer, "ItemViewContainerMissingError");
                }
            } else {
                container = containerView.$el;
            }
        
            containerView.$itemViewContainer = container;
            return container;
        },
    });

    List.MetacardListView = Backbone.View.extend({
        className : 'slide-animate',
        initialize: function (options) {
            _.bindAll(this);
            //options should be -> { results: results, mapView: mapView }
            this.model = options.result;
            this.searchControlView = options.searchControlView;

            this.modelBinder = new Backbone.ModelBinder();
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

            this.listenTo(this.model, 'change', this.showHideLoadMore);

            var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
            this.modelBinder.bind(this.model, this.$el, bindings);

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
            //this.unbind();
            if (this.metacardTable){
                this.metacardTable.close();
            }
        },
        showHideLoadMore: function() {
            if (!_.isUndefined(this.model.get("hits"))) {
                if (this.model.get("results").length >= this.model.get("hits") || this.model.get("hits") === 0) {
                    $("#high-results", this.$el).hide();
                    $("#low-results", this.$el).css("display", "block").show();
                } else {
                    $("#high-results", this.$el).css("display", "block").show();
                    $("#low-results", this.$el).hide();
                }
            }
        }
    });

    return List;

});
