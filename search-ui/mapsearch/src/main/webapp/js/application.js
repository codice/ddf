/**
 * Created with IntelliJ IDEA.
 * User: mmacaula
 * Date: 11/11/13
 * Time: 9:07 PM
 * To change this template use File | Settings | File Templates.
 */
/*global define, window*/

// #Main Application
define(function(require) {
    'use strict';

    // Load non attached libs and plugins
    require('jqueryui');
    require('bootstrap');
    require('backbonerelational');

    // Load attached libs and application modules
    var $ = require('jquery'),
        _ = require('underscore'),
        ddf = require('ddf'),
        Marionette = require('marionette'),
        Backbone = require('backbone'),
        ich = require('icanhaz'),
        Application = ddf.module();


    // Setup templates
    ich.addTemplate('main', require('text!templates/main.html'));
    ich.addTemplate('navbarLayout', require('text!templates/navbar.layout.html'));
    ich.addTemplate('footerLayout', require('text!templates/footer.layout.html'));
    ich.addTemplate('classificationBanner', require('text!templates/classification/classification-banner.html'));


    Application.Router = Backbone.Router.extend({
        routes: {
            '': 'index'
        },

        initialize : function() {
            _.bindAll(this);
        },


        index : function () {

        }

    });

    // ##Main Application View
    Application.Views.Main = Backbone.View.extend({
        tagName: 'div',



        initialize: function() {
            var view = this;

            _.bindAll(view);

            ddf.views = {};

        },

        render : function () {
            var view = this;

            view.$el.html(ich.main());


            return view;
        }

    });


    Application.Views.NavBarLayout = Marionette.Layout.extend({
        template : 'navbarLayout',
        className : 'navbar-layout',

        regions : {
            classification : '.classification-container',
            navbar : '.navbar-container'
        }
    });

    Application.Views.NavBar = Backbone.View.extend({
        className: "navbar navbar-fixed-top dark no-select",

        events : {
            'click #app-home' : 'onHome'
        },

        initialize: function () {
            var view = this;

            _.bindAll(view);
        },

        render: function () {
            var view = this;

            ich.addTemplate('navbar', require('text!templates/navbar.html'));
            view.$el.html(ich.navbar());

            return view;
        }

    });


    Application.Views.FooterLayout = Marionette.Layout.extend({
        template : 'footerLayout',
        className : 'footer-layout',

        regions : {
            classification : '.classification-container',
            branding : '.branding-container'
        }
    });

    Application.Views.ClassificationBanner = Backbone.View.extend({
        className: "classification-banner",

        initialize : function () {
            var view = this;
            _.bindAll(view);
        },

        render : function() {
            var view = this,
                classificationConfig = require('properties').classification,
                style,
                validStyles = ['unclassified','confidential','secret','topsecret'],
                text,
                classification = {};

            if (!classificationConfig) {
                // Place holder for us not showing classification banners if needed.  For now, default to Unclassified if there is a problem with configuration
                // to be safe.
                classification.style = 'unclassified';
                classification.text = 'UNCLASSIFIED';
            } else {
                style = classificationConfig.style;
                text = classificationConfig.text;

                if (style && text && _.contains(validStyles, style)) {
                    classification.style = style;
                    classification.text = text;
                } else {
                    // Handle any unanticipated configuration values by defaulting to Unclassified.
                    classification.style = 'unclassified';
                    classification.text = 'UNCLASSIFIED';
                }
            }

            view.$el.html(ich.classificationBanner(classification));
        }
    });






    return Application;
});
