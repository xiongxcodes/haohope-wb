var XTL = {
  monthNames: ["January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
  ],
  monthNumbers: {
    Jan: 0,
    Feb: 1,
    Mar: 2,
    Apr: 3,
    May: 4,
    Jun: 5,
    Jul: 6,
    Aug: 7,
    Sep: 8,
    Oct: 9,
    Nov: 10,
    Dec: 11
  },
  dayNames: ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday",
    "Friday", "Saturday"
  ],
  getMonthNumber: function(name) {
    return XTL.monthNumbers[name.substring(0, 1).toUpperCase() +
      name.substring(1, 3).toLowerCase()];
  },
  getShortMonthName: function(month) {
    return XTL.monthNames[month].substring(0, 3);
  },
  getShortDayName: function(day) {
    return XTL.dayNames[day].substring(0, 3);
  },
  slotOrder: ['month', 'day', 'year'],
  defaultDateFormat: 'm/d/Y',
  defaultTimeFormat: 'h:iA',
  thousandSeparator: ',',
  decimalSeparator: '.',
  loading: 'Loading...',
  noItems: 'No items available.',
  loadMore: 'Load More...',
  noMoreRecords: 'No More Records',
  loaded: 'Loaded.',
  pullRefresh: 'Pull down to refresh...',
  releaseRefresh: 'Release to refresh...',
  lastUpdated: 'Last Updated:&nbsp;',
  monthText: 'Month',
  dayText: 'Day',
  yearText: 'Year',
  backText: 'Back',
  done: 'Done',
  cancel: 'Cancel',
  ok: 'OK',
  yes: 'Yes',
  no: 'No'
};