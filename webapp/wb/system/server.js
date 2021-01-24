/**
 * @class ServerScript.Wb
 * 
 * ServerScript Wb 提供了服务器端的JavaScript方法库，以方便使用JavaScript语法进行后台的Java编程。
 */
var Base = com.wb.common.Base,
  StringBuilder = java.lang.StringBuilder,
  File = java.io.File,
  HashMap = java.util.HashMap,
  KVBuffer = com.wb.common.KVBuffer,
  Console = com.wb.tool.Console,
  JavaString = java.lang.String,
  Integer = java.lang.Integer,
  Double = java.lang.Double,
  JavaDate = java.util.Date,
  Resource = com.wb.common.Resource,
  Str = com.wb.common.Str,
  Value = com.wb.common.Value,
  Var = com.wb.common.Var,
  Encrypter = com.wb.tool.Encrypter,
  DateUtil = com.wb.util.DateUtil,
  DbUtil = com.wb.util.DbUtil,
  FileUtil = com.wb.util.FileUtil,
  JsonUtil = com.wb.util.JsonUtil,
  LogUtil = com.wb.util.LogUtil,
  SortUtil = com.wb.util.SortUtil,
  StringUtil = com.wb.util.StringUtil,
  SysUtil = com.wb.util.SysUtil,
  WbUtil = com.wb.util.WbUtil,
  WebUtil = com.wb.util.WebUtil,
  ZipUtil = com.wb.util.ZipUtil,
  JSONArray = org.json.JSONArray,
  JSONObject = org.json.JSONObject,
  IOUtils = org.apache.commons.io.IOUtils,
  Wb = {
    /**
     * @method sleep
     * Thread.sleep方法的别名。当前线程暂停执行指定数量的毫秒。
     * @param {long} millis 暂停毫秒数。
     */
    sleep: java.lang.Thread.sleep,
    /**
     * 获取带局部变量request和response的操作对象。app在整个请求周期内共享。
     * @param {HttpServletRequest} request 请求对象。
     * @param {HttpServletResponse} response 响应对象。
     * @return {Object} 操作对象。
     */
    getApp: function(request, response) {
      var app = request.getAttribute('sysx.app');
      if (app && request.getAttribute('sysx.resp') === response)
        return app;
      //start app
      /**
       * @class ServerScript.app
       * 
       * ServerScript app是HttpServletRequest和HttpServletResponse关联的服务器端的JavaScript方法库。
       */
      app = {
        /**
         * 向当前用户的浏览器控制台中输出指定对象的日志信息。
         * @param {Object} object 打印的对象。
         */
        log: function(object) {
          Console.print(request, Wb.encode(object), 'log', true);
        },
        /**
         * 向当前用户的浏览器控制台中输出指定对象的提示信息。
         * @param {Object} object 打印的对象。
         */
        info: function(object) {
          Console.print(request, Wb.encode(object), 'info', true);
        },
        /**
         * 向当前用户的浏览器控制台中输出指定对象的警告信息。
         * @param {HttpServletRequest} request 请求对象。该请求对象用于关联到对应用户。
         * @param {Object} object 打印的对象。
         */
        warn: function(object) {
          Console.print(request, Wb.encode(object), 'warn', true);
        },
        /**
         * 向当前用户的浏览器控制台中输出指定对象的错误信息。
         * @param {Object} object 打印的对象。
         */
        error: function(object) {
          Console.print(request, Wb.encode(object), 'error', true);
        },
        /**
         * 向当前用户的客户端发送指定对象。
         * @param {Object} object 发送的对象。
         * @param {Boolean} [successful] 是否成功标识。如果指定该参数将采用JSON格式封装，该模式仅适用于文件上传模式的数据发送。
         */
        send: function(object, successful) {
          if (Wb.isObject(object) || Wb.isArray(object))
            object = Wb.encode(object);
          if (successful === undefined && WebUtil.jsonResponse(request)) {
            if (object === null)
              object = ''; //兼容java
            else if (object === undefined)
              object = 'undefined';
            else
              object = object.toString();
            successful = true; //upload模式的json格式响应
          }
          if (successful === undefined)
            WebUtil.send(response, object);
          else
            WebUtil.send(response, object, successful);
        },
        /**
         * 获取文件控件上传的单个或多个文件。
         * @param {String} fieldName 文件控件名称。
         * @return {Object[]} 获取的文件数据[{stream:输入流,name:文件名称,size:文件长度},...]，如果未找到返回空数组。
         */
        getFiles: function(fieldName) {
          var files = [],
            index = '',
            prefix = '',
            data;

          function getData(name) {
            var file = request.getAttribute(name);
            if (file instanceof java.io.InputStream)
              return {
                stream: file,
                name: request.getAttribute(name + '__name'),
                size: request.getAttribute(name + '__size')
              };
            else
              return null;
          }
          while ((data = getData(fieldName + prefix + index))) {
            files.push(data);
            if (index === '') {
              index = 0;
              prefix = '@';
            }
            index++;
          }
          return files;
        },
        /**
         * 获取HttpServletRequest和HttpSession对象中指定名称的属性或参数。
         * 如果相同名称的属性或参数都存在则返回优先顺序最高的值。优先顺序依次为
         * HttpSession的attribute，HttpServletRequest的attribute和
         * HttpServletRequest的parameter。如果都不存在则返回null。
         * 如果name参数缺省，将返回所有值组成的对象。
         * @param {String} [name] 参数或属性或称。
         * @param {Boolean} [returnString] 如果获取单个参数，false返回原始对象值，true返回原始对象转换为字符串后的值。默认为false。
         * @return {Object} 请求对象的parameter和attribute中的值组成的对象。
         */
        get: function(name, returnString) {
          if (name === undefined) {
            //获取全部值
            var jo = WebUtil.fetch(request),
              result = {};
            jo.entrySet().forEach(function(e) {
              result[e.key] = e.value;
            });
            return result;
          } else {
            //获取单个值
            return returnString ? WebUtil.fetch(request, name) : WebUtil.fetchObject(request, name);
          }
        },
        /**
         * 获取指定名称的参数，并转换为布尔类型。
         * @param {String} name 参数名称。
         * @return {Boolean} 参数转换后的布尔值。
         */
        getBool: function(name) {
          return Wb.parseBool(app.get(name));
        },
        /**
         * 获取指定名称的参数，并转换为整数类型。
         * @param {String} name 参数名称。
         * @return {Integer} 参数转换后的整数值。
         */
        getInt: function(name) {
          return parseInt(app.get(name), 10);
        },
        /**
         * 获取指定名称的参数，并转换为浮点数类型。
         * @param {String} name 参数名称。
         * @return {Double} 参数转换后的浮点值。
         */
        getFloat: function(name) {
          return parseFloat(app.get(name));
        },
        /**
         * 获取指定名称的参数，并转换为日期类型。参数必须为有效日期格式的字符串。
         * @param {String} name 参数名称。
         * @return {Date} 参数转换后的日期值。
         */
        getDate: function(name) {
          return Wb.strToDate(app.get(name, true));
        },
        /**
         * 获取指定名称的参数，并转换为Java日期类型。参数必须为有效日期格式的字符串。
         * @param {String} name 参数名称。
         * @return {Date} 参数转换后的日期值。
         */
        getJavaDate: function(name) {
          return DateUtil.strToDate(app.get(name, true));
        },
        /**
         * 获取指定名称的参数，并转换为对象/数组。
         * @param {String} name 参数名称。
         * @return {Object/Array} 参数转换后的对象。
         */
        getObject: function(name) {
          var str = app.get(name, true);
          if (!str)
            throw 'Param "' + name + '" is null or blank';
          return Wb.decode(str);
        },
        /**
         * 获取指定名称的参数，并转换为Java字符串数组。
         * @param {String} name 参数名称。
         * @return {JavaString[]} 参数转换后的对象。
         */
        getJavaArray: function(name) {
          return Java.to(app.getObject(name), Java.type('java.lang.String[]'));
        },
        /**
         * 判断指定参数是否不为空。参数是指存储在session的attribute，request的attribute或parameter中的值。
         * @param {String} name 参数名称。
         * @return {Boolean} 如果参数存在且其值不为空则返回true，否则返回false。
         */
        has: function(name) {
          return !Wb.isEmpty(app.get(name, true));
        },
        /**
         * 遍历对象，并把对象中每个条目的值设置到request的attribute对象，attribute名称为对象中条目的名称。
         * 如果首个参数不是对象，将以第1个参数为名称，第2个参数为值，设置到attribute。
         * @param {Object/String/Map/JSONObject} object 设置的对象。
         * @param {Object} [val] 如果object为字符串，该项为设置的值。
         */
        set: function(object, val) {
          if (Wb.isObject(object) || ((SysUtil.isMap(object) || object instanceof JSONObject) && object.entrySet)) {
            Wb.each(object, function(k, v) {
              request.setAttribute(k, v);
            });
          } else if (object)
            request.setAttribute(object, val);
        },
        /**
         * 启动jndi指定的数据库连接的事务，如果指定的连接已经启动事务则该方法没有任何效果。
         * @param {String} [jndi] 数据库jndi变量名称。为空表示使用默认使用库。
         * @param {Integer} [isolation] 事务的孤立程度，值对应Connection.TRANSACTION_XXX值。
         */
        startTrans: function(jndi, isolation) {
          var conn = DbUtil.getConnection(request, (jndi || null));
          if (conn.getAutoCommit()) {
            conn.setAutoCommit(false);
            if (isolation)
              conn.setTransactionIsolation(isolation);
          }
        },
        /**
         * 提交jndi指定的数据库连接的事务，如果指定的连接事务已经提交或回滚则该方法没有任何效果。
         * @param {String} [jndi] 数据库jndi变量名称。为空表示使用默认使用库。
         */
        commit: function(jndi, isolation) {
          var conn = DbUtil.getConnection(request, (jndi || null));
          if (!conn.getAutoCommit()) {
            conn.commit();
          }
        },
        /**
         * 回滚jndi指定的数据库连接的事务，如果指定的连接事务已经提交或回滚则该方法没有任何效果。
         * @param {String} [jndi] 数据库jndi变量名称。为空表示使用默认使用库。
         */
        rollback: function(jndi, isolation) {
          var conn = DbUtil.getConnection(request, (jndi || null));
          if (!conn.getAutoCommit()) {
            conn.rollback();
          }
        },
        /**
         * 判断当前请求对指定模块是否可以访问。
         * @param {String} path 模块路径或其捷径。
         * @return {Boolean} true可以访问，false不可以访问。
         */
        perm: function(path) {
          return WbUtil.canAccess(request, path);
        },
        /**
         * 删除树型结构表中的指定键值的记录。当记录被删除时，其树型结构的所有子节点记录均将被删除。
         * 该方法将使用指定jndi的共享数据库连接，并默认启用事务。
         * @param {String} tableName 数据库表名。
         * @param {String} keyName 主键ID字段名称。
         * @param {String} parentKeyName 上级ID字段名称。
         * @param {Array} delKeys 删除的主键值列表。
         * @param {String} [extraFields] 返回的附加字段名称列表，多个字段以逗号分割。
         * @param {String} [jndi] 数据库jndi名称。
         * @return {JSONArray} 包括子节点在内的所有被删除节点记录值组成的数据列表。
         */
        delTree: function(tableName, keyName, parentKeyName, delKeys, extraFields, jndi) {
          var rs, delRecs, config;
          if (extraFields)
            extraFields = ',' + extraFields;
          else
            extraFields = '';
          if (jndi)
            config = {
              jndi: jndi
            };
          else config = null;
          rs = app.run('select ' + keyName + ',' + parentKeyName + extraFields + ' from ' + tableName, config);
          delRecs = Wb.reverse(Wb.travelTree(rs, keyName, parentKeyName, delKeys));
          app.run('delete from ' + tableName + ' where ' + keyName + '={?' + keyName + '?}', {
            arrayData: delRecs
          });
          return delRecs;
        },
        /**
         * 根据当前语言种类格式化字符串。
         * @param {String} format 模板字符串。
         * @param {String...} values 格式化填充的字符串内容列表。
         * @return {String} 格式化后的字符串。
         */
        format: function() {
          return Str.format(request, arguments[0], [].slice.call(arguments, 1));
        },
        /**
         * 根据客户端当前语言代码，获取langs中对应值组成的对象。
         * @param {Object} langs 按不同语言定义的值对象。
         * @return {Object} 根据客户端语言提取值后组成的对象。
         */
        getLang: function(langs) {
          var list = {},
            lang = Str.getLanguage(request);
          Wb.each(langs, function(k, v) {
            list[k] = v[lang];
          });
          return list;
        },
        /**
         * 运行SQL语句，并获取返回结果对象。
         * @param {String} sql 运行的SQL语句。
         * @param {Object} [config] 配置对象。
         * @param {String} config.jndi 数据库连接jndi。
         * @param {String} config.arrayName 执行批处理时，指定数据源来自request中存储的该变量。
         * @param {JSONArray/Array/String} config.arrayData 执行批处理时，指定数据源来自该值。
         * @param {Boolean} config.batchUpdate 是否允许批处理操作。
         * @param {String} config.errorText 当该值不为空且查询结果集不为空，系统将抛出该信息的异常。
         * @param {String} config.type  执行何种SQL操作，可为"query","update","execute","call"，默认为自动。
         * @param {String} config.transaction 执行何种数据库事务操作，可为"start","commit","none"。
         * @param {String} config.isolation 数据库事务隔离级别，可为"readCommitted","readUncommitted","repeatableRead","serializable"。
         * @param {Boolean} config.uniqueUpdate 指定插入、更改或删除记录操作是否有且只有1条。
         * @return {Object} 运行SQL语句获得的结果，可能值为结果集，影响记录数或输出参数结果Map。
         */
        run: function(sql, config) {
          var arrayData,
            query = new com.wb.tool.Query();
          if (config && config.arrayData) {
            arrayData = config.arrayData;
            if (!(arrayData instanceof JSONArray)) {
              if (Wb.isArray(arrayData))
                arrayData = Wb.reverse(arrayData);
              else
                arrayData = new JSONArray(arrayData);
              config.arrayData = arrayData;
            }
          }
          Wb.apply(query, {
            request: request,
            sql: sql
          }, config);
          return query.run();
        },
        /**
         * 在共享的request和response或独立虚拟的response上下文中运行指定文件的xwl模块并返回生成的脚本。
         * 使用该方法运行模块不验证权限，不释放模块运行过程中生成的资源，所有资源待主模块运行完成之后释放。
         * @param {String} url 运行的模块url地址。
         * @param {Object} [params] 请求的参数对象。
         * @param {Boolean} [isInvoke] 是否为invoke模式的调用。true只返回闭包部分的脚本，
         * false返回全部模块脚本。如果指定该参数，将在独立虚拟的response内运行。
         * @return {String} 返回的脚本。只有当指定isInvoke参数时才返回运行的脚本。
         */
        execute: function(url, params, isInvoke) {
          if (isInvoke === undefined) {
            if (params)
              WebUtil.applyAttributes(request, Wb.reverse(params));
            WbUtil.run(url, request, response);
          } else {
            return WbUtil.run(url, Wb.reverse(params), request, !!isInvoke);
          }
        },
        /**
         * 使用com.wb.tool.DataProvider直接输出数据到客户端，详见该控件说明。
         * @param {ResultSet} rs 结果集。
         * @param {Object} [config] 配置参数对象，见com.wb.tool.DataProvider控件的使用。
         * @param {Boolean} [directOutput] 是否立即输出内容到客户端，true立即输出，false从该方法返回内容。默认为false。
         * @return {String/undefined} 输出脚本或undefined。
         */
        dp: function(rs, config, directOutput) {
          var dp = new com.wb.tool.DataProvider();
          Wb.apply(dp, {
            request: request,
            response: response,
            resultSet: rs
          }, config);
          if (directOutput)
            dp.output();
          else
            return dp.getScript();
        },
        /**
         * 从数据库获取数据，并输出指定格式的脚本、图片或流数据至客户端。
         * @param {String} sql sql语句。
         * @param {Object} [config] 配置参数对象，见DataProvider控件的使用。
         * @param {Boolean} [returnScript] 是否返回脚本，true返回生成的脚本，false直接输出，默认为false。
         * @return {String/undefined} 输出脚本或undefined。
         */
        output: function(sql, config, returnScript) {
          var configText, newConfig, dp = new com.wb.controls.DpControl();
          dp.request = request;
          dp.response = response;
          if (config) {
            newConfig = {};
            Wb.each(config, function(key, value) {
              if (Wb.isObject(value))
                value = Wb.encode(value);
              else value = String(value);
              newConfig[key] = value;
            });
            configText = Wb.encode(newConfig);
          } else
            configText = '{}';
          dp.configs = new JSONObject(configText);
          dp.configs.put('sql', sql);
          if (returnScript)
            return dp.getContent(false);
          else
            dp.create();
        },
        /**
         * 获取由SQL生成的数据对象。详见app.output方法的说明。
         * @param {String} sql sql语句。
         * @param {Object} [config] 配置参数对象，见DataProvider控件的使用。
         * @return {Object} 数据对象。
         */
        getData: function(sql, config) {
          return Wb.decode(app.output(sql, config, true));
        },
        /**
         * 获取SQL生成的数据集首行所有字段名称和值组成的对象。如果首行不存在返回null。
         * @param {String} sql sql语句。
         * @param {Object} [config] 配置对象。详见app.run方法的config参数。
         * @return {Object} 记录数据组成的对象或null。
         */
        getRecord: function(sql, config) {
          return app.getRecords(sql, config, true);
        },
        /**
         * 获取SQL生成的数据集所有行所有字段值组成的数组。数组内每一项为记录数据对象。
         * @param {String} sql sql语句。
         * @param {Object} [config] 配置对象。详见app.run方法的config参数。config.count属性指定返回最大记录数。
         * @param {Boolean} [firstRow] 是否仅获取首行记录组成的对象。默认为false。
         * @return {Array} 所有记录数据组成的数组。
         */
        getRecords: function(sql, config, firstRow) {
          var rs, st;
          try {
            rs = app.run(sql, config);
            st = rs.getStatement();
            if (firstRow) {
              if (rs.next())
                return Wb.getRecord(rs);
              else
                return null;
            } else
              return Wb.getRecords(rs, config ? config.count : -1);
          } finally {
            DbUtil.close(rs);
            DbUtil.close(st);
          }
        },
        /**
         * 执行上下文绑定的insert, update, delete数据库更新操作。
         * @param {Object} config 配置参数对象，见Updater控件的使用。
         */
        update: function(configs) {
          var updater = new com.wb.tool.Updater();

          if (Wb.isObject(configs.fieldsMap))
            configs.fieldsMap = Wb.toJSONObject(configs.fieldsMap);
          Wb.apply(updater, {
            request: request
          }, configs);
          updater.run();
        }
      };
      //end app
      request.setAttribute('sysx.app', app);
      request.setAttribute('sysx.resp', response);
      return app;
    },
    /**
     * 把对象转换成以字符串形式表示的值。
     * @param {Object} object 转换的对象。
     * @return {String} 字符串形式表示的值。
     */
    toString: function(object) {
      return toString.call(object);
    },
    /**
     * 判断是否是JS字符串。
     * @return {Boolean} true是，false不是。
     */
    isString: function(object) {
      return typeof object === 'string';
    },
    /**
     * 判断是否是JS数字。
     * @return {Boolean} true是，false不是。
     */
    isNumber: function(object) {
      return typeof object === 'number' && isFinite(object);
    },
    /**
     * 判断是否是JS对象。
     * @param {Object} object 判断的对象。
     * @return {Boolean} true是，false不是。
     */
    isObject: function(object) {
      return Wb.toString(object) === '[object Object]';
    },
    /**
     * 判断是否是JS数组或Java数组。
     * @param {Object} object 判断的对象。
     * @return {Boolean} true是，false不是。
     */
    isArray: function(object) {
      return Wb.toString(object) === '[object Array]' || SysUtil.isArray(object);
    },
    /**
     * 判断是否是JS日期。
     * @param {Object} object 判断的对象。
     * @return {Boolean} true是，false不是。
     */
    isDate: function(object) {
      return Wb.toString(object) === '[object Date]' && !isNaN(object);
    },
    /**
     * 判断是否是JS布尔型。
     * @param {Object} object 判断的对象。
     * @return {Boolean} true是，false不是。
     */
    isBoolean: function(object) {
      return typeof object === 'boolean';
    },
    /**
     * 判断是否是JS函数。
     * @param {Object} object 判断的对象。
     * @return {Boolean} true是，false不是。
     */
    isFunction: function(object) {
      return typeof object === 'function';
    },
    /**
     * 把对象编码成字符串并返回使用双引号引用的该字符串（不包含双引号本身）。
     * @param {Object} object 需要编码和引用的对象。
     * @return {String} 编码并使用双引号引用后的字符串。
     */
    quote: function(object) {
      return StringUtil.text(Wb.encode(object));
    },
    /**
     * 遍历数组或对象的各个元素。
     * @param {Array/Object/JSONArray/JSONObject/Map} data 遍历的数组或对象。
     * @param {Function} fn 每遍历一个元素所执行的回调方法。对于数组传递的参数为值和索引，对于对象传递的参数为名称和值。
     * 如果函数返回false，将中断遍历。
     * @param {Boolean} [reverse] 是否倒序遍历，仅适用于数组类型值的遍历，默认为false。
     * @return {Boolean} true遍历完成，否则返回索引号（数组）或false（对象）。
     */
    each: function(data, fn, reverse) {
      if (!data)
        return;
      if (Wb.isArray(data) || data instanceof java.util.ArrayList) {
        var i, j = data.length;
        if (reverse !== true) {
          for (i = 0; i < j; i++) {
            if (fn(data[i], i) === false) {
              return i;
            }
          }
        } else {
          for (i = j - 1; i > -1; i--) {
            if (fn(data[i], i) === false) {
              return i;
            }
          }
        }
      } else if (data instanceof JSONArray) {
        var m, n = data.length();
        if (reverse !== true) {
          for (m = 0; m < n; m++) {
            if (fn(data.get(m), m) === false) {
              return m;
            }
          }
        } else {
          for (m = n - 1; m > -1; m--) {
            if (fn(data.get(m), m) === false) {
              return m;
            }
          }
        }
      } else if ((SysUtil.isMap(data) || data instanceof JSONObject) && data.entrySet) {
        data.entrySet().forEach(function(entry) {
          if (fn(entry.key, entry.value) === false) {
            return false;
          }
        });
      } else {
        var property;
        for (property in data) {
          if (data.hasOwnProperty(property)) {
            if (fn(property, data[property]) === false) {
              return false;
            }
          }
        }
      }
      return true;
    },
    /**
     * 抛出Java Exception异常信息。
     * @param {String} message 异常信息。
     */
    error: function(message) {
      SysUtil.error(message);
    },
    /**
     * 判断值是否为空。如果值为null, undefined, 空字符串，空对象或空数组，则为空。
     * @param {Object} value 需要判断的值，可以为字符串或数组等。
     * @return {Boolean} 如果为空则返回true，否则返回false。
     */
    isEmpty: function(value) {
      if (Wb.isObject(value)) {
        for (var key in value) {
          if (value.hasOwnProperty(key)) {
            return false;
          }
        }
        return true;
      } else
        return value === null || value === undefined || value.length === 0;
    },
    /**
     * 判断指定条目在数组中的位置。
     * @param {Array} array 数组对象。
     * @param {Object} item 需要判断的条目。
     * @return {Number} 条目在数组中的位置。第1个为0，第2个为1，依此类推。-1表示没有找到。
     */
    indexOf: function(array, item) {
      if (!array)
        return -1;
      var i, j;
      j = array.length;
      for (i = 0; i < j; i++)
        if (array[i] === item)
          return i;
      return -1;
    },
    /**
     * 对指定值按指定格式转换为字符串。指定值可以是日期，数字，字符串或其他值。
     * @param {Object} value 需要格式化的值。如果该值为字符串表示格式化字符串使用的格式。
     * @param {String} [format] 格式。
     * @return {String} 格式化后的字符串。
     */
    format: function(value, format) {
      if (!value)
        return '';
      var isJsDate = Wb.isDate(value),
        isJavaDate = value instanceof JavaDate;

      if (isJsDate || isJavaDate) {
        if (isJsDate)
          value = new JavaDate(value.getTime());
        if (format)
          return DateUtil.format(value, format);
        else
          return DateUtil.format(value);
      } else if (Wb.isNumber(value)) {
        var decimalFormat = new java.text.DecimalFormat(format);
        decimalFormat.setRoundingMode(java.math.RoundingMode.HALF_UP);
        return decimalFormat.format(value);
      } else {
        return JavaString.format(value, [].slice.call(arguments, 1));
      }
    },
    /**
     * 对字符、数字、日期、对象和数组等进行编码，并转换为以字符串表示的值。
     * @param {Object} o 需要编码的值。
     * @return {String} 编码后的值。
     */
    encode: function(o) {
      var buf, addComma;
      if (o === null || o === undefined || Wb.isFunction(o)) {
        return 'null'; //Java无undefined类型，因此通一为null。
      } else if (typeof o == 'boolean') {
        return String(o);
      } else if (typeof o == 'number') {
        return isFinite(o) ? String(o) : 'null';
      } else if (Wb.isDate(o)) {
        return '"' + Wb.dateToStr(o) + '"';
      } else if (o instanceof JavaDate) {
        return '"' + DateUtil.dateToStr(o) + '"';
      } else if (Wb.isArray(o)) {
        buf = new StringBuilder('[');
        addComma = false;
        Wb.each(o, function(value) {
          if (addComma)
            buf.append(',');
          else
            addComma = true;
          buf.append(Wb.encode(value));
        });
        buf.append(']');
        return buf.toString();
      } else if (SysUtil.isIterable(o)) {
        buf = new StringBuilder('[');
        addComma = false;
        o.forEach(function(value) {
          if (addComma)
            buf.append(',');
          else
            addComma = true;
          buf.append(Wb.encode(value));
        });
        buf.append(']');
        return buf.toString();
      } else if (Wb.isObject(o)) {
        buf = new StringBuilder('{');
        addComma = false;
        Wb.each(o, function(name, value) {
          if (addComma)
            buf.append(',');
          else
            addComma = true;
          buf.append(StringUtil.quote(name));
          buf.append(':');
          buf.append(Wb.encode(value));
        });
        buf.append('}');
        return buf.toString();
      } else if (SysUtil.isMap(o) && o.entrySet) {
        buf = new StringBuilder('{');
        addComma = false;
        o.entrySet().forEach(function(entry) {
          if (addComma)
            buf.append(',');
          else
            addComma = true;
          buf.append(StringUtil.quote(entry.key));
          buf.append(':');
          buf.append(Wb.encode(entry.value));
        });
        buf.append('}');
        return buf.toString();
      } else
        return StringUtil.encode(o);
    },
    /**
     * 对以字符串形式表示的值进行解码，并转换为与之对应的值。
     * @param {String} s 需要解码的值。
     * @param {Boolean} [safe] 如果解码过程发生异常，true返回null，false抛出异常。默认为false。
     * @return {Object} 解码后的值。
     */
    decode: function(s, safe) {
      try {
        return eval('(' + s + ')');
      } catch (e) {
        if (safe)
          return null;
        throw e;
      }
    },
    /**
     * 把指定的数据转换为数组，如果值为null、undefined或数组直接返回该值，否则返回[value]值。
     * @param {Object/Array} value 需要转换为数组的值。
     * @return {Array} 返回的数组或null/undefined。
     */
    toArray: function(data) {
      if (!Wb.isValue(data) || Wb.isArray(data))
        return data;
      else
        return [data];
    },
    /**
     * 判断两个字符串是否相似。如果两个字符串为null,undefined或空串返回true，否则返回转换成JS小写字符串后进行比较的结果。
     * @param {Object} str1 比较的字符串1。
     * @param {Object} str2 比较的字符串2。
     * @return {Boolean} true相似，false不相似。
     */
    isSame: function(str1, str2) {
      var jsStr1, jsStr2;
      jsStr1 = str1 ? String(str1) : '';
      jsStr2 = str2 ? String(str2) : '';
      return jsStr1.toLowerCase() === jsStr2.toLowerCase();
    },
    /**
     * 判断两个字符串是否相等。如果两个字符串为null,undefined或空串返回true，否则返回转换成JS字符串后进行比较的结果。
     * @param {Object} str1 比较的字符串1。
     * @param {Object} str2 比较的字符串2。
     * @return {Boolean} true相等，false不相等。
     */
    isEqual: function(str1, str2) {
      var jsStr1, jsStr2;
      jsStr1 = str1 ? String(str1) : '';
      jsStr2 = str2 ? String(str2) : '';
      return jsStr1 === jsStr2;
    },
    /**
     * 把值解析为布尔型。如果值为false, 'false', 0, '0', null, undefined和空串返回false，其他值返回true。
     * 如果指定默认值，则当值为null或undefined时返回此默认值。
     * @param {Object} value 需要解析的值。
     * @param {Boolean} [defaultValue] 如果value为null或undefined，默认返回此值，未指定将返回false。
     * @return {Boolean} 解析后的布尔值。
     */
    parseBool: function(value, defaultValue) {
      if (Wb.isValue(value)) {
        var str = String(value).toLowerCase();
        return !(str == 'false' || str == '0' || str === '');
      } else {
        if (defaultValue === undefined)
          return false;
        else
          return defaultValue;
      }
    },
    /**
     * 判断值是否为不是null和undefined值。
     * @param {Object} value 任意值。
     * @return {Boolean} 如果值为null和undefined返回false，否则返回true。
     */
    isValue: function(value) {
      return value !== null && value !== undefined;
    },
    /**
     * 依次序合并所有参数值到一个新的数组，如果参数为数组将逐个进行合并。该方法不影响输入参数的值。
     * @param {Object...} values 需要合并的值。
     * @return {Array} 合并后的数组。如果没有合并项返回空数组。
     */
    merge: function() {
      //该方法用于替代concat方法
      var args = [].slice.call(arguments),
        items = [],
        arg, i, j;

      function eachItem(item) {
        items.push(item);
      }

      j = args.length;
      for (i = 0; i < j; i++) {
        arg = args[i];
        if (Wb.isArray(arg))
          Wb.each(arg, eachItem);
        else
          items.push(arg);
      }
      return items;
    },
    /**
     * 在指定对象中查找名称和值匹配的项。
     *
     * Example:
     *
     *     var item = Wb.find([{a: 123}, {a: 'abc'}], 'a', 'abc'); //返回{a: 'abc'}对象
     *
     * @param {Mixed} object 查找的对象。
     * @param {String} name 查找的名称。
     * @param {Object} value 查找的值。
     * @return {Object} 查找到的项。如果没有找到返回null。
     */
    find: function(object, name, value) {
      var result = null;
      Wb.each(object, function(item) {
        if (item[name] === value) {
          result = item;
          return false;
        }
      });
      return result;
    },
    /**
     * 把字符串转换成js Date格式。如果不能转换为日期返回null。
     * @param {String} string 需要转换成日期的字符串。
     * @return {Date} 转换后的日期或null。
     */
    strToDate: function(string) {
      if (Wb.isEmpty(string))
        return null;
      var date = new Date(Date.parse(string));
      if (date != 'Invalid Date')
        return date;
      else
        return null;
    },
    /**
     * 把日期对象转换成标准日期格式yyyy-MM-dd HH:mm:ss.f的字符串。如果日期为空或无效返回null。
     * @param {Date/JavaDate} date 需要转换成字符串的日期。
     * @return {String} 转换后的日期或null。
     */
    dateToStr: function(date) {
      if (Wb.isDate(date))
        date = Wb.reverse(date);
      if (date instanceof JavaDate)
        return DateUtil.dateToStr(date);
      else
        return null;
    },
    /**
     * 从源对象复制一组属性到目录对象。
     * @param {Object} dest 目标对象。
     * @param {Object} source 源对象。
     * @param {String/String[]} names 复制的属性名称，以逗号分隔或数组。
     * @return {Object} 修改后的目标对象。
     */
    copyTo: function(dest, source, names) {
      if (Wb.isString(names))
        names = names.split(',');
      var name, i, j = names ? names.length : 0;

      for (i = 0; i < j; i++) {
        name = names[i];
        if (source.hasOwnProperty(name)) {
          dest[name] = source[name];
        }
      }
      return dest;
    },
    /**
     * 在JS和Java类型之间反转指定值类型。返转的值类型为Date/JavaDate,Object/JSONObject,Array/JSONArray,其他按原值返回。
     * @param {Object} value 需要反转类型的值。
     * @return {Object} 反转类型后的值。
     */
    reverse: function(value) {
      if (value instanceof JavaDate)
        return Wb.createDate(value.getTime());
      else if (Wb.isDate(value))
        return new JavaDate(value.getTime());
      else if (Wb.isObject(value))
        return Wb.toJSONObject(value);
      else if (Wb.isArray(value))
        return Wb.toJSONArray(value);
      else if (value instanceof JSONArray || value instanceof JSONObject)
        return Wb.decode(value.toString());
      else
        return value;
    },
    /**
     * 根据日期时间数字值获得日期时间类型值。
     * @param {Number} timeValue 时间数字值。
     * @return {Date} 日期值。
     */
    createDate: function(timeValue) {
      return new Date(Number(timeValue));
    },
    /**
     * 把config中的值复制到object中，defaults为config的默认值。
     * @param {Object} object 目标对象。
     * @param {Object} config 源对象。
     * @param {Object} [defaults] config的默认值对象。
     * @return {Object} object对象本身。
     */
    apply: function(object, config, defaults) {
      if (defaults)
        Wb.apply(object, defaults);
      if (object && config) {
        var name;
        for (name in config)
          object[name] = config[name];
      }
      return object;
    },
    /**
     * 如果object中不存在相同名称的值，则把config中的值复制到object中。
     * @param {Object} object 目标对象。
     * @param {Object} config 源对象。
     * @return {Object} object对象本身。
     */
    applyIf: function(object, config) {
      var name;
      if (object && config) {
        for (name in config) {
          if (!object.hasOwnProperty(name)) {
            object[name] = config[name];
          }
        }
      }
      return object;
    },
    /**
     * 读取文件中的json对象或数组。
     * @param {File} file 文件。
     * @return {Object/Array} 对象或数组。
     */
    readJson: function(file) {
      return Wb.decode(FileUtil.readString(file));
    },
    /**
     * 把json对象或数组写到文件中。
     * @param {File} file 文件。
     * @param {Object/Array} json 对象或数组。
     */
    writeJson: function(file, json) {
      FileUtil.writeString(file, Wb.encode(json));
    },
    /**
     * 删除数组中首个存在的指定值，如果值不存在，该方法无任何效果。
     * @param {Array} array 数组对象。
     * @param {Object} item 需要删除的存在于数组中的元素。
     */
    remove: function(array, item) {
      if (!array) return;
      var index = array.indexOf(item);
      if (index != -1)
        array.splice(index, 1);
    },
    /**
     * 遍历树型结果集中指定键值的节点记录及其所有子节点记录，并返回这些记录组成的数组。
     * @param {ResultSet} rs 结果集。
     * @param {String} keyName 主键ID字段名称。
     * @param {String} parentKeyName 上级ID字段名称。
     * @param {Array} keys 需要遍历的主键值列表。
     * @return {Array} 包括子节点在内的所有被遍历节点记录值组成的数组。
     */
    travelTree: function(rs, keyName, parentKeyName, keys) {
      var records = {},
        keysMap = {},
        foundRecs = [],
        record, nodes, meta, parentValue;

      function loadNodes(parentId) {
        Wb.each(records[parentId], function(node) {
          foundRecs.push(node);
          loadNodes(node[keyName]);
        });
      }

      keysMap = Wb.toObject(keys);
      meta = rs.getMetaData();
      while (rs.next()) {
        record = Wb.getRecord(rs, meta);
        parentValue = record[parentKeyName];
        nodes = records[parentValue];
        if (nodes) {
          nodes.push(record);
        } else {
          nodes = [record];
          records[parentValue] = nodes;
        }
        if (keysMap[record[keyName]])
          foundRecs.push(record);
      }
      Wb.each(keys, function(key) {
        loadNodes(key);
      });
      return foundRecs;
    },
    /**
     * 把指定的结果集或记录数组的数据生成树型结构的JSON脚本。
     * @param {ResultSet/Object[]} recs 结果集或记录数组。
     * @param {String} keyName 主键ID字段名称。
     * @param {String} parentKeyName 上级ID字段名称。
     * @param {String/Object} rootId 根节点ID值。
     * @return {String} 生成的树型脚本字符串。
     */
    buildTree: function(recs, keyName, parentKeyName, rootId) {
      if (!Wb.isArray(recs))
        return Wb.buildTree(Wb.getRecords(recs), keyName, parentKeyName, rootId);
      var result = {},
        records = {},
        record, nodes, parentValue;

      function createNodes(items, parentDept) {
        items.children = records[parentDept] || [];
        Wb.each(items.children, function(item) {
          createNodes(item, item[keyName]);
        });
      }
      recs.forEach(function(record) {
        parentValue = record[parentKeyName];
        nodes = records[parentValue];
        if (nodes) {
          nodes.push(record);
        } else {
          nodes = [record];
          records[parentValue] = nodes;
        }
      });
      createNodes(result, rootId);
      return result;
    },
    /**
     * 获取指定对象所有值组成的数组。
     * @param {Object} object 获取值的对象。
     * @return {Array} 对象中所有值组成的数组。
     */
    getValues: function(object) {
      var property, values = [];

      for (property in object) {
        if (object.hasOwnProperty(property)) {
          values.push(object[property]);
        }
      }
      return values;
    },
    /**
     * 获取文本的省略显示。超过指定长度的文本将以“...”替代。
     * @param {String} value 需要省略的文本。
     * @param {Number} length 文本显示的最大长度。
     * @return {String} 省略后显示的文本。
     */
    ellipsis: function(value, length, word) {
      value = String(value);
      if (value && value.length > length) {
        return value.substr(0, length - 3) + "...";
      }
      return value;
    },
    /**
     * 在指定值左边填补指定字符，使值总长度达到指定长度。
     * @param {Mixed} value 需要填补的值。
     * @param {Number} size 填补后的总长度。
     * @param {String} [character] 填补的字符，默认为'0'。
     * @return {String} 填补后的字符串值。
     */
    pad: function(value, size, character) {
      var result = String(value);
      character = character || '0';
      while (result.length < size) {
        result = character + result;
      }
      return result;
    },
    /**
     * 在指定值右边填补指定字符，使值总长度达到指定长度。
     * @param {Mixed} value 需要填补的值。
     * @param {Number} size 填补后的总长度。
     * @param {String} [character] 填补的字符，默认为'0'。
     * @return {String} 填补后的字符串值。
     */
    padRight: function(value, size, character) {
      var result = String(value);
      character = character || '0';
      while (result.length < size) {
        result += character;
      }
      return result;
    },
    /**
     * 获取Excel 2007+模板文件转换得到的Web脚本字符串。这些Web脚本字符串可应用于前端容器控件用于直接生成表单或报表。
     * @param {Object/JSONObject} params 参数对象。可以在Excel模板文件中被引用。
     * @param {String/File/InputStream} file Excel模板文件路径、文件对象或输入流。如果是文件路径基于Base.path。
     * @param {Number} [sheetIndex] 引用的模板文件中的Sheet索引号，默认为0。
     * @param {String} [align] 生成的表单对齐方式，有效值为left, center, right。
     * @return {String} 生成的HTML脚本。
     */
    getHtml: function(params, file, sheetIndex, align) {
      params = Wb.toJSONObject(params);
      if (Wb.isString(file))
        file = new File(Base.path, file);
      return com.wb.tool.ExcelForm.getHtml(params, file, sheetIndex || 0, align);
    },
    /**
     * 把数据导入到Excel模板文件中，并输出到指定的输出流中。
     * @param {Object/JSONObject} params 参数对象。可以在Excel模板文件中被引用。
     * @param {String/File/InputStream} file Excel模板文件路径、文件对象或输入流。如果是文件路径基于Base.path。
     * @param {OutputStream} outputStream 生成的Excel文件输出到该流中。
     * @param {Number} [sheetIndex] 引用的模板文件中的Sheet索引号，-1表示全部Sheet。默认为-1。
     */
    getExcel: function(params, file, outputStream, sheetIndex) {
      params = Wb.toJSONObject(params);
      if (Wb.isString(file))
        file = new File(Base.path, file);
      com.wb.tool.DataOutput.importToExcel(params, file, outputStream, sheetIndex === undefined ? -1 : sheetIndex);
    },
    /**
     * 把JS的Object对象转换为Java的JSONObject对象。如果参数不是Object对象，将直接返回此参数。
     * 如果object中的值为原始值，可以使用new JSONObject(object)替代。
     * @param {Object} object 需要转换的对象。
     * @return {JSONObject} 转换后的JSONObject对象或object参数原值。
     */
    toJSONObject: function(object) {
      if (Wb.isObject(object)) {
        var jo = new JSONObject();
        Wb.each(object, function(k, v) {
          if (v === null || v === undefined || Wb.isFunction(v))
            v = null;
          else if (Wb.isObject(v))
            v = Wb.toJSONObject(v);
          else if (Wb.isArray(v))
            v = Wb.toJSONArray(v);
          else if (Wb.isDate(v))
            v = new JavaDate(v.getTime());
          jo.put(k, v);
        });
        return jo;
      }
      return object;
    },
    /**
     * 在独立虚拟的request/response上下文中运行指定文件的xwl模块并返回生成的脚本。
     * 使用该方法运行模块不验证权限，并立即释放模块运行过程中生成的所有资源。
     * 由于该方法运行不基于会话，因此被调用的模块会话相关的变量将无效。
     * @param {String} url 运行的模块url地址。
     * @param {Object} [params] 请求的参数对象。
     * @param {Boolean} [isInvoke] 是否为invoke模式的调用。true只返回闭包部分的脚本，
     * false返回全部模块脚本。默认为false。
     * @return {String} 返回的脚本。
     */
    execute: function(path, params, isInvoke) {
      return WbUtil.run(path, params ? Wb.reverse(params) : null, !!isInvoke);
    },
    /**
     * 把JS的Array对象转换为Java的JSONArray对象。如果参数不是Array对象，将直接返回此参数。
     * 如果array中的值为原始值，可以使用new JSONArray(array)替代。
     * @param {Array} array 需要转换的数组。
     * @return {JSONArray} 转换后的JSONArray对象或array参数原值。
     */
    toJSONArray: function(array) {
      if (Wb.isArray(array)) {
        var ja = new JSONArray();
        Wb.each(array, function(v) {
          if (v === null || v === undefined || Wb.isFunction(v))
            v = null;
          else if (Wb.isObject(v))
            v = Wb.toJSONObject(v);
          else if (Wb.isArray(v))
            v = Wb.toJSONArray(v);
          else if (Wb.isDate(v))
            v = new JavaDate(v.getTime());
          ja.put(v);
        });
        return ja;
      }
      return array;
    },
    /**
     * 获取结果集当前行所有字段名称和值组成的对象。
     * @param {ResultSet} resultSet 结果集。
     * @param {ResultSetMetaData} [meta] 结果集元数据，如果缺少该值，系统将动态生成。基于性能考虑如果多次调用该方法，请提供该参数。
     * @return {Object} 记录数据组成的对象
     */
    getRecord: function(resultSet, meta) {
      if (!meta)
        meta = resultSet.getMetaData();
      var i, j = meta.getColumnCount(),
        record = {},
        value;
      for (i = 0; i < j; i++) {
        value = DbUtil.getObject(resultSet, i + 1, meta.getColumnType(i + 1));
        record[DbUtil.getFieldName(meta.getColumnLabel(i + 1))] = value;
      }
      return record;
    },
    /**
     * 获取结果集所有行所有字段值组成的数组。数组内每一项为记录数据对象。
     * @param {ResultSet} resultSet 结果集。
     * @param {Number} [count] 返回的记录数量。默认为全部。-1表示返回全部。
     * @return {Array} 所有记录数据组成的数组。
     */
    getRecords: function(resultSet, count) {
      var meta = resultSet.getMetaData(),
        i, j = meta.getColumnCount(),
        index = 1,
        fieldNames = [],
        fieldTypes = [],
        record, records = [],
        value;

      //获取字段名称列表
      for (i = 0; i < j; i++) {
        fieldNames.push(DbUtil.getFieldName(meta.getColumnLabel(i + 1)));
        fieldTypes.push(meta.getColumnType(i + 1));
      }
      if (!Wb.isValue(count) || count == -1)
        count = Number.MAX_VALUE;
      while (resultSet.next()) {
        if (index > count)
          break;
        index++;
        record = {};
        for (i = 0; i < j; i++) {
          value = DbUtil.getObject(resultSet, i + 1, fieldTypes[i]);
          record[fieldNames[i]] = value;
        }
        records.push(record);
      }
      return records;
    },
    /**
     * 向指定用户id或http会话中打开的指定名称所有的WebSocket会话列表发送消息。
     * @param {String/HttpSession} sendTo 用户id或http会话对象。
     * @param {String} name socket名称。
     * @param {Object} object 发送的消息对象。系统将把该对象转换为字符串后发送。
     */
    send: function(sendTo, name, object) {
      var isBroadcast;
      if (object === undefined) {
        //发送广播，sendTo为name，name为object
        object = name;
        name = sendTo;
        isBroadcast = true;
      }
      if (!Wb.isValue(object))
        object = '';
      else if (Wb.isObject(object) || Wb.isArray(object))
        object = Wb.encode(object);
      else object = object.toString();
      if (isBroadcast)
        WebUtil.send(name, object);
      else
        WebUtil.send(sendTo, name, object);
    },
    /**
     * 把数组转换为对象。对象key为数据项值，value为true。
     * @param {Array} array 需要转换的数组。
     * @return {Object} 转换得到的对象。
     */
    toObject: function(array) {
      var obj = {};
      Wb.each(array, function(item) {
        obj[item] = true;
      });
      return obj;
    },
    /**
     * 判断两个字符串数组中是否存在相同的字符串。
     * @param {Array} source 源字符串数组。
     * @param {Array} dest 目标字符串数组。
     * @return {Boolean} true存在相同的字符串，false不存在相同的字符串。
     */
    across: function(source, dest) {
      if (!source || !dest)
        return false;
      var i, j, k, l, s, d;
      j = source.length;
      l = dest.length;
      for (i = 0; i < j; i++) {
        s = source[i];
        for (k = 0; k < l; k++) {
          d = dest[k];
          if (s === d)
            return true;
        }
      }
      return false;
    },
    /**
     * 获取对象数据中各个对象指定名称成员值组成的数组。
     * @param {Object[]} items 对象数组。
     * @param {String} name 对象中的成员名称。
     * @return {Array} 值组成的数组。
     */
    pluck: function(items, name) {
      var i, j = items.length,
        item, data = [];
      for (i = 0; i < j; i++) {
        item = items[i];
        data.push(item ? item[name] : null);
      }
      return data;
    },
    /**
     * 去掉数据中的重复项，返回由唯一项组成的新数组。
     * @param {Array} array 数组对象。
     * @return {Array} 新的去重后的数组。
     */
    unique: function(array) {
      var result = {};

      Wb.each(array, function(item) {
        result[item] = 1;
      });
      return Object.keys(result);
    }
  };