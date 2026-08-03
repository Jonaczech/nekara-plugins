package cz.nekara.rpg.skills.perks;

import cz.nekara.rpg.skills.SkillId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PerkCatalog {
    private final Map<PerkId, PerkDefinition> byId;
    private final Map<SkillId, List<PerkDefinition>> bySkill;

    public PerkCatalog(Collection<PerkDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        Map<PerkId, PerkDefinition> indexed = new HashMap<>();
        Map<SkillId, Set<PerkPosition>> occupiedPositions = new HashMap<>();

        for (PerkDefinition definition : definitions) {
            Objects.requireNonNull(definition, "definition");
            if (definition.skill() == SkillId.POWER) {
                throw new IllegalArgumentException("Power uses milestones, not a normal perk tree");
            }
            if (indexed.putIfAbsent(definition.id(), definition) != null) {
                throw new IllegalArgumentException("Duplicate perk ID: " + definition.id());
            }
            Set<PerkPosition> positions = occupiedPositions.computeIfAbsent(
                definition.skill(),
                ignored -> new HashSet<>()
            );
            if (!positions.add(definition.position())) {
                throw new IllegalArgumentException(
                    "Duplicate perk position for " + definition.skill().id() + ": " + definition.position()
                );
            }
        }

        validateRequirements(indexed);
        validateAcyclic(indexed);
        byId = Map.copyOf(indexed);

        Map<SkillId, List<PerkDefinition>> grouped = new HashMap<>();
        for (PerkDefinition definition : definitions) {
            grouped.computeIfAbsent(definition.skill(), ignored -> new ArrayList<>()).add(definition);
        }
        grouped.replaceAll((skill, perks) -> perks.stream().sorted((left, right) -> {
            int row = Integer.compare(left.position().row(), right.position().row());
            return row != 0 ? row : Integer.compare(left.position().column(), right.position().column());
        }).toList());
        bySkill = Map.copyOf(grouped);
    }

    public PerkDefinition require(PerkId id) {
        PerkDefinition definition = byId.get(Objects.requireNonNull(id, "id"));
        if (definition == null) {
            throw new IllegalArgumentException("Unknown perk: " + id);
        }
        return definition;
    }

    public List<PerkDefinition> forSkill(SkillId skill) {
        return bySkill.getOrDefault(Objects.requireNonNull(skill, "skill"), List.of());
    }

    public int size() {
        return byId.size();
    }

    private static void validateRequirements(Map<PerkId, PerkDefinition> indexed) {
        for (PerkDefinition definition : indexed.values()) {
            for (PerkRequirement requirement : definition.requirements()) {
                PerkDefinition required = indexed.get(requirement.perkId());
                if (required == null) {
                    throw new IllegalArgumentException(
                        "Unknown prerequisite " + requirement.perkId() + " for " + definition.id()
                    );
                }
                if (required.skill() != definition.skill()) {
                    throw new IllegalArgumentException("Perk prerequisites cannot cross skill trees");
                }
                if (requirement.minimumRank() > required.maxRank()) {
                    throw new IllegalArgumentException(
                        "Required rank exceeds maximum rank for " + requirement.perkId()
                    );
                }
            }
        }
    }

    private static void validateAcyclic(Map<PerkId, PerkDefinition> indexed) {
        Map<PerkId, VisitState> states = new HashMap<>();
        for (PerkId id : indexed.keySet()) {
            visit(id, indexed, states);
        }
    }

    private static void visit(
        PerkId id,
        Map<PerkId, PerkDefinition> indexed,
        Map<PerkId, VisitState> states
    ) {
        VisitState state = states.get(id);
        if (state == VisitState.VISITED) {
            return;
        }
        if (state == VisitState.VISITING) {
            throw new IllegalArgumentException("Perk graph contains a cycle at " + id);
        }

        states.put(id, VisitState.VISITING);
        for (PerkRequirement requirement : indexed.get(id).requirements()) {
            visit(requirement.perkId(), indexed, states);
        }
        states.put(id, VisitState.VISITED);
    }

    private enum VisitState {
        VISITING,
        VISITED
    }
}
