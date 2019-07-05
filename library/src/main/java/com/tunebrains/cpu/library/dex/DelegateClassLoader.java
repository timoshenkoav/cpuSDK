package com.tunebrains.cpu.library.dex;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * Created by Alexandr Timoshenko <thick.tav@gmail.com> on 5/30/16.
 */
public class DelegateClassLoader extends ClassLoader {
    private ClassLoader delegate;

    public DelegateClassLoader(ClassLoader pDelegate) {
        delegate = pDelegate;
    }

    public static ClassLoader getSystemClassLoader() {
        return ClassLoader.getSystemClassLoader();
    }

    public static URL getSystemResource(String resName) {
        return ClassLoader.getSystemResource(resName);
    }

    public static Enumeration<URL> getSystemResources(String resName) throws IOException {
        return ClassLoader.getSystemResources(resName);
    }

    public static InputStream getSystemResourceAsStream(String resName) {
        return ClassLoader.getSystemResourceAsStream(resName);
    }

    @Override
    public URL getResource(String resName) {
        return delegate.getResource(resName);
    }

    @Override
    public Enumeration<URL> getResources(String resName) throws IOException {
        return delegate.getResources(resName);
    }

    @Override
    public InputStream getResourceAsStream(String resName) {
        return delegate.getResourceAsStream(resName);
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return delegate.loadClass(className);
    }

    @Override
    public void setClassAssertionStatus(String cname, boolean enable) {
        delegate.setClassAssertionStatus(cname, enable);
    }

    @Override
    public void setPackageAssertionStatus(String pname, boolean enable) {
        delegate.setPackageAssertionStatus(pname, enable);
    }

    @Override
    public void setDefaultAssertionStatus(boolean enable) {
        delegate.setDefaultAssertionStatus(enable);
    }

    @Override
    public void clearAssertionStatus() {
        delegate.clearAssertionStatus();
    }
}