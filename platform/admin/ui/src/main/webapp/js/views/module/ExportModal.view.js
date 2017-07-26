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
/* global define */
define([
        'icanhaz',
        'underscore',
        'marionette',
        'backbone',
        'jquery',
        'js/views/Modal',
        'text!templates/module/ExportModal.handlebars',
        'spin',
        'spinnerConfig',
        'js/models/Alerts.js',
        'js/views/Alerts.view',
        'js/models/module/Export.js'
    ],
    function (ich, _, Marionette, Backbone, $, Modal, exportModal, Spinner, spinnerConfig, AlertsModel, AlertsView, Export) {

        function generateAlertTitle(alertType){
            switch(alertType){
                case 'warnings':
                    return 'There were some issues copying certain files.';
                default:
                    return 'System failed to export.';
            }
        }
        function generateAlertLevel(alertType){
            switch(alertType){
                case 'warnings':
                    return 'warning';
                default:
                    return 'danger';
            }
        }
        function generateAlertModel(alertType, alerts){
            return {
                title: generateAlertTitle(alertType),
                details: alerts,
                level: generateAlertLevel(alertType)
            };
        }

        ich.addTemplate('exportModal', exportModal);

        var ExportModal = Modal.extend({
            template: 'exportModal',
            model: new Export(),
            regions: {
                jolokiaError: '.alerts'
            },
            modelEvents: {
                'change:warnings': function () {
                    this.updateAlerts('warnings');
                },
                'change:errors': function () {
                    this.updateAlerts('errors');
                },
                'change:inProgress': 'progressChanged'
            },
            events: {
                'click button.btn-primary': function() {
                    this.model.export();
                }
            },
            spinner: new Spinner(_.clone({}, spinnerConfig, {color: '#000000'})),
            initialize: function () {
                this.modelBinder = new Backbone.ModelBinder();
            },
            onRender: function () {
                this.setupPopOvers();
                this.modelBinder.bind(this.model, this.el);
            },
            setupPopOvers: function () {
                _.each(this.el.querySelectorAll('a.description'), function (element) {
                    var options = {
                        title: $(element).data('popoverTitle'),
                        content: $(element).data('popoverContent'),
                        trigger: 'hover'
                    };
                    $(element).popover(options);
                });
            },
            updateAlerts: function (alertType) {
                var view = this;
                var alerts = this.model.get(alertType);
                if (alerts.length >= 0) {
                    view.jolokiaError.show(new AlertsView.View({
                        'model': new AlertsModel.Alert(generateAlertModel(alertType, alerts))
                    }));
                }
            },
            progressChanged: function () {
                var inProgress = this.model.get('inProgress');
                if (inProgress) {
                    this.spinner.spin(this.el);
                } else {
                    this.spinner.stop();
                    this.handleFinishedExport();
                }
            },
            handleFinishedExport: function () {
                if (this.isAutoClosable()) {
                    this.$el.modal('hide');
                }
            },
            isAutoClosable: function () {
                return this.model.get('warnings').length === 0 && this.model.get('errors').length === 0;
            }
        });

        return ExportModal;
    });