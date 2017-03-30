var path = require('path');
var CopyWebpackPlugin = require('copy-webpack-plugin');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var ExtractTextPlugin = require("extract-text-webpack-plugin");
var LessPluginCleanCSS = require('less-plugin-clean-css');

var resolve = function (place) {
  return path.resolve(__dirname, '../../', place)
};

module.exports = {
    devtool: 'source-map',
    context: resolve('./src/main/webapp/'),
    entry: [
        resolve('./src/main/webapp/js/ApplicationSetup.js')
    ],
    output: {
        path: resolve('./target/webapp'),
        filename: 'bundle.[hash].js'
    },
    lessLoader: {
        lessPlugins: [
            new LessPluginCleanCSS({
                advanced: true
            })
        ]
    },
    plugins: [
        new CopyWebpackPlugin([
            {
                from: resolve('node_modules/cesium/Build/Cesium'),
                to: resolve('target/webapp/cesium'),
                force: true
            },
            {
                from: resolve('node_modules/bootstrap/fonts'),
                to: resolve('target/webapp/fonts'),
                force: true
            },
            {
                from: resolve('node_modules/font-awesome/fonts'),
                to: resolve('target/webapp/fonts'),
                force: true
            }
        ]),
        new HtmlWebpackPlugin({
            title: 'My App',
            filename: 'index.html',
            template: 'index.html'
        }),
        new ExtractTextPlugin("css/styles.[contenthash].css")
    ],
    module: {
        loaders: [
            {
                test: /\.(png|gif|jpg|jpeg)$/,
                loader: 'file'
            },
            {
                test: /Cesium\.js$/,
                loader: 'exports?Cesium!script'
            },
            {
                test: /modelbinder/,
                loader: 'imports?Backbone=backbone,jQuery=jquery,_=underscore'
            },
            {
                test: /jquery-ui/,
                loader: 'imports?jQuery=jquery,$=jquery,jqueryui=jquery-ui'
            },
            {
                test: /bootstrap/,
                loader: 'imports?jQuery=jquery'
            },
            {
                test: /\.jsx$/,
                loader: 'babel?presets[]=react',
                exclude: /(node_modules|target)/
            },
            {
                test: /\.(hbs|handlebars)$/,
                loader: 'handlebars'
            },
            {
                 test: /\.(css|less)$/,
                loader: ExtractTextPlugin.extract("style", "css?url=false&sourceMap!less?sourceMap")
            }
        ]
    },
    resolve: {
        extensions: ['', '.js', '.jsx'],
        alias: {
            bootstrapDatepicker: 'eonasdan-bootstrap-datetimepicker/build/js/bootstrap-datetimepicker.min',
            strapdown: 'strapdown/v/0.2',
            // backbone
            backboneassociations: 'backbone-associations',
            backboneundo: 'Backbone.Undo.js/Backbone.Undo',
            poller: 'backbone-poller/backbone.poller',
            underscore: 'lodash',
            marionette: 'backbone.marionette',
            // TODO test combining
            modelbinder: 'backbone.modelbinder/Backbone.ModelBinder',
            'Backbone.ModelBinder': 'backbone.modelbinder/Backbone.ModelBinder',
            collectionbinder: 'backbone.modelbinder/Backbone.CollectionBinder',
            // application
            application: 'js/application',
            direction: 'js/direction',
            webglcheck: 'js/webglcheck',
            twodcheck: 'js/2dmapcheck',
            maptype: 'js/maptype',
            wreqr: 'js/wreqr',
            properties: 'properties',
            // jquery
            jqueryuiCore: 'jquery-ui/core',
            datepicker: 'jquery-ui/datepicker',
            progressbar: 'jquery-ui/progressbar',
            slider: 'jquery-ui/slider',
            mouse: 'jquery-ui/mouse',
            datepickerOverride: 'jquery/js/plugin/jquery-ui-datepicker-4digitYearOverride-addon',
            purl: 'purl/purl',
            multiselect$: 'jquery-ui-multiselect-widget/src/jquery.multiselect',
            multiselectfilter: 'jquery-ui-multiselect-widget/src/jquery.multiselect.filter',
            'jquery.ui.widget': 'jquery-ui/widget',
            jquerySortable: 'jquery-ui/sortable',
            // map
            //openlayers$: 'openlayers/dist/ol-debug.js',  // useful for debugging openlayers
            //cesium$: 'cesium/Build/CesiumUnminified/Cesium.js',  //useful for debuggin cesium
            cesium$: 'cesium/Build/Cesium/Cesium.js',
            drawHelper: 'cesium-drawhelper/DrawHelper',
            usngs: 'usng.js/usng',
            wellknown: 'wellknown/wellknown'
        },
        root: [
            './node_modules',
            './src/main/webapp/',
            './src/main/webapp/js',
            './src/main/webapp/lib/',
            './target/webapp/lib',
            './target/META-INF/resources/webjars/',
        ].map(resolve)
    }
};
