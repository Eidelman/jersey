/*
 * Copyright (c) 2013, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.jersey.message.filtering.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;

import org.glassfish.jersey.internal.util.ReflectionHelper;

/**
 * Common {@link EntityProcessor entity processor} supposed to be used as a base class for custom implementations. Provides
 * convenient methods for adding entity-filtering scopes to {@link EntityGraph entity graph} as well as a common implementation
 * of {@link #process(org.glassfish.jersey.message.filtering.spi.EntityProcessorContext)}.
 *
 * @author Michal Gajdos
 */
public abstract class AbstractEntityProcessor implements EntityProcessor {

    @Override
    public EntityProcessor.Result process(final EntityProcessorContext context) {
        switch (context.getType()) {
            case CLASS_READER:
            case CLASS_WRITER:
                return process(null, null, FilteringHelper.EMPTY_ANNOTATIONS, context.getEntityClass().getDeclaredAnnotations(),
                        context.getEntityGraph());

            case PROPERTY_READER:
            case PROPERTY_WRITER:
            case METHOD_READER:
            case METHOD_WRITER:
                final Field field = context.getField();
                final Method method = context.getMethod();

                final boolean isProperty = field != null;
                String fieldName;
                Type fieldType;

                if (isProperty) {
                    fieldName = field.getName();
                    fieldType = field.getGenericType();
                } else {
                    fieldName = ReflectionHelper.getPropertyName(method);
                    fieldType = ReflectionHelper.isGetter(method) ? method.getGenericReturnType() : method
                            .getGenericParameterTypes()[0];
                }

                return process(fieldName, FilteringHelper.getEntityClass(fieldType), getAnnotations(field),
                        getAnnotations(method), context.getEntityGraph());

            default:
                // NOOP.
        }
        return EntityProcessor.Result.SKIP;
    }

    private Annotation[] getAnnotations(final AccessibleObject accessibleObject) {
        return accessibleObject == null ? FilteringHelper.EMPTY_ANNOTATIONS : accessibleObject.getDeclaredAnnotations();
    }

    /**
     * Method is called from the default implementation of
     * {@link #process(org.glassfish.jersey.message.filtering.spi.EntityProcessorContext)} and is supposed to be overridden by
     * custom implementations of this class.
     *
     * @param fieldName name of the field (can be {@code null}).
     * @param fieldClass class of the field (can be {@code null}).
     * @param fieldAnnotations annotations associated with the field (cannot be {@code null}).
     * @param annotations annotations associated with class/accessor (cannot be {@code null}).
     * @param graph entity graph to be processed.
     * @return result of the processing (default is {@link Result#SKIP}).
     */
    protected Result process(final String fieldName, final Class<?> fieldClass, final Annotation[] fieldAnnotations,
                             final Annotation[] annotations, final EntityGraph graph) {
        return Result.SKIP;
    }

    /**
     * Add entity-filtering scopes of a field to an entity-graph. The method determines whether the field should be added as a
     * simple field or a subgraph.
     *
     * @param field name of a field to be added to the graph.
     * @param fieldClass class of the field.
     * @param filteringScopes entity-filtering scopes the field will be associated with in the graph.
     * @param graph entity graph the field will be added to.
     */
    protected final void addFilteringScopes(final String field, final Class<?> fieldClass, final Set<String> filteringScopes,
                                            final EntityGraph graph) {
        if (!filteringScopes.isEmpty()) {
            if (FilteringHelper.filterableEntityClass(fieldClass)) {
                graph.addSubgraph(field, fieldClass, filteringScopes);
            } else {
                graph.addField(field, filteringScopes);
            }
        }
    }

    /**
     * Add entity-filtering scopes into given graph. This method should be called only in class-level context.
     *
     * @param filteringScopes entity-filtering scopes to be added to graph.
     * @param graph entity graph to be enhanced by new scopes.
     */
    protected final void addGlobalScopes(final Set<String> filteringScopes, final EntityGraph graph) {
        graph.addFilteringScopes(filteringScopes);
    }
}
