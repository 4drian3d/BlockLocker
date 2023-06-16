package nl.rutgerkok.blocklocker.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Horrible reflection code to access the Folia scheduler if we're on Folia.
 *
 * <p>
 * I guess the "best" solution would be to create a submodule that can access
 * Folia code. However, that complicates the project setup. I guess at some
 * point Paper will offer a better solution, so that you don't need two code
 * paths.
 *
 * <p>
 * Some Java purists will say that you should separate this class into three: an
 * interface, and two implementations (Folia and Bukkit). However, since the
 * reflection code is quite unreadable, I think it's better to keep the
 * reference Bukkit code close by.
 */
final class SchedulerSupport {

    private static MethodHandle getRegionScheduler;
    private static MethodHandle getGlobalRegionScheduler;
    private static MethodHandle getAsyncScheduler;

    private static MethodHandle runDelayedOnGlobal;
    private static MethodHandle runDelayedOnRegion;
    private static MethodHandle runOnRegion;
    private static MethodHandle runAtFixedRateAsync;

    static {
        try {
            final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            final MethodType objectReturn = MethodType.methodType(Object.class);
            getRegionScheduler = lookup.findVirtual(Server.class, "getRegionScheduler", objectReturn);
            getGlobalRegionScheduler = lookup.findVirtual(Server.class, "getGlobalRegionScheduler", objectReturn);
            getAsyncScheduler = lookup.findVirtual(Server.class, "getAsyncScheduler", objectReturn);

            final Class<?> globalRegionScheduler = getGlobalRegionScheduler.type().returnType();
            final Class<?> regionScheduler = getRegionScheduler.type().returnType();
            final Class<?> asyncScheduler = getAsyncScheduler.type().returnType();

            final MethodType runDelayedOnGlobalType = MethodType.methodType(
                    Object.class, Plugin.class, Consumer.class, long.class);
            runDelayedOnGlobal = lookup.findVirtual(globalRegionScheduler, "runDelayed", runDelayedOnGlobalType);
            final MethodType runDelayedOnRegionType = MethodType.methodType(
                    Object.class, Plugin.class, World.class, int.class, int.class, Consumer.class, long.class);
            runDelayedOnRegion = lookup.findVirtual(regionScheduler, "runDelayed", runDelayedOnRegionType);
            final MethodType runOnRegionType = MethodType.methodType(
                    Object.class, Plugin.class, Location.class, Consumer.class);
            runOnRegion = lookup.findVirtual(regionScheduler, "run", runOnRegionType);
            final MethodType runAtFixedRateAsyncType = MethodType.methodType(
                    Object.class, Plugin.class, Consumer.class, long.class, long.class, TimeUnit.class);
            runAtFixedRateAsync = lookup.findVirtual(asyncScheduler, "runAtFixedRate", runAtFixedRateAsyncType);
            folia = true;
        } catch (IllegalAccessException | NoSuchMethodException e) {
            folia = false;
        }
    }

    private static boolean folia;

    /**
     * Used to invoke a parameterless instance method.
     *
     * @param on
     *            The instance.
     * @param name
     *            Name of the method.
     * @return Return value of the method (null for void methods).
     */
    private static Object invoke(Object on, String name) {
        try {
            return on.getClass().getMethod(name).invoke(on);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                | SecurityException e) {
            throw new RuntimeException("Cannot invoke instance." + name + "()", e);
        }
    }

    private final Plugin plugin;

    SchedulerSupport(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void runLater(Block block, Runnable runnable) {
        if (folia) {
            try {
                final Object regionScheduler = getRegionScheduler.invoke(plugin.getServer());
                final Consumer<?> consumer = task -> runnable.run();
                runOnRegion.invoke(regionScheduler, this.plugin, block.getLocation(), consumer);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            plugin.getServer().getScheduler().runTask(plugin, runnable);
        }
    }

    public void runLater(Block block, Runnable runnable, int ticks) {
        if (folia) {
            try {
                final Object regionScheduler = getRegionScheduler.invoke(plugin.getServer());
                final Consumer<?> consumer = task -> runnable.run();
                runDelayedOnRegion.invoke(regionScheduler, plugin, block
                        .getWorld(), block.getX() >> 4, block.getZ() >> 4, consumer, ticks);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, runnable, ticks);
        }
    }

    void runLaterGlobally(Runnable runnable, int ticks) {
        if (folia) {
            try {
                final Object globalRegionScheduler = getGlobalRegionScheduler.invoke(plugin.getServer());
                final Consumer<?> consumer = task -> runnable.run();
                runDelayedOnGlobal.invoke(globalRegionScheduler, plugin, consumer, ticks);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, runnable, ticks);
        }
    }

    void runTimerAsync(Consumer<BukkitTask> task, long checkInterval) {
        if (folia) {
            try {
                Object asyncScheduler = getAsyncScheduler.invoke(plugin.getServer());

                Consumer<?> consumer = foliaTask -> {
                    task.accept(new BukkitTask() {

                        @Override
                        public void cancel() {
                            invoke(foliaTask, "cancel");
                        }

                        @Override
                        public Plugin getOwner() {
                            return (Plugin) invoke(foliaTask, "getOwningPlugin");
                        }

                        @Override
                        public int getTaskId() {
                            throw new UnsupportedOperationException();
                        }

                        @Override
                        public boolean isCancelled() {
                            return (Boolean) invoke(foliaTask, "isCancelled");
                        }

                        @Override
                        public boolean isSync() {
                            return false;
                        }
                    });
                };
                runAtFixedRateAsync
                        .invoke(asyncScheduler, plugin, consumer, 1, checkInterval * 50, TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, task, 1, checkInterval);
        }
    }
}
