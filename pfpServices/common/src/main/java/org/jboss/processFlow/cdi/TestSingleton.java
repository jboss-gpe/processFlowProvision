package org.jboss.processFlow.cdi;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class TestSingleton {

    @PostConstruct
    public void start() {
        System.out.println("TestSingleton.start()");
    }

    @PreDestroy
    public void stop() {
        System.out.println("TestSingleton.stop()");
    }
}
