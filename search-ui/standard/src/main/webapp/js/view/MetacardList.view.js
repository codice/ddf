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

define([
        'marionette',
        'underscore',
        'icanhaz',
        'direction',
        'spin',
        'spinnerConfig',
        'wreqr',
        'text!templates/resultlist/resultListItem.handlebars',
        'text!templates/resultlist/resultList.handlebars',
        'text!templates/resultlist/countlow.handlebars',
        'text!templates/resultlist/counthigh.handlebars',
        'text!templates/resultlist/statusItem.handlebars',
        'text!templates/resultlist/status.handlebars'
    ],
    function (Marionette, _, ich, dir, Spinner, spinnerConfig, wreqr, resultListItemTemplate, resultListTemplate, countLowTemplate, countHighTemplate, statusItemTemplate, statusTemplate) {
        "use strict";

        var List = {};

        function throwError(message, name) {
          var error = new Error(message);
          error.name = name || 'Error';
          throw error;
        }

        ich.addTemplate('resultListItem', resultListItemTemplate);
        ich.addTemplate('resultListTemplate', resultListTemplate);
        ich.addTemplate('countLowTemplate', countLowTemplate);
        ich.addTemplate('countHighTemplate', countHighTemplate);
        ich.addTemplate('statusItemTemplate', statusItemTemplate);
        ich.addTemplate('statusTemplate', statusTemplate);

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

        List.StatusRow = Marionette.ItemView.extend({
            tagName: 'tr',
            template: 'statusItemTemplate',
            modelEvents: {
                "change": "render"
            }
        });
    
        List.StatusTable = Marionette.CompositeView.extend({
            template: 'statusTemplate',
            itemView : List.StatusRow,
            itemViewContainer: 'tbody',
            events: {
                'click #status-icon': 'toggleStatus'
            },
            toggleStatus: function() {
                this.$('#status-table').toggle();
                this.$('#status-icon').toggleClass('fa-caret-down fa-caret-right');
            }
        });
    
        List.CountView = Marionette.ItemView.extend({
            modelEvents: {
                "change": "render"
            },
            getTemplate: function() {
                if (!_.isUndefined(this.model.get('hits'))) {
                    if (this.model.get('results').length >= this.model.get('hits') || this.model.get('hits') === 0) {
                        return 'countLowTemplate';
                    } else {
                        return 'countHighTemplate';
                    }
                }
            }
        });

        List.MetacardListView = Marionette.Layout.extend({
            className: 'slide-animate height-full',
            template: 'resultListTemplate',
            regions: {
                countRegion: '.result-count',
                listRegion: '#resultList',
                statusRegion: '#result-status-list'
            },
            modelEvents: {
                'change': 'updateSpinner'
            },
            spinner: new Spinner(spinnerConfig),
            onRender: function () {
                this.listRegion.show(new List.MetacardTable({
                    collection: this.model.get("results")
                }));
                this.statusRegion.show(new List.StatusTable({
                    collection: this.model.get("sources")
                }));
                this.countRegion.show(new List.CountView({
                    model: this.model
                }));
            },
            onShow: function(){
                this.updateSpinner();
                this.updateScrollbar();
                this.updateScrollPos();
            },
            onClose: function() {
                this.spinner.stop();
            },
            updateSpinner: function () {
                if (!_.isUndefined(this.model.get("hits"))) {
                    this.spinner.stop();
                } else {
                    this.spinner.spin(this.el);
                }
            },
            updateScrollbar: function () {
                var view = this;
                // defer seems to be necessary for this to update correctly
                _.defer(function () {
                    view.$el.perfectScrollbar('update');
                });
            },
            updateScrollPos: function() {
                var view = this;
                _.defer(function () {
                    var selected = view.$el.find('.selected');
                    var container = view.$el.parent();
                    if(selected.length !== 0)
                    {
                        container.scrollTop(selected.offset().top - container.offset().top + container.scrollTop());
                    }
                });
            }
        });

        return List;

});
