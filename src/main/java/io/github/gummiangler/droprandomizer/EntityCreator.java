package io.github.gummiangler.droprandomizer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import java.lang.reflect.Method;

class EntityCreator {

    public static Entity create(EntityType entityType, Location location) {
        try {
            Class<?> craftWorldClass = Class.forName("org.bukkit.craftbukkit.CraftWorld");
            Object craftWorldObject = craftWorldClass.cast(location.getWorld());

            Method createEntityMethod = craftWorldObject.getClass().getMethod("createEntity", Location.class, Class.class);
            Object entity = createEntityMethod.invoke(craftWorldObject, location, entityType.getEntityClass());


            return (Entity) entity;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
