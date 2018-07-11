const React = require('react');

const Enzyme = require('enzyme');
const Adapter = require('enzyme-adapter-react-16');

const { expect } = require('chai');

Enzyme.configure({ adapter: new Adapter() });

const TextField = require('../text-field');
const { Menu, MenuItem } = require('../menu');
const AutoComplete = require('./auto-complete');

const { shallow } = Enzyme;

describe('<AutoComplete />', () => {
    it('should change value on select', (done) => {
        const onChange = (value) => {
            expect(value).to.equal('test');
            done();
        };
        const wrapper = shallow(<AutoComplete onChange={onChange} />);
        wrapper.find(Menu).prop('onChange')('test');
    });

    it('should display all suggestions', () => {
        const wrapper = shallow(<AutoComplete />);
        wrapper.setState({
            suggestions: [
                { id: 'one', name: 'one' },
                { id: 'two', name: 'two' },
                { id: 'three', name: 'three' }
            ]
        });
        expect(wrapper.find(MenuItem).length).to.equal(3);
    });

    it('should fetch suggestions on user input', (done) => {
        const fetch = async (url) => {
            expect(url).to.equal('./suggestions?q=test');
            expect(wrapper.state('loading')).to.equal(true);
            return {
                async json() {
                    done();
                    return [];
                }
            };
        };
        const wrapper = shallow(<AutoComplete url="./suggestions" fetch={fetch} debounce={0} />);
        expect(wrapper.state('loading')).to.equal(false);
        wrapper.find(TextField).prop('onChange')('test');
    });

    it('should inform user when endpoint is unavailable', (done) => {
        const onError = (e) => {
            const { loading, error } = wrapper.state()
            expect(loading).to.equal(false);
            expect(error).to.equal('Endpoint unavailable');
            done();
        };
        const fetch = async (url) => {
            throw new Error('unavailable');
        };
        const wrapper = shallow(<AutoComplete url="./suggestions" fetch={fetch} debounce={0} onError={onError} />);
        wrapper.find(TextField).prop('onChange')('test');
    });
});
