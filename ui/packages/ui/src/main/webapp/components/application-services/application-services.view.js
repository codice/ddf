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
    'marionette',
    'js/models/Service',
    'text!./application-services.hbs',
    'components/service-item/service-item.collection.view',
    'js/CustomElements',
    'js/wreqr.js'
    ],function (Marionette, Service, template, ServiceItemCollectionView, CustomElements, wreqr) {

    return Marionette.Layout.extend({
        tagName: CustomElements.register('application-services'),
        template: template,
        regions: {
            collectionRegion: '.services'
        },
        initialize: function(options) {
            this.options.url = this.model ? 
            "./jolokia/exec/org.codice.ddf.admin.application.service.ApplicationService:service=application-service/getServices/" + this.model.get('appId') : undefined;
            this.model = new Service.Response({url: this.options.url});
            this.model.fetch();
            this.listenTo(wreqr.vent, 'refreshConfigurations', function() {
                this.model.fetch();
            }.bind(this));
        },
        onRender: function() {
            this.collectionRegion.show(new ServiceItemCollectionView({ collection: this.model.get('value'), showWarnings: true }));
        }
    });

});