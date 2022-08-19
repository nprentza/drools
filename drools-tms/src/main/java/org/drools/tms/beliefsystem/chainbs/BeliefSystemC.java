/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
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
package org.drools.tms.beliefsystem.chainbs;

import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.rule.consequence.Activation;
import org.drools.core.common.PropagationContext;
import org.drools.tms.beliefsystem.BeliefSet;
import org.drools.tms.beliefsystem.BeliefSystem;
import org.drools.tms.beliefsystem.ModedAssertion;

public interface BeliefSystemC<M extends ModedAssertion<M>> extends BeliefSystem<M> {

    void setNextBSInChain(BeliefSystemC nextBSInChain);

    void processRequest(Object request);

    /*
     temporary here
     */
    void update(M mode, RuleImpl rule, Activation activation, Object payload, BeliefSet<M> beliefSet, PropagationContext context);

}
