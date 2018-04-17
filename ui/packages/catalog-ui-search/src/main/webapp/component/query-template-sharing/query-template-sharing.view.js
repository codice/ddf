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
/*global define, window*/
define([
    'backbone',
    'marionette',
    'underscore',
    'jquery',
    './query-template-sharing.hbs',
    './query-template-sharing.item.hbs',
    'js/CustomElements',
    'component/singletons/user-instance',
    './editable-rows-template.view',
    'component/dropdown/dropdown.view',
    'component/loading-companion/loading-companion.view',
    'component/announcement'
], function (Backbone, Marionette, _, $, template, itemTemplate, CustomElements, user, EditableRows, DropdownView, Loading, announcement) {

    var Input = Marionette.ItemView.extend({
        template: '<input class="form-control" type="text"/>',
        modelEvents: { change: 'updateValue' },
        events: {
            'change .form-control': 'onChange'
        },
        onRender: function () { this.updateValue(); },
        valueKey: function () {
            return this.options.valueKey || 'value';
        },
        onChange: function (e) {
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
        template: '<div class="email"></div>' +
                  '<div class="action"></div>',
        regions: {
            email: '.email',
            action: '.action'
        },
        onRender: function () {
            this.email.show(new Input({ model: this.model }));
        }
    });

    var EmailSharingEditor = EditableRows.extend({
        embed: function (model) {
            return new SharingByEmailView({ model: model });
        }
    });

    var SharingByRoleView = Marionette.LayoutView.extend({
        className: 'row',
        template: '<div class="role">{{value}}</div>' +
                  '<div class="action"></div>',
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
                        { icon: 'fa-pencil', label: 'Can Access', value: 'access' }
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

    var QueryTemplateSharing = Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('template-sharing-test'),
        modelEvents: {
            reset: 'render',
            sync: 'cleanup'
        },
        regions: {
            byEmail: '.template-sharing-by-email',
            byRole: '.template-sharing-by-role'
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
        getSharingByEmail: function () {
            return this.options.permissions.accessIndividuals != undefined ? 
                this.options.permissions.accessIndividuals
                .map(function (email) {
                    return { value: email }
                }) : [];
        },
        getSharingByRole: function () {
            var view = this;

            var roles = this.options.permissions.accessGroups != undefined ? 
                this.options.permissions.accessGroups
                .map(function (group) {
                    return group;
                }) : [];

            return user.get('user').get('roles').map(function (role) {
                return {
                    attribute: 'role',
                    action: (roles.indexOf(role) === -1) ? 'none' : 'access',
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

            var emailList = this.emailCollection.map(function(email) {
                return email.get('value')
            });

            var roleList = this.collection.chain()
                .filter(function (role) {
                    return role.get('action') !== 'none';
                }).map(function (role) {
                    return role.get('value');
                }).value();

            var templatePerms = {
                    'security.access-individuals': emailList,
                    'security.access-groups': roleList 
            }

            this.updateSharingPermissions(templatePerms)
        },
        updateSharingPermissions: function(templatePerms) {
            var _this = this;
            console.log(this.model.attributes)

            $.ajax({
                //TODO: This URL needs to be a constant pulled from the model?
                url: "/search/catalog/internal/sharing/".concat(_this.options.modelId),
                contentType: "application/json; charset=utf-8",
                dataType: "json",
                type: 'PUT',
                data: JSON.stringify(templatePerms),
                success: function(data) {
                    _this.message('Success!', 'Sharing Settings Saved for Query Template', 'success');
                    _this.cleanup();
                }
            });
        },
        message: function(title, message, type) {
            announcement.announce({
                title: title,
                message: message,
                type: type
            });
        },
        cleanup: function () {
            this.$el.trigger(CustomElements.getNamespace() + 'close-lightbox');
            Loading.endLoading(this);
        }
    });

    return QueryTemplateSharing;
});
