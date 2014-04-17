/*global requirejs*/
(function () {
    'use strict';

    requirejs.config({

        paths: {

            // templates
            applicationWrapperTemplate: '/installer/templates/application.handlebars',
            applicationTemplate: '/installer/lib/application-module/templates/application.handlebars',
            applicationNodeTemplate: '/installer/lib/application-module/templates/applicationNode.handlebars',
            detailsTemplate: '/installer/lib/application-module/templates/details.handlebars',
            applicationNew: '/installer/lib/application-module/templates/applicationNew.handlebars',
            mvnItemTemplate: '/installer/lib/application-module/templates/mvnUrlItem.handlebars',
            fileProgress: '/installer/lib/application-module/templates/fileProgress.handlebars'
        }
    });
}());