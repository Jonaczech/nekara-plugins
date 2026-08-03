package cz.nekara.rpg.skills.abilities;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoundedGraphSearchTest {
    @Test
    void connectedSearchIsBoundedAndDoesNotRepeatCycles() {
        Map<Integer, List<Integer>> graph = Map.of(
            1, List.of(2, 3),
            2, List.of(1, 4),
            3, List.of(1, 4),
            4, List.of(2, 3, 5),
            5, List.of(4)
        );

        assertEquals(List.of(1, 2, 3), BoundedGraphSearch.connected(
            1, 3, node -> graph.getOrDefault(node, List.of()), node -> true));
    }

    @Test
    void rejectedNodeCannotBridgeIntoAnotherComponent() {
        Map<Integer, List<Integer>> graph = Map.of(
            1, List.of(2), 2, List.of(1, 3), 3, List.of(2));

        assertEquals(List.of(1), BoundedGraphSearch.connected(
            1, 10, node -> graph.getOrDefault(node, List.of()), node -> node != 2));
    }
}
