const path = require('path')
const exec = require('child_process').execSync

const glob = require('glob')

const webpack = require('webpack')
const merge = require('webpack-merge')

const ExtractTextPlugin = require('extract-text-webpack-plugin')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const SimpleProgressWebpackPlugin = require('simple-progress-webpack-plugin')

const resolve = (place) => path.resolve(place)
const nodeResolve = (place) => require.resolve(place)

const gitEnv = () => {
  const commitHash = exec('git rev-parse --short HEAD').toString()

  const isDirty = exec('git status').toString()
    .indexOf('working directory clean') === -1

  const commitDate = exec('git log -1 --pretty=format:%cI').toString()

  return {
    __COMMIT_HASH__: JSON.stringify(commitHash),
    __IS_DIRTY__: JSON.stringify(isDirty),
    __COMMIT_DATE__: JSON.stringify(commitDate)
  }
}

const babelLoader = (plugins = []) => ({
  loader: nodeResolve('babel-loader'),
  options: {
    presets: [
      'react',
      ['latest', { modules: false }],
      'stage-0'
    ],
    cacheDirectory: true,
    plugins: [
      nodeResolve('react-hot-loader/babel'),
      ...plugins
    ]
  }
})

const base = ({ alias = {}, env }) => ({
  entry: [nodeResolve('babel-polyfill')],
  output: {
    path: resolve('./target/webapp'),
    filename: 'bundle.[hash].js',
    globalObject: 'this'
  },
  plugins: [
    new webpack.DefinePlugin(gitEnv()),
    new HtmlWebpackPlugin({
      title: 'My App',
      filename: 'index.html',
      template: resolve('src/main/webapp/index.html')
    }),
    new webpack.ProvidePlugin({
      ReactDOM: 'react-dom',
      React: 'react'
    })
  ],
  externals: {
    'react/addons': true,
    'react/lib/ExecutionEnvironment': true,
    'react/lib/ReactContext': true
  },
  module: {
    rules: [
      {
        test: /\.(png|gif|jpg|jpeg)$/,
        use: nodeResolve('file-loader')
      },
      {
        test: /Cesium\.js$/,
        use: [
          {
            loader: nodeResolve('exports-loader'),
            options: { Cesium: true }
          },
          nodeResolve('script-loader')
        ]
      },
      {
        test: /jquery-ui/,
        use: {
          loader: nodeResolve('imports-loader'),
          options: {
            jQuery: 'jquery',
            $: 'jquery',
            jqueryui: 'jquery-ui'
          }
        }
      },
      {
        test: /bootstrap/,
        use: {
          loader: nodeResolve('imports-loader'),
          options: { jQuery: 'jquery' }
        }
      },
      {
        test: /\.jsx?$/,
        exclude: /node_modules/,
        use: babelLoader(env === 'test' ? [
          [
            nodeResolve('babel-plugin-istanbul'),
            { exclude: [ '**/*spec.js' ] }
          ]
        ] : [])
      },
      {
        test: /\.(hbs|handlebars)$/,
        use: {
          loader: nodeResolve('handlebars-loader')
        }
      },
      {
        test: /\.(eot|svg|ttf|woff|woff2)$/,
        use: {
          loader: nodeResolve('file-loader'),
          options: {
            name: 'fonts/[name]-[hash].[ext]'
          }
        }
      },
      {
        test: /\.(css|less)$/,
        use: [
          nodeResolve('style-loader'),
          {
            loader: nodeResolve('css-loader'),
            options: { sourceMap: true }
          },
          {
            loader: nodeResolve('less-loader'),
            options: { sourceMap: true }
          }
        ]
      },
      {
        test: /\.unless$/,
        use: [nodeResolve('raw-loader'), path.resolve(__dirname, 'concat-less.js')],
        exclude: /node_modules/
      },
      {
        test: /\.worker\.js$/,
        use: [nodeResolve('worker-loader'), babelLoader()]
      },
      {
        test: /\.tsx?$/,
        use: nodeResolve('ts-loader')
      }
    ]
  },
  resolve: {
    alias,
    extensions: ['.js', '.json', '.jsx', '.ts', '.tsx'],
    modules: [
      'src/main/webapp/',
      'src/main/webapp/js',
      'src/main/webapp/css',
      'src/main/webapp/lib/',
      'node_modules'
    ]
  }
})

const handleProxyRes = (proxyRes, req, res) => {
  // remove so we can still login in through http
  delete proxyRes.headers['x-xss-protection']
  const cookie = proxyRes.headers['set-cookie']
  if (cookie !== undefined) {
    // force the cookie to be insecure since the proxy is over http
    proxyRes.headers['set-cookie'] = cookie[0].replace(new RegExp(/;\w?Secure/), '')
  }
}

const proxyConfig = ({target = 'https://localhost:8993', auth}) => ({
  target,
  ws: true,
  secure: false,
  changeOrigin: true,
  onProxyRes: handleProxyRes,
  auth
})

const dev = (base, { main, auth }) => merge.smart(base, {
  mode: 'development',
  devtool: 'cheap-module-eval-source-map',
  entry: [
    nodeResolve('react-hot-loader/patch'),
    nodeResolve('console-polyfill'),
    resolve(main)
  ],
  devServer: {
    hotOnly: true,
    inline: true,
    host: 'localhost',
    historyApiFallback: true,
    contentBase: resolve('src/main/resources/'),
    proxy: {
      '/admin/**': proxyConfig({auth}),
      '/search/catalog/**': proxyConfig({auth}),
      '/services/**': proxyConfig({auth}),
      '/webjars/**': proxyConfig({auth})
    }
  },
  plugins: [
    new webpack.NamedModulesPlugin(),
    new webpack.HotModuleReplacementPlugin(),
    new SimpleProgressWebpackPlugin({
      format: 'compact'
    })
  ]
})

const test = (base, { main }) => merge.smart(base, {
  mode: 'development',
  devtool: 'cheap-module-eval-source-map',
  node: {
    __filename: true
  },
  entry: glob.sync('src/main/webapp/**/*spec.js*').map(resolve),
  output: {
    path: resolve('target/test/'),
    filename: 'test.js'
  },
  devServer: {
    hot: true,
    host: 'localhost'
  },
  plugins: [
    new HtmlWebpackPlugin(),
    new webpack.HotModuleReplacementPlugin()
  ],
  module: {
    rules: [
      {
        test: /.*spec\.jsx?$/,
        use: [
          nodeResolve('mocha-loader'),
          path.resolve(__dirname, 'spec-loader.js'),
          babelLoader()
        ],
        exclude: /node_modules/
      }
    ]
  }
})

const prod = (base, { main }) => merge.smart(base, {
  mode: 'production',
  devtool: 'source-map',
  entry: [
    resolve(main)
  ],
  module: {
    rules: [
      {
        test: /\.(css|less)$/,
        loader: ExtractTextPlugin.extract({
          fallback: nodeResolve('style-loader'),
          use: [
            {
              loader: nodeResolve('css-loader'),
              options: { sourceMap: true }
            },
            {
              loader: nodeResolve('less-loader'),
              options: { sourceMap: true }
            }
          ]
        })
      }
    ]
  },
  plugins: [
    new ExtractTextPlugin({ filename: 'styles.[hash].css' })
  ]
})

module.exports = (opts) => {
  const { env = 'development', main, alias, auth } = opts
  const b = base({ env, alias })

  switch (env) {
    case 'production': return prod(b, { main })
    case 'test': return test(b, { main })
    case 'development':
    default: return dev(b, { main, auth })
  }
}
