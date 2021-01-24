var Wb = {
    dateFormat: "Y-m-d H:i:s.u",
    nullImage: "wb/images/null.png",
    maxInt: 2147483647,
    encode: window.Ext ? Ext.encode: null,
    decode: window.Ext ? Ext.decode: null,
    apply: window.Ext ? Ext.apply: null,
    applyIf: window.Ext ? Ext.applyIf: null,
    menuItem: '<div style="height:40px"><div class="wb_gicon">&#x{glyph};</div>{text}</div>',
    promptWindows: {},
    singleScopes: {},
    toast: window.Ext && Ext.isTouch ? Ext.toast: function(d, c) {
        var a, b;
        if (Ext.isObject(d)) {
            b = d.html;
            c = d.topCenter;
            a = Wb.exclude(d, "html,topCener")
        } else {
            b = d;
            a = null
        }
        return new Ext.ux.window.Notification(Wb.apply({
            position: c ? "tc": "br",
            html: b,
            bodyPadding: 10,
            closeAction: "destroy",
            cls: "x_toast_cls",
            bodyCls: "x_toast_body",
            autoCloseDelay: 4500,
            maxWidth: 500,
            border: false,
            frame: false,
            resizable: false,
            slideBackDuration: 500,
            slideInDuration: 500,
            slideInAnimation: "easeIn",
            slideBackAnimation: "easeIn",
            listeners: {
                afterrender: {
                    single: true,
                    fn: function(e) {
                        e.header.hide();
                        e.mon(e.el, "mousedown",
                        function(f) {
                            if (!f.ctrlKey) {
                                e.close()
                            }
                        })
                    }
                }
            }
        },
        a)).show()
    },
    tip: function(e, c, d) {
        var a, b;
        if (Ext.isObject(e)) {
            b = e.html;
            c = e.isError;
            d = e.topCenter;
            a = Wb.exclude(e, "html,isError,topCenter")
        } else {
            b = e;
            a = null
        }
        return new Ext.ux.window.Notification(Wb.apply({
            title: c ? Str.error: Str.information,
            position: d ? "tc": "br",
            iconCls: c ? "error_icon": "info_icon",
            html: b,
            minWidth: 150,
            maxWidth: 500,
            draggable: true,
            bodyPadding: 10,
            closeAction: "destroy",
            bodyStyle: "line-height:20px;",
            autoCloseDelay: 4500,
            slideBackDuration: 500,
            slideInDuration: 500,
            slideInAnimation: "easeIn",
            slideBackAnimation: "easeIn"
        },
        a)).show()
    },
    nullRequest: function() {
        Wb.request({
            url: "empty",
            showError: false,
            showMask: false
        })
    },
    toString: function(a) {
        return Object.prototype.toString.call(a)
    },
    each: function(f, d, b) {
        if (Wb.toString(f) === "[object Array]") {
            var c, a = f.length;
            if (b !== true) {
                for (c = 0; c < a; c++) {
                    if (d(f[c], c) === false) {
                        return c
                    }
                }
            } else {
                for (c = a - 1; c > -1; c--) {
                    if (d(f[c], c) === false) {
                        return c
                    }
                }
            }
        } else {
            var e;
            for (e in f) {
                if (f.hasOwnProperty(e)) {
                    if (d(e, f[e]) === false) {
                        return false
                    }
                }
            }
        }
        return true
    },
    indexOf: function(d, c) {
        if (!d) {
            return - 1
        }
        var b, a;
        a = d.length;
        for (b = 0; b < a; b++) {
            if (d[b] === c) {
                return b
            }
        }
        return - 1
    },
    dom: function(a) {
        return document.getElementById(a)
    },
    emptyFn: function() {},
    isEmpty: function(a) {
        if (Ext.isObject(a)) {
            return Ext.Object.isEmpty(a)
        } else {
            return a === null || a === undefined || a.length === 0
        }
    },
    init: function(a) {
        Wb.defineConsoleMethods();
        Wb.maskTimeout = a.mask === undefined ? 2000 : a.mask;
        if (a.zo == -1) {
            Wb.zoneOffset = -1
        } else {
            Wb.zoneOffset = -a.zo - (new Date()).getTimezoneOffset()
        }
        Wb.lang = a.lang;
        window.onbeforeunload = Wb.onBeforeUnload = function() {
            if (Wb.unloadEvents) {
                var b, e, c, d = 0;
                Wb.each(Wb.unloadEvents,
                function(f, g) {
                    e = g();
                    if (Wb.isValue(e)) {
                        if (d === 0) {
                            c = Ext.getCmp(f);
                            if (c && c.ownerCt instanceof Ext.tab.Panel) {
                                c.ownerCt.setActiveTab(c)
                            }
                            b = e
                        }
                        d++
                    }
                });
                if (d == 1) {
                    return b
                } else {
                    if (d > 1) {
                        return Wb.format(Str.itemsInfo, b, d)
                    }
                }
            }
        };
        if (window.Ext) {
            if (Ext.isTouch) {
                Wb.theme = a.touchTheme || "classic";
                if (a.timeout !== undefined) {
                    Ext.Ajax.setTimeout(a.timeout)
                }
            } else {
                Wb.theme = a.theme || "modern";
                Wb.editTheme = a.editTheme || "default";
                Wb.isNeptune = Wb.theme == "neptune";
                Wb.isModern = Wb.theme == "modern";
                Ext.setGlyphFontFamily("FontAwesome");
                Ext.QuickTips.init();
                if (a.timeout !== undefined) {
                    Ext.Ajax.timeout = a.timeout;
                    Ext.data.Connection.prototype.timeout = a.timeout;
                    Ext.data.proxy.Server.prototype.timeout = a.timeout;
                    Ext.data.JsonP.timeout = a.timeout;
                    Ext.form.Basic.prototype.timeout = Math.round(a.timeout / 1000)
                }
                Ext.getDoc().on("keydown",
                function(c, b) {
                    if (c.getKey() == c.BACKSPACE && (!Wb.isEditor(b) || b.disabled || b.readOnly)) {
                        c.stopEvent()
                    }
                })
            }
        }
    },
    defineConsoleMethods: function() {
        window.Cs = {
            info: function() {
                if (window.console && console.info) {
                    Function.apply.call(console.info, console, arguments)
                }
            },
            warn: function() {
                if (window.console && console.warn) {
                    Function.apply.call(console.warn, console, arguments)
                }
            },
            error: function() {
                if (window.console && console.error) {
                    Function.apply.call(console.error, console, arguments)
                }
            },
            log: function() {
                if (window.console && console.log) {
                    Function.apply.call(console.log, console, arguments)
                }
            }
        }
    },
	toParams: function(obj,name,map){
		if (!Ext.isObject(obj)) return obj;
		map = map ? map : {};
		for (key in obj) {
			var o = obj[key];
			if (Ext.isObject(o)) {
				Wb.toParams(o, key, map);
			} else {
				if (Ext.isEmpty(name)) {
					map[key] = o;
				} else {
					map[name + "." + key] = o;
				}
			}
		}
		return map;
	},
    post: function(config) {
    	config.dataType = "json";
    	config.method = 'POST';
    	var jsonData = config.jsonData;
		if(jsonData){
			for (var key in jsonData) {
				var data = jsonData[key];
				if (Ext.isArray(data)) jsonData[key] = Ext.encode(data);
			}
			config.jsonData = Wb.toParams(jsonData);
		}
		Wb.request(config);
    },
    request: function(b) {
        var f, e, c = Wb.getBool(b.showMask, true),
        d = b.success,
        a = b.failure;
        e = Ext.apply({},
        b.params, Wb.getValue(b.out));
        if (!Ext.Object.isEmpty(e)) {
            b.params = e
        }
        f = Ext.applyIf({
            success: function() {
                if (c) {
                    Wb.unmask(f.mask, f.message)
                }
                Ext.callback(d, f.scope, arguments)
            },
            failure: function(g) {
                if (c) {
                    Wb.unmask(f.mask, f.message)
                }
                if (Wb.getBool(f.showError, true)) {
                    Wb.except(g)
                }
                Ext.callback(a, f.scope, arguments)
            }
        },
        b);
        if (c) {
            Wb.mask(f.mask, f.message, f.maskTimeout)
        }
        return Ext.Ajax.request(f)
    },
    toLocal: function(a) {
        if (Wb.zoneOffset != -1 && a) {
            return Ext.Date.add(a, Ext.Date.MINUTE, Wb.zoneOffset)
        } else {
            return a
        }
    },
    showIconMessage: function(e, c, b, d, a) {
        if (Ext.isString(c)) {
            c = Ext.String.ellipsis(c, 3000)
        }
        if (Ext.isTouch) {
            Ext.Msg.show({
                title: e,
                message: c,
                buttons: Ext.MessageBox.OK,
                fn: b,
                iconCls: a
            })
        } else {
            return Ext.Msg.show({
                title: e,
                msg: c,
                buttons: Ext.MessageBox.OK,
                fn: b,
                icon: a,
                animateTarget: d
            })
        }
    },
    info: function(b, a, d, c) {
        return Wb.showIconMessage(c || Str.information, b, a, d, Ext.MessageBox.INFO)
    },
    warn: function(b, a, d, c) {
        return Wb.showIconMessage(c || Str.warning, b, a, d, Ext.MessageBox.WARNING)
    },
    error: function(b, a, d, c) {
        return Wb.showIconMessage(c || Str.error, b, a, d, Ext.MessageBox.ERROR)
    },
    except: function(a, c, e) {
        var d, b;
        if (Ext.isTouch && a.isUpload || !Ext.isTouch && a instanceof Ext.form.action.Submit) {
            switch (a.failureType) {
            case "client":
                d = Str.clientInvalid;
                break;
            case "connect":
                d = Str.connectFailure;
                break;
            case "load":
                d = Str.loadFailure;
                break;
            default:
                b = a.result ? a.result.value: null;
                if (Ext.String.startsWith(b, "$WBE201")) {
                    Wb.login();
                    return
                } else {
                    if (b) {
                        d = b
                    } else {
                        d = (a.response ? a.response.responseText: null) || Str.unknowError
                    }
                }
            }
        } else {
            b = a.responseText;
            switch (a.status) {
            case 0:
                d = Str.serverNotResp;
                break;
            case 400:
                d = Str.e400;
                break;
            case 401:
                Wb.login();
                return;
            case 403:
                d = Str.e403;
                break;
            case 404:
                d = Str.e404;
                break;
            default:
                d = b || Str.unknowError
            }
        }
        return Wb.error(d, c, e)
    },
    confirm: function(b, a, d, c) {
        if (Ext.isTouch) {
            return Ext.Msg.show({
                title: c || Str.confirm,
                message: b,
                buttons: Ext.MessageBox.OKCANCEL,
                fn: function(e) {
                    var g, f;
                    if (Ext.isArray(a)) {
                        g = a[0];
                        if (a.length > 1) {
                            f = a[1]
                        }
                    } else {
                        g = a
                    }
                    if (e == "ok") {
                        g()
                    } else {
                        if (f) {
                            f()
                        }
                    }
                },
                iconCls: Ext.MessageBox.QUESTION
            })
        } else {
            return Ext.Msg.show({
                title: c || Str.confirm,
                msg: b,
                buttons: Ext.MessageBox.OKCANCEL,
                fn: function(e) {
                    var g, f;
                    if (Ext.isArray(a)) {
                        g = a[0];
                        if (a.length > 1) {
                            f = a[1]
                        }
                    } else {
                        g = a
                    }
                    if (e == "ok") {
                        g()
                    } else {
                        if (f) {
                            f()
                        }
                    }
                },
                icon: Ext.MessageBox.QUESTION,
                animateTarget: d
            })
        }
    },
    confirmDo: function(c, d, h, g, f) {
        if (!Ext.isArray(c)) {
            c = c ? [c] : []
        }
        var e, b, a = c.length;
        if (!g) {
            g = Str.del
        }
        if (a === 0) {
            Wb.warn(Wb.format(Str.selectRecord, g));
            return null
        } else {
            b = c[0];
            if (!h) {
                h = Ext.isTouch ? b.fields.items[0]._name: b.fields.items[0].name
            }
            if (a == 1) {
                e = Wb.format(Str.singleConfirm, g, b.get(h))
            } else {
                e = Wb.format(Str.manyConfirm, g, b.get(h), a)
            }
            return Wb.confirm(e, d, f)
        }
    },
    choose: function(b, a, d, c) {
        if (Ext.isTouch) {
            return Ext.Msg.show({
                title: c || Str.confirm,
                message: b,
                buttons: Ext.MessageBox.YESNOCANCEL,
                fn: a,
                iconCls: Ext.MessageBox.QUESTION
            })
        } else {
            return Ext.Msg.show({
                title: c || Str.confirm,
                msg: b,
                buttons: Ext.MessageBox.YESNOCANCEL,
                fn: a,
                minWidth: 280,
                icon: Ext.MessageBox.QUESTION,
                animateTarget: d
            })
        }
    },
    login: function(b, a) {
        if (window.top != window && window.top.Wb) {
            window.top.Wb.login();
            return
        }
        if (Wb.loginWinShown) {
            return
        }
        if (Ext.isTouch) {
            Wb.run({
                url: "tlogin",
                success: function(c) {
                    c.loginSuccess = function() {
                        if (c.saveToggle.getValue()) {
                            Wb.setCookie("sys.username", c.username.getValue())
                        }
                        c.win.destroy();
                        Wb.each(c.dockedItems,
                        function(d) {
                            d.show()
                        });
                        Wb.hasModalWin = false;
                        Ext.repaint();
                        Ext.callback(a, c, [c])
                    };
                    if (Wb.hasNS("sys.thome")) {
                        sys.thome.container.add(c.win)
                    }
                    c.dockedItems = Ext.Viewport.getDockedItems();
                    Wb.each(c.dockedItems,
                    function(d) {
                        d.hide()
                    });
                    Wb.hasModalWin = true;
                    c.win.show();
                    Wb.loginWinShown = true;
                    Ext.callback(b, c, [c])
                }
            })
        } else {
            Wb.run({
                url: "login-win",
                success: function(c) {
                    c.loginSucessFn = a;
                    Ext.callback(b, c, [c])
                }
            })
        }
    },
    mask: function(g, i, a) {
        var b, e, d = Ext.getBody(),
        f = Wb.isValue(a) ? a: Wb.maskTimeout;
        if (Ext.isFunction(g)) {
            g = g()
        }
        if (!g) {
            if (Ext.isTouch) {
                g = Ext.Viewport
            } else {
                g = d
            }
        }
        if (!Wb.isValue(i)) {
            i = Str.processing
        }
        if (!g.maskMsgs) {
            g.maskMsgs = []
        }
        g.maskMsgs.push(i);
        e = Ext.Array.unique(g.maskMsgs).join("<br>");
        if (Ext.isTouch) {
            var c = g.getMasked(),
            h = '<span class="wb_spiner">&#xf110;</span>&nbsp;';
            if (c) {
                if (g.maskMsgs.length == 1) {
                    c.updateMaskTimeout(f);
                    c.show()
                }
                c.setMessage(h + e)
            } else {
                g.mask({
                    xtype: "loadmask",
                    indicator: false,
                    message: h + e,
                    maskTimeout: f
                })
            }
        } else {
            g.mask(e, null, null, f);
            if (g == d) {
                b = (d.$cache || d.getCache()).data;
                if (b.maskShimEl) {
                    b.maskShimEl.setStyle("zIndex", 90001)
                }
                if (b.maskEl) {
                    b.maskEl.setStyle("zIndex", 90001)
                }
                if (b.maskMsg) {
                    b.maskMsg.setStyle("zIndex", 90001)
                }
            }
        }
    },
    unmask: function(b, c) {
        var a, d, f;
        if (Ext.isFunction(b)) {
            b = b()
        }
        if (!b) {
            if (Ext.isTouch) {
                b = Ext.Viewport
            } else {
                b = Ext.getBody()
            }
        }
        if (!Wb.isValue(c)) {
            c = Str.processing
        }
        d = b.maskMsgs;
        if (d && d.length > 0) {
            a = d.length;
            Ext.Array.remove(d, c);
            if (d.length === a) {
                throw new Error('The component has no mask with message "' + c + '".')
            }
            if (d.length === 0) {
                b.unmask()
            } else {
                f = Ext.Array.unique(d).join("<br>");
                if (Ext.isTouch) {
                    var e = b.getMasked();
                    if (e) {
                        e.setMessage('<span class="wb_spiner">&#xf110;</span>&nbsp;' + f)
                    }
                } else {
                    b.mask(f, null, null, Wb.maskTimeout)
                }
            }
        } else {
            throw new Error("The component has no mask.")
        }
    },
    doMethod: function(a, b) {
        Wb.doMethodInner(true, a, b, [].slice.call(arguments, 2))
    },
    doMethodChild: function(a, b) {
        Wb.doMethodInner(false, a, b, [].slice.call(arguments, 2))
    },
    doMethodInner: function(a, d, g, c) {
        var e, b, f;
        if (g.substring(0, 1) == "*") {
            f = g.substring(1)
        }
        Ext.suspendLayouts();
        try {
            if (Ext.isArray(d)) {
                b = d
            } else {
                b = [d]
            }
            Wb.each(b,
            function(h) {
                if (a) {
                    if (f) {
                        h[f] = c[0]
                    } else {
                        e = h[g];
                        if (e && Ext.isFunction(e)) {
                            e.apply(h, c)
                        }
                    }
                }
                if (h.queryBy) {
                    h.queryBy(function(i) {
                        if (f) {
                            i[f] = c[0]
                        } else {
                            e = i[g];
                            if (e && Ext.isFunction(e)) {
                                e.apply(i, c)
                            }
                        }
                    })
                }
            })
        } finally {
            Ext.resumeLayouts(true)
        }
    },
    equals: function(b, a) {
        if (!b) {
            b = ""
        }
        if (!a) {
            a = ""
        }
        return b == a
    },
    uniqueName: function(d, c) {
        var a = c,
        b = 1;
        while (a in d) {
            a = c + b++
        }
        return a
    },
    getFileSize: function(a) {
        if (Ext.isNumber(a)) {
            if (a >= 1048576) {
                return Wb.format(a / 1048576, "#,##0.#") + " MB"
            } else {
                return Wb.format(Math.ceil(a / 1024), "#,##0") + " KB"
            }
        } else {
            return ""
        }
    },
    getNode: function(a, b) {
        if (!b) {
            b = 0
        }
        while (a && a.getDepth() > b) {
            a = a.parentNode
        }
        return a
    },
    turnTab: function(b) {
        var a = b,
        d = a,
        c = [];
        Ext.suspendLayouts();
        try {
            while ((a = a.ownerCt)) {
                if (a instanceof Ext.tab.Panel) {
                    c.push(d)
                }
                d = a
            }
            while ((a = c.pop())) {
                a.ownerCt.setActiveTab(a)
            }
        } finally {
            Ext.resumeLayouts(true)
        }
    },
    prompt: function(g) {
        if (Ext.isTouch) {
            var a = Wb.touchPrompt(g);
            h(a, g.focusControl);
            return a
        }
        function d() {
            if (!Wb.promptPickList) {
                Wb.promptPickList = {}
            }
            if (!Wb.promptSaveList) {
                Wb.promptSaveList = {}
            }
            e.queryBy(function(o) {
                var n = o.pickKeyname,
                l, p, m;
                if (o.saveKeyname) {
                    Wb.promptSaveList[o.saveKeyname] = o.getValue()
                }
                if (n && o instanceof Ext.form.field.ComboBox) {
                    l = Wb.promptPickList[n];
                    p = o.getValue();
                    if (Wb.isEmpty(p)) {
                        return
                    }
                    if (!l) {
                        Wb.promptPickList[n] = [p];
                        o.store.add({
                            field1: p,
                            field2: p
                        })
                    } else {
                        m = Wb.indexOf(l, p);
                        if (m != -1) {
                            l.splice(m, 1);
                            o.store.clearFilter();
                            o.store.removeAt(m)
                        }
                        l.unshift(p);
                        o.store.insert(0, {
                            field1: p,
                            field2: p
                        })
                    }
                }
                return false
            })
        }
        function h(n, l) {
            var m;
            if (n.items.getCount() > 0 && l !== null) {
                if (l) {
                    m = n.down("#" + l)
                } else {
                    Wb.each(n.query("field"),
                    function(o) {
                        if (!o.isHidden() && !o.isDisabled()) {
                            m = o;
                            return false
                        }
                    })
                }
                if (m && m.focus) {
                    if (Ext.isTouch) {
                        setTimeout(function() {
                            m.focus()
                        },
                        50)
                    } else {
                        m.focus(true, true)
                    }
                }
            }
        }
        function c(l) {
            Wb.each(l,
            function(m) {
                if (!m.id) {
                    m.id = Wb.getId()
                }
                if (Wb.promptPickList && m.pickKeyname && (Wb.isEmpty(m.store) || Ext.isArray(m.store))) {
                    m.store = Ext.Array.merge(Wb.promptPickList[m.pickKeyname] || [], m.store || [])
                }
                if (m.readOnly) {
                    m.selectOnFocus = true
                }
                if (m.items && Ext.isArray(m.items)) {
                    c(m.items)
                }
            })
        }
        function f(l) {
            Wb.each(l,
            function(m) {
                if (Wb.promptSaveList && m.saveKeyname && Wb.promptSaveList[m.saveKeyname] !== undefined) {
                    Ext.getCmp(m.id).setValue(Wb.promptSaveList[m.saveKeyname])
                }
                if (m.items && Ext.isArray(m.items)) {
                    f(m.items)
                }
            })
        }
        if (g.windowName) {
            var b = Wb.promptWindows[g.windowName];
            if (b) {
                if (g.resetScrollbar) {
                    b.body.dom.scrollTop = 0
                }
                b.setTitle(g.title);
                b.setIconCls(g.iconCls);
                b.handler = g.handler;
                Wb.activePrompt = b;
                b.show();
                h(b, g.focusControl);
                return b
            }
        }
        var e, i, k, j;
        j = Ext.apply({},
        g);
        delete j.defaults;
        delete j.listeners;
        j = Ext.apply({
            width: 500,
            minWidth: 100,
            minHeight: 100,
            modal: true,
            dialog: true,
            layoutType: "anchorForm",
            items: i,
            defaults: {
                xtype: "textfield"
            },
            listeners: {
                close: function() {
                    Wb.activePrompt = null
                },
                ok: function() {
                    d();
                    Ext.callback(e.handler, e, [Wb.getValue(e), e])
                }
            }
        },
        j);
        if (Wb.getBool(j.autoReset, g.windowName)) {
            j.listeners.hide = function(l) {
                Wb.reset(l)
            }
        }
        if (!g.windowName) {
            j.closeAction = "destroy"
        }
        Ext.apply(j.defaults, g.defaults);
        Ext.apply(j.listeners, g.listeners);
        i = j.items;
        if (!Ext.isArray(i)) {
            i = [i]
        }
        c(i);
        k = j.focusControl;
        delete j.focusControl;
        if (j.isUpload) {
            j.items = {
                xtype: "form",
                itemId: "form",
                border: false,
                defaults: j.defaults,
                flat: true,
                items: j.items
            };
            if (j.layout) {
                j.items.layout = j.layout
            } else {
                j.items.layoutType = j.layoutType || "anchorForm"
            }
            delete j.defaults;
            delete j.overflowX;
            delete j.overflowY;
            j.layout = "fit";
            delete j.layoutType
        }
        e = new Ext.window.Window(j);
        Wb.activePrompt = e;
        e.show();
        if (!g.win) {
            e.setHeight(Math.min(e.getHeight() + 4, Ext.Element.getViewportHeight() - 8))
        }
        Ext.suspendLayouts();
        try {
            f(i)
        } finally {
            Ext.resumeLayouts(true)
        }
        h(e, k);
        if (j.windowName) {
            Wb.promptWindows[j.windowName] = e;
            if (j.destroyOn) {
                j.destroyOn.mon(j.destroyOn, "destroy",
                function() {
                    e.destroy();
                    delete Wb.promptWindows[j.windowName]
                })
            }
        }
        return e
    },
    touchPrompt: function(e) {
        var a, c = e.handler,
        b = Ext.Viewport.getDockedItems();
        Wb.each(b,
        function(f) {
            f.hide()
        });
        Wb.hasModalWin = true;
        function d() {
            if (c) {
                if (Wb.verify(a)) {
                    Ext.callback(c, a, [Wb.getValue(a), a])
                }
            } else {
                a.destroy()
            }
        }
        a = Ext.Viewport.add({
            xtype: "formpanel",
            height: "100%",
            scrollable: true,
            width: "100%",
            left: 0,
            top: 0,
            listeners: {
                action: function() {
                    d()
                },
                destroy: function() {
                    Wb.each(b,
                    function(f) {
                        f.show()
                    });
                    Wb.hasModalWin = false;
                    Ext.repaint()
                }
            },
            items: [{
                xtype: "titlebar",
                title: e.title,
                docked: "top",
                items: [{
                    xtype: "button",
                    text: Str.cancel,
                    align: "left",
                    handler: function(f) {
                        Wb.each(b,
                        function(g) {
                            g.show()
                        });
                        f.up("panel").destroy()
                    }
                },
                {
                    xtype: "button",
                    text: Str.ok,
                    align: "right",
                    handler: function(f) {
                        d()
                    }
                }]
            },
            {
                xtype: "fieldset",
                defaults: {
                    xtype: "textfield"
                },
                items: e.items
            }]
        });
        return a
    },
    promptText: function(f, c, e) {
        var a = Ext.apply({},
        e),
        b = a.allowBlank,
        g = a.xtype,
        d = a.value;
        if (e) {
            delete e.allowBlank;
            delete e.xtype;
            delete e.value
        }
        return new Ext.window.Window(Ext.apply({
            title: f,
            autoShow: true,
            modal: true,
            closeAction: "destroy",
            maximizable: true,
            layout: g ? "form": "fit",
            iconCls: "edit_icon",
            dialog: true,
            width: 500,
            padding: "8",
            height: g ? null: 300,
            resizable: true,
            focusControl: "text",
            listeners: {
                ok: function(h) {
                    if (c) {
                        c(h.getComponent("text").getValue(), h)
                    }
                }
            },
            items: [{
                allowBlank: Wb.getBool(b, true),
                itemId: "text",
                xtype: g || "textarea",
                value: d || ""
            }]
        },
        e))
    },
    promptString: function(d, b, c) {
        var a = Wb.applyIf({
            xtype: "textfield"
        },
        c);
        return Wb.promptText(d, b, a)
    },
    viewText: function(c, b, a) {
        return new Ext.window.Window({
            title: b || Str.property,
            autoShow: true,
            layout: "fit",
            modal: true,
            maximizable: true,
            closeAction: "destroy",
            resizable: true,
            iconCls: "property_icon",
            width: 600,
            height: 450,
            items: Wb.apply({
                xtype: "textarea",
                fieldStyle: "line-height:1.5",
                readOnly: true,
                value: c
            },
            a),
            buttons: [{
                text: Str.close,
                iconCls: "close_icon",
                handler: function() {
                    this.up("window").close()
                }
            }]
        })
    },
    viewHtml: function(a, c, b) {
        return new Ext.window.Window({
            title: c || Str.property,
            autoShow: true,
            layout: "fit",
            modal: true,
            maximizable: true,
            closeAction: "destroy",
            resizable: true,
            iconCls: "property_icon",
            width: 600,
            height: 450,
            items: Wb.apply({
                autoScroll: true,
                xtype: "box",
                html: a,
                padding: 8,
                fieldStyle: "line-height:1.5"
            },
            b),
            buttons: [{
                text: Str.close,
                iconCls: "close_icon",
                handler: function() {
                    this.up("window").close()
                }
            }]
        })
    },
    getStdValue: function(c, a, b) {
        return Wb.getValue(c, a, b, true)
    },
    getValue: function(f, b, d, e) {
        var a = {},
        h = !e,
        c = Ext.isTouch ? Ext.field.File: Ext.form.field.File;
        if (!f) {
            return a
        }
        function g(j) {
            var i, k = j.itemId;
            if (k && j.getValue && (!b || Wb.indexOf(b, k) != -1) && !a.hasOwnProperty(k)) {
                if (h && j.getTextValue) {
                    a["%" + k] = j.getTextValue()
                }
                if (j instanceof c) {
                    a["$" + k] = j.removeFile ? 1 : 0;
                    if (d) {
                        a[k] = j.getValue();
                        if (Ext.isTouch && !a[k] && j.element.hasCls(Ext.baseCSSPrefix + "field-clearable")) {
                            a[k] = "(file)"
                        }
                    }
                } else {
                    a[k] = j.getValue()
                }
            }
            return false
        }
        if (!Ext.isArray(f)) {
            f = [f]
        }
        if (b && !Ext.isArray(b)) {
            if (b.indexOf(",") == -1) {
                b = [b]
            } else {
                b = b.split(",")
            }
        }
        Wb.each(f,
        function(i) {
            g(i);
            if (i.queryBy) {
                i.queryBy(g)
            }
        });
        return a
    },
    clearNull: function(a) {
        var b, c = Ext.Object.getKeys(a),
        d = c.length;
        while (d--) {
            b = c[d];
            if (!Wb.isValue(a[b])) {
                delete a[b]
            }
        }
        return a
    },
    getVal: function(a, b) {
        if (!a) {
            return undefined
        }
        if (!b) {
            if (Ext.isArray(a)) {
                b = a[0].itemId
            } else {
                b = a.itemId
            }
        }
        return Wb.getValue(a, b)[b]
    },
    setValue: function(c, b) {
        if (!c || !b) {
            return
        }
        var a = Ext.isTouch ? Ext.field.File: Ext.form.field.File;
        Ext.suspendLayouts();
        try {
            function d(e) {
                var f = e.itemId;
                if (f && b.hasOwnProperty(f) && e.setValue) {
                    if (e instanceof a) {
                        if (b[f]) {
                            if (Ext.isTouch) {
                                e.showClearIcon()
                            } else {
                                e.inputEl.dom.value = b[f] ? Str.hasFile: ""
                            }
                        }
                    } else {
                        e.setValue(b[f])
                    }
                }
                return false
            }
            if (!Ext.isArray(c)) {
                c = [c]
            }
            Wb.each(c,
            function(e) {
                d(e);
                if (e.queryBy) {
                    e.queryBy(d)
                }
            })
        } finally {
            Ext.resumeLayouts(true)
        }
    },
    setVal: function(a, d, b) {
        var c = {};
        c[d] = b;
        Wb.setValue(a, c)
    },
    resetScroll: function(a) {
        a.getScrollable().getScroller().scrollTo(0, 0)
    },
    reset: function(b, a) {
        if (!b) {
            return
        }
        Ext.suspendLayouts();
        try {
            function c(d) {
                var e = d.itemId;
                if (e && d.reset && (!a || Wb.indexOf(a, e) != -1)) {
                    d.reset()
                }
                return false
            }
            if (!Ext.isArray(b)) {
                b = [b]
            }
            if (a && !Ext.isArray(a)) {
                a = [a]
            }
            Wb.each(b,
            function(d) {
                c(d);
                if (d.queryBy) {
                    d.queryBy(c)
                }
            })
        } finally {
            Ext.resumeLayouts(true)
        }
    },
    getRefer: function(b, a) {
        function c(d) {
            var e = d.itemId;
            if (e) {
                a[e] = d
            }
            return false
        }
        if (!a) {
            a = {}
        }
        if (b && !Ext.isArray(b)) {
            b = [b]
        }
        Wb.each(b,
        function(d) {
            c(d);
            if (d.queryBy) {
                d.queryBy(c)
            }
        });
        return a
    },
    getDefaultHeight: function() {
        if (Wb.isModern) {
            return 26
        }
        if (Wb.isNeptune) {
            return 24
        }
        return 22
    },
    highlight: function(a, c) {
        var b;
        if (a.isNode) {
            b = a.getOwnerTree().view.getNode(a);
            if (b) {
                b = Ext.get(b)
            }
        } else {
            if (Wb.hasNS("store.bindTable.view", a)) {
                b = a.store.bindTable.view.getNode(a);
                if (b) {
                    b = Ext.get(b)
                }
            } else {
                b = a
            }
        }
        if (b) {
            if (a.isHighlighting) {
                return
            }
            a.isHighlighting = true;
            if (c) {
                b.highlight("ff0000", {
                    duration: 1500,
                    callback: function() {
                        delete a.isHighlighting
                    }
                })
            } else {
                b.highlight(null, {
                    duration: 1500,
                    callback: function() {
                        delete a.isHighlighting
                    }
                })
            }
        }
    },
    verifyGrid: function(e) {
        var g, f, c, h, k, a = true,
        b = e.bindTable || e,
        d = Wb.findEditing(b);
        if (!d) {
            return true
        }
        d.completeEdit();
        h = Wb.fetchColumns(b);
        f = h.length;
        k = b.store;
        k.each(function(i) {
            for (g = 0; g < f; g++) {
                c = h[g];
                if ((c.editor && !c.editor.readOnly && c.editor.allowBlank === false || c.field && !c.field.readOnly && c.field.allowBlank === false) && !c.hidden && Wb.isEmpty(i.get(c.dataIndex))) {
                    a = false;
                    d.startEdit(i, c);
                    if (d.activeEditor) {
                        d.activeEditor.field.validate()
                    }
                    return false
                }
            }
        });
        return a
    },
    verify: function(b) {
        var e, a;
        if (Ext.isArray(b)) {
            e = b
        } else {
            e = [b]
        }
        function d(f) {
            var g;
            if (f.getLabel) {
                g = f.getLabel()
            }
            if (!g) {
                g = f.getPlaceHolder()
            }
            if (!g) {
                g = Str.thisField
            }
            return g
        }
        function c(f) {
            if (Ext.isTouch) {
                if (a) {
                    return false
                }
                var h, g = !Wb.isHidden(f);
                if (f.getRequired && f.getRequired() && f.getValue && g && (!f.isDisabled || !f.isDisabled()) && (!f.getReadOnly || !f.getReadOnly()) && Ext.isEmpty(f.getValue())) {
                    a = f;
                    Wb.warn(Wb.format(Str.requiredField, d(f)),
                    function() {
                        if (f.focus) {
                            f.focus(true, true)
                        }
                    })
                } else {
                    if (g && f instanceof Ext.field.DateTime) {
                        h = f.getValue();
                        if (h && !Ext.isDate(h)) {
                            a = f;
                            Wb.warn(Wb.format(Str.invalidValue, d(f)),
                            function() {
                                f.focus(true, true)
                            })
                        }
                    }
                }
            } else {
                if (f instanceof Ext.form.field.ComboBox) {
                    f.assertValue()
                }
                if (!a && f.validate && !f.hidden && !f.disabled && (!Ext.isFunction(f.up) || !f.up("[hidden=true]") && !f.up("[disabled=true]")) && !f.validate() && !f.inEditor) {
                    a = f
                }
            }
            return false
        }
        Ext.suspendLayouts();
        try {
            Wb.each(e,
            function(f) {
                c(f);
                if (f.queryBy) {
                    f.queryBy(c)
                }
            })
        } finally {
            Ext.resumeLayouts(true)
        }
        if (Ext.isTouch) {
            return ! a
        } else {
            if (a) {
                Wb.turnTab(a);
                if (a.focus) {
                    if (a instanceof Ext.form.field.Base) {
                        a.focus(true, true)
                    } else {
                        if (! (a instanceof Ext.grid.Panel)) {
                            a.focus()
                        }
                    }
                }
                return false
            } else {
                return true
            }
        }
    },
    focusNext: function() {
        var a, b = Ext.Element.getActiveElement();
        if (b && b.id) {
            if (Ext.String.endsWith(b.id, "-inputEl")) {
                a = Ext.getCmp(b.id.slice(0, -8));
                if (a) {
                    if (a instanceof Ext.form.field.TextArea && !a.enterIsSpecial) {
                        return null
                    }
                    do {
                        a = a.nextNode("textfield")
                    } while ( a && ( Wb . isHidden ( a ) || a.disabled));
                    if (a && !Wb.isHidden(a) && !a.disabled) {
                        a.focus(false, true);
                        return a
                    }
                }
            }
        }
        return null
    },
    isHidden: function(b) {
        var c = b,
        a = Ext.isTouch;
        do {
            if (c.isHidden && c.isHidden()) {
                return true
            }
            c = a ? c.parent: c.ownerCt
        } while ( c );
        return false
    },
    replace: function(c, d, b, a) {
        if (!c || !d) {
            return c
        }
        var e;
        if (a) {
            e = new RegExp(Wb.quoteRegexp(d), "i")
        } else {
            e = new RegExp(Wb.quoteRegexp(d))
        }
        return c.replace(e, b)
    },
    replaceAll: function(c, d, b, a) {
        if (!c || !d) {
            return c
        }
        var e;
        if (a) {
            e = "gi"
        } else {
            e = "g"
        }
        return c.replace(new RegExp(Wb.quoteRegexp(d), e), b)
    },
    quoteRegexp: function(a) {
        return a.replace(/[.?*+\^$\[\]\\(){}|\-]/g, "\\$&")
    },
    isValue: function(a) {
        return a !== null && a !== undefined
    },
    getDefined: function() {
        var a, c, b = arguments.length;
        for (c = 0; c < b; c++) {
            a = arguments[c];
            if (a !== undefined) {
                return a
            }
        }
        return undefined
    },
    findEditing: function(a) {
        var b = a.bindTable || a;
        return b.findPlugin("cellediting") || b.findPlugin("rowediting")
    },
    removeFile: function(a) {
        a.reset();
        a.removeFile = true
    },
    remove: function(k, j, d) {
        if (Ext.isTouch) {
            if (!j) {
                j = k.getSelection()
            }
            k.store.remove(j);
            return
        }
        var h, f, g = Ext.isArray(k),
        a = g && (k[0] instanceof Ext.data.Model);
        if (g && !a) {
            Ext.Array.remove(k, j);
            return
        }
        d = Wb.getBool(d, true);
        if (j && !Ext.isArray(j)) {
            j = [j]
        }
        Ext.suspendLayouts();
        try {
            if (k instanceof Ext.tree.Panel || k instanceof Ext.data.TreeStore) {
                k = k.bindTable || k;
                if (!j) {
                    j = Wb.reverse(k.getSelection())
                }
                if (j.length === 0) {
                    return
                }
                var c, e, i;
                if (d) {
                    c = j[j.length - 1];
                    e = c.parentNode;
                    i = c;
                    while (true) {
                        i = i.nextSibling;
                        if (Wb.indexOf(j, i) == -1) {
                            break
                        }
                    }
                    if (!i) {
                        i = c;
                        while (true) {
                            i = i.previousSibling;
                            if (Wb.indexOf(j, i) == -1) {
                                break
                            }
                        }
                    }
                    if (!i && e) {
                        i = e
                    }
                }
                Wb.each(j,
                function(l) {
                    l.remove()
                });
                if (d) {
                    if (i && (i.parentNode || k.rootVisible)) {
                        k.setSelection(i)
                    }
                }
            } else {
                if (k instanceof Ext.grid.Panel || k instanceof Ext.data.Store) {
                    k = k.bindTable || k;
                    var b = Wb.findEditing(k);
                    if (b) {
                        b.completeEdit()
                    }
                    if (!j) {
                        j = k.getSelection()
                    }
                    if (j.length === 0) {
                        return
                    }
                    if (d) {
                        h = k.store.indexOf(j[0])
                    }
                    if (k instanceof Ext.grid.property.Grid) {
                        k.removeProperty(j[0].data.name)
                    } else {
                        k.store.remove(j);
                        Wb.refreshRowNum(k)
                    }
                    if (d) {
                        f = k.store.getCount() - 1;
                        if (h > f) {
                            h = f
                        }
                        if (h > -1) {
                            k.setSelection(h)
                        }
                    }
                } else {
                    if (k instanceof Ext.view.View) {
                        if (!j) {
                            j = k.getSelection()
                        }
                        if (j.length === 0) {
                            return
                        }
                        if (d) {
                            h = k.store.indexOf(j[0])
                        }
                        k.store.remove(j);
                        if (d) {
                            f = k.store.getCount() - 1;
                            if (h > f) {
                                h = f
                            }
                            if (h > -1) {
                                k.setSelection(h)
                            }
                        }
                    } else {
                        if (g) {
                            if (k[0] instanceof Ext.data.Model) {
                                Wb.each(k,
                                function(l) {
                                    l.remove()
                                })
                            }
                        } else {
                            Wb.each(j,
                            function(l) {
                                k.remove(l)
                            })
                        }
                    }
                }
            }
        } finally {
            Ext.resumeLayouts(true)
        }
    },
    setModified: function(a) {
        if (a) {
            if (!a.isModified) {
                a.isModified = true;
                if (!Ext.String.startsWith(a.title, "*")) {
                    a.setTitle("*" + a.title)
                }
            }
        }
        return a
    },
    unModified: function(a) {
        if (a) {
            if (a.isModified) {
                a.isModified = false;
                a.setTitle(a.title.substring(1))
            }
        }
        return a
    },
    setTitle: function(a, c) {
        if (Ext.isTouch) {
            a = a.down("titlebar")
        }
        var b = Wb.isEmpty(a.title || a._title) ? "": String(a.title || a._title),
        d = b.indexOf(" - ");
        if (d != -1) {
            b = b.substring(0, d)
        }
        if (Wb.isEmpty(c)) {
            a.setTitle(b)
        } else {
            a.setTitle(b + " - " + c)
        }
    },
    parseBool: function(b, a) {
        if (Wb.isValue(b)) {
            var c = String(b);
            return ! (c == "false" || c == "0" || c === "")
        } else {
            if (a === undefined) {
                return false
            } else {
                return a
            }
        }
    },
    getBool: function(b, a) {
        return !! (b === undefined ? a: b)
    },
    reload: function(e, d, c) {
        if (e instanceof Ext.grid.Panel || e instanceof Ext.data.Store) {
            var g = e.store || e,
            i = Ext.apply({},
            d);
            i.params = Ext.apply({},
            i.params, g.lastOptions ? g.lastOptions.params: null);
            g.reload(i)
        } else {
            e = e.bindTable || e;
            if (e.isRefreshing) {
                return
            }
            e.isRefreshing = true;
            if (!c) {
                c = {}
            }
            var h, a, b = e.getSelection()[0],
            f = c.field || e.displayField;
            if (b) {
                h = b.getPath(f, c.separator || "\n")
            }
            e.selModel.deselectAll();
            e.store.load({
                callback: function(k, j, l) {
                    if (e.isRefreshing) {
                        delete e.isRefreshing
                    }
                    if (l && h) {
                        e.selectPath(h, f, c.separator || "\n", d)
                    }
                }
            })
        }
    },
    verifyName: function(b) {
        var e, d, a = b.length;
        for (d = 0; d < a; d++) {
            e = b.charAt(d);
            if (! (e >= "a" && e <= "z" || e >= "A" && e <= "Z" || e == "_" || d > 0 && (e >= "0" && e <= "9"))) {
                return Wb.format(Str.invalidChar, e)
            }
        }
        return true
    },
    verifyFile: function(b) {
        var f, e, a = b.length,
        d = '/:*?"<>|';
        for (e = 0; e < a; e++) {
            f = b.charAt(e);
            if (d.indexOf(f) != -1) {
                return Wb.format(Str.invalidChar, f)
            }
        }
        return true
    },
    getDoc: function(a) {
        try {
            return a.contentWindow.document || a.contentDocument || window.frames[a.id].document
        } catch(b) {
            return null
        }
    },
    insertIframe: function(a, b) {
        var d, c = Wb.getId();
        b = Wb.getBool(b, true);
        a.update('<iframe scrolling="auto" id="' + c + '" name="' + c + '" frameborder="0" width="100%" height="100%"></iframe>');
        a.iframe = a.el.down("iframe");
        d = a.iframe.dom;
        a.iframe.submit = function(e, f, g) {
            if (a.isSubmiting) {
                return
            }
            a.isSubmiting = true;
            if (b) {
                Wb.mask(a, Str.loading)
            }
            Wb.submit(e, f, d.id, g)
        };
        a.iframe.getDoc = function() {
            return Wb.getDoc(d)
        };
        Ext.fly(d).on("load",
        function() {
            if (!a.isSubmiting) {
                return
            }
            delete a.isSubmiting;
            if (b) {
                Wb.unmask(a, Str.loading)
            }
        });
        a.mon(a, "beforedestroy",
        function(f) {
            var h, i;
            h = f.iframe.getDoc();
            try {
                if (h) {
                    for (i in h) {
                        if (h.hasOwnProperty && h.hasOwnProperty(i)) {
                            delete h[i]
                        }
                    }
                    d.src = "about:blank";
                    h.write("");
                    h.clear();
                    h.close()
                }
                f.iframe.destroy()
            } catch(g) {}
        });
        return a.iframe
    },
    getFilename: function(a) {
        if (Wb.isEmpty(a)) {
            return ""
        }
        var b = Math.max(a.lastIndexOf("/"), a.lastIndexOf("\\"));
        if (b == -1) {
            return a
        } else {
            return a.substring(b + 1)
        }
    },
    getPath: function(a) {
        if (Wb.isEmpty(a)) {
            return ""
        }
        var b = Math.max(a.lastIndexOf("/"), a.lastIndexOf("\\"));
        if (b == -1) {
            return a
        } else {
            return a.substring(0, b)
        }
    },
    extractFileExt: function(b) {
        if (!Wb.isEmpty(b)) {
            var a = b.lastIndexOf(".");
            if (a != -1) {
                return b.substring(a + 1)
            }
        }
        return ""
    },
    getError: function(d, c) {
        if (d) {
            var b = "#WBE" + c + ":",
            a = b.length;
            if (d.substring(0, a) == b) {
                return d.substring(a)
            }
        }
        return null
    },
    select: function(c) {
        var a = c.getOwnerTree(),
        b = [];
        if (!a) {
            return
        }
        Ext.suspendLayouts();
        c.bubble(function(d) {
            if (a.rootVisible || d.parentNode) {
                b.push(d)
            }
        });
        b.shift();
        Wb.each(b,
        function(d) {
            d.expand()
        },
        true);
        a.setSelection(c);
        Ext.resumeLayouts(true)
    },
    mimicClick: function(b, a, f, c, g) {
        var d = b.ownerCt;
        if (g.getKey() == g.ENTER) {
            if (d.hasListeners.itemclick) {
                d.fireEventArgs("itemclick", arguments)
            } else {
                d.fireEventArgs("itemdblclick", arguments)
            }
            g.stopEvent()
        }
    },
    addPrefix: function(b, a) {
        if (b) {
            return b + "." + a
        } else {
            return a
        }
    },
    getLang: function(b) {
        var a = {};
        Wb.each(b,
        function(d, c) {
            a[d] = c[Wb.lang]
        });
        return a
    },
    getModifiedTitle: function(b, d) {
        if (!b) {
            return null
        }
        var e, c = 0,
        a;
        b.items.each(function(f) {
            if (f.isModified) {
                if (!a) {
                    a = f
                }
                if (!e) {
                    e = f.title.substring(1)
                }
                c++
            }
        });
        if (d && a) {
            b.setActiveTab(a)
        }
        if (c > 1) {
            return Wb.format(Str.itemsInfo, e, c)
        } else {
            if (c == 1) {
                return '"' + e + '"'
            } else {
                return null
            }
        }
    },
    fromPanel: function(a, c) {
        var b = c.target,
        d = a.id;
        if (b.id == (d + "-innerCt")) {
            return true
        }
        if (b.id == (d + "-body")) {
            if (c.getX() > a.getX() + a.body.dom.clientWidth || c.getY() > a.getY() + a.body.dom.clientHeight) {
                return false
            } else {
                return true
            }
        }
        return false
    },
    copy: function(b) {
        function a(f) {
            var d, g = f.data,
            e = {},
            c = ["allowDrag", "allowDrop", "children", "depth", "id", "index", "isFirst", "isLast", "loaded", "loading", "parentId", "root"];
            if (g.id) {
                e.id = Wb.getId()
            }
            Wb.each(g,
            function(h, i) {
                if (Wb.indexOf(c, h) == -1) {
                    e[h] = i
                }
            });
            if (!f.isLeaf() && f.isLoaded()) {
                d = f.childNodes;
                e.children = [];
                Wb.each(d,
                function(h) {
                    e.children.push(a(h))
                })
            }
            return e
        }
        return Ext.clone(a(b))
    },
    append: function(d, c, b, a) {
        var f, e = [];
        if (!Ext.isArray(d)) {
            d = [d]
        }
        Ext.suspendLayouts();
        try {
            Wb.each(d,
            function(g) {
                if (b) {
                    g = Wb.copy(g)
                }
                f = c.appendChild(g);
                f.commit();
                e.push(f)
            });
            if (Wb.getBool(a, true)) {
                c.expand();
                c.getOwnerTree().setSelection(e)
            }
        } finally {
            Ext.resumeLayouts(true)
        }
        return e
    },
    loadSelect: function(e, c, g, a) {
        var b, f, d = e.data.loaded;
        c = Wb.toArray(c);
        e.expand(false,
        function() {
            if (d) {
                b = Wb.append(c, e);
                Ext.callback(g, e, [b, e])
            } else {
                b = [];
                f = {};
                if (!a) {
                    a = "text"
                }
                Wb.each(c,
                function(h) {
                    if (h.data) {
                        f[h.data[a]] = true
                    } else {
                        f[h[a]] = true
                    }
                });
                e.eachChild(function(h) {
                    if (f[h.data[a]]) {
                        b.push(h)
                    }
                });
                e.getOwnerTree().setSelection(b);
                Ext.callback(g, e, [b, e])
            }
        })
    },
    insertBefore: function(e, d, c, b) {
        var a = d.parentNode,
        g, f = [];
        if (!Ext.isArray(e)) {
            e = [e]
        }
        Ext.suspendLayouts();
        try {
            Wb.each(e,
            function(h) {
                if (c) {
                    h = Wb.copy(h)
                }
                g = a.insertBefore(h, d);
                g.commit();
                f.push(g)
            });
            if (Wb.getBool(b, true)) {
                d.getOwnerTree().setSelection(f)
            }
        } finally {
            Ext.resumeLayouts(true)
        }
        return f
    },
    insertAfter: function(f, e, d, c) {
        var b = e.nextSibling,
        a = e.parentNode,
        h, g = [];
        if (!Ext.isArray(f)) {
            f = [f]
        }
        Ext.suspendLayouts();
        try {
            Wb.each(f,
            function(i) {
                if (d) {
                    i = Wb.copy(i)
                }
                if (b) {
                    h = a.insertBefore(i, b)
                } else {
                    h = a.appendChild(i)
                }
                h.commit();
                g.push(h)
            });
            if (Wb.getBool(c, true)) {
                e.getOwnerTree().setSelection(g)
            }
        } finally {
            Ext.resumeLayouts(true)
        }
        return g
    },
    expand: function(c, e) {
        var d = c.bindTable || c,
        b = d.getSelection(),
        a;
        Ext.suspendLayouts();
        try {
            if (b.length === 0) {
                a = d.getRootNode();
                if (d.rootVisible) {
                    b.push(a)
                } else {
                    if (e) {
                        a.collapseChildren(true)
                    } else {
                        a.expandChildren(true)
                    }
                    return
                }
            }
            Wb.each(b,
            function(f) {
                if (e) {
                    f.collapse(true)
                } else {
                    f.expand(true)
                }
            })
        } finally {
            Ext.resumeLayouts(true)
        }
    },
    collapse: function(a) {
        var b = a.bindTable || a;
        Wb.expand(b, true)
    },
    isModal: function() {
        var a = false;
        Ext.WindowMgr.each(function(b) {
            if (b.modal && b.isVisible()) {
                a = true;
                return false
            }
        });
        return a || Ext.getBody().isMasked()
    },
    getId: function() {
        if (!Wb.id) {
            Wb.id = (new Date()).getTime()
        }
        return "wb" + Wb.id++
    },
    getTreeTools: function(a) {
        var b = [];
        if (!a) {
            a = {}
        }
        if (Wb.getBool(a.refresh, true)) {
            b.push({
                type: "refresh",
                tooltip: Str.refresh,
                callback: function(c) {
                    Wb.reload(c)
                }
            })
        }
        if (Wb.getBool(a.expand, true)) {
            b.push({
                type: "expand",
                tooltip: Str.expandSelected,
                callback: function(c) {
                    Wb.expand(c)
                }
            })
        }
        if (Wb.getBool(a.collapse, true)) {
            b.push({
                type: "collapse",
                tooltip: Str.collapseSelected,
                callback: function(c) {
                    Wb.collapse(c)
                }
            })
        }
        if (a.search) {
            b.push({
                type: "search",
                tooltip: Str.toggleSearch,
                callback: function(c) {
                    var e = c.getDockedComponent("_searchNodeBar"),
                    d = c.displayField;
                    if (e) {
                        e.setVisible(!e.isVisible())
                    } else {
                        e = c.addDocked({
                            xtype: "toolbar",
                            itemId: "_searchNodeBar",
                            dock: "top",
                            searchHandler: function() {
                                var g = (e.getComponent("combo").getValue() || "").toLowerCase(),
                                f = c.getRootNode().findChildBy(function(h) {
                                    if (g === (h.data[d] || "").toLowerCase()) {
                                        return true
                                    }
                                },
                                c, true);
                                if (f) {
                                    Wb.select(f)
                                } else {
                                    Wb.warn(Wb.format(Str.notFound, g))
                                }
                            },
                            items: [{
                                xtype: "combo",
                                itemId: "combo",
                                flex: 1,
                                displayField: "text",
                                queryMode: "local",
                                store: {
                                    fields: ["text"]
                                },
                                doQuery: function(i) {
                                    var j, g = this,
                                    h = [],
                                    f = c.rootVisible;
                                    c.getRootNode().cascadeBy(function(k) {
                                        if (!f && k.getDepth() === 0) {
                                            return
                                        }
                                        j = k.data[d] || "";
                                        if (j.toLowerCase().indexOf(i) != -1) {
                                            h.push({
                                                text: j
                                            })
                                        }
                                    });
                                    g.store.loadData(h);
                                    if (h.length) {
                                        g.expand()
                                    } else {
                                        g.collapse()
                                    }
                                    g.doAutoSelect();
                                    return true
                                },
                                listeners: {
                                    specialkey: function(f, g) {
                                        if (g.getKey() == g.ENTER && !f.isExpanded) {
                                            e.searchHandler();
                                            g.stopEvent()
                                        }
                                    },
                                    select: function() {
                                        e.searchHandler()
                                    }
                                }
                            },
                            {
                                iconCls: "seek_icon",
                                tooltip: Str.search,
                                handler: function() {
                                    e.searchHandler()
                                }
                            }]
                        })[0]
                    }
                    if (e.isVisible()) {
                        e.getComponent("combo").focus(false, true)
                    }
                }
            })
        }
        return b
    },
    save: function(a) {
        var c = a.store,
        d = a.getValue(),
        b;
        b = c.findExact("field1", d);
        if (b != -1) {
            c.removeAt(b)
        }
        c.insert(0, {
            field1: d
        })
    },
    reverse: function(a) {
        Ext.Array.sort(a,
        function(d, c) {
            return c.getDepth() - d.getDepth()
        });
        return a
    },
    selFirst: function(c, d) {
        var b, a;
        if (c instanceof Ext.tree.Panel || c instanceof Ext.data.TreeStore) {
            b = c.bindTable || c;
            a = b.getRootNode();
            if (!b.rootVisible) {
                a = a.firstChild
            }
            if (a && !b.selFirstDone) {
                b.selFirstDone = true;
                if (d) {
                    b.selModel.preventFocus = true
                }
                b.setSelection(a);
                if (d) {
                    b.selModel.preventFocus = false
                }
            }
        } else {
            if (c instanceof Ext.grid.Panel || c instanceof Ext.data.Store || c instanceof Ext.view.View) {
                b = c.bindTable || c;
                if (b.store && b.store.getCount() > 0) {
                    if (d) {
                        b.selModel.preventFocus = true
                    }
                    b.setSelection(0);
                    if (d) {
                        b.selModel.preventFocus = false
                    }
                }
            }
        }
    },
    moveTo: function(c, d, b) {
        var a;
        if (b) {
            a = c.store.getRange();
            c.store.remove(a)
        } else {
            a = c.getSelection();
            Wb.remove(c)
        }
        d.store.add(a);
        d.setSelection(a)
    },
    removeExists: function(c, f, e) {
        var d, a = c.store,
        b = Wb.pluck(f.store.getRange(), e);
        d = Wb.toObject(b);
        a.each(function(g) {
            if (d[g.data[e]]) {
                b.push(g)
            }
        });
        a.remove(b)
    },
    getInfo: function(b, c) {
        var a, d;
        if (Ext.isArray(b)) {
            a = b
        } else {
            a = b.getSelection()
        }
        if (a.length) {
            if (a[0] instanceof Ext.data.Model) {
                d = a[0].get(c || "text")
            } else {
                d = c || a[0]
            }
            if (a.length == 1) {
                return d
            } else {
                return Wb.format(Str.itemsInfo, d, a.length)
            }
        } else {
            return ""
        }
    },
    getSection: function(a, c, d) {
        var b = 0,
        e = 0;
        for (b = 0; b < d; b++) {
            e = a.indexOf(c, e);
            if (e == -1) {
                return ""
            }
            e++
        }
        return a.substring(e)
    },
    download: function(c, f, a, g) {
        if (c instanceof Ext.chart.Chart) {
            var d = c;
            Wb.download("get-file", {
                data: Ext.draw.engine.SvgExporter.generate(d.surface),
                filename: f || "chart.svg"
            },
            true);
            return
        }
        var b = Wb.getFrame(),
        e = Wb.getForm(Ext.apply({
            _jsonresp: 1
        },
        f), a);
        e.action = c;
        e.method = g || "POST";
        e.target = b.id;
        e.submit()
    },
    exclude: function(b, c) {
        var a = {};
        if (!Ext.isArray(c)) {
            c = c.split(",")
        }
        c = Wb.toObject(c);
        Wb.each(b,
        function(e, d) {
            if (!c[e]) {
                a[e] = d
            }
        });
        return a
    },
    loadExcel: function(b) {
        var a = b.container,
        c = Wb.exclude(b, "container,file,params,success,failure,sheetIndex,align");
        if (b.fill) {
            b.fill = Wb.toArray(b.fill);
            Wb.each(b.fill,
            function(d) {
                if (d.mergeRows) {
                    d.mergeRows = Wb.toArray(d.mergeRows)
                }
                if (d.mergeCols && d.mergeCols.length > 0) {
                    if (!Ext.isArray(d.mergeCols[0])) {
                        d.mergeCols = [d.mergeCols]
                    }
                } else {
                    d.mergeCols = null
                }
            })
        }
        Wb.request(Wb.applyIf({
            url: "get-excel-form",
            params: Wb.applyIf({
                __file: b.file,
                __sheetIndex: b.sheetIndex,
                __align: b.align,
                __fill: b.fill
            },
            b.params),
            success: function(d) {
                if (a.autoScroll === undefined) {
                    a.setAutoScroll(true)
                }
                a.update(d.responseText);
                a.updateObject();
                Ext.callback(b.success, a, [d])
            },
            failure: function(d) {
                Ext.callback(b.failure, a, [d])
            }
        },
        c))
    },
    getExcel: function(c, b, a, d) {
        Wb.download("use-excel-tpl", {
            filename: b,
            exportFilename: d,
            params: c,
            sheetIndex: a
        })
    },
    fitTableLayout: function(a) {
        var b;
        Ext.suspendLayouts();
        a.items.each(function(c) {
            if (c instanceof Ext.container.AbstractContainer) {
                b = c.el.parent();
                c.setWidth(b.getWidth() - b.getPadding("lr") - c.el.getMargin("lr"))
            }
        });
        Ext.resumeLayouts();
        a.updateLayout()
    },
    toArray: function(a) {
        if (!Wb.isValue(a) || Ext.isArray(a)) {
            return a
        } else {
            return [a]
        }
    },
    getTopColumns: function(b) {
        var c = [],
        a = Wb.fetchColumns(b);
        Wb.each(a,
        function(d) {
            while (d.ownerCt instanceof Ext.grid.column.Column) {
                d = d.ownerCt
            }
            if (Wb.indexOf(c, d) == -1) {
                c.push(d)
            }
        });
        return c
    },
    mergeRows: function(a) {
        var p, o = a.view.el.down("table", true),
        l = Wb.fetchColumns(a),
        e,
        d = l ? l.length: 0,
        m,
        k = o.rows.length,
        n,
        b,
        f,
        g,
        h,
        c;
        for (e = d - 1; e > -1; e--) {
            if (!l[e].mergeRows) {
                continue
            }
            g = 0;
            h = null;
            for (m = k - 1; m > -1; m--) {
                n = o.rows[m].cells[e];
                b = n.firstChild;
                f = b.innerHTML;
                if (h) {
                    if (f === c) {
                        h.parentNode.removeChild(h);
                        if (m === 0) {
                            if (g > 0) {
                                n.rowSpan = (g + 1).toString()
                            }
                        }
                    } else {
                        if (g > 1) {
                            h.rowSpan = g.toString()
                        }
                        g = 0
                    }
                }
                g++;
                h = n;
                c = f
            }
        }
    },
    mergeCols: function(a) {
        var s, q = a.view.el.down("table", true),
        m = Wb.fetchColumns(a),
        e,
        d = m ? m.length: 0,
        n,
        l = q.rows.length,
        p,
        b,
        r,
        f,
        g,
        o,
        h,
        k,
        c;
        for (n = l - 1; n > -1; n--) {
            g = 0;
            r = q.rows[n];
            k = null;
            for (e = d - 1; e > -1; e--) {
                p = r.cells[e];
                if (!p) {
                    continue
                }
                b = p.firstChild;
                f = b.innerHTML;
                o = m[e].mergeColGroup;
                if (k) {
                    if (f === c && o && o === h) {
                        k.parentNode.removeChild(k);
                        if (e === 0) {
                            if (g > 0) {
                                p.colSpan = (g + 1).toString()
                            }
                        }
                    } else {
                        if (h && g > 1) {
                            k.colSpan = g.toString()
                        }
                        g = 0
                    }
                }
                g++;
                k = p;
                c = f;
                h = o
            }
        }
    },
    getHeaders: function(a) {
        function b(c, d) {
            Wb.each(c,
            function(e) {
                if (e.isCheckerHd) {
                    return
                }
                var f = {
                    hidden: e.hidden,
                    dataIndex: e.dataIndex,
                    text: e.text,
                    xtype: e.xtype
                };
                if (Wb.isValue(e.width)) {
                    f.width = e.width
                }
                if (e.xtype != "actioncolumn" && e.items && e.items.length) {
                    f.items = [];
                    b(e.items.items, f.items)
                }
                d.push(f)
            });
            return d
        }
        return b(Wb.getTopColumns(a), [])
    },
    exportData: function(l, k, m) {
        function h(v) {
            if (!v) {
                return null
            }
            var A = v.el.down("table", true);
            if (!A) {
                return null
            }
            var C = A.rows,
            B, z, t, r, w, u = C.length,
            q, s = [];
            for (w = 0; w < u; w++) {
                row = C[w];
                B = row.cells;
                r = B.length;
                q = [];
                for (t = 0; t < r; t++) {
                    z = B[t];
                    q.push({
                        value: z.innerHTML,
                        colSpan: z.colSpan,
                        align: z.align,
                        size: parseInt(z.style.fontSize, 10),
                        height: parseInt(z.style.height, 10),
                        weight: z.style.fontWeight
                    })
                }
                s.push({
                    height: Ext.fly(row).getHeight(),
                    items: q
                })
            }
            return s
        }
        function p(q, r) {
            Wb.each(q,
            function(s) {
                if (s.xtype == "rownumberer" || s instanceof Ext.grid.column.RowNumberer) {
                    j = (s.getWidth ? s.getWidth() : s.width) || s.width;
                    f = s.text;
                    return
                }
                if (s.isCheckerHd) {
                    return
                }
                var t;
                if (Ext.isTouch) {
                    if (!Wb.getBool(s._allowExport, !s._hidden)) {
                        return
                    }
                    t = {
                        text: s._exportText || s._text,
                        titleAlign: s._align
                    };
                    if (s.items && s.items.length) {
                        t.items = [];
                        p(s.items.items, t.items)
                    } else {
                        t.flex = s._flex;
                        t.align = s._align;
                        t.keyName = s._keyName;
                        t.autoWrap = s._autoWrap;
                        t.type = o[s._dataIndex];
                        t.format = s._format;
                        t.field = s._dataIndex;
                        t.width = s.innerElement.getWidth() || s._width
                    }
                } else {
                    if (!Wb.getBool(s.allowExport, !s.hidden)) {
                        return
                    }
                    t = {
                        text: s.exportText || s.text,
                        titleAlign: s.titleAlign || s.align
                    };
                    if (s.items && s.items.length) {
                        t.items = [];
                        p(s.items.items, t.items)
                    } else {
                        t.flex = s.flex;
                        t.align = s.align;
                        t.keyName = s.keyName;
                        t.autoWrap = s.autoWrap;
                        t.type = o[s.dataIndex];
                        t.format = s.format;
                        t.field = s.dataIndex;
                        t.width = (s.getWidth ? s.getWidth() : s.width) || s.width;
                        d.push([ !! s.mergeRows, s.mergeColGroup])
                    }
                }
                r.push(t)
            });
            return r
        }
        var g, c, e, b, j, f, o, a, n, d, i;
        if (!Ext.isTouch && l instanceof Ext.toolbar.Paging) {
            if (l.ownerCt instanceof Ext.grid.Panel) {
                a = l.ownerCt
            } else {
                a = l.ownerCt.items.items[0]
            }
        } else {
            a = l.bindTable || l
        }
        o = {};
        n = a.store;
        d = [];
        i = Ext.isTouch ? n._proxy._reader.getFields() : n.proxy.reader.getFields();
        Wb.each(i,
        function(q) {
            if (Ext.isTouch) {
                o[q._name] = q._type ? q._type.type: ""
            } else {
                o[q.name] = q.type ? q.type.type: ""
            }
        });
        c = (Ext.isTouch ? n._proxy._url: n.url) || "";
        b = c.indexOf("?");
        if (b != -1) {
            e = c.substring(b);
            c = c.substring(0, b)
        } else {
            e = ""
        }
        if (n.getTotalCount() <= n.pageSize) {
            m = false
        }
        g = Wb.applyIf({
            __metaParams: {
                data: m ? (c ? null: n.proxy.data) : Wb.getData(a),
                headers: p(Wb.getTopColumns(a), []),
                url: c,
                type: k || "excel",
                reportInfo: {
                    mergeInfo: d,
                    mergeRows: !!a.mergeRows,
                    mergeCols: !!a.mergeCols,
                    topHtml: a.down ? h(a.down("#topHtml")) : "",
                    bottomHtml: a.down ? h(a.down("#bottomHtml")) : ""
                },
                dateFormat: Ext.isTouch ? XTL.defaultDateFormat: Ext.form.field.Date.prototype.format,
                timeFormat: Ext.isTouch ? XTL.defaultTimeFormat: Ext.form.field.Time.prototype.format,
                rowNumberWidth: j || -1,
                rowNumberTitle: f,
                thousandSeparator: Ext.util.Format.thousandSeparator,
                decimalSeparator: Ext.util.Format.decimalSeparator,
                title: a.exportTitle,
                neptune: Wb.isNeptune,
                filename: a.exportFilename || a.exportTitle
            },
            page: 1,
            start: 0,
            limit: Wb.maxInt
        },
        n.lastOptions ? n.lastOptions.params: null);
        if (k == "html") {
            Wb.submit("transfer" + e, g)
        } else {
            Wb.download("transfer" + e, g)
        }
    },
    update: function(a, c, d) {
        var b;
        a.fields.each(function(e) {
            b = e.name || e._name;
            if (c.hasOwnProperty(b)) {
                a.set(b, c[b])
            }
        });
        if (Wb.getBool(d, true)) {
            a.commit()
        }
    },
    set: function(a, c) {
        var b;
        a.fields.each(function(d) {
            b = d.name || d._name;
            if (c.hasOwnProperty(b)) {
                a.data[b] = c[b]
            }
        })
    },
    getPressed: function(a, c) {
        var b = null;
        a.items.each(function(d) {
            if (d.toggleGroup == c && d.pressed) {
                b = d;
                return false
            }
        });
        return b
    },
    add: function(e, c, g, m, f) {
        var i, d, a = e.bindTable || e,
        b = Ext.isTouch ? null: Wb.findEditing(a),
        l = a.store;
        if (b) {
            b.completeEdit()
        }
        if (!c) {
            c = {}
        }
        if (Ext.isNumber(g)) {
            i = g;
            d = l.insert(i, c)
        } else {
            i = l.indexOf(a.getSelection()[0]);
            if (g) {
                if (i == -1) {
                    if (g == "before") {
                        g = "first"
                    } else {
                        if (g == "after") {
                            g = "last"
                        }
                    }
                }
            } else {
                g = "last"
            }
            switch (g) {
            case "first":
                i = 0;
                d = l.insert(i, c);
                break;
            case "before":
                d = l.insert(i, c);
                break;
            case "after":
                i++;
                d = l.insert(i, c);
                break;
            case "last":
                i = l.getCount();
                d = l.insert(i, c);
                break;
            case "add":
                d = l.add(c);
                i = l.indexOf(d[0]);
                break
            }
        }
        if (!Ext.isTouch && a instanceof Ext.grid.Panel && i < l.getCount() - 1) {
            Wb.refreshRowNum(a)
        }
        f = Wb.getBool(f, true);
        Wb.each(d,
        function(n) {
            if (f) {
                n.commit()
            } else {
                n.dirty = true
            }
        });
        a.setSelection(d);
        if (b && m !== undefined) {
            if (m == -1) {
                var h, k = 0,
                j = Wb.fetchColumns(a);
                m = 0;
                Wb.each(j,
                function(n) {
                    h = n.getEditor();
                    if (h && !h.readOnly && !n.hidden) {
                        m = k;
                        return false
                    }
                    k++
                })
            }
            b.startEdit(d[0], m)
        }
        return d
    },
    addEdit: function(b, a) {
        return Wb.add(b, a, "last", -1, false)
    },
    refreshRowNum: function(c) {
        var e = c.bindTable || c;
        if (!Wb.hasRowNumber(e)) {
            return
        }
        var d, b = 1,
        a = e.store,
        f = e.view.el.query("div[class~=x-grid-cell-inner-row-numberer]"),
        g = (a.currentPage - 1) * a.pageSize;
        Wb.each(f,
        function(h) {
            d = b + g;
            if (d !== h.innerHTML) {
                h.innerHTML = d
            }
            b++
        })
    },
    insert: function(a, b) {
        return Wb.edit(a, b, true)
    },
    edit: function(c, f, d) {
        var e, i, h, b, g, a = c.bindTable || c;
        if (!d) {
            b = a.getSelection()[0];
            if (!b) {
                Wb.warn(Wb.format(Str.selectRecord, Str.modify));
                return
            }
        }
        if (!f) {
            f = {}
        }
        if (f.title === undefined) {
            f.title = d ? Str.add: Str.modify
        }
        if (f.iconCls === undefined) {
            f.iconCls = d ? "record_add_icon": "record_edit_icon"
        }
        i = function(k, m) {
            if (f.beforerequest && Ext.callback(f.beforerequest, m.appScope, [k, m]) === false) {
                return
            }
            var l = Wb.apply(d ? {}: Wb.getData(b, true), f.params, k),
            j;
            if (g) {
                Wb.clearFileItem(l, m)
            }
            j = {
                url: f.url,
                params: l,
                showError: false,
                failure: function(p, o, n) {
                    Wb.except(g ? o: p,
                    function() {
                        if (Wb.getBool(f.autoFocus, true) && p.responseText) {
                            var r, t, q = p.responseText.indexOf(" ");
                            if (q > 0 && q < 100) {
                                r = p.responseText.substring(0, q);
                                try {
                                    t = m.down("field[fieldLabel=* " + r + "]");
                                    if (!t) {
                                        t = m.down("field[fieldLabel=" + r + "]")
                                    }
                                    if (t && t.focus) {
                                        t.focus(true, true)
                                    }
                                } catch(s) {}
                            }
                        }
                    });
                    if (f.failure) {
                        Ext.callback(f.failure, m.appScope, [k, m, g ? n: p.responseText])
                    }
                },
                success: function(r, n, p) {
                    var q, o = g ? p: r.responseText;
                    if (Ext.String.startsWith(o, "{") && Ext.String.endsWith(o, "}")) {
                        q = Wb.decode(o)
                    } else {
                        q = {}
                    }
                    if (d) {
                        Wb.add(a, Wb.applyIf(q, l), f.addPosition)
                    } else {
                        Wb.update(b, Wb.applyIf(q, l))
                    }
                    if (Ext.isTouch) {
                        m.destroy()
                    } else {
                        m.close()
                    }
                    Ext.callback(f.success, m.appScope, [k, m, o])
                }
            };
            if (g) {
                j.form = Ext.isTouch ? m: m.getComponent("form");
                Wb.upload(j)
            } else {
                Wb.request(j)
            }
        };
        if (f.win) {
            e = f.win;
            if (! (f.win instanceof Ext.window.Window)) {
                e.closeAction = "destroy";
                e = new Ext.window.Window(e)
            }
            g = !!e.down("filefield");
            e.editHandler = function() {
                var j = this;
                i(Wb.getValue(j), j)
            };
            e.isNew = d;
            e.editData = Wb.apply({},
            f.params, d ? null: b.data);
            e.setTitle(f.title);
            e.setIconCls(f.iconCls);
            e.show();
            if (!d) {
                if (f.titleField) {
                    Wb.setTitle(e, b.data[f.titleField])
                }
                Wb.setValue(e, Wb.apply({},
                f.params, b.data))
            }
        } else {
            h = [];
            if (f.firstItems) {
                Wb.each(f.firstItems,
                function(j) {
                    h.push(j)
                })
            }
            Wb.each(Wb.fetchColumns(a) || Wb.getColumns(a),
            function(k) {
                var j, l = k.dataIndex;
                if (k.editor) {
                    j = Wb.apply({
                        itemId: l
                    },
                    k.editor)
                } else {
                    if (k.blobEditor) {
                        g = true;
                        if (d || Ext.isTouch) {
                            j = Wb.apply({
                                itemId: l
                            },
                            k.blobEditor)
                        } else {
                            j = {
                                xtype: "fieldcontainer",
                                layout: "hbox",
                                colspan: k.blobEditor.colspan,
                                rowspan: k.blobEditor.rowspan,
                                allowBlank: k.blobEditor.allowBlank,
                                items: [Wb.apply({
                                    flex: 1,
                                    itemId: l
                                },
                                k.blobEditor), {
                                    xtype: "button",
                                    text: Str.del1,
                                    margin: "0 2 0 2",
                                    bindFieldName: l,
                                    handler: function(n) {
                                        var m = n.ownerCt.getComponent(n.bindFieldName);
                                        m.reset();
                                        m.removeFile = true
                                    }
                                }]
                            }
                        }
                    }
                }
                if (j) {
                    if (Ext.isTouch) {
                        j.label = k.text
                    } else {
                        j.fieldLabel = k.text
                    }
                    h.push(j)
                }
            });
            if (f.lastItems) {
                Wb.each(f.lastItems,
                function(j) {
                    h.push(j)
                })
            }
            e = Wb.prompt(Wb.apply({
                items: h,
                isUpload: g,
                handler: i
            },
            f));
            if (!d) {
                if (f.titleField) {
                    Wb.setTitle(e, b.data[f.titleField])
                }
                Wb.setValue(e, Wb.apply({},
                f.params, b.data))
            }
        }
        return e
    },
    del: function(b, d) {
        var c = b.bindTable || b,
        a = c.getSelection();
        Wb.confirmDo(a,
        function() {
            Wb.request({
                url: d.url,
                failure: d.failure,
                params: Wb.apply({
                    destroy: Wb.getData(a, true)
                },
                d.params),
                success: function() {
                    Wb.remove(c, a);
                    Ext.callback(d.success, c, [c])
                }
            })
        },
        d.titleField)
    },
    getData: function(c, d, f) {
        var g, a, e = [],
        b = Ext.isTouch ? Ext.grid.Grid: Ext.grid.Panel;
        if (c instanceof b) {
            c = c.store.getRange();
            a = true
        } else {
            if (c instanceof Ext.data.Store) {
                c = c.getRange();
                a = true
            } else {
                a = Ext.isArray(c);
                if (!a) {
                    c = [c]
                }
            }
        }
        Wb.each(c,
        function(h) {
            g = Ext.apply({},
            h.data);
            if (d) {
                Wb.each(h.data,
                function(i, j) {
                    g["#" + i] = j
                });
                Wb.each(h.modified,
                function(i, j) {
                    g["#" + i] = j
                })
            }
            if (f) {
                e.push(Ext.copyTo({},
                h.data, f))
            } else {
                e.push(g)
            }
        });
        return a ? e: e[0]
    },
    getModified: function(b) {
        var a = b.store || b;
        return {
            destroy: Wb.getData(a.getRemovedRecords(), true),
            update: Wb.getData(a.getUpdatedRecords(), true),
            create: Wb.getData(a.getNewRecords())
        }
    },
    sync: function(e) {
        var a, d, c, b = e.store || e.grid.store;
        if (b.bindTable) {
            a = Wb.findEditing(b.bindTable);
            if (a) {
                a.completeEdit()
            }
        }
        e = Ext.apply({},
        e);
        if (!e.params) {
            e.params = {}
        }
        Ext.apply(e.params, Wb.getModified(b));
        if (e.store) {
            delete e.store
        }
        if (e.grid) {
            delete e.grid
        }
        if (b.bindTable && e.autoUpdate) {
            d = function(f) {
                f = Wb.decode(f);
                if (f.create) {
                    Wb.syncCreate(b.bindTable, f.create, !f.update)
                }
                if (f.update) {
                    Wb.syncUpdate(b.bindTable, f.update, true)
                }
            };
            if (e.success) {
                c = e.success;
                e.success = function(f) {
                    d(f.responseText);
                    Ext.callback(c, this, arguments)
                }
            } else {
                e.success = d
            }
        }
        Wb.request(e)
    },
    doSync: function(e, c, a, g) {
        var d = 0,
        b = e.store || e,
        f = a ? b.getNewRecords() : b.getUpdatedRecords();
        if (!Wb.isEmpty(c)) {
            Wb.each(f,
            function(h) {
                Wb.update(h, c[d++], false)
            })
        }
        if (Wb.getBool(g, true)) {
            b.commitChanges()
        }
    },
    syncCreate: function(b, a, c) {
        Wb.doSync(b, a, true, c)
    },
    syncUpdate: function(b, a, c) {
        Wb.doSync(b, a, false, c)
    },
    reject: function(b) {
        var c = b.bindTable || b,
        a = Wb.findEditing(c);
        if (a) {
            a.cancelEdit()
        }
        c.store.rejectChanges();
        Wb.refreshRowNum(c)
    },
    hasRowNumber: function(b) {
        var a = false,
        c = b.bindTable || b,
        d = Wb.fetchColumns(c);
        if (!d) {
            return false
        }
        Wb.each(d,
        function(e) {
            if (e.xtype == "rownumberer") {
                a = true;
                return false
            }
        });
        return a
    },
    progress: function(c, a, b) {
        if (c > 1) {
            c = 1
        } else {
            if (c < 0) {
                c = 0
            }
        }
        if (c === 0) {
            Ext.MessageBox.show({
                msg: a || Str.processing,
                progressText: "0%",
                width: 300,
                closable: false,
                progress: true,
                animateTarget: b
            })
        } else {
            Ext.MessageBox.updateProgress(c, Math.round(100 * c) + "%")
        }
    },
    upload: function(c) {
        var b, d = (new Date()).getTime(),
        a = c.form;
        b = Ext.apply({
            isUpload: true
        },
        {
            showError: Wb.getBool(c.showError, true),
            showMask: Wb.getBool(c.showMask, true),
            params: Ext.apply(Wb.getValue(c.out), c.params, Wb.getValue(a)),
            progressId: d,
            url: c.url + (c.url.indexOf("?") != -1 ? "&": "?") + "_jsonresp=1&uploadId=" + d
        },
        c);
        if (Ext.isTouch) {
            b.formPanel = a;
            b.form = a.element;
            Ext.Ajax.request(b)
        } else {
            delete b.form;
            a.form.submit(b)
        }
    },
    getFrame: function() {
        if (!Wb.iframe) {
            var a = document.createElement("iframe"),
            b = "ifm" + Wb.getId();
            Ext.fly(a).set({
                id: b,
                name: b,
                cls: Ext.baseCSSPrefix + "hide-display",
                src: Ext.SSL_SECURE_URL
            });
            document.body.appendChild(a);
            if (document.frames) {
                document.frames[b].name = b
            }
            Ext.fly(a).on("load",
            function() {
                var d, f, c, h;
                try {
                    h = Wb.getDoc(Wb.iframe);
                    if (h) {
                        if (h.body) {
                            if ((c = h.body.firstChild) && /pre/i.test(c.tagName)) {
                                d = c.textContent || c.innerText
                            } else {
                                if ((c = h.getElementsByTagName("textarea")[0])) {
                                    d = c.value
                                } else {
                                    d = h.body.textContent || h.body.innerText
                                }
                            }
                            if (d) {
                                d = Wb.decode(d);
                                if (!d.success) {
                                    f = d.value;
                                    if (Ext.String.startsWith(f, "$WBE201")) {
                                        Wb.login()
                                    } else {
                                        Wb.error(f)
                                    }
                                }
                            }
                        }
                    } else {
                        Wb.error(Str.serverNotResp)
                    }
                } catch(g) {
                    if (d) {
                        Wb.error(d)
                    } else {
                        Wb.error(Str.serverNotResp)
                    }
                }
            });
            Wb.iframe = a
        }
        return Wb.iframe
    },
    getForm: function(d, a) {
        var b, c = Wb.defaultForm;
        if (c) {
            while (c.childNodes.length !== 0) {
                c.removeChild(c.childNodes[0])
            }
        } else {
            c = document.createElement("FORM");
            Wb.defaultForm = c;
            document.body.appendChild(c)
        }
        if (d) {
            Wb.each(d,
            function(e, f) {
                b = document.createElement("input");
                b.setAttribute("name", e);
                b.setAttribute("type", "hidden");
                if (Ext.isArray(f) || Ext.isObject(f)) {
                    f = Wb.encode(f)
                } else {
                    if (Ext.isDate(f)) {
                        f = Wb.dateToStr(f)
                    }
                }
                b.setAttribute("value", Wb.isEmpty(f) ? "": f);
                c.appendChild(b)
            })
        }
        if (a) {
            c.encoding = "multipart/form-data"
        } else {
            c.encoding = "application/x-www-form-urlencoded"
        }
        return c
    },
    submit: function(b, e, d, f, a) {
        var c = Wb.getForm(e, a);
        c.action = b;
        c.method = f || "POST";
        c.target = d || "_blank";
        c.submit()
    },
    toLine: function(c, a) {
        var b;
        if (c) {
            b = c.replace(/\r?\n/g, " ")
        } else {
            return ""
        }
        if (a) {
            return Ext.String.ellipsis(b, a)
        } else {
            return b
        }
    },
    clearToolbar: function(c) {
        var b, d, a;
        Ext.suspendLayouts();
        Wb.each(c.items.items,
        function(e) {
            if (e.hidden) {
                return
            }
            d = e instanceof Ext.toolbar.Separator;
            if (d && a) {
                e.destroy()
            } else {
                b = e
            }
            a = d
        },
        true);
        if (b instanceof Ext.toolbar.Separator) {
            b.destroy()
        }
        Ext.resumeLayouts(true)
    },
    numValidator: function(a, b) {
        return function(d) {
            var f = this.getValue();
            if (Wb.isEmpty(f)) {
                return true
            }
            var c, e = String(f);
            if (Ext.String.startsWith(e, "-")) {
                e = e.substring(1)
            }
            if (e.indexOf(".") != -1) {
                c = e.split(".")
            } else {
                if (e.indexOf(",") != -1) {
                    c = e.split(",")
                } else {
                    c = [e, ""]
                }
            }
            if (c[0].length + c[1].length > a || c[1].length > b) {
                return Wb.format(Str.invalidValue, d)
            } else {
                return true
            }
        }
    },
    clearFileItem: function(b, a) {
        Wb.each(a.query("filefield"),
        function(c) {
            if (c.itemId) {
                delete b[c.itemId]
            }
        });
        return b
    },
    strToDate: function(a) {
        if (!a) {
            return undefined
        }
        if (a.indexOf(".") == -1) {
            return Ext.Date.parse(a, "Y-m-d H:i:s")
        } else {
            return Ext.Date.parse(a, Wb.dateFormat)
        }
    },
    dateToStr: function(a) {
        return Wb.format(a, Wb.dateFormat)
    },
    dateToText: function(b, c) {
        var d, a, e;
        if (Ext.isTouch) {
            a = XTL.defaultDateFormat;
            e = XTL.defaultTimeFormat
        } else {
            a = Ext.form.field.Date.prototype.format;
            e = Ext.form.field.Time.prototype.format
        }
        if (!b) {
            return ""
        }
        if (Ext.isString(b)) {
            b = Wb.strToDate(b)
        }
        if (c === true) {
            d = e
        } else {
            if (c === false) {
                d = a
            } else {
                if (c === null) {
                    d = a + " " + e
                } else {
                    if (Wb.format(b, "Hisu") === "000000000") {
                        d = a
                    } else {
                        d = a + " " + e
                    }
                }
            }
        }
        return Wb.format(b, d)
    },
    kv: function(c, a) {
        var b = Wb.find(a, "K", c);
        return b ? b.V: c
    },
    kvRenderer: window.Ext && Ext.isTouch ?
    function(c, b) {
        var d = this.up("grid").getColumns(),
        a = d.indexOf(this);
        return Wb.kv(c, d[a].initialConfig.keyItems)
    }: function(f, a, e, d, b) {
        var g, c = this;
        if (c instanceof Ext.grid.column.Column) {
            c = c.up("grid")
        }
        g = Wb.fetchColumns(c);
        return Wb.kv(f, g[b].keyItems)
    },
    format: function(a) {
        if (Ext.isDate(a)) {
            return Ext.Date.format.apply(this, arguments)
        } else {
            if (Ext.isNumber(a)) {
                return Ext.util.Format.number.apply(this, arguments)
            } else {
                return Ext.String.format.apply(this, arguments)
            }
        }
    },
    getTag: function(b, c) {
        var a = b.store || b;
        return a.proxy.reader.rawData[c]
    },
    getColumns: function(b) {
        if (!b) {
            return null
        }
        var a, c;
        if (Ext.isTouch) {
            a = b._store || b;
            return a ? a._proxy._reader.rawData.columns: null
        } else {
            a = b.store || b;
            c = a.bindTable;
            if (c && c.syncHeadersData) {
                return Wb.applyHeader(a.proxy.reader.rawData.columns, c.syncHeadersData)
            } else {
                return Wb.hasNS("proxy.reader.rawData.columns", a) ? a.proxy.reader.rawData.columns: null
            }
        }
    },
    fetchColumns: function(a) {
        if (Ext.isTouch) {
            if (a instanceof Ext.grid.Grid) {
                return a.getColumns()
            } else {
                return null
            }
        } else {
            return a.headerCt ? a.headerCt.getGridColumns() : a.columns
        }
    },
    getColumn: function(a, d) {
        var c, b = a.bindTable || a;
        if (Wb.isEmpty(b.columns)) {
            c = Wb.getColumns(b)
        } else {
            c = b.columns
        }
        if (Ext.isString(d)) {
            return Wb.find(c, "dataIndex", d)
        } else {
            return c[d] || null
        }
    },
    loadColumns: function(a, c) {
        var d, b = a.bindTable || a;
        if (Ext.isTouch) {
            if (c || Wb.isEmpty(b.getColumns())) {
                d = Wb.getColumns(b._store);
                if (d) {
                    Wb.each(d,
                    function(e) {
                        if (e.width) {
                            e.width = Math.round(e.width * 1.2)
                        } else {
                            if (e.flex) {
                                e.width = 200
                            }
                        }
                    });
                    b.updateColumns(d)
                }
            }
        } else {
            d = Wb.fetchColumns(b);
            if (c || Wb.isEmpty(d) || d.length == 1 && d[0].isCheckerHd) {
                d = Wb.getColumns(b.store);
                if (d) {
                    b.reconfigure(null, d)
                }
            }
        }
    },
    autoLoadColumns: function(a) {
        if (a) {
            var b, c;
            if (Ext.isTouch) {
                b = Wb.getBool(a.loadColumns, a.initialConfig.loadColumns);
                c = a.getColumns()
            } else {
                b = a.loadColumns;
                c = a.headerCt.getGridColumns()
            }
            if ((!b || b == "auto") && (Wb.isEmpty(c) || c.length == 1 && c[0].isCheckerHd)) {
                Wb.loadColumns(a)
            } else {
                if (b == "reload") {
                    Wb.loadColumns(a, true)
                }
            }
        }
    },
    applyHeader: function(d, a) {
        var b = Ext.clone(a);
        function c(j, i) {
            var h, f, g;
            Wb.each(j,
            function(k) {
                f = k.text;
                g = i.text;
                if (f == "&#160;") {
                    f = ""
                }
                if (g == "&#160;") {
                    g = ""
                }
                if ((!k.dataIndex && !i.dataIndex || k.dataIndex === i.dataIndex) && (!f && !g || f === g) && (!k.xtype || k.xtype === i.xtype)) {
                    h = k;
                    return false
                }
                if (k.items) {
                    h = c(k.items, i);
                    if (h) {
                        return false
                    }
                }
            });
            return h
        }
        function e(f, g) {
            Wb.each(g,
            function(i) {
                var j, h;
                h = c(d, i);
                j = i.items;
                Ext.applyIf(i, h);
                if (!j && i.items && i.xtype != "actioncolumn") {
                    delete i.items
                }
                if (i.items) {
                    e(f, i.items)
                }
            })
        }
        e(d, b);
        return b
    },
    timeRenderer: function(a) {
        return Wb.dateToText(a, true)
    },
    clobRenderer: function(e, a, g, f, d) {
        return Ext.htmlEncode(Ext.String.ellipsis(e, 50))
    },
    blobRenderer: function(j, i, g, h, k) {
        var d, l, a, e, f = [];
        if (Ext.isTouch) {
            a = this.up("grid");
            h = a.store.indexOf(i);
            l = Wb.encode(g)
        } else {
            a = this;
            d = Wb.getColumn(a, k);
            l = Wb.encode(d ? (d.dataIndex || "") : "")
        }
        e = a.id;
        if (a.uploadBlob && (!a.ifUploadBlob || a.ifUploadBlob(l) !== false)) {
            f.push("<a href='javascript:Wb.call(\"" + e + '","uploadBlob",' + l + "," + h + ")'>" + Str.upload + "</a>")
        }
        if (a.downloadBlob && j && (!a.ifDownloadBlob || a.ifDownloadBlob(l) !== false)) {
            f.push("<a href='javascript:Wb.call(\"" + e + '","downloadBlob",' + l + "," + h + ")'>" + Str.download + "</a>")
        }
        if (a.removeBlob && j && (!a.ifRemoveBlob || a.ifRemoveBlob(l) !== false)) {
            f.push("<a href='javascript:Wb.call(\"" + e + '","removeBlob",' + l + "," + h + ")'>" + Str.del1 + "</a>")
        }
        if (f.length) {
            return f.join("&nbsp;&nbsp;")
        } else {
            return j
        }
    },
    getIcon: function(a, b) {
        if (Wb.isEmpty(b)) {
            b = ""
        } else {
            b = " title=" + Wb.encode(b)
        }
        return '<span class="wb_icon ' + a + '"' + b + "></span>"
    },
    getUrlIcon: function(a, b) {
        if (Wb.isEmpty(b)) {
            b = ""
        } else {
            b = " title=" + Wb.encode(b)
        }
        return '<span style="background-image:url(' + a + ')" class="wb_icon"' + b + "></span>"
    },
    getGlyph: function(b, c, a) {
        if (Wb.isEmpty(c)) {
            c = ""
        } else {
            c = " title=" + Wb.encode(c)
        }
        if (Wb.isEmpty(a)) {
            a = ""
        } else {
            a = ' style="color:' + a + ';"'
        }
        return '<span class="wb_glyph wb_glyph_icon"' + c + a + ">&#x" + b + ";</span>"
    },
    getBracketText: function(b, a) {
        if (b) {
            var c = b.indexOf("(");
            if (a) {
                return b.substring(c + 1, b.indexOf(")"))
            } else {
                return c == -1 ? b.trim() : b.substring(0, c).trim()
            }
        } else {
            return ""
        }
    },
    cloneMenu: function(b, a) {
        if (Ext.isString(a)) {
            a = a.split(/[,;\s]/)
        }
        Wb.each(a,
        function(c) {
            b.add(c == "-" ? "-": Ext.copyTo({
                itemId: c + "-clone"
            },
            b.appScope[c], "text,iconCls,handler"))
        });
        if (!b.closeMenuEventBinded) {
            b.mon(b, "show",
            function() {
                var c, d;
                Wb.each(a,
                function(e) {
                    if (e == "-") {
                        return
                    }
                    c = b.getComponent(e + "-clone");
                    d = b.appScope[e];
                    c.setVisible(d.isVisible());
                    c.setDisabled(d.disabled)
                })
            });
            b.closeMenuEventBinded = true
        }
    },
    toBottom: function(a) {
        a.body.dom.scrollTop = a.body.dom.scrollHeight
    },
    call: function(c, b, d) {
        var a = Ext.getCmp(c);
        a[b].apply(a, [].slice.call(arguments, 2))
    },
    invoke: function(c) {
        var b, a = Ext.fly(c),
        d = a.getAttribute("method");
        while (a) {
            b = Ext.getCmp(a.id);
            if (b && b.appScope) {
                b.appScope[d].apply(c, [].slice.call(arguments, 1));
                break
            }
            a = a.parent()
        }
    },
    setBox: function(a, j) {
        var k = false,
        d = a.getLocalX(),
        i = a.getLocalY(),
        e = a.getWidth(),
        g = a.getHeight(),
        b = j.getLocalX(),
        h = j.getLocalY(),
        c = j.getWidth(),
        f = j.getHeight;
        if (d !== b) {
            j.setLocalX(d);
            k = true
        }
        if (i != h) {
            j.setLocalY(i);
            k = true
        }
        if (e != c) {
            j.setWidth(e);
            k = true
        }
        if (g != f) {
            j.setHeight(g);
            k = true
        }
        return k
    },
    find: function(c, b, d) {
        var a = null;
        if (Ext.isArray(c)) {
            if (c[0] instanceof Ext.data.Model) {
                Wb.each(c,
                function(e) {
                    if (e.data[b] === d) {
                        a = e;
                        return false
                    }
                })
            } else {
                Wb.each(c,
                function(e) {
                    if (e[b] === d) {
                        a = e;
                        return false
                    }
                })
            }
        } else {
            if (c) {
                c.each(function(e) {
                    if (e.data[b] === d) {
                        a = e;
                        return false
                    }
                })
            } else {
                return null
            }
        }
        return a
    },
    namePart: function(a) {
        if (Wb.isEmpty(a)) {
            return ""
        }
        var b = a.indexOf("=");
        if (b == -1) {
            return a
        } else {
            return a.substring(0, b)
        }
    },
    valuePart: function(a) {
        if (Wb.isEmpty(a)) {
            return ""
        }
        var b = a.indexOf("=");
        if (b == -1) {
            return ""
        } else {
            return a.substring(b + 1)
        }
    },
    getSelText: function() {
        if (window.getSelection) {
            return window.getSelection().toString()
        } else {
            if (document.selection && document.selection.createRange) {
                return document.selection.createRange().text
            } else {
                return ""
            }
        }
    },
    pluck: function(a, c) {
        var e, d, b = a.length,
        f = [];
        if (b > 0) {
            e = a[0].data
        }
        for (d = 0; d < b; d++) {
            if (e) {
                f.push(a[d].data[c])
            } else {
                f.push(a[d][c])
            }
        }
        return f
    },
    clearSelText: function() {
        if (document.selection) {
            document.selection.empty()
        } else {
            if (window.getSelection) {
                window.getSelection().removeAllRanges()
            }
        }
    },
    monEnter: function(a, b) {
        var c = a.getKeyMap();
        c.on(13,
        function(d, h) {
            var f = a,
            g = h.target;
            if (f.el.isMasked() || Ext.getBody().isMasked()) {
                return
            }
            if (g && g.type == "textarea") {
                return
            }
            h.stopEvent();
            b(a)
        })
    },
    isEditor: function(a) {
        return a.tagName == "INPUT" && (a.type == "text" || a.type == "password") || a.tagName == "TEXTAREA"
    },
    toObject: function(b) {
        var a = {};
        Wb.each(b,
        function(c) {
            a[c] = true
        });
        return a
    },
    setCookie: function(a, b) {
        Ext.util.Cookies.set(a, b, Ext.Date.add(new Date(), Ext.Date.MONTH, 1))
    },
    getCookie: function(a) {
        return Ext.util.Cookies.get(a)
    },
    setChecked: function(g, f, a, b) {
        var e, d, c = f === null;
        f = !!f;
        if (!b) {
            g.data.checked = f
        }
        d = Ext.fly(a.view.id + "-record" + g.id);
        if (!d) {
            return
        }
        e = b ? null: d.down("input[type=button]");
        if (c) {
            d.setStyle("text-decoration", "");
            d.setStyle("color", "");
            if (e) {
                e.removeCls("x-tree-checkbox-checked");
                e.addCls("x-tree-checkbox-halfchecked")
            }
        } else {
            if (f) {
                d.setStyle("text-decoration", "");
                d.setStyle("color", "blue");
                if (e) {
                    e.removeCls("x-tree-checkbox-halfchecked");
                    e.addCls("x-tree-checkbox-checked")
                }
            } else {
                d.setStyle("text-decoration", "line-through");
                d.setStyle("color", "gray");
                if (e) {
                    e.removeCls("x-tree-checkbox-halfchecked");
                    e.removeCls("x-tree-checkbox-checked")
                }
            }
        }
    },
    setNodeStatus: function(a, e, b) {
        var d, c;
        e.data.checked = b == 1;
        e.data.checkStatus = b;
        c = Ext.fly(a.view.id + "-record" + e.id);
        if (!c) {
            return
        }
        d = c.down("input[type=button]");
        if (b == 2) {
            if (d) {
                d.removeCls("x-tree-checkbox-checked");
                d.addCls("x-tree-checkbox-halfchecked")
            }
        } else {
            if (b == 1) {
                if (d) {
                    d.removeCls("x-tree-checkbox-halfchecked");
                    d.addCls("x-tree-checkbox-checked")
                }
            } else {
                if (d) {
                    d.removeCls("x-tree-checkbox-halfchecked");
                    d.removeCls("x-tree-checkbox-checked")
                }
            }
        }
    },
    sort: function(d, c, b) {
        var a = Wb.getBool(b, true);
        return d.sort(function(f, e) {
            var h, g;
            if (c) {
                h = f ? (f[c] || "") : "";
                g = e ? (e[c] || "") : ""
            } else {
                h = f || "";
                g = e || ""
            }
            if (Ext.isString(h)) {
                h = h.toUpperCase()
            }
            if (Ext.isString(g)) {
                g = g.toUpperCase()
            }
            if (a) {
                return h.localeCompare(g)
            } else {
                if (h > g) {
                    return 1
                } else {
                    if (h < g) {
                        return - 1
                    } else {
                        return 0
                    }
                }
            }
        })
    },
    editorChangeEvent: function() {
        var a = this;
        a.xInitChangeEvent.apply(a, arguments);
        a.xsetChangeEvent.apply(a, arguments)
    },
    bindChange: function(b, c) {
        if (b.bindTable) {
            b = b.bindTable
        }
        if (!b.changeEventBinded) {
            var a = b.store;
            Wb.each(Wb.fetchColumns(b),
            function(d) {
                if (d.editor) {
                    if (d.editor.listeners) {
                        if (d.editor.listeners.change) {
                            d.editor.xInitChangeEvent = d.editor.listeners.change;
                            d.editor.xsetChangeEvent = c;
                            d.editor.listeners.change = Wb.editorChangeEvent
                        } else {
                            d.editor.listeners.change = c
                        }
                    } else {
                        d.editor.listeners = {
                            change: c
                        }
                    }
                } else {
                    if (d instanceof Ext.grid.column.Check) {
                        d.mon(d, "checkchange", c)
                    }
                }
            });
            a.mon(a, "add", c);
            a.mon(a, "remove", c);
            b.changeEventBinded = true
        }
    },
    setDisabled: function(a, b) {
        a = Ext.Array.from(a);
        Ext.suspendLayouts();
        Wb.each(a,
        function(c) {
            if (c.setDisabled) {
                c.setDisabled(b)
            }
        });
        Ext.resumeLayouts(true)
    },
    optMain: function(f) {
        if (!f) {
            return null
        }
        var b, e, c, a = [],
        d = {
            container: 1,
            panel: 2,
            form: 3,
            tabpanel: 4,
            fieldset: 5
        };
        Wb.each(f,
        function(g, h) {
            if (Ext.isTouch) {
                e = h && !h.ownerCt && h instanceof Ext.Container
            } else {
                e = h && !h.ownerCt && h instanceof Ext.container.Container && !(h instanceof Ext.window.Window)
            }
            if (h && h.itemId == "main" && !h.ownerCt) {
                c = h;
                return false
            }
            if (e) {
                b = d[h.xtype];
                if (!b) {
                    b = 9
                }
                a.push([h, b])
            }
        });
        if (c) {
            return c
        }
        a.sort(function(h, g) {
            return h[1] - g[1]
        });
        return a.length ? a[0][0] : null
    },
    open: function(PortalConfigs) {
        PortalConfigs = Wb.apply({},
        PortalConfigs);
        if (PortalConfigs.topWin && !Ext.isTouch && window.top != window && window.top.Wb && window.top.sys && (window.top.sys.home || window.top.sys.ide)) {
            window.top.Wb.open(PortalConfigs);
            return
        }
        if (PortalConfigs.autoTab) {
            Wb.request({
                url: "get-app-info",
                params: {
                    url: PortalConfigs.url
                },
                success: function(resp) {
                    if (resp.responseText) {
                        PortalConfigs = Wb.apply({},
                        PortalConfigs, Wb.decode(resp.responseText));
                        delete PortalConfigs.autoTab
                    }
                    Wb.open(PortalConfigs)
                }
            });
            return
        }
        if (PortalConfigs.reloadCard) {
            PortalConfigs.url = Wb.toUrl(PortalConfigs.reloadCard.bindFile);
            PortalConfigs.reload = true
        }
        if (PortalConfigs.download) {
            Wb.download(PortalConfigs.url, PortalConfigs.params);
            return
        }
        var PortalVars = {};
        PortalVars.xwlCall = Ext.String.startsWith(PortalConfigs.url, "m?xwl=");
        if (PortalVars.xwlCall) {
            PortalVars.path = PortalConfigs.url.substring(6) + ".xwl"
        } else {
            PortalVars.path = PortalConfigs.url
        }
        PortalVars.hasHome = Wb.hasNS("sys.home");
        PortalVars.hasIde = Wb.hasNS("sys.ide");
        if (!PortalConfigs.newWin && (Wb.isValue(PortalConfigs.container) || PortalVars.hasHome || PortalVars.hasIde)) {
            if (PortalConfigs.container === false) {
                PortalVars.tab = null
            } else {
                if (PortalConfigs.container) {
                    PortalVars.tab = PortalConfigs.container
                } else {
                    if (PortalVars.hasIde) {
                        PortalVars.tab = Ide.fileTab
                    } else {
                        PortalVars.tab = sys.home.tab
                    }
                }
            }
            if (PortalVars.tab) {
                if (PortalConfigs.newTab === false || !PortalConfigs.newTab && !PortalConfigs.params) {
                    if (PortalConfigs.reloadCard) {
                        PortalVars.card = PortalConfigs.reloadCard
                    } else {
                        PortalVars.card = null;
                        PortalVars.tab.items.each(function(item) {
                            if (item.bindFile == PortalVars.path) {
                                PortalVars.card = item;
                                return false
                            }
                        })
                    }
                    if (PortalVars.card) {
                        if (PortalConfigs.reload) {
                            if (Wb.unloadEvents) {
                                delete Wb.unloadEvents[PortalVars.card.id]
                            }
                            Ext.applyIf(PortalConfigs, PortalVars.card.lastPortalConfigs);
                            Ext.Object.clear(PortalVars.card.lastPortalVars);
                            PortalVars.card.lastPortalVars = PortalVars;
                            Ext.Object.clear(PortalVars.card.lastPortalConfigs);
                            PortalVars.card.lastPortalConfigs = PortalConfigs;
                            PortalVars.card.removeAll(true);
                            PortalVars.card.fireEvent("destroy", PortalVars.card)
                        } else {
                            if (!PortalConfigs.notActiveCard) {
                                PortalVars.tab.setActiveTab(PortalVars.card)
                            }
                            return PortalVars.card
                        }
                    }
                }
                if (!PortalVars.card) {
                    if (Ext.isNumeric("0x" + PortalConfigs.iconCls)) {
                        PortalConfigs.glyph = parseInt("0x" + PortalConfigs.iconCls, 16);
                        delete PortalConfigs.iconCls
                    }
                    PortalVars.cardConfig = {
                        iconCls: PortalConfigs.iconCls,
                        glyph: PortalConfigs.glyph,
                        icon: PortalConfigs.icon,
                        title: Ext.String.ellipsis(PortalConfigs.title, 20),
                        closable: true,
                        lastPortalVars: PortalVars,
                        lastPortalConfigs: PortalConfigs,
                        hideMode: Ext.isIE ? "offsets": "display",
                        xtype: "panel",
                        hasParams: !!PortalConfigs.params,
                        bindFile: PortalVars.path,
                        border: false,
                        layout: "fit",
                        listeners: {
                            destroy: function(panel) {
                                Wb.destroy(panel.appScope, panel);
                                if (Wb.unloadEvents) {
                                    delete Wb.unloadEvents[panel.id]
                                }
                            },
                            beforeclose: function(panel) {
                                if (!panel.confirmDisabled && Wb.unloadEvents) {
                                    if (Wb.unloadEvents[panel.id] && panel.confirmClose !== false) {
                                        var result = Wb.unloadEvents[panel.id]();
                                        if (Wb.isValue(result)) {
                                            Wb.confirm(result + "<br><br>" + Str.confirmClose,
                                            function() {
                                                panel.confirmClose = false;
                                                panel.close()
                                            });
                                            return false
                                        }
                                    } else {
                                        delete Wb.unloadEvents[panel.id]
                                    }
                                }
                            }
                        }
                    };
                    if (PortalConfigs.tooltip || PortalVars.cardConfig.title !== PortalConfigs.title) {
                        PortalVars.cardConfig.tabConfig = {
                            tooltip: PortalConfigs.tooltip || PortalConfigs.title
                        }
                    }
                    PortalVars.card = PortalVars.tab.add(PortalVars.cardConfig)
                }
                if (!PortalConfigs.notActiveCard) {
                    PortalVars.tab.setActiveTab(PortalVars.card)
                }
            }
            if (PortalConfigs.frameOnly) {
                return PortalVars.card
            }
            if (PortalVars.tab && (PortalConfigs.inframe || PortalConfigs.inframe === undefined && PortalVars.card.iframe)) {
                if (!PortalVars.card.iframe) {
                    Wb.insertIframe(PortalVars.card, Wb.getBool(PortalConfigs.mask, false))
                }
                PortalVars.card.iframe.submit(PortalConfigs.url, PortalConfigs.params, PortalConfigs.method)
            } else {
                var doRequest = function() {
                    if (PortalVars.path && PortalVars.path.indexOf("?") == -1 && Ext.String.endsWith(PortalVars.path, ".xwl")) {
                        PortalVars.requestUrl = "m?xwl=" + PortalVars.path.slice(0, -4)
                    } else {
                        PortalVars.requestUrl = PortalVars.path
                    }
                    return Wb.request({
                        url: PortalVars.requestUrl + (PortalVars.requestUrl.indexOf("?") == -1 ? "?xwlt=1": "&xwlt=1"),
                        mask: PortalVars.card,
                        timeout: -1,
                        showError: false,
                        method: PortalConfigs.method || "POST",
                        showMask: PortalConfigs.mask,
                        params: PortalConfigs.params,
                        callback: function(o, success, response) {
                            if (PortalVars.card) {
                                delete PortalVars.card.request;
                                if (!success) {
                                    if (!PortalVars.card.notShowError && PortalConfigs.showError !== false) {
                                        Wb.except(response)
                                    }
                                    if (!PortalVars.card.isClosing) {
                                        PortalVars.card.close()
                                    }
                                }
                            } else {
                                if (!success && PortalConfigs.showError !== false) {
                                    Wb.except(response)
                                }
                            }
                            if (!success && PortalConfigs.failure) {
                                Ext.callback(PortalConfigs.failure, PortalVars.card, [{},
                                response.responseText])
                            }
                        },
                        success: function(PortalResp) {
                            if (PortalResp.responseText) {
                                PortalVars.doOpen = function() {
                                    if (PortalResp.responseText.substring(0, 10) == "(function(" && PortalResp.responseText.slice( - 3) == "();") {
                                        PortalVars.loc = window.location;
                                        PortalVars.appScope = eval(PortalResp.responseText.slice(0, -2) + "{}, PortalConfigs.contextOwner||PortalVars.card||null);\n//# sourceURL=" + PortalVars.loc.protocol + "//" + PortalVars.loc.host + PortalVars.loc.pathname.substring(0, PortalVars.loc.pathname.lastIndexOf("/")) + "/" + PortalVars.path);
                                        PortalVars.entry = Wb.optMain(PortalVars.appScope);
                                        if (PortalVars.card) {
                                            PortalVars.card.appScope = PortalVars.appScope
                                        }
                                        if (PortalVars.card && PortalVars.entry) {
                                            PortalVars.card.add(PortalVars.entry)
                                        }
                                        if (PortalConfigs.success) {
                                            Ext.callback(PortalConfigs.success, PortalVars.card, [PortalVars.appScope, PortalResp.responseText])
                                        }
                                    } else {
                                        PortalVars.card.add({
                                            xtype: "container",
                                            autoScroll: true,
                                            style: "font-size:13px;line-height:20px;",
                                            padding: 8,
                                            html: Wb.encodeHtml(PortalResp.responseText)
                                        });
                                        if (PortalConfigs.success) {
                                            Ext.callback(PortalConfigs.success, PortalVars.card, [{},
                                            PortalResp.responseText])
                                        }
                                    }
                                };
                                if (Ext.String.startsWith(PortalResp.responseText, "$$@blink")) {
                                    PortalVars.scriptLinkEnd = PortalResp.responseText.indexOf("$$@elink");
                                    PortalVars.scriptObject = Wb.decode(PortalResp.responseText.substring(8, PortalVars.scriptLinkEnd));
                                    PortalVars.linksArray = [];
                                    if (PortalVars.scriptObject.css) {
                                        Wb.each(PortalVars.scriptObject.css,
                                        function(item) {
                                            PortalVars.linksArray.push({
                                                url: item,
                                                type: "css"
                                            })
                                        })
                                    }
                                    if (PortalVars.scriptObject.js) {
                                        Wb.each(PortalVars.scriptObject.js,
                                        function(item) {
                                            PortalVars.linksArray.push({
                                                url: item,
                                                type: "js"
                                            })
                                        })
                                    }
                                    PortalResp.responseText = PortalResp.responseText.substring(PortalVars.scriptLinkEnd + 9);
                                    Wb.addLink(PortalVars.linksArray, PortalVars.doOpen, PortalVars.scriptObject.recursive)
                                } else {
                                    PortalVars.doOpen()
                                }
                            } else {
                                if (PortalConfigs.success) {
                                    Ext.callback(PortalConfigs.success, PortalVars.card, [{},
                                    ""])
                                }
                            }
                        }
                    })
                };
                if (PortalVars.tab) {
                    if (PortalVars.card.iframe) {
                        PortalVars.card.fireEvent("beforedestroy", PortalVars.card);
                        PortalVars.card.mun(PortalVars.card, "beforedestroy");
                        delete PortalVars.card.iframe
                    }
                    PortalVars.card.update("");
                    PortalVars.card.request = doRequest(PortalVars.card);
                    PortalVars.card.mon(PortalVars.card, "close",
                    function(me) {
                        me.isClosing = true;
                        if (me.request) {
                            var xhr = me.request.xhr;
                            if (xhr) {
                                me.notShowError = true;
                                xhr.abort()
                            }
                        }
                    })
                } else {
                    doRequest()
                }
            }
            if (PortalVars.tab) {
                return PortalVars.card
            }
        } else {
            if (Ext.util.Format.uppercase(PortalConfigs.method) == "GET" && !PortalConfigs.params) {
                window.open(PortalConfigs.url)
            } else {
                Wb.submit(PortalConfigs.url, PortalConfigs.params, null, PortalConfigs.method)
            }
        }
    },
    run: function(d) {
        var b = {
            container: false
        };
        if (d.single) {
            var a = Ext.isString(d.single) ? d.single: d.url,
            c = Wb.singleScopes[a];
            if (c) {
                Ext.callback(d.success, c, [c]);
                return
            } else {
                b.success = function(e) {
                    Wb.singleScopes[a] = e;
                    Ext.callback(d.success, e, [e])
                }
            }
        }
        Ext.applyIf(b, d);
        Wb.open(b)
    },
    onUnload: function(b, a) {
        if (!Wb.unloadEvents) {
            Wb.unloadEvents = {}
        }
        Wb.unloadEvents[a ? a.id: Wb.getId()] = b
    },
    toUrl: function(a) {
        if (Ext.String.endsWith(a, ".xwl")) {
            return "m?xwl=" + a.slice(0, -4)
        } else {
            return a
        }
    },
    close: function(f, a) {
        if (a && window.top != window && window.top.Wb) {
            window.top.Wb.close(f);
            return
        }
        if (Ext.isTouch) {
            if (Wb.hasNS("sys.thome")) {
                sys.thome.close()
            }
            return
        }
        var c = Wb.hasNS("sys.home"),
        e = Wb.hasNS("sys.ide");
        if (c || e) {
            var d = c ? sys.home.tab: Ide.fileTab,
            b = d.getActiveTab();
            if (Ext.isBoolean(f)) {
                Ext.suspendLayouts();
                d.items.each(function(g) {
                    if (f || !f && g != b) {
                        if (g.closable) {
                            g.close()
                        }
                    }
                });
                Ext.resumeLayouts(true)
            } else {
                if (f) {
                    b = d.child("[bindFile=" + f + "]")
                }
                if (b && b.closable) {
                    b.close()
                }
            }
        }
    },
    encodeHtml: function(a) {
        if (!Wb.isValue(a)) {
            return ""
        }
        if (Ext.isString(a)) {
            return Ext.htmlEncode(a).replace(/\r?\n/g, "<br>")
        } else {
            return a
        }
    },
    closeTab: function(b, c) {
        var a = b.getActiveTab();
        if (Wb.isValue(c)) {
            b.items.each(function(d) {
                if (c || d != a) {
                    d.close()
                }
            })
        } else {
            if (a) {
                a.close()
            }
        }
    },
    addLink: function(m, l, c) {
        if (Wb.isEmpty(m)) {
            return
        }
        var g, a = 0,
        e = 0,
        i = m.length,
        j = document.getElementsByTagName("head");
        function b() {
            var n = m[a++];
            if (typeof n === "string") {
                return {
                    url: n,
                    type: n.slice( - 3).toLowerCase() == ".js" ? "js": "css"
                }
            } else {
                return n
            }
        }
        function k() {
            e++;
            if (e >= i) {
                if (l) {
                    l()
                }
            } else {
                if (c) {
                    d()
                }
            }
        }
        function h(p, o) {
            Wb.loadedLinks[o] = true;
            var n = Wb.loadedLinksFn[o];
            Wb.each(n,
            function(q) {
                q()
            })
        }
        function f(o, n) {
            if (o.readyState == "loaded" || o.readyState == "complete") {
                o.onreadystatechange = function() {};
                h(o, n)
            }
        }
        function d() {
            var o, n = b();
            if (!Wb.loadedLinks) {
                Wb.loadedLinks = {}
            }
            if (Wb.loadedLinks[n.url]) {
                k();
                return
            } else {
                if (!Wb.loadedLinksFn) {
                    Wb.loadedLinksFn = {}
                }
                if (Wb.loadedLinksFn[n.url]) {
                    Wb.loadedLinksFn[n.url].push(k);
                    return
                } else {
                    Wb.loadedLinksFn[n.url] = [k]
                }
            }
            if ((n.type || "js").toLowerCase() == "js") {
                o = document.createElement("script");
                o.type = "text/javascript";
                o.src = n.url
            } else {
                o = document.createElement("link");
                o.rel = "stylesheet";
                o.href = n.url
            }
            j[0].appendChild(o);
            if (o.readyState) {
                o.onreadystatechange = Ext.bind(f, o, [o, n.url])
            } else {
                o.onload = Ext.bind(h, o, [o, n.url])
            }
        }
        if (c) {
            d()
        } else {
            for (g = 0; g < i; g++) {
                d()
            }
        }
    },
    getRelPath: function() {
        var a = window.location;
        return a.protocol + "//" + a.host + a.pathname.substring(0, a.pathname.lastIndexOf("/"))
    },
    print: function(b, f) {
        if (!Wb.printFrame) {
            var a = document.createElement("iframe"),
            g = "ifm" + Wb.getId();
            Ext.fly(a).set({
                id: g,
                name: g,
                width: 0,
                height: 0,
                src: Ext.SSL_SECURE_URL
            });
            document.body.appendChild(a);
            if (document.frames) {
                document.frames[g].name = g
            }
            Wb.printFrame = a
        }
        var c, d, e = Wb.getDoc(Wb.printFrame);
        if (Wb.isEmpty(b)) {
            c = ""
        } else {
            if (b instanceof Ext.container.Container) {
                d = b.el.down("#" + b.id + "-innerCt");
                if (!d) {
                    if (b.body) {
                        d = b.body
                    } else {
                        d = b.el
                    }
                }
                c = d.getHTML()
            } else {
                if (b.html) {
                    c = b.html
                } else {
                    if (Ext.isString(b)) {
                        c = b
                    } else {
                        if (Ext.isFunction(b.getValue)) {
                            c = b.getValue()
                        } else {
                            c = ""
                        }
                    }
                }
            }
        }
        e.write('<!DOCTYPE html><html><head><meta http-equiv="content-type" content="text/html;charset=utf-8"/><title>' + (f || "&#160;") + '</title><link type="text/css" rel="stylesheet" href="wb/css/style.css"></head><body onload="this.focus();this.print();" style="font-size:3.174mm;">' + c + "</body></html>");
        e.close()
    },
    printReport: function(a, b, d, c) {
        Wb.report(a, b,
        function(e) {
            Wb.print(e, d)
        },
        c)
    },
    report: function(a, c, e, d) {
        function b(i) {
            var h = "",
            g = true;
            function j(k) {
                return Math.round(k / 4) + "mm;"
            }
            function f(k, l) {
                var m = k.xtype == "image";
                if (!k.reportTpl) {
                    k.reportTpl = new Ext.XTemplate(k.text || k.html)
                }
                h += (m ? "<image": "<div") + ' style="left:' + j(k.x) + "top:" + j(k.y) + "width:" + j(k.width) + (k.xtype == "label" ? "": ("height:" + j(k.height))) + (k.labelAlign ? ("text-align:" + k.labelAlign + ";") : "") + (k.style || "") + '" class="wb_abs ' + (k.cls || "") + '"' + (m ? (' src="' + k.src + '"') : "") + ">" + k.reportTpl.apply(l) + "</" + (m ? "image>": "div>")
            }
            Wb.each(c,
            function(k) {
                if (g) {
                    g = false
                } else {
                    h += '<div class="wb_pb"></div>'
                }
                h += '<div style="width:' + j(i.width) + "height:" + j(i.height) + (i.style || "") + '" class="wb_rel ' + (i.cls || "") + '"><span style="visibility:hidden;">&#160;</span>';
                if (i instanceof Ext.container.Container) {
                    i.items.each(function(l) {
                        f(l, k)
                    })
                } else {
                    Wb.each(i.items,
                    function(l) {
                        f(l, k)
                    })
                }
                h += "</div>"
            });
            if (e) {
                e(h)
            }
        }
        if (Ext.isString(a)) {
            Wb.run({
                url: a,
                params: d,
                success: function(g) {
                    var f = Wb.optMain(g);
                    b(f);
                    f.destroy()
                }
            })
        } else {
            b(a)
        }
    },
    getInputValue: function(a) {
        var c, b = {};
        a = a.el || a.element || a;
        Wb.each(a.query("input"),
        function(d) {
            c = Ext.fly(d).getAttribute("itemId");
            if (c) {
                b[c] = d.value
            }
        });
        return b
    },
    setInputValue: function(b, a) {
        var d, c = {};
        b = b.el || b.element || b;
        Wb.each(b.query("input"),
        function(e) {
            d = Ext.fly(e).getAttribute("itemId");
            if (d && a.hasOwnProperty(d)) {
                e.value = a[d]
            }
        })
    },
    ns: function(e, d) {
        var c, b, a, f = e.split(".");
        if (!d) {
            d = window
        }
        b = f.length;
        for (c = 0; c < b; c++) {
            a = f[c];
            if (!d[a]) {
                d[a] = {}
            }
            d = d[a]
        }
        return d
    },
    hasNS: function(e, d) {
        var c, b, a, f = e.split(".");
        if (!d) {
            d = window
        }
        b = f.length;
        for (c = 0; c < b; c++) {
            a = f[c];
            if (!d[a]) {
                return false
            }
            d = d[a]
        }
        return true
    },
    getRowNum: function(a, b) {
        return a.owner.store.pageSize * (a.owner.store.currentPage - 1) + b
    },
    broadcast: function(a) {
        return Wb.broadcastWithOwner(a, [].slice.call(arguments, 1))
    },
    broadcastWithOwner: function(c, f, b) {
        var d, g, e = {},
        a = []; (b || Ext.ComponentQuery.query("viewport")[0]).queryBy(function(h) {
            d = h.appScope;
            if (d && !e[d.isXNS]) {
                g = d[c];
                if (Ext.isFunction(g)) {
                    a.push(Ext.callback(g, d, f))
                }
                e[d.isXNS] = true
            }
        });
        return a
    },
    readImage: function(b, d) {
        try {
            var a = b.fileInputEl.dom;
            if (a.files && a.files[0]) {
                if (!Wb.fileReader) {
                    Wb.fileReader = new FileReader()
                }
                Wb.fileReader.onload = function(f) {
                    if (d instanceof Ext.Img) {
                        d.setSrc(f.target.result)
                    } else {
                        d.src = f.taget.result
                    }
                };
                Wb.fileReader.readAsDataURL(a.files[0])
            }
        } catch(c) {}
    },
    destroy: function(a, b) {
        if (!a) {
            return
        }
        if (Ext.isTouch) {
            Wb.each(a,
            function(d, c) {
                if (c && c != b && (!Ext.isFunction(c.up) || !c.up()) && !(c instanceof Ext.data.Model) && Ext.isFunction(c.destroy)) {
                    try {
                        c.destroy()
                    } catch(f) {}
                } else {
                    if (Ext.isObject(c) && c.isXNS && d != "contextOwner") {
                        Wb.destroy(c, b)
                    }
                }
            })
        } else {
            Wb.each(a,
            function(d, c) {
                if (c && c != b) {
                    try {
                        if (c.isStore) {
                            c.destroyStore()
                        } else {
                            if (!c.ownerCt && !(c instanceof Ext.data.Model) && Ext.isFunction(c.destroy) && !c.persistentInstance) {
                                c.destroy()
                            } else {
                                if (Ext.isObject(c) && c.isXNS && d != "contextOwner") {
                                    Wb.destroy(c, b)
                                }
                            }
                        }
                    } catch(f) {}
                }
            })
        }
        Ext.Object.clear(a)
    },
    joinArray: function(d, c) {
        var e = [],
        b,
        a = d.length;
        if (a === 0) {
            return ""
        }
        if (!c) {
            return d.join(", ")
        }
        for (b = 0; b < a; b++) {
            e.push(d[b] + " (" + c[b] + ")")
        }
        return e.join(", ")
    },
    formatMilliSecs: function(b) {
        var d = Math.floor(b / 3600000),
        g = Math.floor(b % 3600000),
        a = Math.floor(g / 60000),
        e = Math.floor(g % 60000),
        c = Math.floor(e / 1000),
        f = Math.floor(e % 1000);
        return d + ":" + a + ":" + c + (Wb.format(f / 1000, "0.0##").substring(1))
    },
    highlightCode: function(d, b) {
        var e, a, c = ["java", "js", "css", "html", "sql", "xml", "jsp", "others"],
        g = ["text/x-java", "text/javascript", "text/css", "text/html", "text/x-plsql", "application/xml", "application/x-jsp", "text/plain"],
        f = function(h) {
            if (!Ext.isIE || !h.up("p")) {
                a = CodeMirror.fromTextArea(h.dom, {
                    lineNumbers: true,
                    readOnly: true,
                    mode: g[e]
                });
                if (b && c[e] == "js") {
                    a.autoFormatRange({
                        line: 0,
                        ch: 0
                    },
                    {
                        line: Number.MAX_VALUE,
                        ch: Number.MAX_VALUE
                    })
                }
                a.setSize("auto", "auto")
            }
        };
        for (e = 0; e < 8; e++) {
            d.el.select("textarea." + c[e] + "_class").each(f)
        }
    },
    recordActivity: function(a) {
        if (a.stopRecNav) {
            return
        }
        var b;
        if (Ext.isTouch) {
            b = a.getActiveItem();
            if (b) {
                b = b.id;
                if (a.backList[a.backList.length - 1] == b) {
                    return
                }
            } else {
                return
            }
        } else {
            if (a instanceof Ext.tab.Panel) {
                b = a.getActiveTab();
                if (b) {
                    b = b.id
                } else {
                    return
                }
            } else {
                b = a.getSelection()[0];
                if (b) {
                    b = b.getPath("text", "\n")
                } else {
                    return
                }
            }
        }
        if (a.backList.length > 49) {
            a.backList.splice(0, 1)
        }
        a.backList.push(b)
    },
    navigate: function(a, c, b) {
        if (Ext.isTouch) {
            c = Ext.getCmp(c);
            if (c) {
                a.setActiveItem(c)
            } else {
                return false
            }
        } else {
            if (a instanceof Ext.tree.Panel) {
                a.selectPath(c, "text", "\n")
            } else {
                c = Ext.getCmp(c);
                if (c) {
                    a.setActiveTab(c)
                } else {
                    return false
                }
            }
        }
        return true
    },
    back: function(a) {
        if (a.backList.length < 2) {
            return
        }
        var b = a.backList.pop();
        if (b) {
            if (a.forwardList.length > 49) {
                a.forwardList.splice(0, 1)
            }
            a.forwardList.push(b)
        } else {
            return
        }
        a.stopRecNav = true;
        while (a.backList.length > 0 && !Wb.navigate(a, a.backList[a.backList.length - 1], true)) {
            a.backList.pop()
        }
        a.stopRecNav = false
    },
    forward: function(a) {
        var b;
        a.stopRecNav = true;
        while ((b = a.forwardList.pop())) {
            if (Wb.navigate(a, b, false)) {
                break
            }
        }
        a.stopRecNav = false;
        if (b) {
            if (a.backList.length > 49) {
                a.backList.splice(0, 1)
            }
            a.backList.push(b)
        }
    },
    setNavigate: function(a, b, c) {
        a.backList = [];
        a.forwardList = [];
        if (Ext.isTouch) {
            Wb.recordActivity(a);
            a.on("activeitemchange",
            function() {
                var d = this;
                Wb.recordActivity(d)
            })
        } else {
            if (a instanceof Ext.tree.Panel) {
                a.mon(a, "selectionchange",
                function() {
                    var d = this;
                    Wb.recordActivity(d)
                })
            } else {
                a.mon(a, "tabchange",
                function() {
                    var d = this;
                    Wb.recordActivity(d)
                })
            }
        }
        b.navComp = a;
        if (Ext.isTouch) {
            b.on("tap",
            function(d) {
                Wb.back(d.navComp)
            });
            c.navComp = a;
            c.on("tap",
            function(d) {
                Wb.forward(d.navComp)
            })
        } else {
            b.mon(b, "click",
            function(d) {
                Wb.back(d.navComp)
            });
            c.navComp = a;
            c.mon(c, "click",
            function(d) {
                Wb.forward(d.navComp)
            })
        }
    },
    beautyJson: function(json, options) {
        var reg = null,
        formatted = '',
        pad = 0,
        PADDING = '    ';
        options = options || {};
        options.newlineAfterColonIfBeforeBraceOrBracket = (options.newlineAfterColonIfBeforeBraceOrBracket === true) ? true: false;
        options.spaceAfterColon = (options.spaceAfterColon === false) ? false: true;
        if (typeof json !== 'string') {
            json = JSON.stringify(json);
        } else {
            json = JSON.parse(json);
            json = JSON.stringify(json);
        }
        reg = /([\{\}])/g;
        json = json.replace(reg, '\r\n$1\r\n');
        reg = /([\[\]])/g;
        json = json.replace(reg, '\r\n$1\r\n');
        reg = /(\,)/g;
        json = json.replace(reg, '$1\r\n');
        reg = /(\r\n\r\n)/g;
        json = json.replace(reg, '\r\n');
        reg = /\r\n\,/g;
        json = json.replace(reg, ',');
        if (!options.newlineAfterColonIfBeforeBraceOrBracket) {
            reg = /\:\r\n\{/g;
            json = json.replace(reg, ':{');
            reg = /\:\r\n\[/g;
            json = json.replace(reg, ':[');
        }
        if (options.spaceAfterColon) {
            reg = /\:/g;
            json = json.replace(reg, ':');
        } (json.split('\r\n')).forEach(function(node, index) {
            var i = 0,
            indent = 0,
            padding = '';

            if (node.match(/\{$/) || node.match(/\[$/)) {
                indent = 1;
            } else if (node.match(/\}/) || node.match(/\]/)) {
                if (pad !== 0) {
                    pad -= 1;
                }
            } else {
                indent = 0;
            }

            for (i = 0; i < pad; i++) {
                padding += PADDING;
            }

            formatted += padding + node + '\r\n';
            pad += indent;
        });
        return formatted;
    },
    getBoolean : function(v){
		return v == true || v == '1' || v == 1 || v == 'true';
	},
    componentAuth:function(map){
    	if(!Ext.isObject(map)) return;
    	var newmap = {};
    	var array = new Array();
    	for(var key in map){
    		if(!Ext.isString(key))continue;
    		newmap[key] = map[key];
    		array.push(key);
    	}
    	Wb.post({
    	    url: 'm?xwl=sys/tool/auth/isHidden',
    	    jsonData: array,
    	    success: function(data) {
    	      var responseText = data.responseText;
    	      var jsondata = Wb.decode(responseText);
    	      for(var key in jsondata){
    	    	  var obj = newmap[key];
    	    	  var hidden = jsondata[key];
    	    	  if(obj){
    	    		  try{
    	    			  if(Ext.isArray(obj)){
    	    				  for(var i=0;i<obj.length;i++){
    	    					  obj[i].setVisible(hidden);
    	    				  }
    	    			  }else{
    	    				  obj.setVisible(hidden);
    	    			  }
    	    		  }catch(e){}
    	    	  }
    	      }
    	    }
    	});
    }
};