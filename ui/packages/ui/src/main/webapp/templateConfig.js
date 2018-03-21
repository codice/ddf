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
/*global requirejs*/
(function () {
    'use strict';

    requirejs.config({

        paths: {
            // application module template
            applicationGrid: 'templates/application/applicationGrid.handlebars',
            applicationInfo: 'templates/application/applicationInfo.handlebars',
            applicationDetailLayout:'templates/application/application-detail/ApplicationDetail.layout.handlebars',
            pluginTabItemView:'templates/application/application-detail/PluginTab.item.view.handlebars',
            pluginTabCollectionView:'templates/application/application-detail/PluginTab.collection.view.handlebars',
            pluginTabContentItemView:'templates/application/application-detail/PluginTabContent.item.view.handlebars',
            pluginTabContentCollectionView:'templates/application/application-detail/PluginTabContent.collection.view.handlebars',
            iframeView:'templates/application/iframeView.handlebars',
            featureTemplate: 'templates/application/features/features.handlebars',
            featureRowTemplate: 'templates/application/features/featureRow.handlebars',

            //module
            moduleDetailLayout: 'templates/module/ModuleDetail.layout.handlebars',
            systemInformationTemplate: 'templates/module/systeminformation.template.handlebars',
            systemInformationItemTemplate: 'templates/module/systeminformation.item.template.handlebars'
        }
    });
}());