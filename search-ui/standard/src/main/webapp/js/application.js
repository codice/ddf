/*global define*/

// #Main Application
define(function (require) {
    'use strict';

    // Load non attached libs and plugins
    require('jqueryui');
    require('bootstrap');
    require('backbonerelational');
    require('backbonecometd');
    require('jquerycometd');

    // Load attached libs and application modules
    var $ = require('jquery'),
        _ = require('underscore'),
        ddf = require('ddf'),
        Marionette = require('marionette'),
        Backbone = require('backbone'),
        ich = require('icanhaz'),
        properties = require('properties'),
        maptype = require('maptype'),
        Application = ddf.module();

    // Setup templates
    ich.addTemplate('main', require('text!templates/main.html'));
    ich.addTemplate('headerLayout', require('text!templates/navbar.layout.html'));
    ich.addTemplate('footerLayout', require('text!templates/footer.layout.html'));
    ich.addTemplate('classificationBanner', require('text!templates/classification/classification-banner.html'));

    Application.Router = Backbone.Router.extend({
        routes: {
            '': 'index'
        },

        initialize: function () {
            _.bindAll(this);
        },


        index: function () {

        }

    });

    // ##Main Application View
    Application.Views.Main = Backbone.View.extend({
        tagName: 'div',
        className: 'height-full',

        initialize: function () {
            var view = this;

            _.bindAll(view);

            ddf.views = {};

        },

        render: function () {
            var view = this;

            view.$el.html(ich.main());

            if (maptype.isNone()) {
                $('#searchControls', this.$el).width('100%');
            }

            return view;
        }

    });


    Application.Views.HeaderLayout = Marionette.Layout.extend({
        template: 'headerLayout',
        className: 'header-layout',

        regions: {
            classification: '.classification-container',
        }
    });

    Application.Views.FooterLayout = Marionette.Layout.extend({
        template: 'footerLayout',
        className: 'footer-layout',

        regions: {
            classification: '.classification-container'
        }
    });

    Application.Views.HeaderBanner = Backbone.View.extend({
        className: "classification-banner",

        initialize: function () {
            var view = this;
            _.bindAll(view);
        },

        render: function () {
            var view = this,
                headerText = properties.header,
                style = properties.style,
                textColor = properties.textColor,
                header = {};

            if(headerText && headerText !== "") {
                //set up header
                header.text = headerText;
                header.style = style;
                header.textColor = textColor;

                view.$el.html(ich.classificationBanner(header));
            }
        }
    });

    Application.Views.FooterBanner = Backbone.View.extend({
        className: "classification-banner",

        initialize: function () {
            var view = this;
            _.bindAll(view);
        },

        render: function () {
            var view = this,
                footerText = properties.footer,
                style = properties.style,
                textColor = properties.textColor,
                footer = {};

            if(footerText && footerText !== "") {
                //set up footer
                footer.text = footerText;
                footer.style = style;
                footer.textColor = textColor;

                view.$el.html(ich.classificationBanner(footer));
            }
        }
    });

    return Application;
});
