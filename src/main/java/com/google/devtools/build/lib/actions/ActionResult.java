// Copyright 2017 The Bazel Authors. All rights reserved.
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

package com.google.devtools.build.lib.actions;

import static com.google.common.base.Preconditions.checkState;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;

/** Holds the result(s) of an action's execution. */
@AutoValue
public abstract class ActionResult {

  /** An empty ActionResult used by Actions that don't have any metadata to return. */
  public static final ActionResult EMPTY = ActionResult.create(ImmutableList.of());

  /** Returns the SpawnResults for the action. */
  public abstract ImmutableList<SpawnResult> spawnResults();

  /** Returns a builder that can be used to construct a {@link ActionResult} object. */
  public static Builder builder() {
    return new AutoValue_ActionResult.Builder();
  }

  /**
   * Returns the cumulative time taken by a series of {@link SpawnResult}s.
   *
   * @param getSpawnResultExecutionTime a selector that returns either the wall, user or system time
   *     for each {@link SpawnResult} being considered
   * @return the cumulative time, or null if no spawn results contained this time
   */
  @Nullable
  private Duration getCumulativeTime(Function<SpawnResult, Duration> getSpawnResultExecutionTime) {
    Long totalMillis = null;
    for (SpawnResult spawnResult : spawnResults()) {
      Duration executionTime = getSpawnResultExecutionTime.apply(spawnResult);
      if (executionTime != null) {
        if (totalMillis == null) {
          totalMillis = executionTime.toMillis();
        } else {
          totalMillis += executionTime.toMillis();
        }
      }
    }
    if (totalMillis == null) {
      return null;
    } else {
      return Duration.ofMillis(totalMillis);
    }
  }

  /**
   * Returns the cumulative total of long values taken from a series of {@link SpawnResult}s.
   *
   * @param getSpawnResultLongValue a selector that returns a long value for each {@link
   *     SpawnResult} being considered
   * @return the total, or null if no spawn results contained this long value
   */
  private Long getCumulativeLong(Function<SpawnResult, Long> getSpawnResultLongValue) {
    Long longTotal = null;
    for (SpawnResult spawnResult : spawnResults()) {
      Long longValue = getSpawnResultLongValue.apply(spawnResult);
      if (longValue != null) {
        if (longTotal == null) {
          longTotal = longValue;
        } else {
          longTotal += longValue;
        }
      }
    }
    return longTotal;
  }

  /**
   * Returns the cumulative command execution wall time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeCommandExecutionWallTime() {
    return getCumulativeTime(SpawnResult::getWallTime);
  }

  /**
   * Returns the cumulative command execution user time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeCommandExecutionUserTime() {
    return getCumulativeTime(SpawnResult::getUserTime);
  }

  /**
   * Returns the cumulative command execution system time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeCommandExecutionSystemTime() {
    return getCumulativeTime(SpawnResult::getSystemTime);
  }

  /**
   * Returns the cumulative number of block input operations for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Long cumulativeCommandExecutionBlockInputOperations() {
    return getCumulativeLong(SpawnResult::getNumBlockInputOperations);
  }

  /**
   * Returns the cumulative number of block output operations for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Long cumulativeCommandExecutionBlockOutputOperations() {
    return getCumulativeLong(SpawnResult::getNumBlockOutputOperations);
  }

  /**
   * Returns the cumulative number of involuntary context switches for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Long cumulativeCommandExecutionInvoluntaryContextSwitches() {
    return getCumulativeLong(SpawnResult::getNumInvoluntaryContextSwitches);
  }

  /**
   * Returns the cumulative number of involuntary context switches for the {@link Action}. The
   * spawns on one action could execute simultaneously, so the sum of spawn's memory usage is better
   * estimation.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Long cumulativeCommandExecutionMemoryInKb() {
    return getCumulativeLong(SpawnResult::getMemoryInKb);
  }

  /**
   * Returns the cumulative spawns total time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeSpawnsTotalTime() {
    return getCumulativeTime(s -> s.getMetrics().totalTime());
  }

  /**
   * Returns the cumulative spawns parse time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeSpawnsParseTime() {
    return getCumulativeTime(s -> s.getMetrics().parseTime());
  }

  /**
   * Returns the cumulative spawns network time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeSpawnsNetworkTime() {
    return getCumulativeTime(s -> s.getMetrics().networkTime());
  }

  /**
   * Returns the cumulative spawns fetch time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeSpawnsFetchTime() {
    return getCumulativeTime(s -> s.getMetrics().fetchTime());
  }

  /**
   * Returns the cumulative spawns queue time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeSpawnsQueueTime() {
    return getCumulativeTime(s -> s.getMetrics().queueTime());
  }

  /**
   * Returns the cumulative spawns setup time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeSpawnsSetupTime() {
    return getCumulativeTime(s -> s.getMetrics().setupTime());
  }

  /**
   * Returns the cumulative spawns upload time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeSpawnsUploadTime() {
    return getCumulativeTime(s -> s.getMetrics().uploadTime());
  }

  /**
   * Returns the cumulative spawns execution wall time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeExecutionWallTime() {
    return getCumulativeTime(s -> s.getMetrics().executionWallTime());
  }

  /**
   * Returns the cumulative spawns process output time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeProcessOutputTime() {
    return getCumulativeTime(s -> s.getMetrics().processOutputsTime());
  }

  /**
   * Returns the cumulative spawns retry time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeRetryTime() {
    return getCumulativeTime(s -> s.getMetrics().retryTime());
  }

  /**
   * Indicates whether all {@link Spawn}s executed locally or not.
   *
   * @return true if all spawns of action executed locally
   */
  public boolean locallyExecuted() {
    boolean locallyExecuted = true;
    for (SpawnResult spawnResult : spawnResults()) {
      locallyExecuted &= !spawnResult.wasRemote();
    }
    return locallyExecuted;
  }

  /**
   * Returns the cumulative command execution CPU time for the {@link Action}.
   *
   * @return the cumulative measurement, or null in case of execution errors or when the measurement
   *     is not implemented for the current platform
   */
  @Nullable
  public Duration cumulativeCommandExecutionCpuTime() {
    Duration userTime = cumulativeCommandExecutionUserTime();
    Duration systemTime = cumulativeCommandExecutionSystemTime();

    if (userTime == null && systemTime == null) {
      return null;
    } else if (userTime != null && systemTime == null) {
      return userTime;
    } else if (userTime == null && systemTime != null) {
      return systemTime;
    } else {
      checkState(userTime != null && systemTime != null);
      return userTime.plus(systemTime);
    }
  }

  /** Creates an ActionResult given a list of SpawnResults. */
  public static ActionResult create(List<SpawnResult> spawnResults) {
    if (spawnResults == null) {
      return EMPTY;
    } else {
      return builder().setSpawnResults(ImmutableList.copyOf(spawnResults)).build();
    }
  }

  /** Builder for a {@link ActionResult} instance, which is immutable once built. */
  @AutoValue.Builder
  public abstract static class Builder {

    /** Sets the SpawnResults for the action. */
    public abstract Builder setSpawnResults(ImmutableList<SpawnResult> spawnResults);

    /** Builds and returns an ActionResult object. */
    public abstract ActionResult build();
  }
}
