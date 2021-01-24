package com.wb.controls;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import com.wb.util.DateUtil;
import com.wb.util.WebUtil;

public class Control {
    public HttpServletRequest request;
    public HttpServletResponse response;
    public JSONObject controlData;
    public JSONObject configs;
    protected JSONObject events;
    protected JSONObject controlMeta;
    protected JSONObject generalMeta;
    protected JSONObject configsMeta;
    protected JSONObject eventsMeta;
    protected JSONObject parentGeneral;
    protected boolean lastNode;
    protected boolean normalRunType;

    public void create() throws Exception {}

    public void init(HttpServletRequest request, HttpServletResponse response, JSONObject controlData,
        JSONObject controlMeta, JSONObject parentGeneral, boolean lastNode, boolean normalRunType) {
        this.request = request;
        this.response = response;
        this.controlData = controlData;
        this.configs = (JSONObject)controlData.opt("configs");
        this.events = (JSONObject)controlData.opt("events");
        this.controlMeta = controlMeta;
        this.generalMeta = (JSONObject)controlMeta.opt("general");
        this.configsMeta = (JSONObject)controlMeta.opt("configs");
        this.eventsMeta = (JSONObject)controlMeta.opt("events");
        this.parentGeneral = parentGeneral;
        this.lastNode = lastNode;
        this.normalRunType = normalRunType;
    }

    protected String gs(String name, boolean repalce) {
        Object value = this.configs.opt(name);
        return value == null ? "" : (repalce ? WebUtil.replaceParams(this.request, (String)value) : (String)value);
    }

    protected String gr(String value) {
        return WebUtil.replaceParams(this.request, value);
    }

    protected String gs(String name) {
        return gs(name, true);
        // Object value = this.configs.opt(name);
        // return value == null ? "" : WebUtil.replaceParams(this.request, (String)value);
    }

    protected int gi(String name) {
        return this.gi(name, 0);
    }

    protected int gi(String name, int defaultValue) {
        String value = this.gs(name);
        return value.isEmpty() ? defaultValue : Integer.parseInt(value);
    }

    protected float gf(String name) {
        return this.gf(name, 0.0F);
    }

    protected float gf(String name, float defaultValue) {
        String value = this.gs(name);
        return value.isEmpty() ? defaultValue : (float)Integer.parseInt(value);
    }

    protected Date gd(String name) {
        return this.gd(name, (Date)null);
    }

    protected Date gd(String name, Date defaultValue) {
        String value = this.gs(name);
        return (Date)(value.isEmpty() ? defaultValue : DateUtil.strToDate(value));
    }

    protected boolean gb(String name) {
        return this.gb(name, false);
    }

    protected boolean gb(String name, boolean defaultValue) {
        String value = this.gs(name);
        return value.isEmpty() ? defaultValue : Boolean.parseBoolean(value);
    }

    protected String ge(String name) {
        if (this.events == null) {
            return "";
        } else {
            Object event = this.events.opt(name);
            return event == null ? "" : WebUtil.replaceParams(this.request, (String)event);
        }
    }

    protected String gp(String name) {
        return WebUtil.fetch(this.request, name);
    }
}