// Copyright 2023 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.packages;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import net.starlark.java.syntax.Location;
import net.starlark.java.syntax.TokenKind;

/** A struct-like Info (provider instance) for providers defined in Starlark that have a schema. */
public class StarlarkInfoWithSchema extends StarlarkInfo {
  private final StarlarkProvider provider;

  // For each field in provider.getFields the table contains on corresponding position either null
  // or a legal Starlark value
  private final Object[] table;

  private StarlarkInfoWithSchema(
      StarlarkProvider provider, Object[] table, @Nullable Location loc) {
    super(loc);
    this.provider = provider;
    this.table = table;
  }

  @Override
  public Provider getProvider() {
    return provider;
  }

  /**
   * Constructs a StarlarkInfo from an array of alternating key/value pairs as provided by
   * Starlark.fastcall. Checks that each key is provided at most once, and is defined by the schema,
   * which must be sorted. This function exists solely for the StarlarkProvider constructor.
   */
  static StarlarkInfoWithSchema createFromNamedArgs(
      StarlarkProvider provider, Object[] table, Location loc) throws EvalException {
    ImmutableList<String> fields = provider.getFields();

    Object[] valueTable = new Object[fields.size()];
    List<String> unexpected = null;

    for (int i = 0; i < table.length; i += 2) {
      int pos = Collections.binarySearch(fields, (String) table[i]);
      if (pos >= 0) {
        if (valueTable[pos] != null) {
          throw Starlark.errorf(
              "got multiple values for parameter %s in call to instantiate provider %s",
              table[i], provider.getPrintableName());
        }
        valueTable[pos] = table[i + 1];
      } else {
        if (unexpected == null) {
          unexpected = new ArrayList<>();
        }
        unexpected.add((String) table[i]);
      }
    }

    if (unexpected != null) {
      throw Starlark.errorf(
          "got unexpected field%s '%s' in call to instantiate provider %s",
          unexpected.size() > 1 ? "s" : "",
          Joiner.on("', '").join(unexpected),
          provider.getPrintableName());
    }
    return new StarlarkInfoWithSchema(provider, valueTable, loc);
  }

  @Override
  public ImmutableCollection<String> getFieldNames() {
    ImmutableList.Builder<String> fieldNames = new ImmutableList.Builder<>();
    ImmutableList<String> fields = provider.getFields();
    for (int i = 0; i < fields.size(); i++) {
      if (table[i] != null) {
        fieldNames.add(fields.get(i));
      }
    }
    return fieldNames.build();
  }

  @Override
  public boolean isImmutable() {
    // If the provider is not yet exported, the hash code of the object is subject to change.
    if (!provider.isExported()) {
      return false;
    }
    for (int i = 0; i < table.length; i++) {
      if (table[i] != null && !Starlark.isImmutable(table[i])) {
        return false;
      }
    }
    return true;
  }

  @Nullable
  @Override
  public Object getValue(String name) {
    ImmutableList<String> fields = provider.getFields();
    int i = Collections.binarySearch(fields, name);
    return i >= 0 ? table[i] : null;
  }

  @Nullable
  @Override
  public StarlarkInfoWithSchema binaryOp(TokenKind op, Object that, boolean thisLeft)
      throws EvalException {
    if (op == TokenKind.PLUS && that instanceof StarlarkInfo) {
      final Provider thatProvider = ((StarlarkInfo) that).getProvider();
      if (!provider.equals(thatProvider)) {
        throw Starlark.errorf(
            "Cannot use '+' operator on instances of different providers (%s and %s)",
            provider.getPrintableName(), thatProvider.getPrintableName());
      }
      Preconditions.checkArgument(that instanceof StarlarkInfoWithSchema);
      return thisLeft
          ? plus(this, (StarlarkInfoWithSchema) that) //
          : plus((StarlarkInfoWithSchema) that, this);
    }
    return null;
  }

  private static StarlarkInfoWithSchema plus(StarlarkInfoWithSchema x, StarlarkInfoWithSchema y)
      throws EvalException {
    int n = x.table.length;

    Object[] ztable = new Object[n];
    for (int i = 0; i < n; i++) {
      if (x.table[i] != null && y.table[i] != null) {
        ImmutableList<String> schema = x.provider.getFields();
        throw Starlark.errorf("cannot add struct instances with common field '%s'", schema.get(i));
      }
      ztable[i] = x.table[i] != null ? x.table[i] : y.table[i];
    }
    return new StarlarkInfoWithSchema(x.provider, ztable, Location.BUILTIN);
  }
}
