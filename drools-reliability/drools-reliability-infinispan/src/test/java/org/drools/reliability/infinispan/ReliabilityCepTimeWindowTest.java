/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.reliability.infinispan;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.drools.model.Global;
import org.drools.model.Index;
import org.drools.model.Model;
import org.drools.model.PrototypeVariable;
import org.drools.model.Rule;
import org.drools.model.impl.ModelImpl;
import org.drools.reliability.infinispan.util.TimeAmount;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.api.conf.EventProcessingOption;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.conf.PersistedSessionOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.drools.model.DSL.after;
import static org.drools.model.DSL.globalOf;
import static org.drools.model.DSL.on;
import static org.drools.model.PatternDSL.rule;
import static org.drools.model.PrototypeDSL.protoPattern;
import static org.drools.model.PrototypeDSL.variable;
import static org.drools.reliability.infinispan.util.PrototypeUtils.DEFAULT_PROTOTYPE_NAME;
import static org.drools.reliability.infinispan.util.PrototypeUtils.getPrototype;

@DisabledIf("isProtoStream")
@ExtendWith(BeforeAllMethodExtension.class)
class ReliabilityCepTimeWindowTest extends ReliabilityTestBasics {

    public static final String RULE_NAME = "R";

    /**
     * These rules are created in the same way as TimeWindowDefinition in drools-ansible-rulebook-integration
     */
    private Model ruleModel() {
        Global<List> global = globalOf(List.class, "defaultpkg", "results");

        List<Rule> rules = new ArrayList<>();

        // main rule (all events should be matched within +- 5 minutes each other)
        TimeAmount timeAmount = TimeAmount.parseTimeAmount("5 minutes");
        PrototypeVariable var0 = variable(getPrototype(DEFAULT_PROTOTYPE_NAME), "m_0");
        PrototypeVariable var1 = variable(getPrototype(DEFAULT_PROTOTYPE_NAME), "m_1");
        PrototypeVariable var2 = variable(getPrototype(DEFAULT_PROTOTYPE_NAME), "m_2");
        rules.add(
                  rule( RULE_NAME )
                    .build(
                            protoPattern(var0).expr( "ping.timeout", Index.ConstraintType.EQUAL, true ),
                            protoPattern(var1).expr( "sensu.process.status", Index.ConstraintType.EQUAL, "stopped" )
                                              .expr( after(-timeAmount.getAmount(), timeAmount.getTimeUnit(), timeAmount.getAmount(), timeAmount.getTimeUnit()), var0 ),
                            protoPattern(var2).expr( "sensu.storage.percent", Index.ConstraintType.GREATER_THAN, 95 )
                                              .expr( after(-timeAmount.getAmount(), timeAmount.getTimeUnit(), timeAmount.getAmount(), timeAmount.getTimeUnit()), var0 )
                                              .expr( after(-timeAmount.getAmount(), timeAmount.getTimeUnit(), timeAmount.getAmount(), timeAmount.getTimeUnit()), var1 ),
                            on(global).execute((drools, globalResults) -> {
                                globalResults.add(RULE_NAME);
                            })
            )
        );

        return new ModelImpl().withRules(rules).addGlobal(global);
    }

    @ParameterizedTest
    @MethodSource("strategyProviderStoresOnlyWithExplicitSafepoints")
    void insertFailoverInsertFire_shouldFire(PersistedSessionOption.PersistenceStrategy persistenceStrategy, PersistedSessionOption.SafepointStrategy safepointStrategy) {

        createSession(ruleModel(), persistenceStrategy, safepointStrategy, EventProcessingOption.STREAM, ClockTypeOption.PSEUDO);

        insertMatchingSensuProcessStatusEvent("stopped");
        advanceTimeAndFire(1, TimeUnit.MINUTES);

        insertMatchingPingTimeoutEvent(true);
        advanceTimeAndFire(2, TimeUnit.MINUTES);

        assertThat(getResults()).as("2 events. Not yet fired")
                                .isEmpty();

        failover();
        restoreSession(ruleModel(), persistenceStrategy, safepointStrategy, EventProcessingOption.STREAM, ClockTypeOption.PSEUDO);

        advanceTimeAndFire(1, TimeUnit.MINUTES);

        insertMatchingSensuStoragePercentEvent(98);
        fireAllRules();

        assertThat(getResults()).as("all events are matched within +- 5 minutes each other. the rule should be fired")
                                .hasSize(1);
    }

    @ParameterizedTest
    @MethodSource("strategyProviderStoresOnlyWithExplicitSafepoints")
    void insertExpireFailoverInsertFire_shouldNotFire(PersistedSessionOption.PersistenceStrategy persistenceStrategy, PersistedSessionOption.SafepointStrategy safepointStrategy) {

        createSession(ruleModel(), persistenceStrategy, safepointStrategy, EventProcessingOption.STREAM, ClockTypeOption.PSEUDO);

        insertMatchingSensuProcessStatusEvent("stopped");
        advanceTimeAndFire(1, TimeUnit.MINUTES);

        insertMatchingPingTimeoutEvent(true);
        advanceTimeAndFire(2, TimeUnit.MINUTES);

        assertThat(getResults()).as("2 events. Not yet fired")
                                .isEmpty();

        advanceTime(5, TimeUnit.MINUTES); // timeout

        failover();
        restoreSession(ruleModel(), persistenceStrategy, safepointStrategy, EventProcessingOption.STREAM, ClockTypeOption.PSEUDO);

        advanceTimeAndFire(1, TimeUnit.MINUTES);

        insertMatchingSensuStoragePercentEvent(98);
        fireAllRules();

        assertThat(getResults()).as("all events aren't matched within +- 5 minutes each other. the rule should not be fired")
                                .isEmpty();
    }

    @ParameterizedTest
    @MethodSource("strategyProviderStoresOnlyWithExplicitSafepoints")
    void insertFailoverExpireInsertFire_shouldNotFire(PersistedSessionOption.PersistenceStrategy persistenceStrategy, PersistedSessionOption.SafepointStrategy safepointStrategy) {

        createSession(ruleModel(), persistenceStrategy, safepointStrategy, EventProcessingOption.STREAM, ClockTypeOption.PSEUDO);

        insertMatchingSensuProcessStatusEvent("stopped");
        advanceTimeAndFire(1, TimeUnit.MINUTES);

        insertMatchingPingTimeoutEvent(true);
        advanceTimeAndFire(2, TimeUnit.MINUTES);

        assertThat(getResults()).as("2 events. Not yet fired")
                                .isEmpty();

        failover();
        restoreSession(ruleModel(), persistenceStrategy, safepointStrategy, EventProcessingOption.STREAM, ClockTypeOption.PSEUDO);

        advanceTimeAndFire(5, TimeUnit.MINUTES); // timeout

        insertMatchingSensuStoragePercentEvent(98);
        fireAllRules();

        assertThat(getResults()).as("all events aren't matched within +- 5 minutes each other. the rule should not be fired")
                                .isEmpty();
    }
}
