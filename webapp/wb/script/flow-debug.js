//工作流客户端API
var Flow = {
  /**
   * 发起新的流程。如果用户没有对应流程文件的执行权限将无法发起流程。
   * @param {Object} configs 配置参数对象。
   * @param {String} configs.filename 流程文件名称。
   * @param {Function} [configs.onOpen] 成功打开流程后的回调函数。
   * @param {Function} [configs.onAction] 成功执行动作后的回调函数。
   * @param {Object} [configs.params] 参数。
   */
  startFlow: function(configs) {
    Wb.request({
      url: 'm?xwl=sys/tool/flow/start-flow',
      timeout: -1,
      params: {
        filename: configs.filename,
        params: configs.params
      },
      success: function(resp) {
        Flow.processResponse(Wb.apply({
          responseText: resp.responseText
        }, configs));
      }
    });
  },
  /**
   * 打开指定id的流程。如果用户没有对应流程文件的执行权限将无法打开流程。
   * @param {Object} configs 配置参数对象。
   * @param {String} configs.flowId 流程id。
   * @param {String} configs.nodeName 节点名称。
   * @param {Function} [configs.onOpen] 成功打开流程后的回调函数。
   * @param {Function} [configs.onAction] 成功执行动作后的回调函数。
   * @param {Object} [configs.params] 参数。
   * @param {Boolean} isView 是否执行查看操作。
   */
  openFlow: function(configs, isView) {
    Wb.request({
      url: isView ? 'm?xwl=sys/tool/flow/view-flow' : 'm?xwl=sys/tool/flow/open-flow',
      timeout: -1,
      params: {
        flowId: configs.flowId,
        params: configs.params,
        nodeName: configs.nodeName
      },
      success: function(resp) {
        Flow.processResponse(Wb.apply({
          responseText: resp.responseText
        }, configs));
      }
    });
  },
  /**
   * 查看指定id的流程。
   * @param {Object} configs 配置参数对象。
   * @param {String} configs.flowId 流程id。
   * @param {String} configs.nodeName 节点名称。
   * @param {Function} [configs.onOpen] 成功打开流程后的回调函数。
   * @param {Object} [configs.params] 参数。
   */
  viewFlow: function(configs) {
    Flow.openFlow(configs, true);
  },
  /**
   * 处理发起、打开或查看流程后返回的响应结果。如果存在main窗口则显示，否则提示main不存在。
   * @param {Object} FlowVars 参数配置对象。
   * @param {String} FlowVars.responseText 返回的响应文本。
   * @param {String} FlowVars.filename 流程文件名称。
   * @param {String} FlowVars.flowId 流程id号。
   * @param {Function} [FlowVars.onOpen] 成功打开流程后的回调函数。
   * @param {Function} [FlowVars.onAction] 成功执行动作后的回调函数。
   * @param {Object} [FlowVars.params] 参数。
   */
  processResponse: function(FlowVars) {
    FlowVars.respObject = Wb.decode(FlowVars.responseText);
    if (!FlowVars.respObject.module) {
      Ext.callback(FlowVars.onOpen, FlowVars.appScope || window, [FlowVars.appScope || null]);
      return;
    }
    FlowVars.doOpen = function() {
      if (FlowVars.respObject.module.substring(0, 10) != '(function(' || FlowVars.respObject.module.slice(-3) != '();') {
        Wb.error('对话框 “' + FlowVars.respObject.path + '” 无效。');
        return;
      }
      FlowVars.loc = window.location;
      //添加流程变量
      FlowVars.respObject.module = Ext.String.insert(FlowVars.respObject.module, '\napp.flowParams=' +
        Wb.encode(FlowVars.respObject.flowParams) + ';', FlowVars.respObject.module.indexOf(';') + 1);
      FlowVars.appScope = eval(FlowVars.respObject.module.slice(0, -2) +
        '{}, null);\n//# sourceURL=' +
        FlowVars.loc.protocol + "//" + FlowVars.loc.host +
        FlowVars.loc.pathname.substring(0, FlowVars.loc.pathname.lastIndexOf('/')) +
        '/' + FlowVars.respObject.path);
      if (FlowVars.appScope.main) {
        FlowVars.appScope.main.closeAction = 'destroy';
        FlowVars.appScope.main.modal = true;
        if (!FlowVars.appScope.main.onEnter) {
          FlowVars.appScope.main.onEnter = function() {
            var btn = this.down('[name=pass]');
            if (btn)
              btn.fireEvent('click');
            else
              Wb.verify(this);
          };
        }
        FlowVars.appScope.main.mon(FlowVars.appScope.main, 'destroy', function() {
          Wb.destroy(FlowVars.appScope, FlowVars.appScope.main);
        });
        FlowVars.appScope.xFlowParams = {
          params: FlowVars.params,
          filename: FlowVars.filename,
          flowId: FlowVars.flowId,
          nodeName: FlowVars.nodeName,
          onAction: FlowVars.onAction,
          minWidth: FlowVars.respObject.minWidth,
          history: FlowVars.respObject.history
        };
        Flow.addButtons(FlowVars.appScope, FlowVars.appScope.main, FlowVars.respObject.actions);
        FlowVars.appScope.main.show();
      } else {
        Wb.destroy(FlowVars.appScope);
        Wb.error('没有找到名称为main的主窗口');
        return;
      }
      Ext.callback(FlowVars.onOpen, FlowVars.appScope || window, [FlowVars.appScope || null]);
    };
    if (Ext.String.startsWith(FlowVars.respObject.module, '$$@blink')) {
      FlowVars.scriptLinkEnd = FlowVars.respObject.module.indexOf('$$@elink');
      FlowVars.scriptObject = Wb.decode(FlowVars.respObject.module.substring(8, FlowVars.scriptLinkEnd));
      FlowVars.linksArray = [];
      if (FlowVars.scriptObject.css) {
        Wb.each(FlowVars.scriptObject.css, function(item) {
          FlowVars.linksArray.push({
            url: item,
            type: 'css'
          });
        });
      }
      if (FlowVars.scriptObject.js) {
        Wb.each(FlowVars.scriptObject.js, function(item) {
          FlowVars.linksArray.push({
            url: item,
            type: 'js'
          });
        });
      }
      FlowVars.respObject.module = FlowVars.respObject.module.substring(FlowVars.scriptLinkEnd + 9);
      Wb.addLink(FlowVars.linksArray, FlowVars.doOpen, FlowVars.scriptObject.recursive);
    } else
      FlowVars.doOpen();
  },
  /**
   * 在对话框窗口中添加动态按钮。
   * @param {Ext.window.Window} win 需要添加动作按钮的窗口对象。
   * @param {Array} actions 动作按钮定义列表。
   */
  addButtons: function(scope, win, actions) {
    if (Wb.isEmpty(actions))
      return;
    var newBar, config, buttons = [],
      bar = win.getDockedItems('toolbar[dock="bottom"]');
    if (!bar.length) {
      newBar = true;
      config = {
        xtype: 'toolbar',
        dock: 'bottom',
        ui: 'footer',
        defaults: {
          minWidth: scope.xFlowParams.minWidth || 80
        }
      };
      bar = win.addDocked(config);
    }
    bar = bar[0];
    if (newBar)
      buttons.push('->');
    Wb.each(actions, function(action) {
      action.listeners = {
        click: Flow.actionHandler
      };
      action.appScope = scope;
      buttons.push(action);
    });
    bar.add(buttons);
  },
  /**
   * 点击动作按钮时执行的方法。
   */
  actionHandler: function() {
    var me = this,
      scope = me.appScope,
      xFlowParams = scope.xFlowParams, //外部流程参数
      flowParams, configs;

    if (me.name != 'cancel' && me.name != 'turn' && me.name != 'reject' && !Wb.verify(scope.main))
      return;
    if (me.beforeFn && Ext.callback(scope[me.beforeFn], scope, [me]) === false)
      return;
    if (me.name == 'cancel') {
      if (me.afterFn && Ext.callback(scope[me.afterFn], scope, [me]) === false)
        return;
      scope.main.close();
      return;
    }
    flowParams = {
      filename: xFlowParams.filename,
      flowId: xFlowParams.flowId,
      startFlowId: scope.flowParams['flow.id'],
      module: me.module,
      nodeName: xFlowParams.nodeName
    };
    configs = {
      actionName: me.name,
      flowParams: flowParams,
      main: scope.main,
      params: Wb.apply(Wb.getValue(scope.main), xFlowParams.params, scope.extraParams),
      callback: function(respText) {
        if (me.afterFn && Ext.callback(scope[me.afterFn], scope, [me, respText, scope]) === false)
          return;
        Ext.callback(scope.xFlowParams.onAction, scope, [Wb.decode(respText), me.name, me, scope]);
        Ext.callback(configs.innerCallback);
        scope.main.close();
      }
    };
    switch (me.name) {
      case 'pass':
        Flow.doAction(configs);
        break;
      case 'reject':
        Wb.run({
          url: 'm?xwl=sys/tool/flow/reject-dialog',
          single: true,
          success: function(scope) {
            scope.show(function(id, win) {
              flowParams.rejectTo = id;
              configs.innerCallback = function() {
                win.close();
              };
              Flow.doAction(configs);
            }, {
              rows: xFlowParams.history,
              title: me.text,
              iconCls: me.iconCls
            });
          }
        });
        break;
      case 'beforeSign':
      case 'afterSign':
        Wb.run({
          url: 'm?xwl=sys/tool/flow/sign-dialog',
          single: true,
          success: function(scope) {
            scope.show(function(userData, nodeName, passName, nodeTitle, passUsers, win) {
              Wb.apply(flowParams, {
                userData: userData,
                newNodeName: nodeName,
                passName: passName,
                nodeTitle: nodeTitle,
                passUsers: passUsers
              });
              configs.innerCallback = function() {
                win.close();
              };
              Flow.doAction(configs);
            }, {
              nodeName: xFlowParams.nodeName,
              title: me.text,
              iconCls: me.iconCls
            });
          }
        });
        break;
      case 'plusSign':
      case 'turn':
        Wb.run({
          url: 'user-selector',
          single: true,
          success: function(scope) {
            scope.show(function(userData, win) {
              flowParams.userData = userData;
              configs.innerCallback = function() {
                win.close();
              };
              Flow.doAction(configs);
            }, {
              title: me.text,
              iconCls: me.iconCls
            });
          }
        });
        break;
    }
  },
  /**
   * 执行指定的流程动作类型。
   * @param {Object} configs 配置对象。
   * @param {String} configs.actionName 动作名称。
   * @param {String} configs.flowParams 流程参数。
   * @param {String} configs.params 请求上下文参数。
   * @param {Function} [configs.callback] 成功执行后的回调方法。
   */
  doAction: function(configs) {
    var urlMap = {
        beforeSign: 'before-sign',
        afterSign: 'after-sign',
        plusSign: 'plus-sign'
      },
      form = configs.main.down('form');
    if (form) {
      Wb.upload({
        url: 'm?xwl=sys/tool/flow/' + (urlMap[configs.actionName] || configs.actionName),
        form: form,
        timeout: -1,
        params: {
          xFlowParams: configs.flowParams,
          params: configs.params,
          xFlowId: configs.flowParams.startFlowId
        },
        success: function(a, b, value) {
          Ext.callback(configs.callback, this, [value]);
        }
      });
    } else {
      Wb.request({
        url: 'm?xwl=sys/tool/flow/' + (urlMap[configs.actionName] || configs.actionName),
        timeout: -1,
        out: configs.main,
        params: {
          xFlowParams: configs.flowParams,
          params: configs.params,
          xFlowId: configs.flowParams.startFlowId
        },
        success: function(resp) {
          Ext.callback(configs.callback, this, [resp.responseText]);
        }
      });
    }
  }
};