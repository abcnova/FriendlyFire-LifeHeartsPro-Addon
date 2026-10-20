package io.github.abcnova.friendlyfire;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/** Binds the public LifeHeartsPro team API once, outside the damage-event path. */
final class TeamResolver {
    private final FriendlyFirePlugin plugin;
    private final MethodHandle getTeam;
    private final MethodHandle getTeams;
    private final MethodHandle getTeamSize;
    private final AtomicBoolean warned = new AtomicBoolean();

    TeamResolver(FriendlyFirePlugin plugin, Plugin lifeHearts) throws ReflectiveOperationException {
        this.plugin = plugin;
        Method managerGetter = lifeHearts.getClass().getMethod("getTeamManager");
        Object manager = managerGetter.invoke(lifeHearts);
        if (manager == null) {
            throw new IllegalStateException("LifeHeartsPro returned no team manager");
        }
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        getTeam = bind(lookup, manager, "getTeam", MethodType.methodType(String.class, Player.class),
                MethodType.methodType(Object.class, Player.class));
        getTeams = bind(lookup, manager, "getTeams", MethodType.methodType(Set.class),
                MethodType.methodType(Object.class));
        getTeamSize = bind(lookup, manager, "getTeamSize", MethodType.methodType(int.class, String.class),
                MethodType.methodType(Object.class, String.class));
    }

    private static MethodHandle bind(MethodHandles.Lookup lookup, Object target, String name,
                                     MethodType source, MethodType adapted) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(name, source.parameterArray());
        return lookup.unreflect(method).bindTo(target).asType(adapted);
    }

    TeamMatch resolve(Player attacker, Player victim) {
        try {
            String attackerTeam = normalize((String) getTeam.invokeExact((Object) attacker));
            String victimTeam = normalize((String) getTeam.invokeExact((Object) victim));
            boolean same = attackerTeam != null && victimTeam != null && attackerTeam.equalsIgnoreCase(victimTeam);
            return new TeamMatch(true, same, attackerTeam, victimTeam);
        } catch (Throwable error) {
            warnOnce("Could not read team membership from LifeHeartsPro", error);
            return new TeamMatch(false, false, null, null);
        }
    }

    Set<String> knownTeams() {
        try {
            Object value = (Object) getTeams.invokeExact();
            if (!(value instanceof Iterable<?> iterable)) return Set.of();
            LinkedHashSet<String> result = new LinkedHashSet<>();
            for (Object team : iterable) {
                if (team != null && !team.toString().isBlank()) result.add(team.toString());
            }
            return result;
        } catch (Throwable error) {
            warnOnce("Could not list LifeHeartsPro teams", error);
            return Set.of();
        }
    }

    int teamSize(String team) {
        try {
            Object value = (Object) getTeamSize.invokeExact((Object) team);
            return value instanceof Number number ? number.intValue() : 0;
        } catch (Throwable error) {
            warnOnce("Could not read a LifeHeartsPro team size", error);
            return 0;
        }
    }

    private void warnOnce(String message, Throwable error) {
        if (warned.compareAndSet(false, true)) {
            plugin.getLogger().severe(message + "; team protection is failing closed: " + rootMessage(error));
        }
    }

    private static String rootMessage(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getClass().getSimpleName() + ": " + String.valueOf(cause.getMessage());
    }

    private static String normalize(String team) {
        if (team == null || team.isBlank()) return null;
        return team.trim();
    }

    record TeamMatch(boolean available, boolean sameTeam, String attackerTeam, String victimTeam) { }
}
