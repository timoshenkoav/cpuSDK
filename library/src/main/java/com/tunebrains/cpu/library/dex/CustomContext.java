package com.tunebrains.cpu.library.dex;

import android.content.Context;
import android.content.ContextWrapper;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by Alexandr Timoshenko <thick.tav@gmail.com> on 6/30/16.
 */
public class CustomContext extends ContextWrapper {
    private final Context mApplicationContext;
    private final ClassLoader[] mClassLoaders;

    public CustomContext(Context base, Context pApplicationContext, ClassLoader... pClassLoaders) {
        super(base);
        mApplicationContext = pApplicationContext;
        mClassLoaders = pClassLoaders;
    }

    @Override
    public ClassLoader getClassLoader() {
        return super.getClassLoader();
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public Object getSystemService(String name) {
        if (Context.LAYOUT_INFLATER_SERVICE.equals(name)) {
            LayoutInflater lLayoutInflater =
                    LayoutInflater.from(getBaseContext()).cloneInContext(getBaseContext());

            lLayoutInflater.setFactory(new LayoutInflater.Factory() {
                @Override
                public View onCreateView(String name, Context context, AttributeSet attrs) {
                    Class lClass = null;

                    for (ClassLoader lClassLoader : mClassLoaders) {
                        try {
                            lClass = lClassLoader.loadClass(name);
                        } catch (ClassNotFoundException pE) {
                        }
                    }
                    if (lClass != null) {
                        try {
                            Constructor<View> lConstructor = lClass.getConstructor(Context.class, AttributeSet.class);
                            View lView = lConstructor.newInstance(context, attrs);
                            return lView;
                        } catch (NoSuchMethodException pE) {

                        } catch (IllegalAccessException pE) {

                        } catch (InstantiationException pE) {

                        } catch (InvocationTargetException pE) {

                        }
                    }

                    return null;
                }
            });
            return lLayoutInflater;

        }
        return super.getSystemService(name);
    }
}