package example.completion.func;
// 参数的详细信息
public class ParameterInfo {
    private String desc;
    private String key;
    private String optional; // JSON中是字符串"false"，所以这里用String
    private String type;

    // Getters and Setters

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getOptional() {
        return optional;
    }

    public void setOptional(String optional) {
        this.optional = optional;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ParameterInfo{" +
                "desc='" + desc + '\'' +
                ", key='" + key + '\'' +
                ", optional='" + optional + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}