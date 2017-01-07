var webpack = require('webpack')
var validate = require('webpack-validator')
var merge = require('webpack-merge')
var path = require('path')

var config = {
  output: {
    publicPath: '/',
    filename: 'bundle.js',
    path: path.resolve(__dirname, 'target', 'webapp')
  },
  devtool: 'source-map',
  entry: './src/main/webapp',
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
} else if (process.env.NODE_ENV === 'test') {
  config = merge.smart(config, {
    output: {
      publicPath: '/',
      filename: 'bundle.js',
      path: path.resolve(__dirname, 'target', 'test')
    },
    entry: [
      'source-map-support/register',
      './src/test/webapp/reducer.js'
    ],
    target: 'node',
    resolve: {
      root: [
        './node_modules',
        './src/main/webapp/'
      ].map(function (dir) {
        return path.resolve(__dirname, dir)
      })
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
