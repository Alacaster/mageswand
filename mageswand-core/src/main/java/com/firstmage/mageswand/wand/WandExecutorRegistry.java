package com.firstmage.mageswand.wand;

import com.firstmage.mageswand.MagesWand;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

public final class WandExecutorRegistry implements AutoCloseable {
    private final MagesWand plugin;
    private final Map<String, LoadedExecutorClass> knownExecutorClasses = new LinkedHashMap<>();
    private URLClassLoader externalClassLoader;

    public WandExecutorRegistry(MagesWand plugin) {
        this.plugin = plugin;
    }

    public synchronized LoadReport reload() {
        closeExternalClassLoader();
        this.knownExecutorClasses.clear();

        List<String> issues = new ArrayList<>();
        Path directory = this.plugin.getSpellPackDirectory();

        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create spell directory: " + directory, exception);
        }

        List<Path> jars;
        try (var stream = Files.list(directory)) {
            jars = stream
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".jar"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to list spell jars in " + directory, exception);
        }

        if (!jars.isEmpty()) {
            try {
                URL[] urls = jars.stream().map(this::toUrl).toArray(URL[]::new);
                this.externalClassLoader = new URLClassLoader(urls, this.plugin.getClass().getClassLoader());
            } catch (RuntimeException exception) {
                closeExternalClassLoader();
                throw exception;
            }
        }

        int scannedJars = 0;
        for (Path jar : jars) {
            scannedJars++;
            scanJar(jar, issues);
        }

        return new LoadReport(scannedJars, countExternalExecutorClasses(), List.copyOf(issues));
    }

    public synchronized WandExecutor createExecutor(String className) {
        if (className == null || className.isBlank()) {
            return null;
        }

        Class<? extends WandExecutor> executorClass = findExecutorClass(className);
        if (executorClass == null) {
            return null;
        }

        try {
            return executorClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate executor " + className, exception);
        }
    }

    public synchronized String describeClassSource(String className) {
        LoadedExecutorClass loaded = this.knownExecutorClasses.get(className);
        return loaded == null ? "unknown" : loaded.source();
    }

    public synchronized List<LoadedExecutorClass> getKnownExecutorClasses() {
        return this.knownExecutorClasses.values().stream()
                .sorted(Comparator.comparing(LoadedExecutorClass::className))
                .toList();
    }

    @Override
    public synchronized void close() {
        closeExternalClassLoader();
        this.knownExecutorClasses.clear();
    }

    private Class<? extends WandExecutor> findExecutorClass(String className) {
        if (this.externalClassLoader == null) {
            return null;
        }

        LoadedExecutorClass known = this.knownExecutorClasses.get(className);
        if (known == null) {
            return null;
        }

        return loadExecutorClass(className, this.externalClassLoader);
    }

    private void scanJar(Path jar, List<String> issues) {
        if (this.externalClassLoader == null) {
            return;
        }

        int discovered = 0;
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                if (!name.endsWith(".class") || name.equals("module-info.class") || name.contains("$") ) {
                    continue;
                }

                String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                Class<? extends WandExecutor> executorClass = loadExecutorClass(className, this.externalClassLoader);
                if (executorClass == null) {
                    continue;
                }

                this.knownExecutorClasses.putIfAbsent(className,
                        new LoadedExecutorClass(className, jar.getFileName().toString()));
                discovered++;
            }
        } catch (IOException exception) {
            issues.add("Failed to scan spell jar " + jar.getFileName() + ": " + exception.getMessage());
            this.plugin.getLogger().log(Level.WARNING, "Failed to scan spell jar " + jar.getFileName(), exception);
            return;
        }

        if (discovered == 0) {
            issues.add("Spell jar " + jar.getFileName() + " did not expose any concrete WandExecutor classes.");
        }
    }

    @SuppressWarnings("unchecked")
    private Class<? extends WandExecutor> loadExecutorClass(String className, ClassLoader classLoader) {
        if (classLoader == null) {
            return null;
        }

        try {
            Class<?> rawClass = Class.forName(className, false, classLoader);
            if (!WandExecutor.class.isAssignableFrom(rawClass)) {
                return null;
            }
            if (rawClass.isInterface() || Modifier.isAbstract(rawClass.getModifiers())) {
                return null;
            }
            return (Class<? extends WandExecutor>) rawClass.asSubclass(WandExecutor.class);
        } catch (ClassNotFoundException | LinkageError exception) {
            return null;
        }
    }

    private int countExternalExecutorClasses() {
        int count = 0;
        for (LoadedExecutorClass loaded : this.knownExecutorClasses.values()) {
            count++;
        }
        return count;
    }

    private URL toUrl(Path path) {
        try {
            return path.toUri().toURL();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to convert path to URL: " + path, exception);
        }
    }

    private void closeExternalClassLoader() {
        if (this.externalClassLoader == null) {
            return;
        }

        try {
            this.externalClassLoader.close();
        } catch (IOException exception) {
            this.plugin.getLogger().log(Level.WARNING, "Failed to close spell jar class loader.", exception);
        }
        this.externalClassLoader = null;
    }

    public record LoadedExecutorClass(String className, String source) {
    }

    public record LoadReport(int loadedJars, int discoveredExternalExecutorClasses, List<String> issues) {
    }

}
