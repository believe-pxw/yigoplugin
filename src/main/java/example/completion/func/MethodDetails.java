package example.completion.func;

import java.util.List;

public class MethodDetails {
    private String desc;
    private String descDetail;
    private String key;

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    private String template;

    private List<ParameterInfo> paras;
    private String returnValueDesc;
    private String returnValueType;

    // Getters and Setters

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getDescDetail() {
        return descDetail;
    }

    public void setDescDetail(String descDetail) {
        this.descDetail = descDetail;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<ParameterInfo> getParas() {
        return paras;
    }

    public void setParas(List<ParameterInfo> paras) {
        this.paras = paras;
    }

    public String getReturnValueDesc() {
        return returnValueDesc;
    }

    public void setReturnValueDesc(String returnValueDesc) {
        this.returnValueDesc = returnValueDesc;
    }

    public String getReturnValueType() {
        return returnValueType;
    }

    public void setReturnValueType(String returnValueType) {
        this.returnValueType = returnValueType;
    }

    @Override
    public String toString() {
        return "MethodDetails{" +
                "desc='" + desc + '\'' +
                ", descDetail='" + descDetail + '\'' +
                ", key='" + key + '\'' +
                ", paras=" + paras +
                ", returnValueDesc='" + returnValueDesc + '\'' +
                ", returnValueType='" + returnValueType + '\'' +
                '}';
    }
}