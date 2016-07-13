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
/*global define, setTimeout, clearTimeout, document, window, parseInt*/
define([
    'marionette',
    'templates/menu/menubarItem.handlebars',
    'backbone',
    'wreqr',
    'underscore',
    './task.menu.hbs',
    './task.category.hbs',
    'cometdinit',
    'jquery',
    'modelbinder',
    'backbonecometd',
    'progressbar'
], function (Marionette, menubarItemTemplate, Backbone, wreqr, _, taskTemplate, taskCategoryTemplate, Cometd, $) {

    var Menu = {};
    var MenuItem = Backbone.Model.extend({});

    Menu.TaskItem = Marionette.ItemView.extend({
        template: taskTemplate,
        tagName: 'li',
        events: {
            'click a': 'clickLink',
            'click': 'clickBody'
        },
        modelEvents: {'change': 'render'},
        clickBody: function (e) {
            //stops the menu from closing
            e.stopPropagation();
        },
        clickLink: function (e) {
            var id = e.target.id;
            if (id) {
                if (id !== 'close' && id !== 'cancelRemove') {
                    Cometd.Comet.publish(this.model.url, {
                        data: [{
                            id: this.model.get('id'),
                            action: id
                        }]
                    });
                    if (id === 'remove') {
                        this.model.collection.remove(this.model);
                        wreqr.vent.trigger('task:remove', this.model);
                        Cometd.Comet.publish(this.model.url, {
                            data: [{
                                id: this.model.get('id'),
                                action: id
                            }]
                        });
                    }
                } else {
                    if (id === 'cancelRemove') {
                        this.model.set({closeConfirm: false});
                    } else {
                        this.model.set({closeConfirm: true});
                    }
                }
            }
            this.clickBody(e);
        },
        onRender: function () {
            if (parseInt(this.model.get('progress'), 10) <= 100) {
                if (parseInt(this.model.get('progress'), 10) !== -1) {
                    this.$('.task-progressbar').progressbar({value: parseInt(this.model.get('progress'), 10)});
                }
            }
        }
    });

    Menu.TaskCategory = Marionette.ItemView.extend({
        tagName: 'li',
        template: taskCategoryTemplate,
        events: {
            'click #closeCategory': 'dismissAll',
            'click #removeAll': 'removeAll',
            'click #cancelRemove': 'cancelRemoveAll',
            'click': 'clickBody'
        },
        modelEvents: {'change': 'render'},
        clickBody: function (e) {
            //stops the menu from closing
            e.stopPropagation();
        },
        removeAll: function (e) {
            var activitiesToDelete = [];
            var currentActivities = [];
            currentActivities = this.model.get('collection').where({category: this.model.get('category')});
            this.model.get('collection').remove(currentActivities);
            wreqr.vent.trigger('task:remove', this.model);
            _.each(currentActivities, function (activity) {
                activitiesToDelete.push({
                    id: activity.get('id'),
                    action: 'remove'
                });
            });
            Cometd.Comet.publish('/service/action', {data: activitiesToDelete});
            this.clickBody(e);
        },
        cancelRemoveAll: function () {
            this.model.set({closeConfirm: false});
        },
        dismissAll: function () {
            this.model.set({closeConfirm: true});
        }
    });

    Menu.Item = Marionette.LayoutView.extend({
        //tagName: 'li',
        template: menubarItemTemplate,
        regions: {children: 'ul.dropdown-menu'},
        modelEvents: {'change': 'render'}
    });

    Menu.TaskEmpty = Marionette.ItemView.extend({
        template: 'No current tasks.'
    });

    Menu.TaskList = Marionette.CollectionView.extend({
        className: 'dropdown-width tasks',
        childView: Menu.TaskItem,
        emptyView: Menu.TaskEmpty,
        showCollection: function () {
            var ItemView;
            var category;
            var view = this;
            this.collection.each(function (item, index) {
                if (category !== item.get('category')) {
                    this.addChild(new Backbone.Model({
                        category: item.get('category'),
                        collection: view.collection
                    }), Menu.TaskCategory);
                }
                category = item.get('category');
                ItemView = this.getChildView(item);
                this.addChild(item, ItemView, index);
            }, this);
        }
    });

    var Tasks = Menu.Item.extend({
        className: 'dropdown',
        initialize: function () {
            this.model = new MenuItem({
                id: 'tasks',
                name: 'Tasks',
                classes: 'fa fa-tasks',
                iconOnly: true,
                dropdown: true,
                count: true
            });

            if (wreqr.reqres.hasHandler('tasks')) {
                this.collection = wreqr.reqres.request('tasks');
            }

            this.listenTo(this.collection, 'add', this.updateTask);
            this.listenTo(this.collection, 'remove', this.updateTask);
            this.listenTo(this.collection, 'reset', this.updateTask);

            this.listenTo(wreqr.vent, 'task:update', this.updateTask);
            this.listenTo(wreqr.vent, 'task:remove', this.updateTask);
            this.modelBinder = new Backbone.ModelBinder();
        },
        updateScrollbar: function () {
            var view = this;
            _.defer(function () {
                //view.children.$el.perfectScrollbar('update');
            });
        },
        updateTask: function () {
            if (!this.collection) {
                if (wreqr.reqres.hasHandler('tasks')) {
                    this.collection = wreqr.reqres.request('tasks');
                    this.render();
                    this.updateScrollbar();
                }
            }
            if (this.collection) {
                if (this.collection.length === 0) {
                    this.model.set({countNum: ''});
                } else {
                    this.model.set({countNum: this.collection.length});
                }
                this.render();
                this.updateScrollbar();
            }
        },
        onRender: function () {
            var view = this;
            if (this.collection) {
                this.children.show(new Menu.TaskList({
                    collection: this.collection,
                    childViewContainer: this.children.$el
                }));
                var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
                this.modelBinder.bind(this.model, this.$el, bindings);
            } else {
                this.children.show(new Menu.TaskEmpty());
            }
            _.defer(function () {
                //view.children.$el.perfectScrollbar();
            });
        }
    });

    return Tasks;

});
