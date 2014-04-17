/*global requirejs*/
(function () {
    'use strict';

    requirejs.config({

        paths: {

            // templates
            applicationTemplate: '/applications/templates/application.handlebars',
            applicationNodeTemplate: '/applications/templates/applicationNode.handlebars',
            detailsTemplate: '/applications/templates/details.handlebars',
            applicationNew: '/applications/templates/applicationNew.handlebars',
            mvnItemTemplate: '/applications/templates/mvnUrlItem.handlebars',
            fileProgress: '/applications/templates/fileProgress.handlebars'
        }
    });
}());