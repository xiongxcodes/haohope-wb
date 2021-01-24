var XTL = {
  monthNames: ["一月", "二月", "三月", "四月", "五月", "六月", "七月", "八月", "九月", "十月",
    "十一月", "十二月"
  ],
  monthNumbers: {
    "一": 0,
    "二": 1,
    "三": 2,
    "四": 3,
    "五": 4,
    "六": 5,
    "七": 6,
    "八": 7,
    "九": 8,
    "十": 9,
    "十一": 10,
    "十二": 11
  },
  dayNames: ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"],
  getMonthNumber: function(name) {
    var i = name.indexOf('月');
    if (i === -1)
      i = name.length();
    return XTL.monthNumbers[name.substring(0, i)];
  },
  getShortMonthName: function(month) {
    var s = XTL.monthNames[month];
    return s.substring(0, s.indexOf('月'));
  },
  getShortDayName: function(day) {
    return XTL.dayNames[day].substring(2);
  },
  slotOrder: ['year', 'month', 'day'],
  defaultDateFormat: 'Y-m-d',
  defaultTimeFormat: 'H:i',
  thousandSeparator: ',',
  decimalSeparator: '.',
  loading: '加载中...',
  noItems: '没有可用的条目。',
  loadMore: '加载更多...',
  noMoreRecords: '没有更多的记录',
  loaded: '加载完成。',
  pullRefresh: '下拉刷新...',
  releaseRefresh: '放开刷新...',
  lastUpdated: '最后更新：',
  monthText: '月',
  dayText: '日',
  yearText: '年',
  backText: '返回',
  done: '确定',
  cancel: '取消',
  ok: '确定',
  yes: '是',
  no: '否'
};