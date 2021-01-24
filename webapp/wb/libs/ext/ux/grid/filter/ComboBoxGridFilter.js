Ext.define('Ext.ux.grid.filter.ComboBoxGridFilter', {
    extend: 'Ext.ux.grid.filter.Filter',
    alias: 'gridfilter.comboboxgrid',
    init : function (config) {
    	delete config.picker;
    	this.dt = Ext.create('Ext.util.DelayedTask', this.fireUpdate, this);
    	config.store = Ext.create('Ext.data.Store', {
			fields : [ config.idField || 'value', config.labelField || 'text' ],
			proxy : {
				type : 'ajax',
				extraParams : config.extraParams || {},
				url : config.url ,
				reader : {
					type : 'json',
					root : config.root || 'rows',
					totalProperty : config.total || 'total'
				}
			},
			autoLoad : true
		});
		config.idField = config.idField || 'value';
		config.labelField = config.labelField || 'text';
		config.pageSize = config.pageSize || 20;
		config.width = config.width || 250;
		config.emptyText = 'Please select...';
		config.selectOnFocus = true;
		config.forceSelection = config.forceSelection || true;
        this.inputItem = Ext.create('Ext.form.field.ComboBox', config);
        this.inputItem.on('change', this.onChange, this);
        this.menu.add(this.inputItem);
    },
    getValue : function () {
        return this.inputItem.getValue();
    },
    setValue : function (value) {
        this.inputItem.setValue(value);
        this.fireEvent('update', this);
    },
    isActivatable : function () {
        return this.inputItem.getValue().length > 0;
    },
    getSerialArgs : function () {
        return {type: 'comboboxgrid',comparison: 'in' , value: this.getValue()};
    },
    onChange : function() {
		this.dt.delay(this.updateBuffer);
	},
    validateRecord : function (record) {
        var val = record.get(this.dataIndex);

        if(typeof val != 'string') {
            return (this.getValue().length === 0);
        }

        return val.toLowerCase().indexOf(this.getValue().toLowerCase()) > -1;
    }
});