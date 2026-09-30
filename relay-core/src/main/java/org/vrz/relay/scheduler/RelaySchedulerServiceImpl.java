package org.vrz.relay.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.vrz.relay.api.scheduler.RelayTask;
import org.vrz.relay.api.scheduler.SchedulerService;
import org.vrz.relay.api.scheduler.TaskExecutionType;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class RelaySchedulerServiceImpl implements SchedulerService {

    private final boolean folia;
    private final Map<Plugin, Set<RelayTaskImpl>> pluginTasks = new ConcurrentHashMap<>();

    // Mock/offline fallback executor for unit testing or when Bukkit server is absent
    private final ScheduledExecutorService testExecutor;

    public RelaySchedulerServiceImpl() {
        this.folia = detectFolia();
        if (Bukkit.getServer() == null) {
            this.testExecutor = Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "relay-test-scheduler");
                t.setDaemon(true);
                return t;
            });
        } else {
            this.testExecutor = null;
        }
    }

    private static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean isFolia() {
        return folia;
    }

    private void trackTask(@NotNull RelayTaskImpl task) {
        pluginTasks.computeIfAbsent(task.getPlugin(), k -> ConcurrentHashMap.newKeySet()).add(task);
    }

    private void untrackTask(@NotNull RelayTaskImpl task) {
        Set<RelayTaskImpl> tasks = pluginTasks.get(task.getPlugin());
        if (tasks != null) {
            tasks.remove(task);
        }
    }

    // ==========================================
    // Entity Scheduling
    // ==========================================

    @Override
    @NotNull
    public RelayTask runFor(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task) {
        return runForLater(plugin, entity, task, 1L);
    }

    @Override
    @NotNull
    public RelayTask runForLater(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(entity, "entity cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        AtomicReference<RelayTaskImpl> taskRef = new AtomicReference<>();
        AtomicReference<Runnable> cancelAction = new AtomicReference<>(() -> {});

        RelayTaskImpl relayTask = new RelayTaskImpl(
                plugin,
                TaskExecutionType.ENTITY,
                false,
                () -> cancelAction.get().run(),
                this::untrackTask
        );
        taskRef.set(relayTask);
        trackTask(relayTask);

        if (testExecutor != null) {
            ScheduledFuture<?> future = testExecutor.schedule(() -> {
                if (!relayTask.isCancelled()) {
                    task.run();
                    relayTask.markCompleted();
                }
            }, Math.max(1, delayTicks) * 50L, TimeUnit.MILLISECONDS);
            cancelAction.set(() -> future.cancel(false));
            return relayTask;
        }

        if (folia) {
            ScheduledTask st = entity.getScheduler().runDelayed(
                    plugin,
                    t -> {
                        if (!relayTask.isCancelled()) {
                            task.run();
                            relayTask.markCompleted();
                        }
                    },
                    relayTask::cancel,
                    Math.max(1L, delayTicks)
            );
            if (st != null) {
                cancelAction.set(st::cancel);
            }
        } else {
            BukkitTask bt = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!relayTask.isCancelled() && entity.isValid()) {
                    task.run();
                    relayTask.markCompleted();
                }
            }, Math.max(1L, delayTicks));
            cancelAction.set(bt::cancel);
        }

        return relayTask;
    }

    @Override
    @NotNull
    public RelayTask runForRepeating(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Runnable task, long delayTicks, long periodTicks) {
        return runForRepeating(plugin, entity, t -> task.run(), delayTicks, periodTicks);
    }

    @Override
    @NotNull
    public RelayTask runForRepeating(@NotNull Plugin plugin, @NotNull Entity entity, @NotNull Consumer<RelayTask> task, long delayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(entity, "entity cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        AtomicReference<RelayTaskImpl> taskRef = new AtomicReference<>();
        AtomicReference<Runnable> cancelAction = new AtomicReference<>(() -> {});

        RelayTaskImpl relayTask = new RelayTaskImpl(
                plugin,
                TaskExecutionType.ENTITY,
                true,
                () -> cancelAction.get().run(),
                this::untrackTask
        );
        taskRef.set(relayTask);
        trackTask(relayTask);

        if (testExecutor != null) {
            ScheduledFuture<?> future = testExecutor.scheduleAtFixedRate(() -> {
                if (!relayTask.isCancelled()) {
                    task.accept(relayTask);
                }
            }, Math.max(1, delayTicks) * 50L, Math.max(1, periodTicks) * 50L, TimeUnit.MILLISECONDS);
            cancelAction.set(() -> future.cancel(false));
            return relayTask;
        }

        if (folia) {
            ScheduledTask st = entity.getScheduler().runAtFixedRate(
                    plugin,
                    t -> {
                        if (!relayTask.isCancelled()) {
                            task.accept(relayTask);
                        } else {
                            t.cancel();
                        }
                    },
                    relayTask::cancel,
                    Math.max(1L, delayTicks),
                    Math.max(1L, periodTicks)
            );
            if (st != null) {
                cancelAction.set(st::cancel);
            }
        } else {
            BukkitTask bt = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!relayTask.isCancelled() && entity.isValid()) {
                    task.accept(relayTask);
                } else {
                    relayTask.cancel();
                }
            }, Math.max(1L, delayTicks), Math.max(1L, periodTicks));
            cancelAction.set(bt::cancel);
        }

        return relayTask;
    }

    // ==========================================
    // Region Scheduling (Location)
    // ==========================================

    @Override
    @NotNull
    public RelayTask runAt(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task) {
        return runAtLater(plugin, location, task, 1L);
    }

    @Override
    @NotNull
    public RelayTask runAtLater(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        AtomicReference<Runnable> cancelAction = new AtomicReference<>(() -> {});
        RelayTaskImpl relayTask = new RelayTaskImpl(
                plugin,
                TaskExecutionType.REGION,
                false,
                () -> cancelAction.get().run(),
                this::untrackTask
        );
        trackTask(relayTask);

        if (testExecutor != null) {
            ScheduledFuture<?> future = testExecutor.schedule(() -> {
                if (!relayTask.isCancelled()) {
                    task.run();
                    relayTask.markCompleted();
                }
            }, Math.max(1, delayTicks) * 50L, TimeUnit.MILLISECONDS);
            cancelAction.set(() -> future.cancel(false));
            return relayTask;
        }

        if (folia && location.getWorld() != null) {
            ScheduledTask st = Bukkit.getRegionScheduler().runDelayed(plugin, location, t -> {
                if (!relayTask.isCancelled()) {
                    task.run();
                    relayTask.markCompleted();
                }
            }, Math.max(1L, delayTicks));
            cancelAction.set(st::cancel);
        } else {
            BukkitTask bt = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!relayTask.isCancelled()) {
                    task.run();
                    relayTask.markCompleted();
                }
            }, Math.max(1L, delayTicks));
            cancelAction.set(bt::cancel);
        }

        return relayTask;
    }

    @Override
    @NotNull
    public RelayTask runAtRepeating(@NotNull Plugin plugin, @NotNull Location location, @NotNull Runnable task, long delayTicks, long periodTicks) {
        return runAtRepeating(plugin, location, t -> task.run(), delayTicks, periodTicks);
    }

    @Override
    @NotNull
    public RelayTask runAtRepeating(@NotNull Plugin plugin, @NotNull Location location, @NotNull Consumer<RelayTask> task, long delayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        AtomicReference<Runnable> cancelAction = new AtomicReference<>(() -> {});
        RelayTaskImpl relayTask = new RelayTaskImpl(
                plugin,
                TaskExecutionType.REGION,
                true,
                () -> cancelAction.get().run(),
                this::untrackTask
        );
        trackTask(relayTask);

        if (testExecutor != null) {
            ScheduledFuture<?> future = testExecutor.scheduleAtFixedRate(() -> {
                if (!relayTask.isCancelled()) {
                    task.accept(relayTask);
                }
            }, Math.max(1, delayTicks) * 50L, Math.max(1, periodTicks) * 50L, TimeUnit.MILLISECONDS);
            cancelAction.set(() -> future.cancel(false));
            return relayTask;
        }

        if (folia && location.getWorld() != null) {
            ScheduledTask st = Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, t -> {
                if (!relayTask.isCancelled()) {
                    task.accept(relayTask);
                } else {
                    t.cancel();
                }
            }, Math.max(1L, delayTicks), Math.max(1L, periodTicks));
            cancelAction.set(st::cancel);
        } else {
            BukkitTask bt = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!relayTask.isCancelled()) {
                    task.accept(relayTask);
                } else {
                    relayTask.cancel();
                }
            }, Math.max(1L, delayTicks), Math.max(1L, periodTicks));
            cancelAction.set(bt::cancel);
        }

        return relayTask;
    }

    // ==========================================
    // Global Region Scheduling
    // ==========================================

    @Override
    @NotNull
    public RelayTask runGlobal(@NotNull Plugin plugin, @NotNull Runnable task) {
        return runGlobalLater(plugin, task, 1L);
    }

    @Override
    @NotNull
    public RelayTask runGlobalLater(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        AtomicReference<Runnable> cancelAction = new AtomicReference<>(() -> {});
        RelayTaskImpl relayTask = new RelayTaskImpl(
                plugin,
                TaskExecutionType.GLOBAL,
                false,
                () -> cancelAction.get().run(),
                this::untrackTask
        );
        trackTask(relayTask);

        if (testExecutor != null) {
            ScheduledFuture<?> future = testExecutor.schedule(() -> {
                if (!relayTask.isCancelled()) {
                    task.run();
                    relayTask.markCompleted();
                }
            }, Math.max(1, delayTicks) * 50L, TimeUnit.MILLISECONDS);
            cancelAction.set(() -> future.cancel(false));
            return relayTask;
        }

        if (folia) {
            ScheduledTask st = Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> {
                if (!relayTask.isCancelled()) {
                    task.run();
                    relayTask.markCompleted();
                }
            }, Math.max(1L, delayTicks));
            cancelAction.set(st::cancel);
        } else {
            BukkitTask bt = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!relayTask.isCancelled()) {
                    task.run();
                    relayTask.markCompleted();
                }
            }, Math.max(1L, delayTicks));
            cancelAction.set(bt::cancel);
        }

        return relayTask;
    }

    @Override
    @NotNull
    public RelayTask runGlobalRepeating(@NotNull Plugin plugin, @NotNull Runnable task, long delayTicks, long periodTicks) {
        return runGlobalRepeating(plugin, t -> task.run(), delayTicks, periodTicks);
    }

    @Override
    @NotNull
    public RelayTask runGlobalRepeating(@NotNull Plugin plugin, @NotNull Consumer<RelayTask> task, long delayTicks, long periodTicks) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(task, "task cannot be null");

        AtomicReference<Runnable> cancelAction = new AtomicReference<>(() -> {});
        RelayTaskImpl relayTask = new RelayTaskImpl(
                plugin,
                TaskExecutionType.GLOBAL,
                true,
                () -> cancelAction.get().run(),
                this::untrackTask
        );
        trackTask(relayTask);

        if (testExecutor != null) {
            ScheduledFuture<?> future = testExecutor.scheduleAtFixedRate(() -> {
                if (!relayTask.isCancelled()) {
                    task.accept(relayTask);
                }
            }, Math.max(1, delayTicks) * 50L, Math.max(1, periodTicks) * 50L, TimeUnit.MILLISECONDS);
            cancelAction.set(() -> future.cancel(false));
            return relayTask;
        }

        if (folia) {
            ScheduledTask st = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> {
                if (!relayTask.isCancelled()) {
                    task.accept(relayTask);
                } else {
                    t.cancel();
                }
            }, Math.max(1L, delayTicks), Math.max(1L, periodTicks));
            cancelAction.set(st::cancel);
        } else {
            BukkitTask bt = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!relayTask.isCancelled()) {
                    task.accept(relayTask);
                } else {
                    relayTask.cancel();
                }
            }, Math.max(1L, delayTicks), Math.max(1L, periodTicks));
            cancelAction.set(bt::cancel);
        }

        return relayTask;
    }

    // ==========================================
    // Asynchronous Worker Scheduling
    // ==========================================

    @Override
    @NotNull
    public RelayTask runAsync(@NotNull Plugin plugin, @NotNull Runnable task) {
        return runAsyncLater(plugin, task, 0L, TimeUnit.MILLISECONDS);
    }

    @Override
    @NotNull
    public RelayTask runAsyncLater(@NotNull Plugin plugin, @NotNull Runnable task, long delay, @NotNull TimeUnit timeUnit) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(task, "task cannot be null");
        Objects.requireNonNull(timeUnit, "timeUnit cannot be null");

        AtomicReference<Runnable> cancelAction = new AtomicReference<>(() -> {});
        RelayTaskImpl relayTask = new RelayTaskImpl(
                plugin,
                TaskExecutionType.ASYNC,
                false,
                () -> cancelAction.get().run(),
                this::untrackTask
        );
        trackTask(relayTask);

        if (testExecutor != null) {
            ScheduledFuture<?> future = testExecutor.schedule(() -> {
                if (!relayTask.isCancelled()) {
                    task.run();
                    relayTask.markCompleted();
                }
            }, delay, timeUnit);
            cancelAction.set(() -> future.cancel(false));
            return relayTask;
        }

        if (folia) {
            ScheduledTask st = delay <= 0
                    ? Bukkit.getAsyncScheduler().runNow(plugin, t -> {
                        if (!relayTask.isCancelled()) {
                            task.run();
                            relayTask.markCompleted();
                        }
                    })
                    : Bukkit.getAsyncScheduler().runDelayed(plugin, t -> {
                        if (!relayTask.isCancelled()) {
                            task.run();
                            relayTask.markCompleted();
                        }
                    }, delay, timeUnit);
            cancelAction.set(st::cancel);
        } else {
            long ticks = Math.max(1L, timeUnit.toMillis(delay) / 50L);
            BukkitTask bt = delay <= 0
                    ? Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        if (!relayTask.isCancelled()) {
                            task.run();
                            relayTask.markCompleted();
                        }
                    })
                    : Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
                        if (!relayTask.isCancelled()) {
                            task.run();
                            relayTask.markCompleted();
                        }
                    }, ticks);
            cancelAction.set(bt::cancel);
        }

        return relayTask;
    }

    @Override
    @NotNull
    public RelayTask runAsyncRepeating(@NotNull Plugin plugin, @NotNull Runnable task, long delay, long period, @NotNull TimeUnit timeUnit) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Objects.requireNonNull(task, "task cannot be null");
        Objects.requireNonNull(timeUnit, "timeUnit cannot be null");

        AtomicReference<Runnable> cancelAction = new AtomicReference<>(() -> {});
        RelayTaskImpl relayTask = new RelayTaskImpl(
                plugin,
                TaskExecutionType.ASYNC,
                true,
                () -> cancelAction.get().run(),
                this::untrackTask
        );
        trackTask(relayTask);

        if (testExecutor != null) {
            ScheduledFuture<?> future = testExecutor.scheduleAtFixedRate(() -> {
                if (!relayTask.isCancelled()) {
                    task.run();
                }
            }, delay, period, timeUnit);
            cancelAction.set(() -> future.cancel(false));
            return relayTask;
        }

        if (folia) {
            ScheduledTask st = Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> {
                if (!relayTask.isCancelled()) {
                    task.run();
                } else {
                    t.cancel();
                }
            }, delay, period, timeUnit);
            cancelAction.set(st::cancel);
        } else {
            long delayTicks = Math.max(1L, timeUnit.toMillis(delay) / 50L);
            long periodTicks = Math.max(1L, timeUnit.toMillis(period) / 50L);
            BukkitTask bt = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                if (!relayTask.isCancelled()) {
                    task.run();
                } else {
                    relayTask.cancel();
                }
            }, delayTicks, periodTicks);
            cancelAction.set(bt::cancel);
        }

        return relayTask;
    }

    @Override
    @NotNull
    public CompletableFuture<Void> runAsyncPromise(@NotNull Runnable task) {
        Objects.requireNonNull(task, "task cannot be null");
        return CompletableFuture.runAsync(task);
    }

    @Override
    @NotNull
    public <T> CompletableFuture<T> supplyAsyncPromise(@NotNull Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        return CompletableFuture.supplyAsync(supplier);
    }

    // ==========================================
    // Thread Verification Checks
    // ==========================================

    @Override
    public boolean isEntityThread(@NotNull Entity entity) {
        if (Bukkit.getServer() == null) {
            return false;
        }
        if (folia) {
            try {
                return (boolean) Bukkit.getServer().getClass().getMethod("isOwnedByCurrentRegion", Entity.class).invoke(Bukkit.getServer(), entity);
            } catch (Throwable ignored) {
                return false;
            }
        }
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isRegionThread(@NotNull Location location) {
        if (Bukkit.getServer() == null) {
            return false;
        }
        if (folia && location.getWorld() != null) {
            try {
                return (boolean) Bukkit.getServer().getClass().getMethod("isOwnedByCurrentRegion", Location.class).invoke(Bukkit.getServer(), location);
            } catch (Throwable ignored) {
                return false;
            }
        }
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isGlobalThread() {
        if (Bukkit.getServer() == null) {
            return false;
        }
        if (folia) {
            try {
                return (boolean) Bukkit.getServer().getClass().getMethod("isGlobalTickThread").invoke(Bukkit.getServer());
            } catch (Throwable ignored) {
                return false;
            }
        }
        return Bukkit.isPrimaryThread();
    }

    // ==========================================
    // Lifecycle Management (Zero-Leak)
    // ==========================================

    @Override
    public void cancelAll(@NotNull Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin cannot be null");
        Set<RelayTaskImpl> tasks = pluginTasks.remove(plugin);
        if (tasks != null) {
            for (RelayTaskImpl task : tasks) {
                task.cancel();
            }
            tasks.clear();
        }
    }

    @Override
    public int getActiveTaskCount() {
        return pluginTasks.values().stream().mapToInt(Set::size).sum();
    }

    @Override
    public int getActiveTaskCount(@NotNull Plugin plugin) {
        Set<RelayTaskImpl> tasks = pluginTasks.get(plugin);
        return tasks != null ? tasks.size() : 0;
    }

    public void shutdown() {
        for (Set<RelayTaskImpl> tasks : pluginTasks.values()) {
            for (RelayTaskImpl task : tasks) {
                task.cancel();
            }
            tasks.clear();
        }
        pluginTasks.clear();

        if (testExecutor != null) {
            testExecutor.shutdownNow();
        }
    }
}
