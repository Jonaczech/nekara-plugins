package cz.nekara.rpg.items.custom;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface CustomItemRepository {
    List<CustomItemDefinition> findAll();

    Optional<CustomItemDefinition> find(String id);

    void create(CustomItemDefinition definition) throws IOException;

    void update(CustomItemDefinition definition) throws IOException;
}
