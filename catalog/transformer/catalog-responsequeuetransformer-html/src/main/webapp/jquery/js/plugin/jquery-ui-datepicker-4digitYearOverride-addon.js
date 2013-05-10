// overriding function to force 4 digit years on yy (e.g. 0213 instead of 213)

	/* Format a date object into a string value.
	The format can be combinations of the following:
	d  - day of month (no leading zero)
	dd - day of month (two digit)
	o  - day of year (no leading zeros)
	oo - day of year (three digit)
	D  - day name short
	DD - day name long
	m  - month of year (no leading zero)
	mm - month of year (two digit)
	M  - month name short
	MM - month name long
	y  - year (two digit)
	yy - year (four digit)
	@ - Unix timestamp (ms since 01/01/1970)
	! - Windows ticks (100ns since 01/01/0001)
	'...' - literal text
	'' - single quote

	@param  format    string - the desired format of the date
	@param  date      Date - the date value to format
	@param  settings  Object - attributes include:
	                  dayNamesShort    string[7] - abbreviated names of the days from Sunday (optional)
	                  dayNames         string[7] - names of the days from Sunday (optional)
	                  monthNamesShort  string[12] - abbreviated names of the months (optional)
	                  monthNames       string[12] - names of the months (optional)
	@return  string - the date in the above format */

$.datepicker.formatDate = function (format, date, settings) {
	if (!date)
		return '';
	var dayNamesShort = (settings ? settings.dayNamesShort : null) || this._defaults.dayNamesShort;
	var dayNames = (settings ? settings.dayNames : null) || this._defaults.dayNames;
	var monthNamesShort = (settings ? settings.monthNamesShort : null) || this._defaults.monthNamesShort;
	var monthNames = (settings ? settings.monthNames : null) || this._defaults.monthNames;
	// Check whether a format character is doubled
	var lookAhead = function(match) {
		var matches = (iFormat + 1 < format.length && format.charAt(iFormat + 1) == match);
		if (matches)
			iFormat++;
		return matches;
	};
	// Format a number, with leading zero if necessary
	var formatNumber = function(match, value, len) {
		var num = '' + value;
		if (lookAhead(match))
			while (num.length < len)
				num = '0' + num;
		return num;
	};
	// Format a name, short or long as requested
	var formatName = function(match, value, shortNames, longNames) {
		return (lookAhead(match) ? longNames[value] : shortNames[value]);
	};
	var output = '';
	var literal = false;
	if (date)
		for (var iFormat = 0; iFormat < format.length; iFormat++) {
			if (literal)
				if (format.charAt(iFormat) == "'" && !lookAhead("'"))
					literal = false;
				else
					output += format.charAt(iFormat);
			else
				switch (format.charAt(iFormat)) {
					case 'd':
						output += formatNumber('d', date.getDate(), 2);
						break;
					case 'D':
						output += formatName('D', date.getDay(), dayNamesShort, dayNames);
						break;
					case 'o':
						output += formatNumber('o',
							Math.round((new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime() - new Date(date.getFullYear(), 0, 0).getTime()) / 86400000), 3);
						break;
					case 'm':
						output += formatNumber('m', date.getMonth() + 1, 2);
						break;
					case 'M':
						output += formatName('M', date.getMonth(), monthNamesShort, monthNames);
						break;
					case 'y': // here is the workaround to enforce 4-digit output of a year when 'yy' is the year code
						output += (lookAhead('y') ? ( (date.getFullYear() < 1000 ? '0' : '') +
								(date.getFullYear() < 100 ? '0' : '') +
								(date.getFullYear() < 10 ? '0' : '') + date.getFullYear() 
							) : (date.getYear() % 100 < 10 ? '0' : '') + date.getYear() % 100);
						break;
					case '@':
						output += date.getTime();
						break;
					case '!':
						output += date.getTime() * 10000 + this._ticksTo1970;
						break;
					case "'":
						if (lookAhead("'"))
							output += "'";
						else
							literal = true;
						break;
					default:
						output += format.charAt(iFormat);
				}
		}
	return output;
}
