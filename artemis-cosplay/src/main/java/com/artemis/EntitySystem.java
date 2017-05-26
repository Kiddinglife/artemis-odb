package com.artemis;

import com.artemis.annotations.DelayedComponentRemoval;
import com.artemis.utils.Bag;
import com.artemis.utils.IntBag;

import java.util.Arrays;


/**
 * Tracks a subset of entities, but does not implement any sorting or iteration.
 * <p>
 * Like {@link BaseEntitySystem}, but uses Entity references instead of int.
 * <p>
 * This system exists as a convenience for users migrating from other Artemis
 * clones or older versions of odb. We recommend using the int systems over
 * the Entity variants.
 *
 * @author Arni Arent
 * @author Adrian Papari
 */
public abstract class EntitySystem<T extends Entity> extends CosplayBaseEntitySystem<T>
        implements EntitySubscription.SubscriptionListener {

    private boolean shouldSyncEntities;
    private WildBag<T> entities;

    private int methodFlags;

    /**
     * Creates an entity system that uses the specified aspect as a matcher
     * against entities.
     *
     * @param aspect to match against entities
     */
    public EntitySystem(Aspect.Builder aspect) {
        super(aspect);
    }

    /**
     * Set the world this system works on.
     *
     * @param world the world to set
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void setWorld(CosplayWorld<T> world) {
        super.setWorld(world);
        entities = new WildBag<T>(world.getEntityClass());
        if (implementsObserver("inserted"))
            methodFlags |= FLAG_INSERTED;
        if (implementsObserver("removed"))
            methodFlags |= FLAG_REMOVED;
    }

    @Override
    public final void inserted(IntBag entities) {
        shouldSyncEntities = true;
        // performance hack, skip calls to entities if system lacks implementation of added.
        if ((methodFlags & FLAG_INSERTED) > 0)
            super.inserted(entities);
    }

    @Override
    protected final void inserted(int entityId) {
        inserted(world.getEntity(entityId));
    }

    @Override
    public final void removed(IntBag entities) {
        shouldSyncEntities = true;
        // performance hack, skip calls to entities if system lacks implementation of deleted.
        if ((methodFlags & FLAG_REMOVED) > 0)
            super.removed(entities);
    }

    @Override
    protected final void removed(int entityId) {
        removed(world.getEntity(entityId));
    }

    /**
     * Called if entity has come into scope for this system, e.g
     * created or a component was added to it.
     *
     * @param e the entity that was added to this system
     */
    public void inserted(T e) {
        throw new RuntimeException("everything changes");
    }

    /**
     * <p>Called if entity has gone out of scope of this system, e.g deleted
     * or had one of it's components removed.</p>
     * <p>
     * <p>Explicitly removed components are only retrievable at this point
     * if annotated with {@link DelayedComponentRemoval}.</p>
     * <p>
     * <p>Deleted entities retain all their components - until all listeners
     * have been informed.</p>
     *
     * @param e the entity that was removed from this system
     */
    public void removed(Entity e) {
        throw new RuntimeException("everything breaks");
    }

    /**
     * Gets the entities processed by this system. Do not delete entities from
     * this bag - it is the live thing.
     *
     * @return System's entity bag, as matched by aspect.
     */
    public Bag<T> getEntities() {
        if (shouldSyncEntities) {
            int oldSize = entities.size();
            entities.setSize(0);
            IntBag entityIds = subscription.getEntities();
            int[] ids = entityIds.getData();
            for (int i = 0; i < entityIds.size(); i++) {
                entities.add(world.getEntity(ids[i]));
            }

            if (oldSize > entities.size()) {
                Arrays.fill(entities.getData(), entities.size(), oldSize, null);
            }

            shouldSyncEntities = false;
        }

        return entities;
    }
}
