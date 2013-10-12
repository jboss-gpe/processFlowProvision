package org.jboss.processFlow.services.remote.cdi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.kie.api.marshalling.ObjectMarshallingStrategy;


public class TestStrategy implements ObjectMarshallingStrategy {
    
    public TestStrategy() {
        System.out.println("TestStrategy() constructor");
    }

    public Object read(ObjectInputStream os) throws IOException, ClassNotFoundException {
        System.out.println("TestStrategy() read");
        return null;
    }

    public void write(ObjectOutputStream os, Object object) throws IOException {
        System.out.println("TestStrategy() write");
    }

    public boolean accept(Object object) {
            System.out.println("accept() object = "+object);
            return true;
    }

    public byte[] marshal(Context context, ObjectOutputStream os, Object object) throws IOException {
        System.out.println("TestStrategy() marshal");
            return "test".getBytes();
    }

    public Object unmarshal(Context context, ObjectInputStream is, byte[] object, ClassLoader classloader) throws IOException, ClassNotFoundException {
        System.out.println("TestStrategy() unmarshal");
        return "test";
    }

    public Context createContext() {
        System.out.println("TestStrategy() createContext");
        return null;
    }

}
