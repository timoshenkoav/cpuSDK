package com.tunebrains.cpu.library.dex;


import dalvik.system.PathClassLoader;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Alexandr Timoshenko <thick.tav@gmail.com> on 5/31/16.
 */
public class MultipleClassLoader extends ClassLoader {
    private List<ClassLoader> parent;
    private PathClassLoader mDex;

    public MultipleClassLoader(ClassLoader... pParent) {
        super(pParent[0]);
        parent = new LinkedList<>();
        parent.addAll(Arrays.asList(pParent));
    }


    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (name.startsWith("android.support.")) {
            throw new ClassNotFoundException(name);
        }
        if (name.equals("com.northghost.dropbox.plugin.view.CustomRadioButton")) {
            throw new ClassNotFoundException(name);
        }
        for (ClassLoader lClassLoader : parent) {
            try {
                return lClassLoader.loadClass(name);
            } catch (ClassNotFoundException cne) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    public void setDex(PathClassLoader pDex) {
        mDex = pDex;
    }

    public void add(PathClassLoader pDex) {
        parent.add(pDex);
    }
}