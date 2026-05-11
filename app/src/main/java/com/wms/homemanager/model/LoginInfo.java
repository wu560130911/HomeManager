package com.wms.homemanager.model;

import java.io.Serializable;

public class LoginInfo implements Serializable {
    private String id;
    private String host;
    private int port;
    private String username;
    private String certPath;
    private String certPass;

    public LoginInfo() {
        this.id = generateId();
    }

    public LoginInfo(String host, int port, String username, String certPath, String certPass) {
        this.id = generateId();
        this.host = host;
        this.port = port;
        this.username = username;
        this.certPath = certPath;
        this.certPass = certPass;
    }

    public LoginInfo(String id, String host, int port, String username, String certPath, String certPass) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.username = username;
        this.certPath = certPath;
        this.certPass = certPass;
    }

    private String generateId() {
        return System.currentTimeMillis() + "";
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCertPath() {
        return certPath;
    }

    public void setCertPath(String certPath) {
        this.certPath = certPath;
    }

    public String getCertPass() {
        return certPass;
    }

    public void setCertPass(String certPass) {
        this.certPass = certPass;
    }

    @Override
    public String toString() {
        return username + "@" + host + ":" + port ;
    }
}