package cz.nekara.rpg.skills.perks;

import java.util.ArrayList;
import java.util.List;

public final class PerkConnectionPath {
    private PerkConnectionPath() {
    }

    public static List<PerkPosition> between(PerkPosition from, PerkPosition to, BendOrder bendOrder) {
        List<PerkPosition> path = new ArrayList<>();
        if (bendOrder == BendOrder.VERTICAL_FIRST) {
            appendVertical(path, from, to, from.column(), from.column() != to.column());
            appendHorizontal(path, from, to, to.row(), false);
        } else {
            appendHorizontal(path, from, to, from.row(), from.row() != to.row());
            appendVertical(path, from, to, to.column(), false);
        }
        return List.copyOf(path);
    }

    private static void appendHorizontal(
        List<PerkPosition> path,
        PerkPosition from,
        PerkPosition to,
        int row,
        boolean includeBend
    ) {
        int step = Integer.compare(to.column(), from.column());
        for (int column = from.column() + step; column != to.column(); column += step) {
            path.add(new PerkPosition(column, row));
        }
        if (includeBend && step != 0) {
            path.add(new PerkPosition(to.column(), row));
        }
    }

    private static void appendVertical(
        List<PerkPosition> path,
        PerkPosition from,
        PerkPosition to,
        int column,
        boolean includeBend
    ) {
        int step = Integer.compare(to.row(), from.row());
        for (int row = from.row() + step; row != to.row(); row += step) {
            path.add(new PerkPosition(column, row));
        }
        if (includeBend && step != 0) {
            path.add(new PerkPosition(column, to.row()));
        }
    }

    public enum BendOrder {
        HORIZONTAL_FIRST,
        VERTICAL_FIRST
    }
}
