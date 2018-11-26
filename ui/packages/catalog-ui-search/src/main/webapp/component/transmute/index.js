const React = require('react')
const ReactDOM = require('react-dom')

const Marionette = require('marionette')
const CustomElements = require('../../js/CustomElements.js')

const withAdapter = Component =>
  class extends React.Component {
    constructor(props) {
      super(props)
      this.state = props.model.toJSON()
    }
    setModelState() {
      this.setState(this.props.model.toJSON())
    }
    componentWillMount() {
      this.props.model.on('change', this.setModelState, this)
    }
    componentWillUnmount() {
      this.props.model.off('change', this.setModelState)
    }
    render() {
      return (
        <Component
          state={this.state}
          options={this.props.options}
          setState={(...args) => this.props.model.set(...args)}
        />
      )
    }
  }

const template = () => ''
const tagName = CustomElements.register('marionette-bridge')

exports.reactToMarionette = Component => {
  Component = withAdapter(Component)

  const views = {}

  return Marionette.LayoutView.extend(
    {
      tagName,
      template,
      initialize: function(options) {
        this.options = options
        views[this.cid] = this.onRender.bind(this)
      },
      onRender: function() {
        if (!this.isDestroyed) {
          try {
            ReactDOM.render(
              <Component options={this.options} model={this.model} />,
              this.el
            )
          } catch (e) {
            ReactDOM.unmountComponentAtNode(this.el)
          }
        }
      },
      onDestroy: function() {
        ReactDOM.unmountComponentAtNode(this.el)
        delete views[this.cid]
      },
    },
    {
      reload: function(Comp) {
        Component = withAdapter(Comp)
        Object.keys(views).forEach(cid => {
          views[cid]()
        })
      },
    }
  )
}
