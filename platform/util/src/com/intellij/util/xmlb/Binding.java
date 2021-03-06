/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.xmlb;

import com.intellij.openapi.diagnostic.Logger;
import org.jdom.Content;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.List;

public abstract class Binding {
  static final Logger LOG = Logger.getInstance(Binding.class);

  protected final MutableAccessor myAccessor;

  protected Binding(MutableAccessor accessor) {
    myAccessor = accessor;
  }

  @NotNull
  public MutableAccessor getAccessor() {
    return myAccessor;
  }

  @Nullable
  public abstract Object serialize(@NotNull Object o, @Nullable Object context, @Nullable SerializationFilter filter);

  public boolean isBoundTo(@NotNull Element element) {
    return false;
  }

  public void init(@NotNull Type originalType, @NotNull Serializer serializer) {
  }

  @Nullable
  public static Object deserializeList(@NotNull Binding binding, @Nullable Object context, @NotNull List<Element> nodes) {
    if (binding instanceof MultiNodeBinding) {
      return ((MultiNodeBinding)binding).deserializeList(context, nodes);
    }
    else {
      if (nodes.size() == 1) {
        return binding.deserializeUnsafe(context, nodes.get(0));
      }
      else if (nodes.isEmpty()) {
        return null;
      }
      else {
        throw new AssertionError("Duplicate data for " + binding + " will be ignored");
      }
    }
  }

  public abstract Object deserializeUnsafe(Object context, @NotNull Element element);

  protected static void addContent(@NotNull final Element targetElement, final Object node) {
    if (node instanceof Content) {
      Content content = (Content)node;
      targetElement.addContent(content);
    }
    else if (node instanceof List) {
      //noinspection unchecked
      targetElement.addContent((List)node);
    }
    else {
      throw new IllegalArgumentException("Wrong node: " + node);
    }
  }
}
