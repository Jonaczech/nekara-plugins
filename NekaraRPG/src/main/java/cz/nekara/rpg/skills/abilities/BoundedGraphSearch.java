package cz.nekara.rpg.skills.abilities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class BoundedGraphSearch {
    private BoundedGraphSearch() {
    }

    public static <T> List<T> connected(
        T source,
        int maximumResults,
        Function<T, ? extends Iterable<T>> neighbours,
        Predicate<T> accepted
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(neighbours, "neighbours");
        Objects.requireNonNull(accepted, "accepted");
        if (maximumResults < 1 || maximumResults > 4096) {
            throw new IllegalArgumentException("Search result limit must be between 1 and 4096");
        }

        ArrayDeque<T> pending = new ArrayDeque<>();
        Set<T> visited = new HashSet<>();
        List<T> results = new ArrayList<>();
        pending.add(source);
        visited.add(source);
        while (!pending.isEmpty() && results.size() < maximumResults) {
            T current = pending.removeFirst();
            if (!accepted.test(current)) {
                continue;
            }
            results.add(current);
            for (T neighbour : neighbours.apply(current)) {
                if (neighbour != null && visited.add(neighbour)) {
                    pending.addLast(neighbour);
                }
            }
        }
        return List.copyOf(results);
    }
}
