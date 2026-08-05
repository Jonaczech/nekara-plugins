package cz.nekara.rpg.skills.perks;

import java.util.Collection;
import java.util.Objects;

/**
 * A movable window over a perk graph. Inventory rendering decides where this
 * window lives; the graph itself is deliberately not constrained by chest size.
 */
public record PerkTreeViewport(int column, int row, int width, int height) {
    private static final int GRAPH_PADDING = 1;

    public PerkTreeViewport {
        if (column < 0 || row < 0 || width < 1 || height < 1) {
            throw new IllegalArgumentException("Viewport coordinates and dimensions must be positive");
        }
    }

    public static PerkTreeViewport initial(Collection<PerkDefinition> perks, int width, int height) {
        Objects.requireNonNull(perks, "perks");
        PerkPosition root = perks.stream()
            .filter(perk -> perk.requiredSkillLevel() == 0 && perk.requirements().isEmpty())
            .map(PerkDefinition::position)
            .findFirst()
            .orElseGet(() -> perks.stream().map(PerkDefinition::position).findFirst().orElse(new PerkPosition(0, 0)));
        return new PerkTreeViewport(
            Math.max(0, root.column() - width / 2),
            Math.max(0, root.row() - height + 2),
            width,
            height
        )
            .clampTo(perks);
    }

    public boolean contains(PerkPosition position) {
        Objects.requireNonNull(position, "position");
        return position.column() >= column && position.column() < column + width
            && position.row() >= row && position.row() < row + height;
    }

    public int slot(PerkPosition position) {
        if (!contains(position)) {
            throw new IllegalArgumentException("Position is outside the viewport: " + position);
        }
        return (position.row() - row) * width + position.column() - column;
    }

    public PerkTreeViewport move(int horizontal, int vertical, Collection<PerkDefinition> perks) {
        Objects.requireNonNull(perks, "perks");
        return new PerkTreeViewport(Math.max(0, column + horizontal), Math.max(0, row + vertical), width, height)
            .clampTo(perks);
    }

    public boolean canMove(int horizontal, int vertical, Collection<PerkDefinition> perks) {
        return !move(horizontal, vertical, perks).equals(this);
    }

    private PerkTreeViewport clampTo(Collection<PerkDefinition> perks) {
        int maximumColumn = perks.stream().mapToInt(perk -> perk.position().column()).max().orElse(0);
        int maximumRow = perks.stream().mapToInt(perk -> perk.position().row()).max().orElse(0);
        return new PerkTreeViewport(
            Math.min(column, Math.max(0, maximumColumn - width + 1 + GRAPH_PADDING)),
            Math.min(row, Math.max(0, maximumRow - height + 1 + GRAPH_PADDING)),
            width,
            height
        );
    }
}
