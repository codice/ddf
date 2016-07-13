/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
/*global define*/
define([
    'backbone',
    'marionette',
    'underscore',
    'jquery',
    './workspace-sharing.hbs',
    './workspace-sharing.item.hbs',
    'js/CustomElements',
    'component/singletons/user-instance',
    './editable-rows.view',
    'component/dropdown/dropdown.view',
    'component/loading-companion/loading-companion.view',
], function (Backbone, Marionette, _, $, template, itemTemplate, CustomElements, user, EditableRows, DropdownView, Loading) {

    var Input = Marionette.ItemView.extend({
        template: '<input class="form-control" type="text"/>',
        modelEvents: { change: 'updateValue' },
        events: {
            'keyup .form-control': 'onKeyup'
        },
        onRender: function () { this.updateValue(); },
        valueKey: function () {
            return this.options.valueKey || 'value';
        },
        onKeyup: function (e) {
            this.model.set(this.valueKey(), e.target.value);
        },
        updateValue: function () {
            this.$('input').val(this.model.get(this.valueKey()));
        }
    });

    var IconView = Marionette.ItemView.extend({
        className: 'icon-view',
        template: '<i class="fa {{icon}}"></i> <span class="icon-label">{{label}}</span>'
    });

    var SharingByEmailView = Marionette.LayoutView.extend({
        className: 'row',
        template: '<div class="col-md-8 email"></div>' +
                  '<div class="col-md-4 action"></div>',
        regions: {
            email: '.email',
            action: '.action'
        },
        initialize: function () {
            this.model.set('action', this.model.get('action') || 'view');
        },
        updateAction: function () {
            this.model.set('action', this.action.currentView.model.get('value')[0]);
        },
        onRender: function () {
            this.email.show(new Input({ model: this.model }));

            this.action.show(DropdownView.createSimpleDropdown(
                {
                    list: [
                        { icon: 'fa-eye',    label: 'Can view',  value: 'view' },
                        { icon: 'fa-pencil', label: 'Can edit',  value: 'edit' }
                    ],
                    defaultSelection: [this.model.get('action')],
                    customChildView: IconView
                }
            ));

            this.listenTo(this.action.currentView.model, 'change:value', this.updateAction);
        }
    });

    var EmailSharingEditor = EditableRows.extend({
        embed: function (model) {
            return new SharingByEmailView({ model: model });
        }
    });

    var SharingByRoleView = Marionette.LayoutView.extend({
        className: 'row',
        template: '<div class="col-md-8 role">{{value}}</div>' +
                  '<div class="col-md-4 action"></div>',
        regions: {
            role: '.role',
            action: '.action'
        },
        updateAction: function () {
            this.model.set('action', this.action.currentView.model.get('value')[0]);
        },
        onRender: function () {
            this.action.show(DropdownView.createSimpleDropdown(
                {
                    list: [
                        { icon: 'fa-ban',    label: 'No Access',  value: 'none' },
                        { icon: 'fa-pencil', label: 'Can edit',  value: 'edit' },
                        { icon: 'fa-eye',    label: 'Can view',  value: 'view' }
                    ],
                    defaultSelection: [this.model.get('action')],
                    customChildView: IconView
                }
            ));

            this.listenTo(this.action.currentView.model, 'change:value', this.updateAction);
        }
    });

    var RoleSharingEditor = Marionette.CollectionView.extend({
        childView: SharingByRoleView
    });

    // aggregates a list of action types
    // read = view
    // read + update = edit
    var aggregateActions = function (actions) {
        if (_.contains(actions, 'read')) {
            if (_.contains(actions, 'update')) {
                return 'edit'
            }
            return 'view';
        }
        return 'none';
    };

    var aggregatePermissions = function (permissions) {
        return _.chain(permissions)
            .groupBy(function (permission) {
                return permission.attribute + permission.value;
            })
            .map(function (permissions) {
                return _.extend({}, permissions[0], {
                    action: aggregateActions(_.pluck(permissions, 'action'))
                });
            })
            .value();
    };

    // explodes action type
    // view = read
    // edit = read + update
    var explodeActions = function (action) {
        if (action === 'edit') {
            return ['read', 'update'];
        } else if (action === 'view') {
            return ['read'];
        } else {
            return [];
        }
    };

    var explodePermissions = function (permissions) {
        return _.chain(permissions)
            .map(function (permission) {
                return _.map(explodeActions(permission.action), function (action) {
                    return _.extend({}, permission, { action: action })
                });
            })
            .flatten(true)
            .value();
    };

    var WorkspaceSharing = Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('workspace-sharing'),
        modelEvents: {
            reset: 'render',
            sync: 'cleanup'
        },
        regions: {
            byEmail: '.workspace-sharing-by-email',
            byRole: '.workspace-sharing-by-role'
        },
        events: {
            'click .save': 'save',
            'click .reset': 'render' // resetting can be done by re-rendering and flushing the state
        },
        serializeData: function () {
            return {
                link: window.location.href + '/' + this.model.get('id')
            }
        },
        getSharing: function () {
            return aggregatePermissions(this.model.get('metacard.sharing') || []);
        },
        getSharingByEmail: function () {
            return _.where(this.getSharing(), { attribute: 'email' });
        },
        getSharingByRole: function () {
            var view = this;

            return user.get('user').get('roles').map(function (role) {
                return _.findWhere(view.getSharing(), { value: role }) || {
                    attribute: 'role',
                    action: 'none',
                    value: role
                };
            });
        },
        onRender: function () {
            this.collection = new Backbone.Collection(this.getSharingByRole());
            this.emailCollection = new Backbone.Collection(this.getSharingByEmail());

            this.byEmail.show(new EmailSharingEditor({
                collection: this.emailCollection
            }));

            this.byRole.show(new RoleSharingEditor({
                collection: this.collection
            }));
        },
        save: function () {
            var view = this;

            Loading.beginLoading(view);

            var roles = this.collection.chain()
                .filter(function (role) {
                    return role.get('action') !== 'none';
                }).map(function (role) {
                    return role.toJSON();
                }).value();

            var emails = this.emailCollection.chain()
                .map(function (email) {
                    return _.extend(email.toJSON(), {
                        attribute: 'email'
                    });
                }).value();

            this.model.set('metacard.sharing', explodePermissions(roles.concat(emails)));
            this.model.save()
        },
        cleanup: function () {
            this.$el.trigger(CustomElements.getNamespace() + 'close-lightbox');
            Loading.endLoading(this);
        }
    });

    return WorkspaceSharing;
});
