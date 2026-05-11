package com.wms.homemanager.model;

import com.wms.homemanager.utils.StringUtils;

import java.util.Objects;

public class PortForwardingConfig {
    private String name;
    private String sourceIp;
    private int sourcePort;
    private String targetIp;
    private int targetPort;
    private ForwardType forwardType;

    public enum ForwardType {
        TARGET_TO_LOCAL("目标转发到本地"),
        LOCAL_TO_TARGET("本地转发到目标");

        private final String displayName;

        ForwardType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public PortForwardingConfig(String name, String sourceIp, int sourcePort, String targetIp, int targetPort, ForwardType forwardType) {
        this.name = name;
        this.sourceIp = sourceIp;
        this.sourcePort = sourcePort;
        this.targetIp = targetIp;
        this.targetPort = targetPort;
        this.forwardType = forwardType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public int getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(int sourcePort) {
        this.sourcePort = sourcePort;
    }

    public String getTargetIp() {
        return targetIp;
    }

    public void setTargetIp(String targetIp) {
        this.targetIp = targetIp;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public ForwardType getForwardType() {
        return forwardType;
    }

    public void setForwardType(ForwardType forwardType) {
        this.forwardType = forwardType;
    }

    // impl equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PortForwardingConfig that = (PortForwardingConfig) o;
        return sourcePort == that.sourcePort &&
                targetPort == that.targetPort &&
                forwardType == that.forwardType &&
                //使用工具类比较字符串是否相等
                StringUtils.equals(name, that.name) &&
                StringUtils.equals(sourceIp, that.sourceIp) &&
                StringUtils.equals(targetIp, that.targetIp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, sourceIp, sourcePort, targetIp, targetPort, forwardType);
    }
}
