const Enzyme = require('enzyme')
const Adapter = require('enzyme-adapter-react-16')

const { expect } = require('chai')

Enzyme.configure({ adapter: new Adapter() })

const { Radio, RadioItem } = require('./radio')

const { shallow, mount } = Enzyme

describe('<Radio />', () => {
  it('should render without children', () => {
    shallow(<Radio />)
  })

  it('should select the right child', () => {
    const wrapper = mount(
      <Radio value="two">
        <RadioItem value="one" />
        <RadioItem value="two" />
        <RadioItem value="three" />
      </Radio>
    )
    expect(wrapper.find({ selected: true }).prop('value')).to.equal('two')
  })

  it('should select child three', done => {
    const onChange = value => {
      expect(value).to.equal('three')
      done()
    }

    const wrapper = mount(
      <Radio onChange={onChange}>
        <RadioItem value="one" />
        <RadioItem value="two" />
        <RadioItem value="three" />
      </Radio>
    )

    wrapper.find({ value: 'three' }).prop('onClick')()
  })
})
