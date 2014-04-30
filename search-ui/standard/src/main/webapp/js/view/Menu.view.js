/*global define*/
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
    'modelbinder'
], function(Marionette, ich, menubarTemplate, menubarItemTemplate, UserModel, Backbone, notificationMenuTemplate, wreqr, _, loginTemplate, logoutTemplate) {

    ich.addTemplate('menubarItemTemplate', menubarItemTemplate);

    ich.addTemplate('menubarTemplate', menubarTemplate);

    ich.addTemplate('notificationMenuTemplate', notificationMenuTemplate);

    ich.addTemplate('loginTemplate', loginTemplate);

    ich.addTemplate('logoutTemplate', logoutTemplate);

    var Menu = {};

    Menu.UserModel = new UserModel();
    Menu.UserModel.fetch();

    var MenuItem = Backbone.Model.extend({

    });

    Menu.NotificationItem = Marionette.ItemView.extend({
        template: 'notificationMenuTemplate',
        tagName: 'li',
        events: {
            'click' : 'openNotification'
        },
        openNotification: function() {
            wreqr.vent.trigger('notification:open', this.model);
        }
    });

    Menu.NotificationEmpty = Marionette.ItemView.extend({
        className: 'dropdown-width',
        render: function() {
            this.$el.html("No recent notifications.");
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
        initialize: function() {
            this.listenTo(this.model, 'change', this.render);
        }
    });

    Menu.LoginForm = Marionette.ItemView.extend({
        template: 'loginTemplate',
        events: {
            'click .btn-login': 'logInUser',
            'click .btn-clear': 'clearFields'
        },
        logInUser: function() {
            //this doesn't do anything yet
        },
        clearFields: function() {
            this.$('#username').val('');
            this.$('#password').val('');
        }
    });

    Menu.LogoutForm = Marionette.ItemView.extend({
        template: 'logoutTemplate',
        events: {
            'click .btn-logout': 'logOutUser'
        },
        logOutUser: function() {
            //this doesn't do anything yet
        }
    });

    Menu.Bar = Marionette.Layout.extend({
        template: 'menubarTemplate',
        className: 'container-fluid navbar',
        regions: {
            welcome: '#welcome',
            notification: '#notification',
            help: '#help'
        },
        onRender: function() {
            var menuBarView = this;
            if(this.model.get('showWelcome')) {
                var Welcome = Menu.Item.extend({
                    className: 'dropdown',
                    initialize: function() {
                        if(Menu.UserModel && Menu.UserModel.get('user')){
                            this.model.set({name: Menu.UserModel.get('user')});
                        }
                        this.listenTo(Menu.UserModel, 'change', this.updateUser);
                    },
                    updateUser: function() {
                        if(Menu.UserModel.get('user')) {
                            this.model.set({name: Menu.UserModel.get('user')});
                        }
                        this.render();
                    },
                    onRender: function() {
                        if(Menu.UserModel.get('user')) {
                            //uncomment this when logout does something
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
                    dropdown: false
                })}));
            }

            var Notification = Menu.Item.extend({
                className: 'dropdown',
                initialize: function() {
                    wreqr.vent.on('notification:new', _.bind(this.addNotification, this));
                    wreqr.vent.on('notification:close', _.bind(this.removeNotification, this));
                    this.modelBinder = new Backbone.ModelBinder();
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
                    if(this.collection) {
                        this.children.show(new Menu.NotificationList({collection: this.collection, itemViewContainer: this.children.$el}));
                        var bindings = Backbone.ModelBinder.createDefaultBindings(this.el, 'name');
                        this.modelBinder.bind(this.model, this.$el, bindings);
                    } else {
                        this.children.show(new Menu.NotificationEmpty());
                    }
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
                    //TODO replace this with something better once we have help
                    var DefaultView = Marionette.ItemView.extend({
                        className: 'dropdown-width',
                        onRender: function() {
                            this.$el.html(menuBarView.model.get('branding') + " " + menuBarView.model.get('version'));
                        }
                    });
                    this.children.show(new DefaultView());
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