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
import org.drools.tms.beliefsystem.chainbs.BeliefSystemC;
import org.drools.tms.beliefsystem.chainbs.UpdateRestoreCommand;
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
    public void processRequest(Object request) {
        this.nextBSInChain.processRequest(request);
    }

    @Override
    public void update(SimpleMode mode, RuleImpl rule, Activation activation, Object payload, BeliefSet<SimpleMode> beliefSet, PropagationContext context) {

    }

    public BeliefSet<SimpleMode> insert(LogicalDependency<SimpleMode> node,
                                        BeliefSet<SimpleMode> beliefSet,
                                        PropagationContext context,
                                        ObjectTypeConf typeConf) {

        beliefSet.add( node.getMode() );

        if (node.getObject() instanceof UpdateRestoreCommand){
            UpdateRestoreCommand objRequest = (UpdateRestoreCommand)node.getObject();
            this.processRequest(objRequest);
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

        beliefSet.add( mode );

        if (mode.getObject() instanceof UpdateRestoreCommand){
            UpdateRestoreCommand objRequest = (UpdateRestoreCommand)mode.getObject();
            this.processRequest(objRequest);
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

        beliefSet.remove( mode );

        InternalFactHandle bfh = beliefSet.getFactHandle();

        if ( beliefSet.isEmpty() && bfh.getEqualityKey() != null && bfh.getEqualityKey().getStatus() == EqualityKey.JUSTIFIED ) {
            this.nextBSInChain.delete(mode,rule,activation,payload,beliefSet,context);
        } else if ( !beliefSet.isEmpty() && bfh.getObject() == payload && payload != bfh.getObject() ) {
            // prime has changed, to update new object
            // Equality might have changed on the object, so remove (which uses the handle id) and add back in
            this.nextBSInChain.update(mode,rule,activation,payload,beliefSet,context);
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

    }

    public void unstage(PropagationContext context,
                        BeliefSet<SimpleMode> beliefSet) {
        this.nextBSInChain.unstage(context,beliefSet);
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

