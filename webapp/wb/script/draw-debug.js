//画图控件类，需要引用raphael.js
Ext.define('Wb.draw.DragSelector', {
  init: function(panel) {
    this.panel = panel;
    panel.mon(panel, {
      beforecontainerclick: this.cancelClick,
      scope: this,
      render: {
        fn: this.onRender,
        scope: this,
        single: true
      }
    });
  },
  onRender: function() {
    this.tracker = Ext.create('Ext.dd.DragTracker', {
      panel: this.panel,
      el: this.panel.body,
      dragSelector: this,
      onBeforeStart: this.onBeforeStart,
      onStart: this.onStart,
      onDrag: this.onDrag,
      onEnd: this.onEnd
    });
    this.dragRegion = Ext.create('Ext.util.Region');
  },
  onBeforeStart: function(e) {
    return e.target == this.panel.paper.canvas;
  },
  onStart: function(e) {
    var dragSelector = this.dragSelector;
    this.dragging = true;
    dragSelector.fillRegions();
    dragSelector.getProxy().show();
  },
  cancelClick: function() {
    return !this.tracker.dragging;
  },
  onDrag: function(e) {
    var me = this,
      panel = me.panel,
      dragSelector = me.dragSelector,
      dragRegion = dragSelector.dragRegion,
      bodyRegion = dragSelector.bodyRegion,
      proxy = dragSelector.getProxy(),
      startXY = me.startXY,
      currentXY = me.getXY(),
      minX = Math.min(startXY[0], currentXY[0]),
      minY = Math.min(startXY[1], currentXY[1]),
      width = Math.abs(startXY[0] - currentXY[0]),
      height = Math.abs(startXY[1] - currentXY[1]),
      region, selected;

    Ext.apply(dragRegion, {
      top: minY,
      left: minX,
      right: minX + width,
      bottom: minY + height
    });
    dragRegion.constrainTo(bodyRegion);
    proxy.setRegion(dragRegion);
    panel.items.each(function(item) {
      region = item.el.getRegion();
      selected = dragRegion.intersect(region);
      panel.select(item, selected);
    });
  },
  onEnd: Ext.Function.createDelayed(function(e) {
    var dragSelector = this.dragSelector;
    this.dragging = false;
    dragSelector.getProxy().hide();
  }, 1),
  getProxy: function() {
    if (!this.proxy) {
      this.proxy = this.panel.body.createChild({
        tag: 'div',
        cls: 'x-view-selector'
      });
    }
    return this.proxy;
  },
  fillRegions: function() {
    var panel = this.panel;
    this.bodyRegion = panel.body.getRegion();
  }
});
//绘图组件
Ext.define('Wb.drag.drawComp', {
  alias: 'widget.drawcomp',
  extend: 'Ext.panel.Panel',
  layout: 'absolute',
  cls: 'wb_design x-unselectable',
  isFlow: true,
  initComponent: function() {
    var me = this;
    if (!me.disableDesign)
      me.plugins = Ext.create('Wb.draw.DragSelector');
    me.callParent();
  },
  afterRender: function() {
    var me = this;
    me.callParent();
    me.paper = new Raphael(me.body.dom.id, me.body.getWidth(), me.body.getHeight());
    if (!me.disableDesign) {
      me.mon(me.body, 'mousedown', function(e, target) {
        var el, cmp = null;
        me.fireEvent('beforemousedown', me, cmp);
        el = Ext.fly(target);
        me.startMouseXY = e.getXY();
        if (target.pathComp)
          cmp = target.pathComp;
        else if (target.parentNode && target.parentNode.pathComp)
          cmp = target.parentNode.pathComp;
        else {
          if (el.hasCls('x-component'))
            cmp = Ext.getCmp(el.getAttribute('id'));
          me.allowMouseUp = me.fireEvent('mousedown', me, cmp) !== false;
        }
        if (cmp) {
          if (e.shiftKey)
            me.select(cmp, !cmp.selected);
          else if (!cmp.selected)
            me.deselectAll(cmp);
        } else if (!me.isResizer(target)) me.deselectAll();
      });
      me.mon(me.body, 'mouseup', function(e, target) {
        var xy = e.getXY(),
          el = Ext.fly(target),
          cmp = null;
        if (el.hasCls('x-component'))
          cmp = Ext.getCmp(el.getAttribute('id'));
        if (me.allowMouseUp && !target.pathComp)
          me.fireEvent('mouseup', me, cmp, me.startMouseXY[0] - me.getX(), me.startMouseXY[1] - me.getY(),
            xy[0] - me.startMouseXY[0], xy[1] - me.startMouseXY[1]);
      });
    }
    me.mon(me, 'resize', function(comp, width, height) {
      me.paper.setSize(width, height);
    });
  },
  /**
   * 在当前控件上画指定样式的矩形。
   * @param {Object} config 配置参数对象。
   * @param {Boolean} [selected] 是否选中，默认为false。
   * @return {Component} 图形关联的mask对象。
   */
  rect: function(config, selected) {
    config = Wb.apply({
      color: '#000',
      fontSize: 12,
      backColor: '#eee'
    }, config);
    var me = this,
      paper = me.paper,
      x = Ext.Number.snap(config.x, 8),
      y = Ext.Number.snap(config.y, 8),
      width = Ext.Number.snap(config.width || 120, 8),
      height = Ext.Number.snap(config.height || 24, 8),
      label, rect, image, mask;
    rect = paper.rect(x, y, width, height, 10);
    rect.attr({
      fill: config.backColor,
      stroke: '#aaa',
      "stroke-width": 1
    });
    image = paper.image('wb/images/' + (config.iconCls || 'null') + '.png', x + 10, y + height / 2 - 8, 16, 16);
    label = paper.text(x, y, config.name);
    label.attr({
      'font-size': config.fontSize,
      fill: config.color
    });
    mask = me.addMask(rect, label, image, config);
    if (selected)
      me.select(mask);
    me.fireEvent('changed', me);
    return mask;
  },
  /**
   * 调整连接线的标签和图标位置。
   * @param {Path} path 路径。
   */
  adjustLabelIconPos: function(path) {
    var pathInfo = path.pathInfo,
      x = pathInfo.x,
      y = pathInfo.y,
      nl = pathInfo.nl;
    if (nl) {
      path.label.attr({
        x: x,
        y: y
      });
      path.image.attr({
        x: x - 8,
        y: y - 28
      });
    } else {
      path.label.attr({
        x: x,
        y: path.image.attrs.src == 'wb/images/null.png' ? y : (y + 13)
      });
      path.image.attr({
        x: x - 8,
        y: y - 15
      });
    }
  },
  /**
   * 刷新路径。
   * @param {Path} path 路径对象。
   */
  reloadPath: function(path) {
    var me = this,
      pathInfo = me.getPath(path.srcNode, path.dstNode, path.props.beeline);
    path.pathInfo = pathInfo;
    path.attr({
      path: pathInfo.path
    });
    path.maskPath.attr({
      path: pathInfo.path
    });
    me.adjustLabelIconPos(path);
  },
  /**
   * 控件拖动和调整大小时的处理方法。
   */
  adjustSize: function() {
    var me = this,
      rect = me.rect,
      label = me.label,
      image = me.image,
      x = me.getLocalX(),
      y = me.getLocalY(),
      width = me.getWidth(),
      height = me.getHeight(),
      path, pathInfo;

    function movePath() {
      path.attr({
        path: pathInfo.path
      });
      path.maskPath.attr({
        path: pathInfo.path
      });
      me.ownerCt.adjustLabelIconPos(path);
    }

    Ext.suspendLayouts();
    rect.attr({
      x: x,
      y: y,
      width: width,
      height: height
    });
    label.attr({
      x: x + width / 2,
      y: y + height / 2
    });
    image.attr({
      x: x + 10,
      y: y + height / 2 - 8
    });
    Wb.each(me.toNodes, function(node) {
      path = me.paths[node.id];
      path.pathInfo = pathInfo = me.ownerCt.getPath(me, node, path.props.beeline);
      movePath();
    });
    Wb.each(me.fromNodes, function(node) {
      path = node.paths[me.id];
      path.pathInfo = pathInfo = me.ownerCt.getPath(node, me, path.props.beeline);
      movePath();
    });
    me.ownerCt.fireEvent('changed', me);
    Ext.resumeLayouts(true);
  },
  /**
   * 删除指定的节点或连接线。
   * @param {Component/Path} comp 节点或连线。
   */
  removeNode: function(comp) {
    var me = this;
    me.deselect(comp);
    //节点
    if (comp instanceof Ext.Component) {
      //删除目标连线
      if (comp.paths) {
        Wb.each(comp.paths, function(k, path) {
          if (path)
            me.removeNode(path);
        }, true);
      }
      //删除源连线
      if (comp.fromNodes) {
        Wb.each(comp.fromNodes, function(node) {
          if (node)
            me.removeNode(node.paths[comp.id]);
        }, true);
      }
      comp.label.remove();
      comp.image.remove();
      comp.rect.remove();
      me.remove(comp);
    } else {
      //连线
      comp.maskPath.remove();
      comp.label.remove();
      comp.image.remove();
      Ext.Array.remove(comp.srcNode.toNodes, comp.dstNode);
      Ext.Array.remove(comp.dstNode.fromNodes, comp.srcNode);
      delete comp.srcNode.paths[comp.dstNode.id];
      comp.remove();
    }
    me.fireEvent('changed', me);
  },
  /**
   * 删除选择的节点和连接线。
   */
  removeSelection: function() {
    var me = this,
      sels = me.getSelection();
    Ext.suspendLayouts();
    Wb.each(sels, function(item) {
      me.removeNode(item);
    });
    Ext.resumeLayouts();
  },
  /**
   * 删除所有的节点和连接线。
   */
  removeAll: function() {
    var me = this;
    me.items.each(function(node) {
      me.removeNode(node);
    });
  },
  /**
   * 选择全部了节点和连接线。
   */
  selectAll: function() {
    var me = this;
    me.setSelection(me.getItems());
  },
  /**
   * 取消选中全部节点并选择某一个节点。
   * @param {Component} [selNode] 选择的节点。缺省表示不选择任何节点
   */
  deselectAll: function(selNode) {
    var me = this;
    this.items.each(function(item) {
      if (item == selNode && !item.selected)
        me.select(item);
      else
        me.deselect(item);
      if (item.paths) {
        //支持按shift的反选
        Wb.each(item.paths, function(k, path) {
          if (path == selNode && !path.selected)
            me.select(path);
          else
            me.deselect(path);
        });
      }
    });
  },
  /**
   * 取消选择指定的控件。
   * @param {Component} comp 遮盖对象。
   */
  deselect: function(comp) {
    this.select(comp, false);
  },
  /**
   * 获取所有选择的节点对象列表。
   * @return {Array} 选择的节点列表。
   */
  getSelection: function() {
    return this.getItems(true);
  },
  /**
   * 设置节点或连接为选中状态。
   * @param {Array} items 节点或连接对象。
   * @param {Boolean} [clearSelection] 是否清除原有选择的节点或连接。默认为true。
   */
  setSelection: function(items, clearSelection) {
    var me = this;
    if (Wb.getBool(clearSelection, true))
      me.deselectAll();
    Wb.each(items, function(item) {
      me.select(item);
    });
  },
  /**
   * 获取所有选择的节点对象列表。
   * @param {Boolean} selectedOnly 是否仅获得选择节点的数据。
   * @return {Array} 选择的节点列表。
   */
  getItems: function(selectedOnly) {
    var sels = [];
    //先获得连线
    this.items.each(function(item) {
      if (item.paths) {
        Wb.each(item.paths, function(k, path) {
          if (!selectedOnly || path.selected) {
            //更新相关属性
            Wb.apply(path.props, {
              from: path.srcNode.props.name,
              to: path.dstNode.props.name
            });
            sels.push(path);
          }
        });
      }
    });
    //再获得节点
    this.items.each(function(item) {
      if (!selectedOnly || item.selected) {
        //更新相关属性
        Wb.apply(item.props, {
          x: item.getLocalX(),
          y: item.getLocalY(),
          width: item.getWidth(),
          height: item.getHeight()
        });
        sels.push(item);
      }
    });
    return sels;
  },
  /**
   * 查找指定名称的节点。
   * @param {String} name 名称。
   * @return {Node} 找到的节点。如果不存在返回null。
   */
  findItem: function(name) {
    var result = null,
      me = this;
    Wb.each(me.getItems(), function(item) {
      if (item.isNode && item.props.name == name) {
        result = item;
        return false;
      }
    });
    return result;
  },
  /**
   * 查找指定起点和终点的连接线。
   * @param {String} name 名称。
   * @return {Path} 找到的连接线。如果不存在返回null。
   */
  findPath: function(src, dst) {
    var result = null,
      me = this;
    Wb.each(me.getItems(), function(item) {
      if (item.isPath && item.srcNode.props.name == src && item.dstNode.props.name == dst) {
        result = item;
        return false;
      }
    });
    return result;
  },
  /**
   * 选择或取消选择指定的控件。
   * @param {Component/Path} comp 节点或路径。
   * @param {Boolean} [selected] 是否选择。true选择，false取消选择。默认为true。
   */
  select: function(comp, selected) {
    var me = this;
    selected = Wb.getBool(selected, true);
    if (comp.selected === selected)
      return;
    comp.selected = selected;
    if (comp instanceof Ext.Component) {
      //选择节点
      if (comp.resizer) {
        var dot, resizer = comp.resizer,
          positions = resizer.possiblePositions;
        Ext.suspendLayouts();
        Wb.each(positions, function(key, value) {
          dot = resizer[value];
          if (selected)
            dot.show();
          else
            dot.hide();
        });
        Ext.resumeLayouts(true);
      } else if (selected && !me.disableDesign) {
        comp.initResizable({
          handles: 'all',
          pinned: true,
          widthIncrement: 8,
          heightIncrement: 8,
          listeners: {
            // resize: Ide.fitResizer
          }
        });
      }
    } else {
      //选择连线
      if (comp.selected) {
        comp.attr({
          'stroke-width': 3
        });
      } else {
        comp.attr({
          'stroke-width': 2
        });
      }
    }
    this.fireEvent('selectionchange', comp, selected);
  },
  /**
   * 在指定的对象上绑定可调整大小可拖动的遮盖对象。
   * @param {DrawComponent} rect 需要绑定的绘图对象。
   * @param {Label} 标签。
   * @param {Image} 图片。
   * @param {Object} 配置属性。
   * @return {Component} 绑定的遮盖对象。
   */
  addMask: function(rect, label, image, config) {
    var me = this,
      attr = rect.attrs,
      mask;
    mask = me.add({
      xtype: 'box',
      isMask: true,
      x: attr.x,
      y: attr.y,
      rect: rect,
      label: label,
      isNode: true,
      image: image,
      props: config,
      width: attr.width,
      height: attr.height,
      style: me.disableDesign ? '' : 'cursor:move;',
      draggable: me.disableDesign ? false : {
        listeners: {
          drag: function(obj, e) {
            var xy = obj.lastXY,
              fx = Ext.Number.snap(xy[0] - obj.startXY[0], 8),
              fy = Ext.Number.snap(xy[1] - obj.startXY[1], 8);

            Ext.suspendLayouts();
            me.items.each(function(item) {
              if (item.selected) {
                var x = item.originXY[0] + fx,
                  y = item.originXY[1] + fy;
                item.setLocalXY(x, y);
                item.fireEvent('move', item, x, y);
              }
            });
            Ext.resumeLayouts(true);
          },
          mousedown: function(obj, e) {
            if (me.isResizer(e.target))
              return false;
            obj.startXY = obj.lastXY;
            me.items.each(function(item) {
              item.originXY = item.getLocalXY();
            });
          }
        }
      },
      listeners: {
        resize: this.adjustSize,
        move: this.adjustSize
      }
    });
    return mask;
  },
  /**
   * 判断设计器中指定对象是否是Resizer。
   * @param {HTMLElement} el 需要判断的HTML元素。
   * @return {Boolean} true是Resizer，否则不是。
   */
  isResizer: function(el) {
    return Ext.fly(el).hasCls('x-resizable-handle');
  },
  /**
   * 获取指定对象上下左右4个锚点。
   * @param {Component} comp对象。
   * @return {Array} 上下左右4个锚点。
   */
  getPoints: function(comp) {
    var x = comp.getLocalX(),
      y = comp.getLocalY(),
      w = comp.getWidth(),
      h = comp.getHeight(),
      cw = x + w / 2,
      ch = y + h / 2;
    return [
      [cw, y],
      [x + w, ch],
      [cw, y + h],
      [x, ch]
    ];
  },
  /**
   * 获得源节点到目标节点的最优路径。
   * @param {Component} src 源节点。
   * @param {Component} dst 目标节点。
   * @param {Boolean} beeline 是否直接连接。
   * @return {Object} 路径和标签坐标信息。
   */
  getPath: function(src, dst, beeline) {
    var srcPoints = this.getPoints(src),
      dstPoints = this.getPoints(dst),
      allLink = [],
      sortLink, result;

    function getDistance(node1, node2) {
      //仅判断距离远近因此不需要开方
      return (node1[0] - node2[0]) * (node1[0] - node2[0]) + (node1[1] - node2[1]) * (node1[1] - node2[1]);
    }

    //如果路径显示效果较佳返回path，否则返回null。
    function computePath(srcPt, dstPt, directLink) {
      var lx, ly, path, srcVertical, dstVertical, srcX, srcY, dstX, dstY, midX, midY, srcIndex, dstIndex, srcPath, dstPath, normalLabel = false,
        tv = 10;
      srcIndex = Wb.indexOf(srcPoints, srcPt);
      dstIndex = Wb.indexOf(dstPoints, dstPt);
      srcVertical = srcIndex % 2 === 0;
      dstVertical = dstIndex % 2 === 0;
      srcX = srcPt[0];
      srcY = srcPt[1];
      dstX = dstPt[0];
      dstY = dstPt[1];
      if (Math.abs(srcX - dstX) < tv && Math.abs(srcY - dstY) < tv) {
        if (srcX < dstX)
          dstX = srcX + tv;
        else
          dstX = srcX - tv;
        if (srcY < dstY)
          dstY = srcY + tv;
        else
          dstY = srcY - tv;
      }
      midX = srcX + (dstX - srcX) / 2;
      midY = srcY + (dstY - srcY) / 2;
      lx = midX;
      ly = midY;
      if (directLink)
        return {
          path: 'M' + srcX + ' ' + srcY + ' L' + dstX + ' ' + dstY,
          x: midX,
          y: midY
        };
      switch (srcIndex) {
        case 0:
          if (dstY > srcY) return null;
          break;
        case 1:
          if (dstX < srcX) return null;
          break;
        case 2:
          if (dstY < srcY) return null;
          break;
        case 3:
          if (dstX > srcX) return null;
          break;
      }
      switch (dstIndex) {
        case 0:
          if (dstY < srcY) return null;
          break;
        case 1:
          if (dstX > srcX) return null;
          break;
        case 2:
          if (dstY > srcY) return null;
          break;
        case 3:
          if (dstX < srcX) return null;
          break;
      }
      path = 'M' + srcX + ' ' + srcY + ' L';
      if (srcVertical) {
        if (dstVertical) {
          if (Math.abs(midY - srcY) < tv || Math.abs(dstY - midY) < tv)
            return null;
          if (srcX != dstX) {
            normalLabel = true;
            ly = midY - 12;
          }
          path += srcX + ' ' + midY + ' L' + dstX + ' ' + midY;
        } else {
          if (Math.abs(dstY - srcY) < tv || Math.abs(dstX - srcX) < tv)
            return null;
          lx = srcX;
          path += srcX + ' ' + dstY;
        }
      } else {
        if (dstVertical) {
          if (Math.abs(dstX - srcX) < tv || Math.abs(dstY - srcY) < tv)
            return null;
          lx = dstX;
          path += dstX + ' ' + srcY;
        } else {
          if (Math.abs(midX - srcX) < tv || Math.abs(dstX - midX) < tv)
            return null;
          path += midX + ' ' + srcY + ' L' + midX + ' ' + dstY;
        }
      }
      path += ' L' + dstX + ' ' + dstY;
      if (srcY == dstY) {
        normalLabel = true;
        ly = midY - 12;
      }
      return {
        path: path,
        x: lx,
        y: ly,
        nl: normalLabel
      };
    }
    Wb.each(srcPoints, function(p1) {
      Wb.each(dstPoints, function(p2) {
        allLink.push([p1, p2]);
      });
    });
    //对所有路径组合排序
    sortLink = Ext.Array.sort(allLink, function(v1, v2) {
      var x = getDistance(v1[0], v1[1]),
        y = getDistance(v2[0], v2[1]);
      return (x < y) ? -1 : ((x == y) ? 0 : 1);
    });
    if (beeline)
      return computePath(sortLink[0][0], sortLink[0][1], true);
    //获取首个最佳显示效果的路径
    Wb.each(sortLink, function(link) {
      result = computePath(link[0], link[1]);
      if (result)
        return false;
    });
    //判断路径是否重复
    if (result) {
      if (dst.paths && dst.paths[src.id]) {
        srcPath = result.path;
        dstPath = dst.paths[src.id].pathInfo.path;
        if (dstPath.substring(1, dstPath.indexOf(' L')) == srcPath.substring(srcPath.lastIndexOf('L') + 1) &&
          srcPath.substring(1, srcPath.indexOf(' L')) == dstPath.substring(dstPath.lastIndexOf('L') + 1)) {
          result = computePath(sortLink[2][0], sortLink[2][1]);
        }
      }
    }
    //如果无可用路径直接联接
    if (!result)
      return computePath(sortLink[0][0], sortLink[0][1], true);
    return result;
  },
  /**
   * 把源节点和目标节点使用带箭头的线条连接起来。
   * @param {Component} src 源节点。
   * @param {Component} dst 目标节点。
   * @param {Object} config 配置对象。
   */
  link: function(src, dst, config) {
    config = Wb.apply({
      color: '#000',
      fontSize: 12,
      backColor: '#bbb',
      beeline: false,
      lineTitle: ''
    }, config);
    //如果已经存在连接返回
    if (src.paths && src.paths[dst.id])
      return;
    var path, maskPath, label, image, me = this,
      paper = me.paper,
      pathInfo = me.getPath(src, dst, config.beeline),
      pathText = pathInfo.path,
      x = pathInfo.x,
      y = pathInfo.y;
    label = paper.text(x, y, config.lineTitle);
    label.toBack();
    label.attr({
      'font-size': config.fontSize,
      cursor: me.disableDesign ? 'default' : 'pointer',
      fill: config.color
    });
    image = paper.image('wb/images/' + (config.iconCls || null) + '.png', x - 8, y - 15, 16, 16);
    image.toBack();
    image.attr({
      cursor: me.disableDesign ? 'default' : 'pointer'
    });
    path = paper.path(pathText);
    path.pathInfo = pathInfo;
    path.toBack();
    path.attr({
      'arrow-end': 'classic-wide-long',
      stroke: config.backColor,
      'stroke-width': 2
    });
    path.selected = false;
    path.srcNode = src;
    path.dstNode = dst;
    path.isPath = true;
    path.props = config;
    path.maskPath = maskPath = paper.path(pathText);
    maskPath.attr({
      opacity: 0,
      cursor: me.disableDesign ? 'default' : 'pointer',
      'stroke-width': 16
    });
    maskPath.bindPath = path;
    maskPath.node.pathComp = path;
    path.label = label;
    label.node.pathComp = path;
    path.image = image;
    image.node.pathComp = path;
    if (!src.paths)
      src.paths = {};
    src.paths[dst.id] = path;
    if (!src.toNodes)
      src.toNodes = [];
    if (Wb.indexOf(src.toNodes, dst) == -1)
      src.toNodes.push(dst);
    if (!dst.fromNodes)
      dst.fromNodes = [];
    if (Wb.indexOf(dst.fromNodes, src) == -1)
      dst.fromNodes.push(src);
    me.adjustLabelIconPos(path);
    me.fireEvent('changed', me);
  },
  //排除选择的连接，并添加源节点和目标节点均存在的连接
  preparePath: function(items) {
    var nodes = [],
      links = [];
    Wb.each(items, function(item) {
      if (item.isNode)
        nodes.push(item);
    });
    Wb.each(nodes, function(node) {
      if (node.paths) {
        Wb.each(node.paths, function(k, path) {
          if (Wb.indexOf(nodes, path.dstNode) != -1)
            links.push(path);
        });
      }
    });
    return links.concat(nodes);
  },
  /**
   * 获得流程的数据。
   * @param {Boolean} selectedOnly 是否仅获得选择节点的数据。
   * @return {Object} 流程数据。
   */
  getData: function(selectedOnly) {
    var me = this,
      items = me.getItems(selectedOnly),
      nodes = [],
      links = [];
    if (selectedOnly)
      items = me.preparePath(items);
    Wb.each(items, function(item) {
      if (item.isNode)
        nodes.push(item.props);
      else
        links.push(item.props);
    });
    return {
      nodes: nodes,
      links: links,
      flow: selectedOnly ? null : me.props
    };
  },
  /**
   * 加载流程数据。
   * @param {Object} data 流程数据。
   * @param {Boolean} offset 加载节点时在x,y上添加的偏移量，默认为0。   
   * @return {Nodes[]} 生成的节点列表。
   */
  load: function(data, offset) {
    if (!data) return;
    var me = this,
      nodeMap = {};
    Wb.each(data.nodes, function(node) {
      if (offset)
        node = Wb.applyIf({
          x: node.x + offset,
          y: node.y + offset
        }, node);
      nodeMap[node.name] = me.rect(node);
    });
    Wb.each(data.links, function(link) {
      me.link(nodeMap[link.from], nodeMap[link.to], link);
    }, true);
    if (data.flow)
      me.props = data.flow;
    return Ext.Object.getValues(nodeMap);
  }
});