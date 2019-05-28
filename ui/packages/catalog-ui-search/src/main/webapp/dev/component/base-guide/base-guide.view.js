const Marionette = require('marionette')
import React from 'react'
import { render } from 'react-dom'
import AceEditor from 'react-ace'
import 'brace/mode/html'
import 'brace/mode/less'
import 'brace/mode/javascript'
import 'brace/theme/tomorrow_night'
const beautify = require('js-beautify')

const renderAce = ({ where, mode, value, view }) => {
  where.innerHTML =
    '<div class="is-large-font"><span class="fa fa-refresh fa-spin"></span></div>'
  view.timeout += 1000
  setTimeout(() => {
    if (view.isDestroyed) {
      return
    }
    render(
      <AceEditor
        mode={mode}
        theme="tomorrow_night"
        value={value}
        readOnly
        height="200px"
        maxLines={Infinity}
        wrapEnabled={true}
        editorProps={{ $blockScrolling: Infinity }}
        onLoad={editor => {
          view.on('destroy', () => {
            editor.destroy()
          })
        }}
        width="100%"
      />,
      where
    )
  }, view.timeout)
}

module.exports = Marionette.LayoutView.extend({
  timeout: 0,
  onBeforeShow() {
    this.showEditors()
    this.showComponents()
  },
  styles: {},
  templates: {},
  showComponents() {
    //override
  },
  showEditors() {
    this.$el.find('.editor[data-html]').each((index, element) => {
      const dataHTML = element.getAttribute('data-html')
      let instanceHTML
      switch (dataHTML) {
        case 'outer':
          instanceHTML = element.parentNode.querySelector('.instance').outerHTML
          break
        case '':
          instanceHTML = element.parentNode.querySelector('.instance').innerHTML
          break
        default:
          instanceHTML = this.templates[dataHTML]()
          break
      }
      renderAce({
        where: element,
        mode: 'html',
        value: beautify.html_beautify(instanceHTML, {
          unformatted: [''],
        }),
        view: this,
      })
    })
    this.$el.find('.editor[data-css]').each((index, element) => {
      const instanceCSS = this.styles[element.getAttribute('data-css')]
      renderAce({
        where: element,
        mode: 'less',
        value: beautify.css_beautify(instanceCSS),
        view: this,
      })
    })
    this.$el.find('.editor[data-js]').each((index, element) => {
      const instanceJS = this[element.getAttribute('data-js')].toString()
      const raw = element.getAttribute('data-raw') === 'true' ? true : false
      const value = raw
        ? instanceJS
        : beautify(
            instanceJS.slice(
              instanceJS.indexOf('{') + 1,
              instanceJS.lastIndexOf('}')
            )
          )
      renderAce({
        where: element,
        mode: 'javascript',
        value,
        view: this,
      })
    })
  },
})
