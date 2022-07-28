package org.drools.tms.beliefsystem.newbs;

import org.drools.core.common.EqualityKey;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.InternalWorkingMemoryEntryPoint;
import org.drools.core.common.ObjectTypeConfigurationRegistry;
import org.drools.core.common.PropagationContext;
import org.drools.core.common.TruthMaintenanceSystem;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.reteoo.ObjectTypeConf;
import org.drools.core.rule.consequence.Activation;
import org.drools.tms.LogicalDependency;
import org.drools.tms.agenda.TruthMaintenanceSystemActivation;
import org.drools.tms.beliefsystem.BeliefSet;
import org.drools.tms.beliefsystem.BeliefSystemC;
import org.drools.tms.beliefsystem.ModedAssertion;


public class EndPointAdapter implements BeliefSystemC {

    private InternalWorkingMemoryEntryPoint ep;

    public EndPointAdapter(InternalWorkingMemoryEntryPoint ep){
        this.ep =ep;
    }

    @Override
    public BeliefSet insert(LogicalDependency node, BeliefSet beliefSet, PropagationContext context, ObjectTypeConf typeConf) {

        InternalFactHandle bfh = beliefSet.getFactHandle();

        this.ep.insert( bfh, //ep.insert( bfh,
                    bfh.getObject(),
                    node.getJustifier().getRule(),
                    node.getJustifier().getTuple().getTupleSink(),
                    typeConf );

        return null;
    }

    @Override
    public BeliefSet insert(ModedAssertion mode, RuleImpl rule, TruthMaintenanceSystemActivation activation, Object payload, BeliefSet beliefSet, PropagationContext context, ObjectTypeConf typeConf) {
        InternalFactHandle bfh = beliefSet.getFactHandle();
        ep.insert( bfh,
                    bfh.getObject(),
                    rule,
                    activation != null ? activation.getTuple().getTupleSink() : null,
                    typeConf );
        return null;
    }

    @Override
    public void delete(LogicalDependency node, BeliefSet beliefSet, PropagationContext context) {

    }

    @Override
    public void delete(ModedAssertion mode, RuleImpl rule, Activation activation, Object payload, BeliefSet beliefSet, PropagationContext context) {
        InternalFactHandle bfh = beliefSet.getFactHandle();
        ep.delete(bfh, bfh.getObject(), getObjectTypeConf(beliefSet), context.getRuleOrigin(),
                null, activation != null ? activation.getTuple().getTupleSink() : null);
    }

    private ObjectTypeConf getObjectTypeConf(BeliefSet beliefSet) {
        InternalFactHandle fh = beliefSet.getFactHandle();
        ObjectTypeConfigurationRegistry reg; // it used to be commented out
        ObjectTypeConf typeConf;
        reg = ep.getObjectTypeConfigurationRegistry(); // it used to be commented out
        //  the following line used to refer to this.reg instead of reg
        //  and this.entrypoint instead of ep.getEntryPoint()
        typeConf = reg.getOrCreateObjectTypeConf(ep.getEntryPoint(), fh.getObject() );
        return typeConf;
    }

    @Override
    public BeliefSet newBeliefSet(InternalFactHandle fh) {
        return null;
    }

    @Override
    public LogicalDependency newLogicalDependency(TruthMaintenanceSystemActivation activation, BeliefSet beliefSet, Object object, Object value) {
        return null;
    }

    @Override
    public void read(LogicalDependency node, BeliefSet beliefSet, PropagationContext context, ObjectTypeConf typeConf) {

    }

    @Override
    public void stage(PropagationContext context, BeliefSet beliefSet) {
        InternalFactHandle bfh = beliefSet.getFactHandle();
        // Remove the FH from the network
        ep.delete(bfh, bfh.getObject(), getObjectTypeConf(beliefSet), context.getRuleOrigin(), null);

        bfh.getEqualityKey().setStatus( EqualityKey.STATED ); // revert to stated
    }

    @Override
    public void unstage(PropagationContext context, BeliefSet beliefSet) {
        InternalFactHandle bfh = beliefSet.getFactHandle();
        bfh.getEqualityKey().setStatus( EqualityKey.JUSTIFIED ); // revert to justified

        // Add the FH back into the network
        ep.insert(bfh, bfh.getObject(), context.getRuleOrigin(), null, getObjectTypeConf(beliefSet) );
    }

    @Override
    public TruthMaintenanceSystem getTruthMaintenanceSystem() {
        return null;
    }

    @Override
    public ModedAssertion asMode(Object value) {
        return null;
    }

    @Override
    public void setNextBSInChain(BeliefSystemC nextBSInChain) {

    }

    @Override
    public void update(ModedAssertion mode, RuleImpl rule, Activation activation, Object payload, BeliefSet beliefSet, PropagationContext context) {
        InternalFactHandle bfh = beliefSet.getFactHandle();
        // prime has changed, to update new object
        // Equality might have changed on the object, so remove (which uses the handle id) and add back in
        /*
            the following 3 lines of code are copied from SimpleBeliefSet (BeliefSet<SimpleMode> beliefSet)
            at this point, beliefSet.getFirst().getObject() returns error as beliefSet is of type BeliefSet and not BeliefSet<SimpleMode>
        WorkingMemoryEntryPoint ep = bfh.getEntryPoint(this.ep.getReteEvaluator());
        ep.getObjectStore().updateHandle(bfh, beliefSet.getFirst().getObject().getObject());
        ep.update( bfh, bfh.getObject(), allSetButTraitBitMask(), Object.class, null );
        */
    }

}
