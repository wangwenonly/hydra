/*
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
package com.addthis.hydra.job.alert.types;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.addthis.basis.util.Strings;

import com.addthis.codec.annotations.Time;
import com.addthis.hydra.job.Job;
import com.addthis.hydra.job.alert.AbstractJobAlert;
import com.addthis.hydra.job.alert.JobAlertUtil;
import com.addthis.meshy.MeshyClient;

import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MapCanaryJobAlert extends AbstractJobAlert {

    @JsonProperty public final String canaryPath;
    @JsonProperty public final int canaryConfigThreshold;

    public MapCanaryJobAlert(@Nullable @JsonProperty("alertId") String alertId,
                             @JsonProperty("description") String description,
                             @Time(TimeUnit.MINUTES) @JsonProperty("timeout") long timeout,
                             @Time(TimeUnit.MINUTES) @JsonProperty("delay") long delay,
                             @JsonProperty("email") String email,
                             @JsonProperty(value = "jobIds", required = true) List<String> jobIds,
                             @JsonProperty("canaryPath") String canaryPath,
                             @JsonProperty("canaryConfigThreshold") int canaryConfigThreshold,
                             @JsonProperty("lastAlertTime") long lastAlertTime,
                             @JsonProperty("activeJobs") Map<String, String> activeJobs,
                             @JsonProperty("activeTriggerTimes") Map<String, Long> activeTriggerTimes) {
        super(alertId, description, timeout, delay, email, jobIds,lastAlertTime, activeJobs, activeTriggerTimes);
        this.canaryPath = canaryPath;
        this.canaryConfigThreshold = canaryConfigThreshold;
    }

    private MapCanaryJobAlert(@Nonnull MapCanaryJobAlert original, @Nonnull ImmutableList<String> jobIds) {
        super(original, jobIds);
        this.canaryPath = original.canaryPath;
        this.canaryConfigThreshold = original.canaryConfigThreshold;
    }

    @JsonIgnore
    @Override protected String getTypeStringInternal() {
        return "Map canary";
    }

    @Nullable @Override
    protected String testAlertActiveForJob(@Nullable MeshyClient meshClient, Job job, String previousErrorMessage) {
        try {
            long queryVal = JobAlertUtil.getQueryCount(job.getId(), canaryPath);
            consecutiveCanaryExceptionCount.set(0);
            if (queryVal < canaryConfigThreshold) {
                return "query value: " + queryVal + " < " + canaryConfigThreshold;
            }
        } catch (Exception ex) {
            return handleCanaryException(ex, previousErrorMessage);
        }
        return null;
    }

    @Override public String isValid() {
        if (Strings.isEmpty(canaryPath)) {
            return "Canary path is empty";
        } else if (canaryConfigThreshold <= 0) {
            return "Canary config is not a positive integer";
        } else {
            return null;
        }
    }

    @Override public AbstractJobAlert copyWithNewJobIds(ImmutableList<String> jobIds) {
        return new MapCanaryJobAlert(this, jobIds);
    }
}
