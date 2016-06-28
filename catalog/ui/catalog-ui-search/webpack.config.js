var path = require('path')

var resolve = function (place) {
  return path.resolve(__dirname, place)
}

module.exports = {
  context: resolve('./src/main/webapp/js'),
  entry: resolve('./src/main/webapp/main.js'),
  output: {
    path: resolve('./target/webapp'),
    filename: 'bundle.js'
  },
  devServer: {
    host: '0.0.0.0',
    port: 8282,
    contentBase: './target/webapp/',
    proxy: {
      //'/lib/*': { target: 'https://localhost:8993', secure: false },
      '/search/catalog/*': {
        target: 'https://localhost:8993',
        secure: false
      },
      '/search/cometd/*': {
        target: 'https://localhost:8993',
        secure: false
      },
      '/services/*': {
        target: 'https://localhost:8993',
        secure: false
      }
    }
  },
  devtool: 'eval',
  module: {
    loaders: [
      { test: /^text/, loader: 'text' },
      { test: /\.css$/, loader: "css" },
      {
        test: /\.(png|gif|jpg|jpeg)$/,
        loader: 'file-loader'
      },
      { test: /Cesium\.js$/, loader: 'exports?Cesium!script' },
      {
        test: /modelbinder/,
        loader: 'imports?Backbone=backbone,jQuery=jquery,_=underscore'
      }
    ]
  },
  resolve: {
    alias: {
      bootstrap: 'bootstrap/dist/js/bootstrap.min',
      bootstrapselect: 'bootstrap-select/dist/js/bootstrap-select.min',
      cometd$: 'cometd/org/cometd',
      bootstrapDatepicker: 'eonasdan-bootstrap-datetimepicker/build/js/bootstrap-datetimepicker.min',
      jquerycometd: 'cometd/jquery/jquery.cometd',
      moment: 'moment/min/moment.min',
      perfectscrollbar: 'perfect-scrollbar/min/perfect-scrollbar.min',
      spin: 'spin.js/spin',
      q: 'q/q',
      strapdown: 'strapdown/v/0.2',
      spectrum: 'spectrum/spectrum',
      // backbone
      //backbone: 'components-backbone/backbone',
      //backboneassociations: 'backbone-associations/backbone-associations',
      backboneassociations: 'backbone-associations',
      backbonepaginator: 'backbone.paginator/lib/backbone.paginator.min',
      backbonecometd: 'backbone-cometd/backbone.cometd.extension',
      backboneundo: 'Backbone.Undo.js/Backbone.Undo',
      poller: 'backbone-poller/backbone.poller',
      underscore: 'lodash/lodash',
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
      jquery$: 'jquery/dist/jquery.min',
      jqueryCookie: 'jquery-cookie/jquery.cookie',
      jqueryuiCore: 'jquery-ui/ui/minified/jquery.ui.core.min',
      datepicker: 'jquery-ui/ui/minified/jquery.ui.datepicker.min',
      progressbar: 'jquery-ui/ui/minified/jquery.ui.progressbar.min',
      slider: 'jquery-ui/ui/minified/jquery.ui.slider.min',
      mouse: 'jquery-ui/ui/minified/jquery.ui.mouse.min',
      datepickerOverride: 'jquery/js/plugin/jquery-ui-datepicker-4digitYearOverride-addon',
      purl: 'purl/purl',
      multiselect$: 'jquery-ui-multiselect-widget/src/jquery.multiselect',
      multiselectfilter: 'jquery-ui-multiselect-widget/src/jquery.multiselect.filter',
      'jquery.ui.widget': 'jquery-ui/ui/minified/jquery.ui.widget.min',
      fileupload: 'jquery-file-upload/js/jquery.fileupload',
      jquerySortable: 'jquery-ui/ui/minified/jquery.ui.sortable.min',
      // handlebars
      handlebars: 'handlebars/handlebars.min',
      // require plugins
      text: 'requirejs-plugins/lib/text',
      css: 'require-css/css.min',
      // pnotify
      pnotify: 'pnotify/jquery.pnotify.min',
      // map
      cesium$: 'cesiumjs/Cesium/Cesium.js',
      'cesium.css': 'cesiumjs/Cesium/Widgets/widgets.css',
      //cesium: 'cesiumjs/Cesium/Cesium',
      //cesium: 'cesium/Cesium/Cesium.js',
      //cesium: 'cesium/Build/Cesium/Cesium',
      //cesium$: 'cesium/Build/CesiumUnminified/Cesium.js',
      //'map.css': 'cesium/Build/Cesium/Widgets/widgets.css',
      //cesium: 'cesium/Source/Cesium',
      //cesium: 'cesium/Source/Core/buildModuleUrl',
      terraformer: 'terraformer/terraformer',
      terraformerWKTParser: 'terraformer-wkt-parser/terraformer-wkt-parser',
      drawHelper: 'cesium-drawhelper/DrawHelper',
      // openlayers: 'openlayers3/build/ol',
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
}
