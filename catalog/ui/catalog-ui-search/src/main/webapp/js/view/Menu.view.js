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
    'text!templates/menu/menubar.handlebars',
    'text!templates/menu/menubarItem.handlebars',
    'backbone',
    'text!templates/notification/notification.menu.handlebars',
    'text!templates/notification/notification.category.handlebars',
    'wreqr',
    'underscore',
    'text!templates/menu/menubarLogin.handlebars',
    'text!templates/menu/menubarLogout.handlebars',
    'text!templates/tasks/task.menu.handlebars',
    'text!templates/tasks/task.category.handlebars',
    'text!templates/menu/help.handlebars',
    'cometdinit',
    'jquery',
    'js/view/ingest/IngestMenu',
    'js/view/preferences/PreferencesMenu',
    'application',
    'properties',
    'js/CustomElements',
    'js/view/WorkspaceIndicator.view',
    'js/store',
    'modelbinder',
    'perfectscrollbar',
    'backbonecometd',
    'progressbar'
], function (Marionette, menubarTemplate, menubarItemTemplate, Backbone, notificationMenuTemplate,
             notificationCategoryTemplate, wreqr, _, loginTemplate, logoutTemplate, taskTemplate,
             taskCategoryTemplate, helpTemplate, Cometd, $, IngestMenu, PreferencesMenu, Application,
             properties, CustomElements, WorkspaceIndicator, store) {
    var iconOnly = false;
    var Menu = {};
    var MenuItem = Backbone.Model.extend({});
    Menu.NotificationItem = Marionette.ItemView.extend({
        template: notificationMenuTemplate,
        tagName: 'li',
        events: {
            'click': 'clickBody',
            'click a': 'removeNotification'
        },
        modelEvents: {'change': 'render'},
        onDestroy: function () {
            clearTimeout(this.timeout);
        },
        onRender: function () {
            this.timeout = setTimeout(this.render, 60000);
        },
        clickBody: function (e) {
            //stops the menu from closing
            e.stopPropagation();
        },
        removeNotification: function (e) {
            var id = e.target.id;
            if (id) {
                if (id !== 'close' && id !== 'cancelRemove') {
                    if (id === 'remove') {
                        this.model.collection.remove(this.model);
                        wreqr.vent.trigger('notification:delete', this.model);
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
            e.stopPropagation();
        }
    });
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
    Menu.NotificationCategory = Marionette.ItemView.extend({
        tagName: 'li',
        template: notificationCategoryTemplate,
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
            var notificationsInCategory = [];
            var notifications = [];
            notificationsInCategory = this.model.get('collection').where({application: this.model.get('category')});
            this.model.get('collection').remove(notificationsInCategory);
            wreqr.vent.trigger('notification:delete', this.model);
            for (var i = 0; i < notificationsInCategory.length; ++i) {
                notifications.push({
                    id: notificationsInCategory[i].get('id'),
                    action: 'remove'
                });
            }
            Cometd.Comet.publish('/notification/action', {data: notifications});
            this.clickBody(e);
        },
        cancelRemoveAll: function () {
            this.model.set({closeConfirm: false});
        },
        dismissAll: function () {
            this.model.set({closeConfirm: true});
        }
    });
    Menu.NotificationEmpty = Marionette.ItemView.extend({
        render: function () {
            this.$el.html('No recent notifications.');
        }
    });
    Menu.TaskEmpty = Marionette.ItemView.extend({
        render: function () {
            this.$el.html('No current tasks.');
        }
    });
    Menu.TaskList = Marionette.CollectionView.extend({
        className: 'dropdown-width',
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
    Menu.NotificationList = Marionette.CollectionView.extend({
        childView: Menu.NotificationItem,
        emptyView: Menu.NotificationEmpty,
        showCollection: function () {
            var ItemView;
            var category;
            var view = this;
            this.collection.each(function (item, index) {
                if (category !== item.get('application')) {
                    this.addChild(new Backbone.Model({
                        category: item.get('application'),
                        collection: view.collection
                    }), Menu.NotificationCategory);
                }
                category = item.get('application');
                ItemView = this.getChildView(item);
                this.addChild(item, ItemView, index);
            }, this);
        }
    });
    Menu.Item = Marionette.LayoutView.extend({
        tagName: 'li',
        template: menubarItemTemplate,
        regions: {children: 'ul.dropdown-menu'},
        modelEvents: {'change': 'render'}
    });
    Menu.LoginForm = Marionette.ItemView.extend({
        template: loginTemplate,
        events: {
            'click .btn-signin': 'logInUser',
            'keypress #username': 'logInEnter',
            'keypress #password': 'logInEnter'
        },
        logInEnter: function (e) {
            if (e.keyCode === 13) {
                this.logInUser();
            }
        },
        logInUser: function () {
            var view = this;
            this.deleteCookie();
            $.ajax({
                type: 'GET',
                url: document.URL,
                async: false,
                beforeSend: function (xhr) {
                    var base64 = window.btoa(view.$('#username').val() + ':' + view.$('#password').val());
                    xhr.setRequestHeader('Authorization', 'Basic ' + base64);
                },
                error: function () {
                    view.showErrorText();
                    view.setErrorState();
                },
                success: function () {
                    document.location.reload();
                }
            });
        },
        showErrorText: function () {
            this.$('#loginError').show();
        },
        setErrorState: function () {
            this.$('#password').focus(function () {
                this.select();
            });
        },
        deleteCookie: function () {
            document.cookie = 'JSESSIONID=;path=/;domain=;expires=Thu, 01 Jan 1970 00:00:00 GMT;secure';
        }
    });
    Menu.LogoutForm = Marionette.ItemView.extend({
        template: logoutTemplate,
        events: {'click .btn-logout': 'logout'},
        logout: function () {
            //this function is only here to handle clearing basic auth credentials
            //if you aren't using basic auth, this shouldn't do anything
            $.ajax({
                type: 'GET',
                url: document.URL,
                async: false,
                username: '1',
                password: '1'
            }).then(function () {
                window.location = '/logout';
            });
        }
    });
    Menu.Bar = Marionette.LayoutView.extend({
        template: menubarTemplate,
        className: 'navbar',
        tagName: CustomElements.register('menu-bar'),
        regions: {
            welcome: '#welcome',
            notification: '#notification',
            help: '#help',
            tasks: '#tasks',
            ingest: '#ingest',
            preferences: '#preferences',
            workspaces: '#workspaceSelector'
        },
        onRender: function () {
            var menuBarView = this;
            if (this.model.get('showWelcome')) {
                var Welcome = Menu.Item.extend({
                    className: 'dropdown',
                    initialize: function () {
                        if (this.isNotGuestUser()) {
                            this.model.set({name: Application.UserModel.get('user').get('username')});
                        } else if (!this.isNotGuestUser() && properties.externalAuthentication) {
                            this.model.set({
                                name: typeof Application.UserModel.get('user').get('username') === 'undefined' ?
                                    'ERROR' : Application.UserModel.get('user').get('username').split('@')[0]
                            });
                        }
                        this.listenTo(Application.UserModel, 'change', this.updateUser);
                    },
                    updateUser: function () {
                        if (this.isNotGuestUser()) {
                            this.model.set({name: Application.UserModel.get('user').get('username')});
                        }
                        this.render();
                    },
                    isNotGuestUser: function () {
                        return Application.UserModel && Application.UserModel.get('user') && Application.UserModel.get('user').get('username') && !Application.UserModel.get('user').isGuestUser();
                    },
                    onRender: function () {
                        if (this.isNotGuestUser() || properties.externalAuthentication) {
                            this.children.show(new Menu.LogoutForm());
                        } else if (properties.externalAuthentication) {
                            this.children.show(new Menu.LoginExternalForm());
                        } else {
                            this.children.show(new Menu.LoginForm());
                        }
                    }
                });
                this.welcome.show(new Welcome({
                    model: new MenuItem({
                        id: 'signin',
                        name: 'Sign In',
                        classes: 'fa fa-user',
                        iconOnly: iconOnly,
                        //change this to true when we can log in or out
                        dropdown: true
                    })
                }));
            }
            if (this.model.get('showTask')) {
                var Tasks = Menu.Item.extend({
                    className: 'dropdown',
                    initialize: function () {
                        if (wreqr.reqres.hasHandler('tasks')) {
                            this.collection = wreqr.reqres.request('tasks');
                        }
                        this.listenTo(wreqr.vent, 'task:update', this.updateTask);
                        this.listenTo(wreqr.vent, 'task:remove', this.updateTask);
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
                            view.children.$el.perfectScrollbar();
                        });
                    }
                });
                this.tasks.show(new Tasks({
                    model: new MenuItem({
                        id: 'tasks',
                        name: 'Tasks',
                        classes: 'fa fa-tasks',
                        iconOnly: iconOnly,
                        dropdown: true,
                        count: true
                    })
                }));
            }
            var Notification = Menu.Item.extend({
                className: 'dropdown',
                initialize: function () {
                    this.listenTo(wreqr.vent, 'notification:delete', this.deleteNotification);
                    this.listenTo(wreqr.vent, 'notification:new', this.addNotification);
                    this.listenTo(wreqr.vent, 'notification:close', this.removeNotification);
                    this.modelBinder = new Backbone.ModelBinder();
                },
                updateScrollbar: function () {
                    var view = this;
                    _.defer(function () {
                        view.children.$el.perfectScrollbar('update');
                    });
                },
                deleteNotification: function () {
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
                removeNotification: function () {
                    if (this.collection) {
                        var len;
                        if (this.collection.length !== 0) {
                            len = this.collection.length;
                        } else {
                            len = '';
                        }
                        this.model.set({countNum: len});
                    }
                },
                addNotification: function () {
                    if (!this.collection) {
                        if (wreqr.reqres.hasHandler('notifications')) {
                            this.collection = wreqr.reqres.request('notifications');
                            this.model.set({countNum: this.collection.length});
                        }
                        this.render();
                    } else {
                        this.model.set({countNum: this.collection.length});
                    }
                },
                onRender: function () {
                    var view = this;
                    if (this.collection) {
                        this.children.show(new Menu.NotificationList({
                            collection: this.collection,
                            childViewContainer: this.children.$el
                        }));
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
            this.notification.show(new Notification({
                model: new MenuItem({
                    id: 'notification',
                    name: 'Notification',
                    classes: 'fa fa-bell',
                    iconOnly: iconOnly,
                    dropdown: true,
                    count: true
                })
            }));
            var Help = Menu.Item.extend({
                className: 'dropdown',
                onRender: function () {
                    var HelpView = Marionette.ItemView.extend({
                        template: helpTemplate,
                        serializeData: function () {
                            return {
                                branding: menuBarView.model.get('branding'),
                                version: menuBarView.model.get('version'),
                                helpUrl: properties.helpUrl || 'help.html'
                            };
                        }
                    });
                    this.children.show(new HelpView());
                }
            });
            this.help.show(new Help({
                model: new MenuItem({
                    id: 'help',
                    name: 'Help',
                    classes: 'fa fa-question-circle',
                    iconOnly: iconOnly,
                    dropdown: true
                })
            }));
            if (this.model.get('showIngest')) {
                var ingest = new IngestMenu({
                    model: new MenuItem({
                        id: 'Ingest',
                        name: 'Ingest',
                        classes: 'fa fa-upload showModal',
                        iconOnly: iconOnly,
                        dropdown: false
                    })
                });
                this.ingest.show(ingest);
            }
            var preferences = new PreferencesMenu({
                model: new MenuItem({
                    id: 'Preferences',
                    name: 'Preferences',
                    classes: 'fa fa-sliders showModal',
                    iconOnly: iconOnly,
                    dropdown: false,
                    preferences: Application.UserModel.get('user>preferences')
                })
            });
            this.preferences.show(preferences);
            var workspaces = new WorkspaceIndicator({model: store.get('workspaces')});
            this.workspaces.show(workspaces);
            this._turnOnCollapsibleMenu();
        },
        _turnOnCollapsibleMenu: function () {
            this._resizeHandler();
            $(window).off('resize.menubar').on('resize.menubar', this._resizeHandler);
        },
        _collapsed: false,
        _widthWhenCollapsed: 0,
        _resizeHandler: function () {
            var view = this;
            view._resizeHandler = _.debounce(function () {
                var menu = view.el.querySelector('.menu-items');
                var collapsedDropdown = view.el.querySelector('#collapsed');
                if (view._collapsed) {
                    if (menu.clientWidth > view._widthWhenCollapsed) {
                        view._collapsed = false;
                        menu.classList.remove('collapsed');
                        menu.classList.remove('is-dropdown');
                        $(menu).off('click');
                    }
                } else {
                    if (menu.scrollWidth !== menu.clientWidth) {
                        view._collapsed = true;
                        view._widthWhenCollapsed = menu.scrollWidth;
                        menu.classList.add('collapsed');
                        menu.classList.add('is-dropdown');
                        $(collapsedDropdown).off('click').on('click', function () {
                            menu.classList.toggle('is-open');
                            if (menu.classList.contains('is-open')) {
                                $('body').on('click.menubar', function (e) {
                                    if (e.target !== menu && $(menu).find(e.target).length === 0) {
                                        $('body').off('click.menubar');
                                        menu.classList.remove('is-open');
                                    }
                                });
                            } else {
                                $('body').off('click.menubar');
                            }
                        });
                    }
                }
            }, 250);
            view._resizeHandler();
        }
    });
    return Menu;
});
