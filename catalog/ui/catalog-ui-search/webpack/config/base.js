var path = require('path');
var CopyWebpackPlugin = require('copy-webpack-plugin');

var resolve = function (place) {
  return path.resolve(__dirname, '../../', place)
};

module.exports = {
    devtool: 'source-map',
    context: resolve('./src/main/webapp/'),
    entry: [
        resolve('./src/main/webapp/main.js')
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
            cometd$: 'cometd/org/cometd',
            bootstrapDatepicker: 'eonasdan-bootstrap-datetimepicker/build/js/bootstrap-datetimepicker.min',
            jquerycometd: 'cometd/jquery/jquery.cometd',
            perfectscrollbar: 'perfect-scrollbar/min/perfect-scrollbar.min',
            spin: 'spin.js/spin',
            q: 'q/q',
            strapdown: 'strapdown/v/0.2',
            spectrum: 'spectrum/spectrum',
            // backbone
            backboneassociations: 'backbone-associations',
            backbonepaginator: 'backbone.paginator/lib/backbone.paginator.min',
            backbonecometd: 'backbone-cometd/backbone.cometd.extension',
            backboneundo: 'Backbone.Undo.js/Backbone.Undo',
            poller: 'backbone-poller/backbone.poller',
            underscore: 'lodash',
            marionette: 'backbone.marionette',
            // TODO test combining
            modelbinder: 'backbone.modelbinder/Backbone.ModelBinder',
            collectionbinder: 'backbone.modelbinder/Backbone.CollectionBinder',
            // application
            application: 'js/application',
            cometdinit: 'js/cometd',
            direction: 'js/direction',
            webglcheck: 'js/webglcheck',
            twodcheck: 'js/2dmapcheck',
            maptype: 'js/maptype',
            spinnerConfig: 'js/spinnerConfig',
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
            fileupload: 'jquery-file-upload/js/jquery.fileupload',
            jquerySortable: 'jquery-ui/sortable',
            // map
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
        ].map(resolve)
    }
};
