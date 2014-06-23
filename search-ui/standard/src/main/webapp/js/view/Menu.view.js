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
    'icanhaz',
    'text!templates/menubar.handlebars',
    'text!templates/menubarItem.handlebars',
    'js/model/user',
    'backbone',
    'text!templates/notification/notification.menu.handlebars',
    'wreqr',
    'underscore',
    'text!templates/menubarLogin.handlebars',
    'text!templates/menubarLogout.handlebars',
    'text!templates/tasks/task.menu.handlebars',
    'text!templates/tasks/task.category.handlebars',
    'text!templates/help.handlebars',
    'cometdinit',
    'jquery',
    'modelbinder',
    'perfectscrollbar',
    'backbonecometd'
], function(Marionette, ich, menubarTemplate, menubarItemTemplate, User, Backbone, notificationMenuTemplate, wreqr, _, loginTemplate, logoutTemplate, taskTemplate, taskCategoryTemplate, helpTemplate, Cometd, $) {

    ich.addTemplate('menubarItemTemplate', menubarItemTemplate);

    ich.addTemplate('menubarTemplate', menubarTemplate);

    ich.addTemplate('notificationMenuTemplate', notificationMenuTemplate);

    ich.addTemplate('loginTemplate', loginTemplate);

    ich.addTemplate('logoutTemplate', logoutTemplate);

    ich.addTemplate('taskTemplate', taskTemplate);

    ich.addTemplate('taskCategoryTemplate', taskCategoryTemplate);

    ich.addTemplate('helpTemplate', helpTemplate);

    var Menu = {};

    Menu.UserModel = new User.Response();
    Menu.UserModel.fetch();

    var MenuItem = Backbone.Model.extend({

    });

    Menu.NotificationItem = Marionette.ItemView.extend({
        template: 'notificationMenuTemplate',
        tagName: 'li',
        events: {
            'click' : 'openNotification',
            'click a': 'removeNotification'
        },
        onClose: function() {
            clearTimeout(this.timeout);
        },
        onRender: function() {
            this.timeout = setTimeout(this.render, 60000);
        },
        openNotification: function() {
            wreqr.vent.trigger('notification:open', this.model);
        },
        removeNotification: function(e) {
            var id = e.target.id;
            if(id) {
                if(id !== 'close' && id !== 'cancelRemove') {
                    if(id === 'remove') {
                        this.model.collection.remove(this.model);
                        wreqr.vent.trigger('notification:delete', this.model);
                        Cometd.Comet.publish(this.model.url, {id: this.model.get('id'), action: id});
                    }
                } else {
                    if(id === 'cancelRemove') {
                        this.model.set({closeConfirm: false});
                    } else {
                        this.model.set({closeConfirm: true});
                    }
                }
            }
            e.stopPropagation();
        },
        initialize: function() {
            this.listenTo(this.model, 'change', this.render);
        }
    });

    Menu.TaskItem = Marionette.ItemView.extend({
        template: 'taskTemplate',
        tagName: 'li',
        events: {
            'click a': 'clickLink',
            'click': 'clickBody'
        },
        modelEvents: {
            'change': 'render'
        },
        clickBody: function(e) {
            //stops the menu from closing
            e.stopPropagation();

        },
        clickLink: function(e) {
            var id = e.target.id;
            if(id) {
                if(id !== 'close' && id !== 'cancelRemove') {
                    Cometd.Comet.publish(this.model.url, {id: this.model.get('id'), action: id});
                    if(id === 'remove') {
                        this.model.collection.remove(this.model);
                        wreqr.vent.trigger('task:remove', this.model);
                    }
                } else {
                    if(id === 'cancelRemove') {
                        this.model.set({closeConfirm: false});
                    } else {
                        this.model.set({closeConfirm: true});
                    }
                }
            }
            this.clickBody(e);
        },
        onClose: function() {

        },
        onRender: function() {
            if(parseInt(this.model.get('progress'), 10) <= 100) {
                if(parseInt(this.model.get('progress'), 10) !== -1) {
                    this.$('.task-progressbar').progressbar({value: parseInt(this.model.get('progress'), 10)});
                }
            }

        },
        openNotification: function() {
            wreqr.vent.trigger('notification:open', this.model);
        }
    });

    Menu.TaskCategory = Marionette.ItemView.extend({
        tagName: 'li',
        template: 'taskCategoryTemplate',
        events: {
            'click': 'clickBody'
        },
        clickBody: function(e) {
            //stops the menu from closing
            e.stopPropagation();
        }
    });

    Menu.NotificationEmpty = Marionette.ItemView.extend({
        render: function() {
            this.$el.html("No recent notifications.");
        }
    });

    Menu.TaskEmpty = Marionette.ItemView.extend({
        render: function() {
            this.$el.html("No current tasks.");
        }
    });

    Menu.TaskList = Marionette.CollectionView.extend({
        className: 'dropdown-width',
        itemView: Menu.TaskItem,
        emptyView: Menu.TaskEmpty,
        showCollection: function(){
            var ItemView;
            var category;
            this.collection.each(function(item, index){
                if(category !== item.get('category')) {
                    this.addItemView(new Backbone.Model({category: item.get('category')}), Menu.TaskCategory);
                }
                category = item.get('category');
                ItemView = this.getItemView(item);
                this.addItemView(item, ItemView, index);
            }, this);
        }
    });

    Menu.NotificationList = Marionette.CollectionView.extend({
        className: 'dropdown-width',
        itemView: Menu.NotificationItem,
        emptyView: Menu.NotificationEmpty
    });

    Menu.Item = Marionette.Layout.extend({
        tagName: 'li',
        template: 'menubarItemTemplate',
        regions: {
            children: 'ul.dropdown-menu'
        },
        modelEvents: {
            'change': 'render'
        }
    });

    Menu.LoginForm = Marionette.ItemView.extend({
        template: 'loginTemplate',
        events: {
            'click .btn-signin': 'logInUser',
            'click .btn-clear': 'clearFields',
            'keypress #username': 'logInEnter',
            'keypress #password': 'logInEnter'
        },
        logInEnter: function(e) {
            if (e.keyCode === 13) {
                this.logInUser();
            }
        },
        logInUser: function() {
            var view = this;
            this.deleteCookie();
            $.ajax({
                type: "GET",
                url: document.URL,
                async: false,
                beforeSend: function (xhr) {
                    var base64 = window.btoa(view.$('#username').val() + ":" + view.$('#password').val());
                    xhr.setRequestHeader ("Authorization", "Basic " + base64);
                },
                error: function() {
                    document.location.reload();
                },
                complete: function() {
                    document.location.reload();
                }
            });
        },
        clearFields: function() {
            this.$('#username').val('');
            this.$('#password').val('');
        },
        deleteCookie: function() {
            document.cookie = 'org.codice.websso.saml.token=; Path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
        }
    });

    Menu.LogoutForm = Marionette.ItemView.extend({
        template: 'logoutTemplate',
        events: {
            'click .btn-logout': 'logOutUser'
        },
        logOutUser: function() {
            this.deleteCookie();
            $.ajax({
                type: "GET",
                url: document.URL,
                async: false,
                username: Menu.UserModel.guestUser,
                password: Menu.UserModel.guestPass,
                error: function() {
                    document.location.reload();
                },
                complete: function() {
                    document.location.reload();
                }
            });
        },
        deleteCookie: function() {
            document.cookie = 'org.codice.websso.saml.token=; Path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
        }
    });

    Menu.Bar = Marionette.Layout.extend({
        template: 'menubarTemplate',
        className: 'container-fluid navbar',
        regions: {
            welcome: '#welcome',
            notification: '#notification',
            help: '#help',
            tasks: '#tasks'
        },
        onRender: function() {
            var menuBarView = this;
            if(this.model.get('showWelcome')) {
                var Welcome = Menu.Item.extend({
                    className: 'dropdown',
                    initialize: function() {
                        if(this.isNotGuestUser()){
                            this.model.set({name: Menu.UserModel.get('user').get('username')});
                        }
                        this.listenTo(Menu.UserModel, 'change', this.updateUser);
                    },
                    updateUser: function() {
                        if(this.isNotGuestUser()) {
                            this.model.set({name: Menu.UserModel.get('user').get('username')});
                        }
                        this.render();
                    },
                    isNotGuestUser: function() {
                        return Menu.UserModel && Menu.UserModel.get('user') && Menu.UserModel.get('user').get('username') && !Menu.UserModel.get('user').isGuestUser();
                    },
                    onRender: function() {
                        if(this.isNotGuestUser()) {
                            this.children.show(new Menu.LogoutForm());
                        }
                        else {
                            this.children.show(new Menu.LoginForm());
                        }
                    }
                });
                this.welcome.show(new Welcome({model: new MenuItem({
                    id: 'signin',
                    name: 'Sign In',
                    //change this to true when we can log in or out
                    dropdown: true
                })}));
            }

            if(this.model.get('showTask')) {
                var Tasks = Menu.Item.extend({
                    className: 'dropdown',
                    initialize: function () {
                        if (wreqr.reqres.hasHandler('tasks')) {
                            this.collection = wreqr.reqres.request('tasks');
                        }

                        wreqr.vent.on('task:update', _.bind(this.updateTask, this));
                        wreqr.vent.on('task:remove', _.bind(this.updateTask, this));
                        this.modelBinder = new Backbone.ModelBinder();
                    },
                    updateScrollbar: function () {
                        var view = this;
                        _.defer(function () {
                            view.children.$el.perfectScrollbar('update');
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
                            this.children.show(new Menu.TaskList({collection: this.collection, itemViewContainer: this.children.$el}));
                            var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
                            this.modelBinder.bind(this.model, this.$el, bindings);
                        } else {
                            this.children.show(new Menu.TaskEmpty());
                        }
                        _.defer(function () {
                            view.children.$el.perfectScrollbar();
                        });
                    }
                });
                this.tasks.show(new Tasks({model: new MenuItem({
                    id: 'tasks',
                    name: 'Tasks',
                    classes: 'fa fa-tasks center-icon',
                    iconOnly: true,
                    dropdown: true,
                    count: true
                })}));
            }

            var Notification = Menu.Item.extend({
                className: 'dropdown',
                initialize: function() {
                    wreqr.vent.on('notification:delete', _.bind(this.deleteNotification, this));
                    wreqr.vent.on('notification:new', _.bind(this.addNotification, this));
                    wreqr.vent.on('notification:close', _.bind(this.removeNotification, this));
                    this.modelBinder = new Backbone.ModelBinder();
                },
                updateScrollbar: function () {
                    var view = this;
                    _.defer(function () {
                        view.children.$el.perfectScrollbar('update');
                    });
                },
                deleteNotification: function() {
                        if (!this.collection) {
                            if (wreqr.reqres.hasHandler('notifications')) {
                                this.collection = wreqr.reqres.request('notifications');
                                this.updateScrollbar();
                            }
                        }
                        if (this.collection) {
                            if (this.collection.length === 0) {
                                this.model.set({countNum: ''});
                            } else {
                                this.model.set({countNum: this.collection.length});
                            }
                            this.updateScrollbar();
                        }
                },
                removeNotification: function() {
                    if(this.collection) {
                        var len;
                        if(this.collection.length !== 0) {
                            len = this.collection.length;
                        } else {
                            len = '';
                        }
                        this.model.set({countNum: len});
                    }
                },
                addNotification: function() {
                    if(!this.collection) {
                        if(wreqr.reqres.hasHandler('notifications')) {
                            this.collection = wreqr.reqres.request('notifications');
                            this.model.set({countNum: this.collection.length});
                        }
                        this.render();
                    } else {
                        this.model.set({countNum: this.collection.length});
                    }
                },
                onRender: function() {
                var view = this;
                    if(this.collection) {
                        this.children.show(new Menu.NotificationList({collection: this.collection, itemViewContainer: this.children.$el}));
                        var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
                        this.modelBinder.bind(this.model, this.$el, bindings);
                    } else {
                        this.children.show(new Menu.NotificationEmpty());
                    }
                    _.defer(function () {
                        view.children.$el.perfectScrollbar();
                    });
                }
            });
            this.notification.show(new Notification({model: new MenuItem({
                id: 'notification',
                name: 'Notification',
                classes: 'fa fa-bell center-icon',
                iconOnly: true,
                dropdown: true,
                count: true
            })}));

            var Help = Menu.Item.extend({
                className: 'dropdown',
                onRender: function() {
                    var HelpView = Marionette.ItemView.extend({
                        template: 'helpTemplate',
                        className: 'dropdown-width',
                        serializeData: function(){
                             return {
                                branding: menuBarView.model.get('branding'),
                                version: menuBarView.model.get('version')
                             };
                        }
                    });
                    this.children.show(new HelpView());
                }
            });
            this.help.show(new Help({model: new MenuItem({
                id: 'help',
                name: 'Help',
                classes: 'fa fa-question-circle center-icon',
                iconOnly: true,
                dropdown: true
            })}));
        }
    });

    return Menu;
});