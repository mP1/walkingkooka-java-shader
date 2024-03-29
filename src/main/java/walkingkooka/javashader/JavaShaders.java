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

import walkingkooka.reflect.PackageName;
import walkingkooka.reflect.PublicStaticHelper;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.BiFunction;

public final class JavaShaders implements PublicStaticHelper {

    /**
     * {@see ClassFilePackageShader}
     */
    @SuppressWarnings("unused")
    public static BiFunction<byte[], Map<PackageName, PackageName>, byte[]> classFilePackageShader() {
        return ClassFilePackageShader.INSTANCE;
    }

    /**
     * {@see JavaFilePackageShader}
     */
    @SuppressWarnings("unused")
    public static BiFunction<byte[], Map<PackageName, PackageName>, byte[]> javaFilePackageShader(final Charset charset) {
        return JavaFilePackageShader.with(charset);
    }

    /**
     * Stop creation
     */
    private JavaShaders() {
        throw new UnsupportedOperationException();
    }
}
