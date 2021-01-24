Ext.define('Ext.ux.grid.filter.PickerFilter', {
    extend: 'Ext.ux.grid.filter.Filter',
    alias: 'gridfilter.picker',
    init : function (config) {
    	this.dt = Ext.create('Ext.util.DelayedTask', this.fireUpdate, this);
		if (config.picker){
			this.inputItem = config.picker;
			this.inputItem.on('change', this.onChange, this);
			this.menu.add(this.inputItem);
		}
    },
    getValue : function () {
        return this.inputItem?this.inputItem.getValue():'';
    },
    setValue : function (value) {
    	if(this.inputItem){
    		this.inputItem.setValue(value);
    	}
        this.fireEvent('update', this);
    },
    isActivatable : function () {
    	if(!this.inputItem) return false;
        return this.inputItem.getValue().length > 0;
    },
    getSerialArgs : function () {
        return {type: 'picker',comparison: this.config.comparison || 'eq' , value: this.getValue()};
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