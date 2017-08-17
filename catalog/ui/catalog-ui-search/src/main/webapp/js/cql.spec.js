import { expect } from 'chai'
import cql from './cql'

describe('tokenize', () => {
    it('test relative function parsing', () => {
        const cqlString = `(("created" = 'RELATIVE(P0DT0H5M)') OR ("modified" = 'RELATIVE(P0DT0H5M)') OR ("effective" = 'RELATIVE(P0DT0H5M)') OR ("metacard.created" = 'RELATIVE(P0DT0H5M)') OR ("metacard.modified" = 'RELATIVE(P0DT0H5M)'))`;
        const result = cql.simplify(cql.read(cqlString));
        const filters = result.filters;

        expect(filters).to.be.an('array');
        expect(filters, 'Result does not have the proper number of filters').to.have.lengthOf(5);

        filters.forEach(e => {
            switch(e.property) {
                case "\"metacard.modified\"":
                case "\"metacard.created\"":
                case "\"modified\"":
                case "\"created\"":
                case "\"effective\"":
                    expect(e.value, 'Unexpected filter value.').to.equal("RELATIVE(P0DT0H5M)");
                    expect(e.type, 'Unexpected filter operator.').to.equal("=");
                    break;
                default:
                    expect.fail(0, 1, 'Unexpected filters present');
            }
        });
    })
});