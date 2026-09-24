package gg.nationsglory.mcpcj17.shim;

import java.lang.reflect.InvocationTargetException;

/**
 * Stands in for {@code sun.reflect.ConstructorAccessor}.
 */
public interface ConstructorAccessor {

    Object newInstance(Object[] args)
            throws InstantiationException, IllegalArgumentException, InvocationTargetException;
}
