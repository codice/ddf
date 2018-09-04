const React = require('react');

const Enzyme = require('enzyme');
const Adapter = require('enzyme-adapter-react-16');

const { expect } = require('chai');

Enzyme.configure({ adapter: new Adapter() });

const TextField = require('./text-field');

const { shallow } = Enzyme;

describe('<TextField />', () => {
    it('<input /> should have the right value', () => {
        const wrapper = shallow(<TextField value="test" />);
        expect(wrapper.find('input').prop('value')).to.equal('test');
    });

    it('should update input on change', (done) => {
        const onChange = (value) => {
            expect(value).to.equal('test');
            done();
        };
        const wrapper = shallow(<TextField onChange={onChange} />);
        wrapper.find('input').prop('onChange')({ target: { value: 'test' } });
    });

    it('should allow users to pass parse function', (done) => {
        const onChange = (value) => {
            expect(value).to.equal(123);
            done();
        };
        const wrapper = shallow(<TextField parse={parseInt} onChange={onChange} />);
        wrapper.find('input').prop('onChange')({ target: { value: '123' } });
    });
});
