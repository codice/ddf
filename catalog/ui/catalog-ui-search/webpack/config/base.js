var path = require('path');
var CopyWebpackPlugin = require('copy-webpack-plugin');

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
        filename: 'bundle.js'
    },
    plugins: [
        new CopyWebpackPlugin([
            {
                from: resolve('node_modules/cesium/Build/Cesium'),
                to: resolve('target/webapp/cesium'),
                force: true
            },
            {
                from: resolve('node_modules/eonasdan-bootstrap-datetimepicker/build/css/bootstrap-datetimepicker.min.css'),
                to: resolve('target/webapp/lib/eonasdan-bootstrap-datetimepicker/css/bootstrap-datetimepicker.css'),
                force:true
            },
            {
                from: resolve('node_modules/bootstrap'),
                to: resolve('target/webapp/lib/bootstrap'),
                force: true
            },
            {
                from: resolve('node_modules/font-awesome'),
                to: resolve('target/webapp/lib/font-awesome'),
                force: true
            },
            {
                from: resolve('target/META-INF/resources/webjars/jquery-ui-multiselect-widget/1.14'),
                to: resolve('target/webapp/lib/jquery-ui-multiselect-widget'),
                force: true
            }
        ])
    ],
    module: {
        loaders: [
            {
                test: /\.css$/,
                loader: 'css'
            },
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
            }
        ]
    },
    resolve: {
        extensions: ['', '.js', '.jsx'],
        alias: {
            bootstrap: 'bootstrap/dist/js/bootstrap.min',
            bootstrapselect: 'bootstrap-select/dist/js/bootstrap-select.min',
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
            multiselect$: 'jquery-ui-multiselect-widget/1.14/src/jquery.multiselect',
            multiselectfilter: 'jquery-ui-multiselect-widget/1.14/src/jquery.multiselect.filter',
            'jquery.ui.widget': 'jquery-ui/widget',
            jquerySortable: 'jquery-ui/sortable',
            // map
            //openlayers$: 'openlayers/dist/ol-debug.js',  // useful for debugging openlayers
            //cesium$: 'cesium/Build/CesiumUnminified/Cesium.js',  //useful for debuggin cesium
            cesium$: 'cesium/Build/Cesium/Cesium.js',
            'cesium.css': 'cesium/Build/Cesium/Widgets/widgets.css',
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
