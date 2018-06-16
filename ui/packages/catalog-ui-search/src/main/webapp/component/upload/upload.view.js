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
    'marionette',
    'underscore',
    'jquery',
    './upload.hbs',
    'js/CustomElements',
    'component/router/router',
    'component/content/upload/content.upload.view',
    'js/model/Query',
    'js/cql',
    'component/singletons/user-instance',
    'component/upload/upload'
], function (Marionette, _, $, template, CustomElements, router,
             uploadContentView, Query, cql, user, uploadInstance) {

    return Marionette.LayoutView.extend({
        template: template,
        tagName: CustomElements.register('upload'),
        regions: {
            uploadDetails: '.upload-details'
        },
        initialize: function(){
            this.listenTo(router, 'change', this.handleRoute);
            this.listenTo(uploadInstance, 'change:currentUpload', this.onBeforeShow);
        },
        handleRoute: function(){
            const routerJSON = router.toJSON();
            if (routerJSON.name === 'openUpload'){
                var uploadId = routerJSON.args[0];
                var upload = user.get('user').get('preferences').get('uploads').get(uploadId);
                if (!upload) {
                    router.notFound();
                } else {
                    const queryForMetacards = new Query.Model({
                        cql: cql.write({
                            type: 'OR',
                            filters: _.flatten(upload.get('uploads').filter(function(file){
                                return file.id || file.get('children') !== undefined;
                            }).map(function(file){
                                if (file.get('children') !== undefined) {
                                    return file.get('children').map((child) => ({
                                        type: '=',
                                        value: child,
                                        property: '"id"'
                                    }));
                                } else {
                                    return {
                                        type: '=',
                                        value: file.id,
                                        property: '"id"'
                                    };
                                }
                            }).concat({
                                type: '=',
                                value: '-1',
                                property: '"id"'
                            }))
                        }),
                        federation: 'enterprise'
                    });
                    if (uploadInstance.get('currentQuery')){
                        uploadInstance.get('currentQuery').cancelCurrentSearches();
                    }
                    queryForMetacards.startSearch();
                    uploadInstance.set({
                        currentResult: queryForMetacards.get('result'),
                        currentUpload: upload,
                        currentQuery: queryForMetacards
                    });
                }
            }
        },
        onRender: function() {
            this.handleRoute();
        },
        onBeforeShow: function(){
            if (uploadInstance.get('currentUpload')) {
                this.showSubViews();
            }
        },
        showSubViews: function() {
            this.uploadDetails.show(new uploadContentView());
        }
    });
});
