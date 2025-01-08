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

import walkingkooka.collect.list.Lists;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

final class ShadedClassTestingHelper {

    static ShadedClassTestingHelper with(final UnaryOperator<Class<?>> typeMapper) {
        return new ShadedClassTestingHelper(typeMapper);
    }

    private ShadedClassTestingHelper(final UnaryOperator<Class<?>> typeMapper) {
        super();
        this.typeMapper = typeMapper;
    }

    Class<?> map(final Class<?> type) {
        return this.typeMapper.apply(type);
    }

    Class<?> mapDifferentOrFail(final Class<?> type) {
        final Class<?> different = this.map(type);
        if (type.equals(different)) {
            throw new IllegalArgumentException("Type " + type.getName() + " not shaded");
        }
        return different;
    }

    /**
     * Reflection APIs use arrays.
     */
    Class<?>[] mapArray(final Class<?>... types) {
        return Arrays.stream(types)
            .map(this::map)
            .toArray(Class[]::new);
    }

    private final UnaryOperator<Class<?>> typeMapper;

    List<Class<?>> checkDeclaredThrows(final Class<?>[] throwns,
                                       final Class<?>[] targetThrowns) {
        final List<Class<?>> extra = Lists.array();

        for (final Class<?> thrown : throwns) {
            boolean pass = false;

            for (final Class<?> possible : targetThrowns) {
                pass = possible.isAssignableFrom(this.map(thrown));
                if (pass) {
                    break;
                }
            }

            if (!pass) {
                extra.add(thrown);
            }
        }

        return extra;
    }
}
