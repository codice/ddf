const React = require('react')

const Enzyme = require('enzyme')
const Adapter = require('enzyme-adapter-react-16')

const { expect } = require('chai')

Enzyme.configure({ adapter: new Adapter() })

const TextField = require('./text-field')

const { mount } = Enzyme

describe('<TextField />', () => {
  it('<input /> should have the right value', () => {
    const wrapper = mount(<TextField value="test" />)
    expect(wrapper.find('input').prop('value')).to.equal('test')
  })

  it('should update input on change', done => {
    const onChange = value => {
      expect(value).to.equal('test')
      done()
    }
    const wrapper = mount(<TextField onChange={onChange} />)
    wrapper.find('input').prop('onChange')({ target: { value: 'test' } })
  })
})
