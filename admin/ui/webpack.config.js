var webpack = require('webpack')
var validate = require('webpack-validator')
var merge = require('webpack-merge')
var path = require('path')
var glob = require('glob')
var HtmlWebpackPlugin = require('html-webpack-plugin')

var config = {
  output: {
    publicPath: '/',
    filename: 'bundle.js',
    path: path.resolve(__dirname, 'target', 'webapp')
  },
  devtool: 'source-map',
  entry: ['babel-polyfill'],
  module: {
    loaders: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        loader: 'babel'
      },
      {
        test: /\.less$/,
        loader: 'style!css?modules&importLoaders=1&localIdentName=[name]__[local]___[hash:base64:5]!less'
      },
      {
        test: /\.json$/,
        loader: 'json'
      }
    ]
  },
  plugins: [
    new webpack.ProvidePlugin({
      Promise: 'es6-promise'
    })
  ]
}

if (process.env.NODE_ENV === 'production') {
  config = merge.smart(config, {
    entry: ['./src/main/webapp'],
    plugins: [
      new webpack.DefinePlugin({
        'process.env.NODE_ENV': '"production"'
      }),
      new webpack.optimize.UglifyJsPlugin({
        output: {
          comments: false
        },
        compress: {
          drop_console: true,
          warnings: false
        }
      })
    ]
  })
} else if (process.env.NODE_ENV === 'ci') {
  config = merge.smart(config, {
    devtool: 'source-map',
    node: {
      __filename: true
    },
    output: {
      publicPath: '',
      filename: 'bundle.js',
      path: path.resolve(__dirname, 'target', 'ci')
    },
    entry: glob.sync('./src/main/webapp/**/*spec.js')
        .map(function (spec) { return path.resolve(spec) }),
    plugins: [new HtmlWebpackPlugin()],
    module: {
      loaders: [
        {
          test: /spec\.js$/,
          loaders: [
            'mocha',
            path.resolve(__dirname, 'spec-loader.js')
          ],
          exclude: /(node_modules|target)/
        }
      ]
    },
    externals: {
      'react/addons': true,
      'react/lib/ExecutionEnvironment': true,
      'react/lib/ReactContext': true
    }
  })
} else if (process.env.NODE_ENV === 'test') {
  config = merge.smart(config, {
    devtool: 'source-map',
    output: {
      publicPath: '',
      filename: 'bundle.js',
      path: path.resolve(__dirname, 'target', 'ci')
    },
    entry: [
      'stack-source-map/register'
    ].concat(
      glob.sync('./src/main/webapp/**/*spec.js')
          .map(function (spec) { return path.resolve(spec) })
    ),
    devServer: {
      noInfo: true,
      contentBase: 'src/main/resources/',
      inline: true,
      compress: true,
      hot: true,
      host: '0.0.0.0',
      port: 8181
    },
    plugins: [
      new HtmlWebpackPlugin(),
      new webpack.HotModuleReplacementPlugin()
    ],
    module: {
      loaders: [
        {
          test: /spec\.js$/,
          loader: 'mocha',
          exclude: /(node_modules|target)/
        }
      ]
    },
    externals: {
      'react/addons': true,
      'react/lib/ExecutionEnvironment': true,
      'react/lib/ReactContext': true
    }
  })
} else {
  config = merge.smart(config, {
    entry: [
      'react-hot-loader/patch',
      './src/main/webapp'
    ],
    devServer: {
      noInfo: true,
      contentBase: 'src/main/resources/',
      inline: true,
      compress: true,
      hot: true,
      historyApiFallback: true,
      host: '0.0.0.0',
      proxy: {
        '/admin': {
          target: 'https://localhost:8993',
          secure: false
        }
      }
    },
    plugins: [
      new webpack.HotModuleReplacementPlugin()
    ]
  })
}

module.exports = validate(config)
