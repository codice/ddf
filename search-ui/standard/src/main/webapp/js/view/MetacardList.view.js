/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/

define(function (require) {
    "use strict";

    var Marionette = require('marionette'),
        _ = require('underscore'),
        ich = require('icanhaz'),
        dir = require('direction'),
        Spinner = require('spin'),
        spinnerConfig = require('spinnerConfig'),
        wreqr = require('wreqr'),
        List = {};
    
    function throwError(message, name) {
      var error = new Error(message);
      error.name = name || 'Error';
      throw error;
    }

    ich.addTemplate('resultListItem', require('text!templates/resultlist/resultListItem.handlebars'));
    ich.addTemplate('resultListTemplate', require('text!templates/resultlist/resultList.handlebars'));
    ich.addTemplate('countLowTemplate', require('text!templates/resultlist/countlow.handlebars'));
    ich.addTemplate('countHighTemplate', require('text!templates/resultlist/counthigh.handlebars'));

    List.MetacardRow = Marionette.ItemView.extend({
        tagName: "tr",
        template : 'resultListItem',
        events: {
            'click .metacard-link': 'viewMetacard'
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
            if (this.model.get('context')) {
                wreqr.vent.trigger('metacard:selected', this.model);
            }
            this.model.set('context', true);
        }

    });

    List.MetacardTable = Marionette.CollectionView.extend({
        itemView : List.MetacardRow,
        tagName: 'table',
        className: 'table resultTable',
        itemViewOptions : function(model){
            return {
                model : model.get('metacard')
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
        }
    });

    List.MetacardListView = Marionette.Layout.extend({
        className: 'slide-animate height-full',
        template: 'resultListTemplate',
        regions: {
            countRegion: '.result-count',
            listRegion: '#resultList'
        },
        spinner: new Spinner(spinnerConfig),
        initialize: function (options) {
            _.bindAll(this);
            //options should be -> { results: results, mapView: mapView }
            this.model = options.result;
        },
        onRender: function () {
            this.listRegion.show(new List.MetacardTable({
                collection: this.model.get("results")
            }));
            this.updateCount();
            this.listenTo(this.model, 'change', this.updateCount);
        },
        onShow: function(){
            this.updateSpinner();
            this.listenTo(this.model, 'change', this.updateSpinner);
        },
        updateCount: function() {
            if (!_.isUndefined(this.model.get("hits"))) {
                if (this.model.get("results").length >= this.model.get("hits") || this.model.get("hits") === 0) {
                    this.countRegion.show(new Marionette.ItemView({
                        template: 'countLowTemplate',
                        model: this.model
                    }));
                } else {
                    this.countRegion.show(new Marionette.ItemView({
                        template: 'countHighTemplate',
                        model: this.model
                    }));
                }
            }
        },
        updateSpinner: function () {
            if (!_.isUndefined(this.model.get("hits"))) {
                this.spinner.stop();
            } else {
                this.spinner.spin(this.el);
            }
        }
    });

    return List;

});
