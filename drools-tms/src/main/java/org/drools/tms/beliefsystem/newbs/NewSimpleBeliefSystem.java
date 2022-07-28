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

package org.drools.tms.beliefsystem.newbs;

import org.drools.core.common.EqualityKey;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.ObjectTypeConfigurationRegistry;
import org.drools.core.common.PropagationContext;
import org.drools.core.common.TruthMaintenanceSystem;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.reteoo.ObjectTypeConf;
import org.drools.core.rule.EntryPointId;
import org.drools.core.rule.consequence.Activation;
import org.drools.tms.LogicalDependency;
import org.drools.tms.SimpleMode;
import org.drools.tms.TruthMaintenanceSystemEqualityKey;
import org.drools.tms.agenda.TruthMaintenanceSystemActivation;
import org.drools.tms.beliefsystem.BeliefSet;
import org.drools.tms.beliefsystem.BeliefSystemC;
import org.drools.tms.beliefsystem.simple.SimpleLogicalDependency;


public class NewSimpleBeliefSystem implements BeliefSystemC<SimpleMode> {

    private BeliefSystemC nextBSInChain;
    private ObjectTypeConfigurationRegistry reg;
    private EntryPointId entrypoint;
    private TruthMaintenanceSystem tms;

    public NewSimpleBeliefSystem(BeliefSystemC nextBSInChain,ObjectTypeConfigurationRegistry reg, EntryPointId entrypoint, TruthMaintenanceSystem tms) {
        super();
        this.nextBSInChain = nextBSInChain;
        this.reg = reg;
        this.entrypoint = entrypoint;
        this.tms = tms;
    }

    @Override
    public SimpleMode asMode( Object value ) {
        return new SimpleMode();
    }

    @Override
    public void setNextBSInChain(BeliefSystemC nextBSInChain) {
        this.nextBSInChain = nextBSInChain;
    }

    @Override
    public void update(SimpleMode mode, RuleImpl rule, Activation activation, Object payload, BeliefSet<SimpleMode> beliefSet, PropagationContext context) {

    }

    public BeliefSet<SimpleMode> insert(LogicalDependency<SimpleMode> node,
                                        BeliefSet<SimpleMode> beliefSet,
                                        PropagationContext context,
                                        ObjectTypeConf typeConf) {

        boolean empty = beliefSet.isEmpty();

        beliefSet.add( node.getMode() );

        InternalFactHandle bfh = beliefSet.getFactHandle();
        if ( empty && bfh.getEqualityKey().getStatus() == EqualityKey.JUSTIFIED ) {
            this.nextBSInChain.insert(node, beliefSet, context, typeConf);
            /*ep.insert( bfh, //ep.insert( bfh,
                    bfh.getObject(),
                    node.getJustifier().getRule(),
                    node.getJustifier().getTuple().getTupleSink(),
                    typeConf );*/
        }
        return beliefSet;
    }

    public BeliefSet<SimpleMode> insert( SimpleMode mode,
                                         RuleImpl rule,
                                         TruthMaintenanceSystemActivation activation,
                                         Object payload,
                                         BeliefSet<SimpleMode> beliefSet,
                                         PropagationContext context,
                                         ObjectTypeConf typeConf) {
        boolean empty = beliefSet.isEmpty();

        beliefSet.add( mode );

        InternalFactHandle bfh = beliefSet.getFactHandle();
        if ( empty && bfh.getEqualityKey().getStatus() == EqualityKey.JUSTIFIED ) {
            this.nextBSInChain.insert(mode,rule,activation,payload,beliefSet,context,typeConf);
            /*ep.insert( bfh,
                    bfh.getObject(),
                    rule,
                    activation != null ? activation.getTuple().getTupleSink() : null,
                    typeConf );*/
        }
        return beliefSet;

    }

    public void read(LogicalDependency<SimpleMode> node,
                     BeliefSet<SimpleMode> beliefSet,
                     PropagationContext context,
                     ObjectTypeConf typeConf) {
        //insert(node, beliefSet, context, typeConf );
        beliefSet.add( node.getMode() );
    }

    public void delete(LogicalDependency<SimpleMode> node,
                       BeliefSet<SimpleMode> beliefSet,
                       PropagationContext context) {
        delete( node.getMode(), node.getJustifier().getRule(), node.getJustifier(), node.getObject(), beliefSet, context );
    }

    @Override
    public void delete( SimpleMode mode, RuleImpl rule, Activation activation, Object payload, BeliefSet<SimpleMode> beliefSet, PropagationContext context ) {

        //this.nextBSInChain.delete(mode, rule, activation, payload, beliefSet, context);
        beliefSet.remove( mode );

        InternalFactHandle bfh = beliefSet.getFactHandle();

        if ( beliefSet.isEmpty() && bfh.getEqualityKey() != null && bfh.getEqualityKey().getStatus() == EqualityKey.JUSTIFIED ) {
            this.nextBSInChain.delete(mode,rule,activation,payload,beliefSet,context);
            /*ep.delete(bfh, bfh.getObject(), getObjectTypeConf(beliefSet), context.getRuleOrigin(),
                    null, activation != null ? activation.getTuple().getTupleSink() : null);*/
        } else if ( !beliefSet.isEmpty() && bfh.getObject() == payload && payload != bfh.getObject() ) {
            // prime has changed, to update new object
            // Equality might have changed on the object, so remove (which uses the handle id) and add back in
            this.nextBSInChain.update(mode,rule,activation,payload,beliefSet,context);
            /*WorkingMemoryEntryPoint ep = bfh.getEntryPoint(this.ep.getReteEvaluator());
            ep.getObjectStore().updateHandle(bfh, beliefSet.getFirst().getObject().getObject());
            ep.update( bfh, bfh.getObject(), allSetButTraitBitMask(), Object.class, null );*/
        }

        if ( beliefSet.isEmpty() && bfh.getEqualityKey() != null ) {
            // if the beliefSet is empty, we must null the logical handle
            EqualityKey key = bfh.getEqualityKey();
            key.setLogicalFactHandle( null );
            ((TruthMaintenanceSystemEqualityKey)key).setBeliefSet( null );

            if ( key.getStatus() == EqualityKey.JUSTIFIED ) {
                // if it's stated, there will be other handles, so leave it in the TMS
                tms.remove( key );
            }
        }
    }

    public void stage(PropagationContext context,
                      BeliefSet<SimpleMode> beliefSet) {

        this.nextBSInChain.stage(context, beliefSet);

        /*
        InternalFactHandle bfh = beliefSet.getFactHandle();
        // Remove the FH from the network
        ep.delete(bfh, bfh.getObject(), getObjectTypeConf(beliefSet), context.getRuleOrigin(), null);

        bfh.getEqualityKey().setStatus( EqualityKey.STATED ); // revert to stated
        */
    }

    public void unstage(PropagationContext context,
                        BeliefSet<SimpleMode> beliefSet) {
        this.nextBSInChain.unstage(context,beliefSet);
        /*
        InternalFactHandle bfh = beliefSet.getFactHandle();
        bfh.getEqualityKey().setStatus( EqualityKey.JUSTIFIED ); // revert to justified

        // Add the FH back into the network
        ep.insert(bfh, bfh.getObject(), context.getRuleOrigin(), null, getObjectTypeConf(beliefSet) );
        */
    }

    @Override
    public TruthMaintenanceSystem getTruthMaintenanceSystem() {
        return null;
    }


    private ObjectTypeConf getObjectTypeConf(BeliefSet beliefSet) {
        InternalFactHandle fh = beliefSet.getFactHandle();
        //ObjectTypeConfigurationRegistry reg;
        ObjectTypeConf typeConf;
        //reg = ep.getObjectTypeConfigurationRegistry();
        typeConf = this.reg.getOrCreateObjectTypeConf( this.entrypoint, fh.getObject() ); //p.getEntryPoint()
        return typeConf;
    }

    public BeliefSet newBeliefSet(InternalFactHandle fh) {
        return new NewSimpleBeliefSet( this, fh );
    }

    public LogicalDependency newLogicalDependency(TruthMaintenanceSystemActivation activation,
                                                  BeliefSet beliefSet,
                                                  Object object,
                                                  Object value) {
        SimpleMode mode = new SimpleMode();
        SimpleLogicalDependency dep =  new SimpleLogicalDependency( activation, beliefSet, object, mode );
        mode.setObject( dep );
        return dep;
    }

    public BeliefSystemC getNextBSInChain() {
        return this.nextBSInChain;
    }

    public void setEp( BeliefSystemC nextBSInChain ) {
        this.nextBSInChain = nextBSInChain;
    }

}
