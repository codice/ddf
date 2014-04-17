/*global requirejs*/
(function () {
    'use strict';

    requirejs.config({

        paths: {

            bootstrap: 'lib/components-bootstrap/js/bootstrap.min',
            spin: 'lib/spin.js/spin',
            q: 'lib/q/q',

            // backbone
            backbone: 'lib/components-backbone/backbone-min',
            backbonerelational: 'lib/backbone-relational/backbone-relational',
            underscore: 'lib/lodash/dist/lodash.underscore.min',
            marionette: 'lib/marionette/lib/backbone.marionette.min',
            // TODO test combining
            modelbinder: 'lib/backbone.modelbinder/Backbone.ModelBinder.min',
            collectionbinder: 'lib/backbone.modelbinder/Backbone.CollectionBinder.min',
            poller: 'lib/backbone-poller/backbone.poller',

            // ddf
            spinnerConfig : 'js/spinnerConfig',
            properties: 'properties',

            // jquery
            jquery: 'lib/jquery/jquery.min',
            jqueryui: 'lib/jquery-ui/ui/minified/jquery-ui.min',
            'jquery.ui.widget': 'lib/jquery-ui/ui/minified/jquery.ui.widget.min',
            multiselect: 'lib/bootstrap-multiselect/js/bootstrap-multiselect',
            perfectscrollbar: 'lib/perfect-scrollbar/min/perfect-scrollbar-0.4.8.with-mousewheel.min',
            fileupload: 'lib/jquery-file-upload/js/jquery.fileupload',
            fileuploadiframe: 'lib/jquery-file-upload/js/jquery.iframe-transport',

            // handlebars
            handlebars: 'lib/handlebars/handlebars.min',
            icanhaz: 'lib/icanhandlebarz/ICanHandlebarz',

            // require plugins
            text: 'lib/requirejs-plugins/lib/text',

            // templates
            applicationTemplate: '/applications/templates/application.handlebars',
            applicationNodeTemplate: '/applications/templates/applicationNode.handlebars',
            detailsTemplate: '/applications/templates/details.handlebars',
            applicationNew: '/applications/templates/applicationNew.handlebars',
            mvnItemTemplate: '/applications/templates/mvnUrlItem.handlebars',
            fileProgress: '/applications/templates/fileProgress.handlebars'
        },


        shim: {

            backbone: {
                deps: ['underscore', 'jquery'],
                exports: 'Backbone'
            },

            backbonerelational: ['backbone'],

            marionette: {
                deps: ['jquery', 'underscore', 'backbone'],
                exports: 'Marionette'
            },
            underscore: {
                exports: '_'
            },
            handlebars: {
                exports: 'Handlebars'
            },
            icanhaz: {
                deps: ['handlebars'],
                exports: 'ich'
            },

            jqueryui: ['jquery'],
            bootstrap: ['jqueryui']

        },

        waitSeconds: 15
    });

}());