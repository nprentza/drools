package org.drools.tms.beliefsystem.newbs;

import org.drools.core.common.*;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.reteoo.ObjectTypeConf;
import org.drools.core.spi.Activation;
import org.drools.core.spi.PropagationContext;
import org.drools.tms.LogicalDependency;
import org.drools.tms.agenda.TruthMaintenanceSystemActivation;
import org.drools.tms.beliefsystem.BeliefSet;
import org.drools.tms.beliefsystem.BeliefSystem;
import org.drools.tms.beliefsystem.UpdateRestoreCommand;
import org.drools.tms.beliefsystem.jtms.JTMSBeliefSystem;
import org.drools.tms.beliefsystem.newbs.NewBSBeliefSet.MODE;
import org.drools.tms.beliefsystem.simple.SimpleLogicalDependency;
import org.drools.core.common.EqualityKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class NewBSBeliefSystem <M extends NewBSMode<M>>
        implements
        BeliefSystem<M> {
    protected static final Logger log    = LoggerFactory.getLogger(JTMSBeliefSystem.class);

    private InternalWorkingMemoryEntryPoint ep;
    private TruthMaintenanceSystem tms;

    public NewBSBeliefSystem(InternalWorkingMemoryEntryPoint ep,
                            TruthMaintenanceSystem tms) {
        this.ep = ep;
        this.tms = tms;
    }

    public BeliefSet<M> insert(LogicalDependency<M> node, BeliefSet<M> beliefSet, PropagationContext context, ObjectTypeConf typeConf) {
        return insert( node.getMode(), node.getJustifier().getRule(), node.getJustifier(), node.getObject(), beliefSet, context, typeConf );
    }

    // method conflictsInPropertyUpdates same as in FAI-687:
    private boolean conflictsInPropertyUpdates(NewBSMode lastNode, NewBSMode prevNode){
        UpdateRestoreCommand obj = (UpdateRestoreCommand) prevNode.getLogicalDependency().getObject();
        return ((UpdateRestoreCommand) lastNode.getLogicalDependency().getObject()).conflicts_stateRequired_keyElements(obj);
    }

    // method insert_UpdateCommand from FAI-687:
    private BeliefSet<M> insert_UpdateCommand( M mode, RuleImpl rule, Activation activation, Object payload, BeliefSet<M> beliefSet, PropagationContext context, ObjectTypeConf typeConf ) {

        //JTMSBeliefSet jtmsBeliefSet = (JTMSBeliefSet) beliefSet;
        //JTMSBeliefSetImpl jtmsBeliefSet = (JTMSBeliefSetImpl) beliefSet;
        NewBSBeliefSet nbBeliefSet = (NewBSBeliefSet) beliefSet;

        boolean wasEmpty = nbBeliefSet.isEmpty();
        boolean hadNegCommands = nbBeliefSet.isNegated();
        InternalFactHandle fh =  nbBeliefSet.getFactHandle();

        nbBeliefSet.add( mode );

        UpdateRestoreCommand obj = (UpdateRestoreCommand) mode.getLogicalDependency().getObject();

        if (wasEmpty){
            // if the command inserted is positive then we apply the changes to the instance of the object
            if (mode.getValue().equals("pos")){
                obj.update();
                ep.update(obj.getFactHandle(),obj.getFactHandle().getObject());
            }
            // insert the object into the wm ??
            ep.insert(obj.getFactHandle(),
                    payload,
                    rule,
                    activation != null ? activation.getTuple().getTupleSink() : null,
                    typeConf);
        }else { // we need to check for conflicts first
            if (mode.getValue().equals("neg")){
                // the new update is an update with negation
                //      we need to check for conflicts with previous positive commands,
                //          if such commands are found, we need to undo the changes applied to the instance of the object
                //JTMSMode lastNode= (JTMSMode) jtmsBeliefSet.getLast();
                NewBSMode lastNode= (NewBSMode) nbBeliefSet.getLast(); //
                NewBSMode prevNode= (NewBSMode) nbBeliefSet.getFirst(); //
                while (prevNode!=null && !prevNode.equals(lastNode)){
                    // check for conflicts with previous positive commands
                    if (prevNode.getValue().equals("pos")){
                        if (conflictsInPropertyUpdates(lastNode, prevNode)){
                            // restore changes applied by prevNode
                            UpdateRestoreCommand upRCommObj = (UpdateRestoreCommand) prevNode.getLogicalDependency().getObject();
                            upRCommObj.restoreStateRequired();
                            ep.update(upRCommObj.getFactHandle(),upRCommObj.getFactHandle().getObject());
                            // add conflict reference
                            nbBeliefSet.addConflict(((UpdateRestoreCommand) lastNode.getLogicalDependency().getObject()).getCommandId(), upRCommObj.getCommandId());
                        }
                    }
                    prevNode = (NewBSMode) prevNode.getNext();
                }
            } else { // mode.getValue() == "pos"
                boolean applyUpdates = true;
                if (hadNegCommands){
                    // we need to check for conflicts with previous negative commands,
                    //  if conflicts then we don't apply the updates of the command
                    NewBSMode lastNode= (NewBSMode) nbBeliefSet.getFirst();
                    NewBSMode prevNode= (NewBSMode) lastNode.getNext();
                    while (prevNode!=null && !prevNode.equals(lastNode)){
                        if (prevNode.getValue().equals("neg")) {
                            if (conflictsInPropertyUpdates(lastNode, prevNode)) {
                                applyUpdates = false;
                                // add conflict reference
                                nbBeliefSet.addConflict(((UpdateRestoreCommand) prevNode.getLogicalDependency().getObject()).getCommandId(), ((UpdateRestoreCommand) lastNode.getLogicalDependency().getObject()).getCommandId());
                                break;
                            }
                        }
                        prevNode = (NewBSMode) prevNode.getNext();
                    }
                }
                if (applyUpdates) {
                    obj.update();
                    ep.update(obj.getFactHandle(),obj.getFactHandle().getObject());
                }
            }
        }
        return beliefSet;
    }

    public BeliefSet<M> insert(M mode, RuleImpl rule, TruthMaintenanceSystemActivation activation, Object payload, BeliefSet<M> beliefSet, PropagationContext context, ObjectTypeConf typeConf) {
        if ( log.isTraceEnabled() ) {
            log.trace( "TMSInsert {} {}", payload, mode.getValue() );
        }
        // a temporary solution to updateRestoreCommand native support
        if (mode.getLogicalDependency().getObject() instanceof UpdateRestoreCommand){
            // using FAI-687 solution
            return insert_UpdateCommand(mode,rule,activation,payload, beliefSet, context, typeConf);

        } else { return null; }
    }

    // method from FAI-687
    private void delete_updateCommand( M mode, RuleImpl rule, Activation activation, Object payload, BeliefSet<M> beliefSet, PropagationContext context ){

        NewBSBeliefSet<M> newBSBeliefSet = (NewBSBeliefSet<M>) beliefSet;

        InternalFactHandle fh =  newBSBeliefSet.getFactHandle();

        // todo: remove from beliefSet at the end of the process ???
        beliefSet.remove( mode );

        UpdateRestoreCommand obj = (UpdateRestoreCommand) mode.getLogicalDependency().getObject();

        boolean commandInConflict = newBSBeliefSet.commandInConflict(obj.getCommandId(),mode.getValue());

        // TODO:
        if (commandInConflict){
            UpdateRestoreCommand firstObj;
            if (mode.getValue().equals("neg")){
                // get positive commands in conflict with this negative command
                //  apply there updates
                ArrayList<String> posCommansInConflict = newBSBeliefSet.getConflicts(obj.getCommandId());
                NewBSMode firstNode= (NewBSMode) newBSBeliefSet.getLast();
                while (firstNode!=null && firstNode.getValue().equals("pos")){
                    firstObj = (UpdateRestoreCommand) firstNode.getLogicalDependency().getObject();
                    if (posCommansInConflict.contains(firstObj.getCommandId())){
                        firstObj.update();
                        ep.update(firstObj.getFactHandle(),firstObj.getFactHandle().getObject());
                    }
                    firstNode = (NewBSMode) firstNode.getPrevious();
                }
                // remove the conflict from the list
                newBSBeliefSet.removeConflict_NegCommand(obj.getCommandId());
            }else {
                // deleting a positive command that is inConflict with a negative command
                //  this means that changes of this command were not applied or applied and then retracted (because of the conflict)
                //  we only have to remove the reference of this positive command from the conflicts list
                newBSBeliefSet.removeConflict_PosCommand(obj.getCommandId());
            }
        }else{
            //  deleting a command not in conflict
            //      if the command is positive we need to undo the changes applied first
            if (mode.getValue().equals("pos")){
                obj.restoreStateRequired();
                ep.update(obj.getFactHandle(),obj.getFactHandle().getObject());
            }
        }

        if ( beliefSet.isEmpty() && fh.getEqualityKey().getStatus() == EqualityKey.JUSTIFIED ) {
            // the set is empty, so delete form the EP, so things are cleaned up.
            ep.delete(fh, fh.getObject(), getObjectTypeConf(beliefSet), context.getRuleOrigin(),
                    null, activation != null ? activation.getTuple().getTupleSink() : null );
        } else  {
            // ??
        }

        if ( beliefSet.isEmpty() ) {
            // if the beliefSet is empty, we must null the logical handle
            EqualityKey key = fh.getEqualityKey();
            key.setLogicalFactHandle( null );
            //  the following method is missing from the  current version of EqualityKey??
            //key.setBeliefSet(null);

            if ( key.getStatus() == EqualityKey.JUSTIFIED ) {
                // if it's stated, there will be other handles, so leave it in the TMS
                tms.remove( key );
            }
        }
    }

    @Override
    public void delete(LogicalDependency<M> node, BeliefSet<M> beliefSet, PropagationContext context) {
        delete( node.getMode(), node.getJustifier().getRule(), node.getJustifier(), node.getObject(), beliefSet, context );
    }

    @Override
    public void delete(M mode, RuleImpl rule, Activation activation, Object payload, BeliefSet<M> beliefSet, PropagationContext context) {

        // a temporary solution to updateRestoreCommand native support
        if (mode.getLogicalDependency().getObject() instanceof UpdateRestoreCommand){
            // using FAI-687 solution
            delete_updateCommand(mode,rule,activation,payload, beliefSet, context);
        }
    }

    public BeliefSet newBeliefSet(InternalFactHandle fh) {
        return new NewBSBeliefSet( this, fh );
    }

    public LogicalDependency newLogicalDependency(TruthMaintenanceSystemActivation<M> activation, BeliefSet<M> beliefSet, Object object, Object value) {
        NewBSMode<M> mode = asMode( value );
        SimpleLogicalDependency dep =  new SimpleLogicalDependency(activation, beliefSet, object, mode);
        mode.setLogicalDependency( dep );

        return dep;
    }

    public void read(LogicalDependency<M> node, BeliefSet<M> beliefSet, PropagationContext context, ObjectTypeConf typeConf) {
        throw new UnsupportedOperationException( "This is not serializable yet" );
    }

    //
    public void stage(PropagationContext context, BeliefSet<M> beliefSet) {
        InternalFactHandle bfh = beliefSet.getFactHandle();
        // Remove the FH from the network
        ep.delete(bfh, bfh.getObject(), getObjectTypeConf(beliefSet), context.getRuleOrigin(), null);
    }

    //
    public void unstage(PropagationContext context, BeliefSet<M> beliefSet) {
        InternalFactHandle bfh = beliefSet.getFactHandle();

        // Add the FH back into the network
        ep.insert(bfh, bfh.getObject(), context.getRuleOrigin(), null, getObjectTypeConf(beliefSet) );
    }

    // what is the purpose of this?
    private ObjectTypeConf getObjectTypeConf(BeliefSet<M> jtmsBeliefSet) {
        InternalFactHandle fh = jtmsBeliefSet.getFactHandle();
        ObjectTypeConfigurationRegistry reg;
        ObjectTypeConf typeConf;
        reg = ep.getObjectTypeConfigurationRegistry();
        typeConf = reg.getOrCreateObjectTypeConf( ep.getEntryPoint(), fh.getObject() );
        return typeConf;
    }

    public TruthMaintenanceSystem getTruthMaintenanceSystem() {
        return this.tms;
    }

    @Override
    public M asMode(Object value) {
        NewBSMode<M> mode;
        if ( value == null ) {
            mode = new NewBSMode(MODE.POSITIVE.getId(), this);
        } else if ( value instanceof String ) {
            if ( MODE.POSITIVE.getId().equals( value ) ) {
                mode = new NewBSMode(MODE.POSITIVE.getId(), this);
            }   else {
                mode = new NewBSMode(MODE.NEGATIVE.getId(), this);
            }
        } else {
            mode = new NewBSMode(((MODE)value).getId(), this);
        }
        return (M) mode;

    }
}
