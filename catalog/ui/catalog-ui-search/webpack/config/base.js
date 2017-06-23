var path = require('path');
var CopyWebpackPlugin = require('copy-webpack-plugin');
var HtmlWebpackPlugin = require('html-webpack-plugin');
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
    jshint: {
        asi: true,            // tolerate automatic semicolon insertion
        bitwise: true,        // Prohibits the use of bitwise operators such as ^ (XOR), | (OR) and others.
        forin: true,          // Requires all for in loops to filter object's items.
        latedef: true,        // Prohibits the use of a variable before it was defined.
        newcap: true,         // Requires you to capitalize names of constructor functions.
        noarg: true,          // Prohibits the use of arguments.caller and arguments.callee. Both .caller and .callee make quite a few optimizations impossible so they were deprecated in future versions of JavaScript.
        noempty: true,         // Warns when you have an empty block in your code.
        regexp: true,         // Prohibits the use of unsafe . in regular expressions.
        undef: true,          // Prohibits the use of explicitly undeclared variables.
        unused: true,         // Warns when you define and never use your variables.
        maxlen: 250,          // Set the maximum length of a line to 250 characters.  If triggered, the line should be wrapped.
        eqeqeq: true,         // Prohibits the use of == and != in favor of === and !==

        // Relaxing Options
        scripturl: true,      // This option suppresses warnings about the use of script-targeted URLsâ€”such as

        reporter: require('jshint-loader-reporter')('stylish'),

        // options here to override JSHint defaults
        globals: {
            console: true,
            module: true,
            define: true
        }
    },
    plugins: [
        new CopyWebpackPlugin([
            {
                from: resolve('node_modules/cesium/Build/Cesium'),
                to: resolve('target/webapp/cesium'),
                force: true
            },
            {
                from: resolve('src/main/webapp/styles/fonts'),
                to: resolve('target/webapp/css/fonts'),
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
            },
            {
                from: resolve('src/main/webapp/styles/vars.less'),
                to: resolve('target/webapp/styles/vars.less'),
                force: true
            }
        ]),
        new HtmlWebpackPlugin({
            title: 'My App',
            filename: 'index.html',
            template: 'index.html'
        })
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
                test: /\.jsx?$/,
                loader: 'babel',
                exclude: /(node_modules|target)/
            },
            {
                test: /\.(hbs|handlebars)$/,
                loader: 'handlebars'
            },
            {
                test: /\.woff($|\?)|\.woff2($|\?)|\.ttf($|\?)|\.eot($|\?)|\.svg($|\?)/,
                loader: 'url-loader'
            },
            {
                 test: /\.(css|less)$/,
                loader: "style!css?sourceMap!less?sourceMap"
            }
        ],
        postLoaders: [
            {
                test: /\.js$/,
                exclude: [/node_modules/],
                loader: 'jshint-loader'
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
            purl: 'purl/purl',
            multiselect$: 'jquery-ui-multiselect-widget/src/jquery.multiselect',
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
            './src/main/webapp/css',
            './src/main/webapp/lib/',
            './target/webapp/lib',
            './target/META-INF/resources/webjars/',
        ].map(resolve)
    }
};
