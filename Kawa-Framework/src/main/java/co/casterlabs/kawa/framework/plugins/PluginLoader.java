package co.casterlabs.kawa.framework.plugins;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import co.casterlabs.kawa.framework.KawaPlugin;
import co.casterlabs.kawa.framework.KawaPluginImplementation;
import lombok.NonNull;
import xyz.e3ndr.reflectionlib.helpers.AccessHelper;

public class PluginLoader {

    @SuppressWarnings("deprecation")
    public static void loadFile(@NonNull File file) throws IOException, ClassNotFoundException {
        if (!file.isFile()) {
            throw new IOException("Target plugin must be a valid file");
        }

        URLClassLoader classLoader = GlobalClassLoader.create(file.toURI().toURL());
        List<Class<?>> types = new LinkedList<>();

        // Forcefully load all class files.
        // Reflections sux and doesn't work reliably enough.
        {
            JarFile jarFile = new JarFile(file);
            Enumeration<JarEntry> e = jarFile.entries();

            while (e.hasMoreElements()) {
                JarEntry je = e.nextElement();

                if (je.isDirectory() || !je.getName().endsWith(".class")) {
                    continue;
                }

                // Transform the file name into a class name.
                String className = je
                    .getName()
                    .substring(0, je.getName().length() - ".class".length())
                    .replace('/', '.');

                Class<?> c = classLoader.loadClass(className);

                if (c.isAnnotationPresent(KawaPluginImplementation.class)) {
                    types.add(c);
                }
            }

            jarFile.close();
        }

        if (types.isEmpty()) {
            classLoader.close();
            classLoader = null;

            throw new IOException("No implementations are present");
        }

        for (Class<?> clazz : types) {
            if (!KawaPlugin.class.isAssignableFrom(clazz)) continue;

            try {
                KawaPlugin plugin = (KawaPlugin) clazz.newInstance();
                ServiceLoader<java.sql.Driver> sqlDrivers = ServiceLoader.load(java.sql.Driver.class, classLoader);

                Field classLoaderField = KawaPlugin.class.getDeclaredField("classLoader");
                Field sqlDriversField = KawaPlugin.class.getDeclaredField("sqlDrivers");

                AccessHelper.makeAccessible(classLoaderField);
                AccessHelper.makeAccessible(sqlDriversField);

                classLoaderField.set(plugin, classLoader);
                sqlDriversField.set(plugin, sqlDrivers);

                // Load them in
                for (java.sql.Driver driver : sqlDrivers) {
                    driver.getClass().toString();
                }

                plugin.onLoad();
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | SecurityException | NoSuchFieldException e) {
                throw new IOException("Unable to load plugin", e);
            }
        }
    }

}
