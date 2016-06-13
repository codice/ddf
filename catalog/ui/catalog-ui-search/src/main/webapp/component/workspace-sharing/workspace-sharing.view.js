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
    'text!./workspace-sharing.hbs',
    'text!./workspace-sharing.item.hbs',
    'js/CustomElements',
    'js/store',
    './editable-rows.view',
    'component/dropdown/dropdown.view',
    'component/loading-companion/loading-companion.view',
], function (Backbone, Marionette, _, $, template, itemTemplate, CustomElements, store, EditableRows, DropdownView, Loading) {

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
                  '<div class="col-md-4 permission"></div>',
        regions: {
            email: '.email',
            permission: '.permission'
        },
        updatePermission: function () {
            this.model.set('permission', this.permission.currentView.model.get('value')[0]);
        },
        onRender: function () {
            this.email.show(new Input({ model: this.model }));

            this.permission.show(DropdownView.createSimpleDropdown([
                { icon: 'fa-pencil', label: 'Can edit',  value: 'edit' },
                { icon: 'fa-eye',    label: 'Can view',  value: 'view' }
            ], false, [this.model.get('permission')], IconView));

            this.listenTo(this.permission.currentView.model, 'change:value', this.updatePermission);
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
                  '<div class="col-md-4 permission"></div>',
        regions: {
            role: '.role',
            permission: '.permission'
        },
        updatePermission: function () {
            this.model.set('permission', this.permission.currentView.model.get('value')[0]);
        },
        onRender: function () {
            this.permission.show(DropdownView.createSimpleDropdown([
                { icon: 'fa-ban',    label: 'No Access',  value: 'none' },
                { icon: 'fa-pencil', label: 'Can edit',  value: 'edit' },
                { icon: 'fa-eye',    label: 'Can view',  value: 'view' }
            ], false, [this.model.get('permission')], IconView));

            this.listenTo(this.permission.currentView.model, 'change:value', this.updatePermission);
        }
    });

    var RoleSharingEditor = Marionette.CollectionView.extend({
        childView: SharingByRoleView
    });

    var WorkspaceSharing = Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('workspace-sharing'),
        modelEvents: { 'all': 'render' },
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
            return this.model.get('metacard.sharing') || [];
        },
        getSharingByEmail: function () {
            return _.where(this.getSharing(), { type: 'email' });
        },
        getSharingByRole: function () {
            var view = this;
            var user = store.get('user').get('user');

            return user.get('roles').map(function (role) {
                return _.findWhere(view.getSharing(), { value: role }) || {
                    type: 'role',
                    permission: 'none',
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
                    return role.get('permission') !== 'none';
                }).map(function (role) {
                    return role.toJSON();
                }).value();

            var emails = this.emailCollection.chain()
                .map(function (email) {
                    return _.extend(email.toJSON(), {
                        type: 'email'
                    });
                }).value();

            this.model.set('metacard.sharing', roles.concat(emails));
            this.model.save(null, {
                success: function () {
                    view.$el.trigger(CustomElements.getNamespace()+'close-lightbox');
                    Loading.endLoading(view);
                },
                error: function () {
                    // TODO: handle error case :(
                    Loading.endLoading(view);
                }
            });
        }
    });

    return WorkspaceSharing;
});
