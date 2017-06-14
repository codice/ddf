var webpack = require('webpack')
var merge = require('webpack-merge')
var path = require('path')
var fs = require('fs')
var glob = require('glob')
var HtmlWebpackPlugin = require('html-webpack-plugin')

module.exports = function (env) {

  process.env.NODE_ENV = env

  var config = {
    output: {
      filename: 'bundle.[hash].js',
      path: path.resolve(__dirname, 'target', 'webapp')
    },
    devtool: 'source-map',
    entry: ['babel-polyfill'],
    module: {
      rules: [
        {
          test: /\.js$/,
          exclude: /node_modules/,
          use: [
            'babel-loader'
          ]
        },
        {
          test: /\.css$/,
          use: [
            'style-loader',
            'css-loader'
          ]
        }
      ]
    },
    resolve: {
      modules: [
        path.resolve('./src/main/webapp/lib'),
        'node_modules'
      ]
    },
    plugins: [
      new webpack.DefinePlugin({
        'process.env.NODE_ENV': JSON.stringify(env)
      }),
      new webpack.ProvidePlugin({
        Promise: 'es6-promise'
      })
    ]
  }

  if (env === 'production') {
    return merge.smart(config, {
      entry: ['./src/main/webapp'],
      output: {
        libraryTarget: 'umd'
      },
      plugins: [
        new webpack.optimize.UglifyJsPlugin({
          sourceMap: true,
          output: {
            comments: false
          },
          compress: {
            drop_console: true,
            warnings: false
          }
        }),
        new HtmlWebpackPlugin({ template: 'src/main/resources/index.ejs' })
      ]
    })
  } else if (env === 'ci') {
    return merge.smart(config, {
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
        .concat('./src/main/webapp/app.js')
        .map(function (spec) { return path.resolve(spec) }),
      plugins: [
        new HtmlWebpackPlugin()
      ],
      module: {
        rules: [
          {
            test: /spec\.js$/,
            use: [
              'mocha-loader',
              path.resolve(__dirname, 'spec-loader.js'),
              'babel-loader'
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
  } else if (env === 'test') {
    return merge.smart(config, {
      devtool: 'source-map',
      node: {
        __filename: true
      },
      output: {
        publicPath: '',
        filename: 'bundle.js',
        path: path.resolve(__dirname, 'target', 'ci'),
        devtoolModuleFilenameTemplate: '~[resource-path]?[loaders]',
        devtoolFallbackModuleFilenameTemplate: '~[resource-path]?[loaders]'
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
        rules: [
          {
            test: /spec\.js$/,
            use: [
              'mocha-loader',
              path.resolve(__dirname, 'spec-loader.js'),
              'babel-loader'
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
  } else {
    return merge.smart(config, {
      entry: [
        'react-hot-loader/patch',
        'stack-source-map/register',
        './src/main/webapp'
      ],
      output: {
        devtoolModuleFilenameTemplate: '~[resource-path]?[loaders]',
        devtoolFallbackModuleFilenameTemplate: '~[resource-path]?[loaders]'
      },
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
        new webpack.HotModuleReplacementPlugin(),
        new webpack.NamedModulesPlugin(),
        new webpack.NoEmitOnErrorsPlugin(),
        new HtmlWebpackPlugin({ template: 'src/main/resources/index.ejs' })
      ]
    })
  }
}
