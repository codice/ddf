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
/*global define, setTimeout*/
define([
    'marionette',
    'underscore',
    'jquery',
    './metacard-quality.hbs',
    'js/CustomElements',
    'component/loading-companion/loading-companion.view',
    'js/store',
    'js/Common'
], function(Marionette, _, $, template, CustomElements, LoadingCompanionView, store, Common) {

    var selectedVersion;

    return Marionette.ItemView.extend({
        setDefaultModel: function() {
            this.model = this.selectionInterface.getSelectedResults().first();
        },
        template: template,
        tagName: CustomElements.register('metacard-quality'),
        events: {},
        ui: {},
        selectionInterface: store,
        initialize: function(options) {
            this.selectionInterface = options.selectionInterface || this.selectionInterface;
            if (!options.model) {
                this.setDefaultModel();
            }
            this.loadData();
        },
        loadData: function() {
            LoadingCompanionView.beginLoading(this);
            var self = this;
            setTimeout(function() {
                $.when($.get('./internal/metacard/' + self.model.get('metacard').id + '/attribute/validation').then(function(response) {
                    self._attributeValidation = response;
                }), $.get('./internal/metacard/' + self.model.get('metacard').id + '/validation').then(function(response) {
                    self._metacardValidation = response;
                })).always(function() {
                    self.checkForDuplicate();
                    LoadingCompanionView.endLoading(self);
                    if (!self.isDestroyed) {
                        self.render();
                    }
                });
            }, 1000);
        },
        checkForDuplicate: function() {
            if (this._metacardValidation) {
                this._metacardValidation.forEach(function(validationIssue) {
                    if (validationIssue.message.indexOf('Duplicate data found in catalog') === 0) {
                        var idRegEx = new RegExp("{(.*?)\}");
                        var ids = idRegEx.exec(validationIssue.message)[1].split(', ');
                        ids.forEach(function(metacardId) {
                            validationIssue.message =
                                validationIssue.message.replace(metacardId,
                                    '<a href="#metacards/' + metacardId + '">' + metacardId + '</a>')
                        });
                    }
                });
            }
        },
        onRender: function() {},
        serializeData: function() {
            var self = this;
            var hasMetacardValidation = false;
            var hasAttributeValidation = false;
            if (this._metacardValidation) {
                hasMetacardValidation = this._metacardValidation.length > 0;
            }
            if (this._attributeValidation) {
                hasAttributeValidation = this._attributeValidation.length > 0;
            }
            return {
                attributeValidation: this._attributeValidation,
                hasAttributeValidation: hasAttributeValidation,
                hasMetacardValidation: hasMetacardValidation,
                metacardValidation: this._metacardValidation
            };
        }
    });
});