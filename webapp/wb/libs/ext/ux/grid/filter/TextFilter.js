/**
 * Filter by a configurable Ext.form.field.Text
 * <p><b><u>Example Usage:</u></b></p>
 * <pre><code>
var filters = Ext.create('Ext.ux.grid.GridFilters', {
    ...
    filters: [{
        // required configs
        type: 'string',
        dataIndex: 'name',

        // optional configs
        value: 'foo',
        active: true, // default is false
        iconCls: 'ux-gridfilter-text-icon' // default
        // any Ext.form.field.Text configs accepted
    }]
});
 * </code></pre>
 */

Ext.define('Ext.ux.grid.filter.TextFilterField', {
	  extend : 'Ext.container.Container',
	  alias : 'widget.textFilterField',
	  layout : 'hbox',
      getValue:function(){
	     var me = this;
         return Ext.getCmp(me.valueid).getValue();
      },
      setValue:function(val){
	     var me = this;
         return Ext.getCmp(me.valueid).setValue(val);
      },
      getComparison:function(){
	     var me = this;
         return Ext.getCmp(me.operatorid).getValue();
      },
	  initComponent : function(){
		  var me = this;
          me.operatorid = Ext.id();
          me.valueid = Ext.id();
          var operatorstore = Ext.create('Ext.data.Store', {
               fields: ['id', 'text'],
               data : [{
                        id : 'contains',
                        text : '包含'
                       }, {
                        id : 'not contains',
                        text : '不包含'
                       }, {
                        id : 'eq',
                        text : '等于'
                       }, {
                        id : 'ne',
                        text : '不等于'
                       }, {
                        id : 'lt',
                        text : '小于'
                       }, {
                        id : 'gt',
                        text : '大于'
                       }, {
                        id : 'startwith',
                        text : '开始于'
                       }, {
                        id : 'not startwith',
                        text : '不开始'
                       }, {
                        id : 'endwith',
                        text : '结束于'
                       }, {
                        id : 'not endwith',
                        text : '不结束'
                       }, {
                        id : 'regexp',
                        text : '正则'
                       }]
          });
		  me.items = [{
			      id:me.operatorid,
			      xtype : 'combobox',
			      hideTrigger : true,
			      width : 50,
			      displayField : 'text',
			      valueField : 'id',
			      queryMode : 'local',
			      editable : false,
			      allowBlank : false,
			      margin : '0 2 0 0',
			      value : 'contains',
                  listeners : {
				      change : function(combo, value){
					      me.fireEvent('focus');
				      }
			      },
			      store : operatorstore
		      }, {
			      id:me.valueid,
			      xtype : 'textfield',
			      flex : 1,
			      emptyText:me.emptyText,
			      enableKeyEvents : true,
                  listeners : {
	                  keypress : function(field, e, eOpts){
		                 me.fireEvent('focus');
		              },
				      blur : function(field){
					      if (field.getValue()) field.setValue(field.getValue().replace(/[']/g, ''));
				      }
	              }
		      }];
		  me.callParent(arguments);
	  }
});

Ext.define('Ext.ux.grid.filter.TextFilter', {
    extend: 'Ext.ux.grid.filter.Filter',
    alias: 'gridfilter.text',

    /**
     * @cfg {String} iconCls
     * The iconCls to be applied to the menu item.
     * Defaults to <tt>'ux-gridfilter-text-icon'</tt>.
     */
    iconCls : 'ux-gridfilter-text-icon',

    emptyText: 'Enter Filter Text...',
    selectOnFocus: true,
    width: 125,

    /**
     * @private
     * Template method that is to initialize the filter and install required menu items.
     */
    init : function (config) {
        Ext.applyIf(config, {
            enableKeyEvents: true,
            labelCls: 'ux-rangemenu-icon ' + this.iconCls,
            hideEmptyLabel: false,
            labelSeparator: '',
            labelWidth: 29,
            listeners: {
                scope: this,
                focus: this.onInputKeyUp,
                el: {
                    click: function(e) {
                        e.stopPropagation();
                    }
                }
            }
        });

        this.inputItem = Ext.create('Ext.ux.grid.filter.TextFilterField', config);
        this.menu.add(this.inputItem);
        this.menu.showSeparator = false;
        this.updateTask = Ext.create('Ext.util.DelayedTask', this.fireUpdate, this);
    },

    /**
     * @private
     * Template method that is to get and return the value of the filter.
     * @return {String} The value of this filter
     */
    getValue : function () {
        return this.inputItem.getValue();
    },
    getComparison:function () {
	    return this.inputItem.getComparison();
	},
    /**
     * @private
     * Template method that is to set the value of the filter.
     * @param {Object} value The value to set the filter
     */
    setValue : function (value) {
        this.inputItem.setValue(value);
        this.fireEvent('update', this);
    },

    /**
     * Template method that is to return <tt>true</tt> if the filter
     * has enough configuration information to be activated.
     * @return {Boolean}
     */
    isActivatable : function () {
        return this.inputItem.getValue().length > 0;
    },

    /**
     * @private
     * Template method that is to get and return serialized filter data for
     * transmission to the server.
     * @return {Object/Array} An object or collection of objects containing
     * key value pairs representing the current configuration of the filter.
     */
    getSerialArgs : function () {
	    //getComparison
        return {type: 'text', comparison: this.getComparison() ,value: this.getValue()};
    },

    /**
     * Template method that is to validate the provided Ext.data.Record
     * against the filters configuration.
     * @param {Ext.data.Record} record The record to validate
     * @return {Boolean} true if the record is valid within the bounds
     * of the filter, false otherwise.
     */
    validateRecord : function (record) {
        var val = record.get(this.dataIndex);

        if(typeof val != 'string') {
            return (this.getValue().length === 0);
        }

        return val.toLowerCase().indexOf(this.getValue().toLowerCase()) > -1;
    },

    /**
     * @private
     * Handler method called when there is a keyup event on this.inputItem
     */
    onInputKeyUp : function (field, e) {
        /**
       var k = e.getKey();
        if (k == e.RETURN && field.isValid()) {
            e.stopEvent();
            this.menu.hide();
            return;
        } */
        // restart the timer
        this.updateTask.delay(this.updateBuffer);
    }
});
