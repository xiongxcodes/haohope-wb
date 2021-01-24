package com.wb.tool;

import java.io.Serializable;

public class DictRecord implements Serializable {
    /**
     * @Fields serialVersionUID : TODO(用一句话描述这个变量表示什么)
     */
    private static final long serialVersionUID = -1559824784177994414L;
    public String linkTo;
    public String dispText;
    public int dispWidth;
    public String dispFormat;
    public boolean noList;
    public boolean noEdit;
    public boolean autoWrap;
    public boolean noBlank;
    public boolean readOnly;
    public String keyName;
    public int fieldSize;
    public int decimalPrecision;
    public String validator;
    public String renderer;

    /**
     * @return the linkTo
     */
    public String getLinkTo() {
        return linkTo;
    }

    /**
     * @param linkTo
     *            the linkTo to set
     */
    public void setLinkTo(String linkTo) {
        this.linkTo = linkTo;
    }

    /**
     * @return the dispText
     */
    public String getDispText() {
        return dispText;
    }

    /**
     * @param dispText
     *            the dispText to set
     */
    public void setDispText(String dispText) {
        this.dispText = dispText;
    }

    /**
     * @return the dispWidth
     */
    public int getDispWidth() {
        return dispWidth;
    }

    /**
     * @param dispWidth
     *            the dispWidth to set
     */
    public void setDispWidth(int dispWidth) {
        this.dispWidth = dispWidth;
    }

    /**
     * @return the dispFormat
     */
    public String getDispFormat() {
        return dispFormat;
    }

    /**
     * @param dispFormat
     *            the dispFormat to set
     */
    public void setDispFormat(String dispFormat) {
        this.dispFormat = dispFormat;
    }

    /**
     * @return the noList
     */
    public boolean isNoList() {
        return noList;
    }

    /**
     * @param noList
     *            the noList to set
     */
    public void setNoList(boolean noList) {
        this.noList = noList;
    }

    /**
     * @return the noEdit
     */
    public boolean isNoEdit() {
        return noEdit;
    }

    /**
     * @param noEdit
     *            the noEdit to set
     */
    public void setNoEdit(boolean noEdit) {
        this.noEdit = noEdit;
    }

    /**
     * @return the autoWrap
     */
    public boolean isAutoWrap() {
        return autoWrap;
    }

    /**
     * @param autoWrap
     *            the autoWrap to set
     */
    public void setAutoWrap(boolean autoWrap) {
        this.autoWrap = autoWrap;
    }

    /**
     * @return the noBlank
     */
    public boolean isNoBlank() {
        return noBlank;
    }

    /**
     * @param noBlank
     *            the noBlank to set
     */
    public void setNoBlank(boolean noBlank) {
        this.noBlank = noBlank;
    }

    /**
     * @return the readOnly
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * @param readOnly
     *            the readOnly to set
     */
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * @return the keyName
     */
    public String getKeyName() {
        return keyName;
    }

    /**
     * @param keyName
     *            the keyName to set
     */
    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    /**
     * @return the fieldSize
     */
    public int getFieldSize() {
        return fieldSize;
    }

    /**
     * @param fieldSize
     *            the fieldSize to set
     */
    public void setFieldSize(int fieldSize) {
        this.fieldSize = fieldSize;
    }

    /**
     * @return the decimalPrecision
     */
    public int getDecimalPrecision() {
        return decimalPrecision;
    }

    /**
     * @param decimalPrecision
     *            the decimalPrecision to set
     */
    public void setDecimalPrecision(int decimalPrecision) {
        this.decimalPrecision = decimalPrecision;
    }

    /**
     * @return the validator
     */
    public String getValidator() {
        return validator;
    }

    /**
     * @param validator
     *            the validator to set
     */
    public void setValidator(String validator) {
        this.validator = validator;
    }

    /**
     * @return the renderer
     */
    public String getRenderer() {
        return renderer;
    }

    /**
     * @param renderer
     *            the renderer to set
     */
    public void setRenderer(String renderer) {
        this.renderer = renderer;
    }
}