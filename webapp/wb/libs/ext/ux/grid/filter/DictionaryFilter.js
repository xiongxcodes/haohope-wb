Ext.define('Ext.ux.grid.filter.DictionaryFilter', {
	extend : 'Ext.ux.grid.filter.Filter',
	alias : 'gridfilter.dictionary',
	phpMode : false,
	init : function(config) {
		this.dt = Ext.create('Ext.util.DelayedTask', this.fireUpdate, this);
	},
	createMenu : function(config) {
		config.store = Ext.create('Ext.data.Store', {
			fields : [ config.idField || 'value', config.labelField || 'text' ],
			proxy : {
				type : 'ajax',
				extraParams : {
					dickey : config.dickey,
				},
				url : config.url || '/m?xwl=sys/tool/dictionary/getDictionaryCombox',
				reader : {
					type : 'json',
					root : config.root || 'rows'
				}
			},
			autoLoad : true
		});
		config.idField = config.idField || 'value';
		config.labelField = config.labelField || 'text';
		var menu = Ext.create('Ext.ux.grid.menu.ListMenu', config);
		menu.on('checkchange', this.onCheckChange, this);
		return menu;
	},
	getValue : function() {
		return this.menu.getSelected();
	},
	setValue : function(value) {
		this.menu.setSelected(value);
		this.fireEvent('update', this);
	},
	isActivatable : function() {
		return this.getValue().length > 0;
	},
	getSerialArgs : function() {
		return {
			type : 'dictionary',
			comparison: 'in' ,
			value : this.phpMode ? this.getValue().join(',') : this.getValue()
		};
	},
	onCheckChange : function() {
		this.dt.delay(this.updateBuffer);
	},
	validateRecord : function(record) {
		var valuesArray = this.getValue();
		return Ext.Array.indexOf(valuesArray, record.get(this.dataIndex)) > -1;
	}
});