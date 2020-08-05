/*
 * Copyright 2019 Miroslav Pokorny (github.com/mP1)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package walkingkooka.javashader;

import org.junit.jupiter.api.Test;
import walkingkooka.collect.list.Lists;
import walkingkooka.reflect.ClassAttributes;
import walkingkooka.reflect.ClassTesting;
import walkingkooka.reflect.FieldAttributes;
import walkingkooka.reflect.JavaVisibility;
import walkingkooka.reflect.MethodAttributes;
import walkingkooka.reflect.PackageName;
import walkingkooka.text.CharSequences;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Useful to test that a shaded class only has ctors, methods and fields that also appear in the emulated {@link Class}.
 */
public interface ShadedClassTesting<T> extends ClassTesting<T> {

    // class............................................................................................................

    @Test
    default void testClassStatic() {
        final Class<T> type = this.type();
        final Class<?> targetType = ShadedClassTestingHelper.with(this.typeMapper()).map(type);

        final boolean targetTypeStatic = ClassAttributes.STATIC.is(targetType);
        final boolean typeStatic = ClassAttributes.STATIC.is(type);
        assertEquals(targetTypeStatic,
                typeStatic,
                () -> (targetTypeStatic ? "Static" : "Instance") + " expected " + (typeStatic ? "static" : "instance") + ": " + type.toGenericString());
    }

    @Test
    default void testClassFinal() {
        final Class<T> type = this.type();
        final Class<?> targetType = ShadedClassTestingHelper.with(this.typeMapper()).map(type);

        final boolean targetTypeFinal = ClassAttributes.FINAL.is(targetType);
        final boolean typeFinal = ClassAttributes.FINAL.is(type);
        assertEquals(targetTypeFinal,
                typeFinal,
                () -> (targetTypeFinal ? "Final" : "Not final") + " expected " + (typeFinal ? "final" : "not final") + ": " + type.toGenericString());
    }

    @Test
    default void testClassAbstract() {
        final Class<T> type = this.type();
        final Class<?> targetType = ShadedClassTestingHelper.with(this.typeMapper()).map(type);

        final boolean targetTypeAbstract = ClassAttributes.ABSTRACT.is(targetType);
        final boolean typeAbstract = ClassAttributes.ABSTRACT.is(type);
        assertEquals(targetTypeAbstract,
                typeAbstract,
                () -> (targetTypeAbstract ? "Abstract" : "Not abstract") + " expected " + (typeAbstract ? "abstract" : "not abstract") + ": " + type.toGenericString());
    }

    // constructors......................................................................................................

    @Test
    default void testConstructors() {
        final ShadedClassTestingHelper helper = ShadedClassTestingHelper.with(this.typeMapper());

        final List<String> messages = Lists.array();
        final Predicate<Constructor> required = this.requiredConstructors();

        final Class<T> type = this.type();
        final Class<?> target = helper.map(type);

        for (final Constructor constructor : type.getDeclaredConstructors()) {
            if (false == required.test(constructor)) {
                continue;
            }

            final Constructor targetConstructor;
            final Class<?>[] parameters = helper.mapArray(constructor.getParameterTypes());
            try {
                targetConstructor = target.getDeclaredConstructor(parameters);
            } catch (final NoSuchMethodException cause) {
                if(JavaVisibility.of(constructor).isOrLess(JavaVisibility.PACKAGE_PRIVATE)) {
                    continue; // private/package private ctor doesnt exist on target ignore.
                }
                messages.add("Constructor missing from target: " + constructor.toGenericString());
                continue;
            }

            {
                final JavaVisibility targetVisibility = JavaVisibility.of(targetConstructor);
                if (targetVisibility != JavaVisibility.of(constructor)) {
                    messages.add("Constructor visibility " + targetVisibility + " different: " + targetConstructor.toGenericString());
                    continue;
                }
            }

            {
                final List<Class<?>> extraThrows = helper.checkDeclaredThrows(constructor.getExceptionTypes(),
                        targetConstructor.getExceptionTypes());
                if (false == extraThrows.isEmpty()) {
                    messages.add("Constructor includes unexpected throws: " + targetConstructor.toGenericString());
                    continue;
                }
            }
        }

        assertEquals(Lists.empty(), messages);
    }

    /**
     * This {@link Predicate} is used to filter constructors that should be present on the shaded {@link Class}.
     */
    Predicate<Constructor> requiredConstructors();

    // methods...........................................................................................................

    @Test
    default void testMethods() {
        final ShadedClassTestingHelper helper = ShadedClassTestingHelper.with(this.typeMapper());

        final List<String> messages = Lists.array();
        final Predicate<Method> required = this.requiredMethods();

        final Class<T> type = this.type();
        final Class<?> target = helper.map(type);

        for (final Method method : type.getDeclaredMethods()) {
            if (method.isSynthetic() || method.isBridge() || false == required.test(method)) {
                continue;
            }

            final Method targetMethod;
            final Class<?>[] parameters = helper.mapArray(method.getParameterTypes());
            try {
                targetMethod = target.getDeclaredMethod(method.getName(), parameters);
            } catch (final NoSuchMethodException cause) {
                if(JavaVisibility.of(method).isOrLess(JavaVisibility.PACKAGE_PRIVATE)) {
                    continue; // private/package private method doesnt exist on target ignore.
                }
                messages.add("Method missing from target: " + method.toGenericString());
                continue;
            }

            {
                final Class<?> targetReturnType = targetMethod.getReturnType();
                final Class<?> returnType = method.getReturnType();
                if (false == targetReturnType.equals(helper.map(returnType))) {
                    messages.add("Method return type " + returnType.getName() + " different: " + method.toGenericString());
                }
            }

            {
                final JavaVisibility targetVisibility = JavaVisibility.of(targetMethod);
                if (targetVisibility != JavaVisibility.of(method)) {
                    messages.add("Method visibility " + targetVisibility + " different: " + targetMethod.toGenericString());
                    continue;
                }
            }

            {
                final List<Class<?>> extraThrows = helper.checkDeclaredThrows(method.getExceptionTypes(),
                        targetMethod.getExceptionTypes());
                if (false == extraThrows.isEmpty()) {
                    messages.add("Method includes unexpected throws(" + extraThrows.stream().map(Class::getName).collect(Collectors.joining(", ")) + "): " + method.toGenericString());
                }
            }

            {
                final boolean targetMethodStatic = MethodAttributes.STATIC.is(targetMethod);
                final boolean methodStatic = MethodAttributes.STATIC.is(method);
                if (targetMethodStatic != methodStatic) {
                    messages.add((targetMethodStatic ? "Static" : "Instance") + " expected " + (methodStatic ? "static" : "instance") + ": " + method.toGenericString());
                }
            }
            {
                final boolean targetMethodAbstract = MethodAttributes.ABSTRACT.is(targetMethod);
                final boolean methodAbstract = MethodAttributes.ABSTRACT.is(method);
                if (targetMethodAbstract != methodAbstract) {
                    messages.add((targetMethodAbstract ? "Abstract" : "Non abstract") + " expected " + (methodAbstract ? "abstract" : "non abstract") + ": " + method.toGenericString());
                }
            }

            {
                final boolean targetMethodFinal = MethodAttributes.FINAL.is(targetMethod);
                final boolean methodFinal = MethodAttributes.FINAL.is(method);
                if (targetMethodFinal != methodFinal) {
                    messages.add((targetMethodFinal ? "Final" : "Non final") + " expected " + (methodFinal ? "final" : "non final") + ": " + method.toGenericString());
                }
            }
        }

        assertEquals(Lists.empty(), messages);
    }

    /**
     * This {@link Predicate} is used to filter methods that should be present on the shaded {@link Class}.
     */
    Predicate<Method> requiredMethods();

    // fields...........................................................................................................

    @Test
    default void testFields() throws Exception {
        final ShadedClassTestingHelper helper = ShadedClassTestingHelper.with(this.typeMapper());

        final List<String> messages = Lists.array();
        final Predicate<Field> required = this.requiredFields();

        final Class<T> type = this.type();
        final Class<?> target = helper.map(type);

        for (final Field field : type.getDeclaredFields()) {
            if (field.isSynthetic() || false == required.test(field)) {
                continue;
            }

            final Field targetField;
            try {
                targetField = target.getDeclaredField(field.getName());
            } catch (final NoSuchFieldException cause) {
                if(JavaVisibility.of(field).isOrLess(JavaVisibility.PACKAGE_PRIVATE)) {
                    continue; // private/package private field doesnt exist on target ignore.
                }

                messages.add("Field missing from target: " + field.toGenericString());
                continue;
            }

            final Class<?> targetFieldType = targetField.getType();
            final Class<?> fieldType = field.getType();
            if (false == targetFieldType.equals(helper.map(fieldType))) {
                messages.add("Field type " + fieldType.getName() + " different: " + field.toGenericString());
            }

            final JavaVisibility targetVisibility = JavaVisibility.of(targetField);
            if (targetVisibility != JavaVisibility.of(field)) {
                messages.add("Field visibility " + targetVisibility + " different: " + targetField.toGenericString());
            }

            final boolean targetFieldStatic = FieldAttributes.STATIC.is(targetField);
            final boolean fieldStatic = FieldAttributes.STATIC.is(field);
            if (targetFieldStatic != fieldStatic) {
                messages.add((targetFieldStatic ? "Static" : "Instance") + " expected " + (fieldStatic ? "static" : "instance") + ": " + field.toGenericString());
            }
            final boolean targetFieldFinal = FieldAttributes.FINAL.is(targetField);
            final boolean fieldFinal = FieldAttributes.FINAL.is(field);
            if (targetFieldFinal != fieldFinal) {
                messages.add((targetFieldFinal ? "Final" : "Non final") + " expected " + (fieldFinal ? "final" : "non final") + ": " + field.toGenericString());
            }

            if ((targetFieldType.isPrimitive() || targetFieldType == String.class) &&
                    (fieldType.isPrimitive() || fieldType == String.class) &&
                    fieldStatic &&
                    targetFieldFinal) {
                field.setAccessible(true);
                targetField.setAccessible(true);

                final Object fieldValue = field.get(null);
                final Object targetFieldValue = targetField.get(null);
                if (false == Objects.equals(fieldValue, targetFieldValue)) {
                    messages.add("Field value " + CharSequences.quoteIfChars(fieldValue) + " different " + CharSequences.quoteIfChars(targetFieldValue) + ": " + field.toGenericString());
                }
            }
        }

        assertEquals(Lists.empty(), messages);
    }

    /**
     * This {@link Predicate} is used to filter fields that should be present on the shaded {@link Class}.
     */
    Predicate<Field> requiredFields();

    // helpers...........................................................................................................

    /**
     * A {@link UnaryOperator} that maps some but not necessarily all types to their shaded form.
     * This should map {@link #type()} to another {@link Class}.
     */
    UnaryOperator<Class<?>> typeMapper();

    /**
     * A basic {@link UnaryOperator} that maps a class from one {@link Package} to another.
     */
    static UnaryOperator<Class<?>> typeMapper(final PackageName from,
                                              final PackageName to) {
        Objects.requireNonNull(from, "from package");
        Objects.requireNonNull(to, "to package");

        return (c) -> {
            final String className = c.getName();
            final String packageName = from.value();

            final Class<?> mapped;
            if (className.startsWith(packageName + ".")) {
                try {
                    mapped = Class.forName(to.value() + "." + className.substring(packageName.length() + 1));
                } catch (final ClassNotFoundException cause) {
                    throw new IllegalArgumentException("Unable to map " + className + " from " + packageName + " to " + to.value(), cause);
                }
            } else {
                mapped = c;
            }


            return mapped;
        };
    }

    /**
     * Copy the visibility of the shade target class
     */
    @Override
    default JavaVisibility typeVisibility() {
        return JavaVisibility.of(ShadedClassTestingHelper.with(this.typeMapper()).map(this.type()));
    }
}
