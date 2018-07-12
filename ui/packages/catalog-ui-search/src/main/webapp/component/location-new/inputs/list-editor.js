const React = require('react');

const Group = require('./group');
const CustomElements = require('js/CustomElements');
const Component = CustomElements.registerReact('list-editor');
const { set, remove } = require('immutable');

const DmsEntry = (props) => {
    const { value } = props;
    const point = value.latitude.coordinate
                + value.latitude.direction
                + ", "
                + value.longitude.coordinate
                + value.longitude.direction;
    return (
        <li className="entry">
            <span className="entry-text">{point}</span>
            <button className="button-remove" onClick={() => props.onRemove(props.index)}>
                <span>x</span>
            </button>
        </li>
    );
};

const DdEntry = (props) => {
    const { value } = props;
    const point = value.latitude + "°"
                + ", "
                + value.longitude + "°"
    return (
        <li className="entry">
            <span className="entry-text">{point}</span>
            <button className="button-remove" onClick={() => props.onRemove(props.index)}>
                <span>x</span>
            </button>
        </li>
    );
};

const UsngEntry = (props) => {
    return (
        <li className="entry">
            <span className="entry-text">{props.value}</span>
            <button className="button-remove" onClick={() => props.onRemove(props.index)}>
                <span>x</span>
            </button>
        </li>
    );
};

class ListEditor extends React.Component {
    onAdd() {
        const { list, input, validateInput, onAdd, onError } = this.props;
        const newList = list.slice();
        newList.push(input);
        if (validateInput(input)) {
            onAdd(newList);
        } else {
            onError();
        }
    }

    onRemove(index) {
        const newList = this.props.list.slice();
        newList.splice(index, 1);
        this.props.onRemove(newList);
    }

    render() {
        const { EntryType, list, children } = this.props;
        const entries = list.map((entry, index) =>
            <EntryType
                value={entry}
                key={index}
                index={index}
                onRemove={this.onRemove.bind(this)}
            />
        );

        return (
            <Component>
                <Group>
                    {children}
                    <button className="button-add" onClick={this.onAdd.bind(this)}>+</button>
                </Group>
                <ul className="list">
                    {entries}
                </ul>
            </Component>
        );
    }
}

module.exports = {
    ListEditor,
    DmsEntry,
    DdEntry,
    UsngEntry
};