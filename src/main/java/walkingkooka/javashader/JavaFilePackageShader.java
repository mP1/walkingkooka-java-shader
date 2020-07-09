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

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import walkingkooka.collect.list.Lists;
import walkingkooka.collect.map.Maps;

import java.nio.charset.Charset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Shades a references for packages within a java source file
 */
final class JavaFilePackageShader implements BiFunction<byte[], Map<String, String>, byte[]> {

    static JavaFilePackageShader with(final Charset charset) {
        return new JavaFilePackageShader(charset);
    }

    private JavaFilePackageShader(final Charset charset) {
        super();
        this.charset = charset;
    }

    @Override
    public byte[] apply(final byte[] content,
                        final Map<String, String> shadings) {
        final Charset charset = this.charset;
        return shade(new String(content, charset), shadings).getBytes(charset);
    }

    private final Charset charset;

    static String shade(final String content,
                        final Map<String, String> shadings) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setSource(content.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        final CompilationUnit unit = (CompilationUnit) parser.createAST(null);
        final List<Name> qualifieds = collectNames(unit);
        return collectText(reverseSort(qualifieds), content, shadings);
    }

    private static List<Name> collectNames(final ASTNode root) {
        final List<Name> names = Lists.array();

        root.accept(new ASTVisitor() {

            @Override
            public boolean visit(final QualifiedName node) {
                names.add(node);
                return false;
            }

            @Override
            public boolean visit(final SimpleName node) {
                names.add(node);
                return false;
            }

            @Override
            public boolean visit(final SimpleType node) {
                final Name name = node.getName();
                if (name instanceof QualifiedName) {
                    names.add(name);
                }

                return false; // dont want to update simple type names.
            }
        });
        return names;
    }

    private static List<Name> reverseSort(final List<Name> nodes) {
        final Map<Integer, Name> offsetToQualified = Maps.sorted(Comparator.reverseOrder());

        nodes.forEach(q -> offsetToQualified.put(q.getStartPosition(), q));

        return offsetToQualified.values().stream().collect(Collectors.toList());
    }

    private static String collectText(final List<Name> names,
                                      final String file,
                                      final Map<String, String> shadings) {
        final StringBuilder text = new StringBuilder();
        text.append(file);

        for (final Name node : names) {
            final int start = node.getStartPosition();
            final int end = start + node.getLength();

            final String typeName = text.substring(start, end);

            for (final Entry<String, String> oldAndNew : shadings.entrySet()) {
                final String old = oldAndNew.getKey();
                final String neww = oldAndNew.getValue();

                if (typeName.equals(old) || typeName.startsWith(old)) {
                    text.delete(start, start + old.length());
                    text.insert(start, neww);
                    break;
                }
            }
        }

        return text.toString();
    }

    public String toString() {
        return this.charset.toString();
    }
}
