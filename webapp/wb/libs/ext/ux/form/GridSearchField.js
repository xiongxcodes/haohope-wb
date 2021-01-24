Ext.define('Ext.ux.form.GridSearchField', {
    extend: 'Ext.form.field.Trigger',
    alias: 'widget.gridsearchfield',
    trigger1Cls: Ext.baseCSSPrefix + 'form-clear-trigger',
    trigger2Cls: Ext.baseCSSPrefix + 'form-search-trigger',
    hasSearch : false,
    paramName : 'query',
    initComponent: function() {
        var me = this;

        me.callParent(arguments);
        me.on('specialkey', function(f, e){
            if (e.getKey() == e.ENTER) {
                me.onTrigger2Click();
            }
        });
        me.store = me.grid.getStore();
        me.columns = me.grid.columnManager.getColumns();
        // We're going to use filtering
        me.store.remoteFilter = true;

        // Set up the proxy to encode the filter in the simplest way as a name/value pair

        // If the Store has not been *configured* with a filterParam property, then use our filter parameter name
        if (!me.store.proxy.hasOwnProperty('filterParam')) {
            me.store.proxy.filterParam = me.paramName;
        }
        me.store.proxy.encodeFilters = function(filters) {
            return filters[0].value;
        }
    },

    afterRender: function(){
        this.callParent();
        this.triggerCell.item(0).setDisplayed(false);
    },

    onTrigger1Click : function(){
        var me = this;

        if (me.hasSearch) {
            me.setValue('');
            me.store.clearFilter();
            me.hasSearch = false;
            me.triggerCell.item(0).setDisplayed(false);
            me.updateLayout();
        }
    },

    onTrigger2Click : function(){
        var me = this,
            value = me.getValue();

        if (value.length > 0) {
            // Param name is ignored here since we use custom encoding in the proxy.
            // id is used by the Store to replace any previous filter
            if(Ext.isArray(me.columns)){
	            var column = null;
                var _values_ = [];
                var _value_ = {};
	            for(var i=0;i<me.columns.length;i++){
		            column = me.columns[i];//hidden
                    if(column.hidden ===false && (column.category == 'string' || column.category == 'text')){
	                    _value_ = {};
	                   _value_['field'] = column.dataIndex;
                       _value_['value'] = value;
                       _value_['type'] = 'string';//comparison
                       _value_['comparison'] = 'contains';
                       _values_.push(_value_);
                    }
	            }
                if(_values_.length > 0)value = _values_;
            }
            me.store.filter({
                id: me.paramName,
                property: me.paramName,
                value: value
            });
            me.hasSearch = true;
            me.triggerCell.item(0).setDisplayed(true);
            me.updateLayout();
        }
    }
});