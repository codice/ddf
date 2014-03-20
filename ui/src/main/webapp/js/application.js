/*global define*/

// #Main Application
define(function (require) {
    'use strict';

    // Load non attached libs and plugins


    // Load attached libs and application modules
    var _ = require('underscore'),
        Backbone = require('backbone'),
        Marionette = require('marionette'),
        ich = require('icanhaz'),
        poller = require('poller'),
        Application = {};
    require('marionette');

    // Setup initial templates that we know we'll need
    ich.addTemplate('tabs', require('text!templates/tabs.handlebars'));
    ich.addTemplate('headerLayout', require('text!templates/header.handlebars'));
    ich.addTemplate('footerLayout', require('text!templates/footer.handlebars'));
    ich.addTemplate('moduleTab', require('text!templates/moduleTab.handlebars'));

    Application.App = new Marionette.Application();

    //add regions
    Application.App.addRegions({
        headerRegion: 'header',
        footerRegion: 'footer',
        mainRegion: 'main'
    });

    //setup models
    var Module = require('js/models/Module');
    var options = {
        delay: 30000
    };

    var addModuleRegions = function() {
        //add tab regions
        Application.ModuleModel.get('value').each(function(module) {
            var obj = {};
            obj[module.get('id')] = '#' + module.get('id');
            if(!Application.App.getRegion(module.get('id'))) {
                Application.App.addRegions(obj);
            }
        });
    };

    Application.ModuleModel = new Module.Model();
    Application.ModuleModel.fetch().success(addModuleRegions);

    var modulePoller = poller.get(Application.ModuleModel, options);
    modulePoller.on('success', addModuleRegions);

    modulePoller.start();

    //configure the router (we aren't using this yet)
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

    return Application;
});
