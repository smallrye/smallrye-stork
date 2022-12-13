package io.smallrye.stork.test;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MyDataBean {

    private String value;

    public void set(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

}
