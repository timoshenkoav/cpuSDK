package com.tunebrains.cpu.library.dex;

import android.content.Context;
import com.tunebrains.cpu.dexlibrary.BaseCommand;
import dalvik.system.DexClassLoader;

import java.io.File;
import java.lang.reflect.Constructor;

/**
 * Created by xElvis89x on 5/20/2016.
 */
public class DexPluginLoader {

    public static BaseCommand loadCommand(final Context context, String dexfile, String className, File root) {

        try {
            File optimizedDexOutputPath = context.getDir("commandsDex", Context.MODE_PRIVATE);
            DexClassLoader cl = new DexClassLoader(dexfile,
                    optimizedDexOutputPath.getAbsolutePath(),
                    null,
                    new MultipleClassLoader(context.getClassLoader(), context.getClassLoader()));

            Class<?> plugin = cl.loadClass(className);
            Constructor lConstructor = plugin.getConstructor(Context.class, File.class);
            BaseCommand result = (BaseCommand) lConstructor.newInstance(context, root);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}