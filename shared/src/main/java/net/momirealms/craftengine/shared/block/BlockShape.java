package net.momirealms.craftengine.shared.block;

public interface BlockShape {

    Object getShape(Object thisObj, Object[] args) throws Exception;

    Object getCollisionShape(Object thisObj, Object[] args);
}
