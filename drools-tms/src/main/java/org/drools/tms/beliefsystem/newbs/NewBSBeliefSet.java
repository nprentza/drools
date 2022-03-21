package org.drools.tms.beliefsystem.newbs;

import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.WorkingMemoryAction;
import org.drools.core.spi.PropagationContext;
import org.drools.core.util.LinkedList;
import org.drools.tms.LogicalDependency;
import org.drools.tms.beliefsystem.BeliefSystem;
import org.drools.tms.beliefsystem.jtms.JTMSBeliefSet;
import org.drools.tms.beliefsystem.jtms.JTMSBeliefSetImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NewBSBeliefSet<M extends NewBSMode<M>> extends LinkedList<M> implements JTMSBeliefSet<M> {

    private BeliefSystem beliefSystem;
    private InternalFactHandle rootHandle;
    private WorkingMemoryAction wmAction;

    private int posCounter = 0;
    private int negCounter = 0;

    private Map<String, Object> conflicts;

    public NewBSBeliefSet(BeliefSystem<M> beliefSystem, InternalFactHandle rootHandle) {
        this.beliefSystem = beliefSystem;
        this.rootHandle = rootHandle;
        this.conflicts = new HashMap<>();
    }

    public NewBSBeliefSet() {
    }

    @Override
    public void add(M node ) {
        NewBSMode mode = node;
        String value = mode.getValue();
        boolean neg = MODE.NEGATIVE.getId().equals( value );
        if ( neg ) {
            super.addLast( node ); //we add negatives to end
            negCounter++;
        } else {
            super.addFirst( node ); // we add positied to start
            posCounter++;
        }
        //TODO: check for conflicts ??
    }

    @Override
    public void remove(M node ) {
        super.remove(node);

        NewBSMode mode = node;
        String value = mode.getValue();
        boolean neg = MODE.NEGATIVE.getId().equals( value );
        if ( neg ) {
            negCounter--;
        } else {
            posCounter--;
        }

        //TODO: check for conflicts ??
    }

    /*
     methods for conflicts management - start
     */
    // todo: document, explain ..
    public void addConflict(String negCommandId, String posCommandId){
        ArrayList<String> posCommands = new ArrayList<String>();
        if (conflicts.get(negCommandId) != null) {
            posCommands = (ArrayList<String>) conflicts.get(negCommandId);
            this.conflicts.remove(negCommandId);
        }
        posCommands.add(posCommandId);
        this.conflicts.put(negCommandId, posCommands);
    }
    // todo: document, explain ..
    public void removeConflict_NegCommand(String negCommandId){
        if (conflicts.get(negCommandId) != null) {
            this.conflicts.remove(negCommandId);
        }
    }
    // todo: document, explain ..
    public void removeConflict_PosCommand(String posCommandId){
        ArrayList<String> posCommands = new ArrayList<String>();
        for (Map.Entry<String, Object> entry : conflicts.entrySet()){
            posCommands = (ArrayList<String>) entry.getValue();
            if (posCommands.contains(posCommandId)){
                posCommands.remove(posCommandId);
                conflicts.remove(entry.getKey());
                conflicts.put(entry.getKey(),posCommands);
            }
        }
    }
    // todo: document, explain ..
    public ArrayList<String> getConflicts(String negCommandId){
        ArrayList<String> posCommands = new ArrayList<String>();
        if (conflicts.get(negCommandId) != null) {
            posCommands = (ArrayList<String>) conflicts.get(negCommandId);
        }
        return posCommands;
    }
    // todo: document, explain ..
    public boolean posCommandInConflict(String posCommandId){
        boolean inConflict = false;

        for (Map.Entry<String, Object> entry : conflicts.entrySet()){
            if (((ArrayList<String>) entry.getValue()).contains(posCommandId)){
                inConflict = true;
                break;
            }
        }

        return inConflict;
    }
    // todo: document, explain ..
    public boolean commandInConflict(String commandId, String value){
        boolean inConflict = false;

        for (Map.Entry<String, Object> entry : conflicts.entrySet()){
            if (value.equals("pos")){
                if (((ArrayList<String>) entry.getValue()).contains(commandId)){
                    inConflict = true;
                    break;
                }
            }else {
                if (entry.getKey().equals(commandId)){
                    inConflict = true;
                    break;
                }
            }

        }
        return inConflict;
    }
    /*
       conflicts management methods - end
     */
    public BeliefSystem getBeliefSystem() {
        return beliefSystem;
    }

    public InternalFactHandle getFactHandle() {
        return this.rootHandle;
    }

    public void cancel(PropagationContext context) {
        // get all but last, as that we'll do via the BeliefSystem, for cleanup
        // note we don't update negative, conflict counters. It's needed for the last cleanup operation
        NewBSMode<M> entry = getFirst();
        while (entry != getLast()) {
            NewBSMode<M> temp = entry.getNext(); // get next, as we are about to remove it
            final LogicalDependency<M> node =  entry.getLogicalDependency();
            node.getJustifier().getLogicalDependencies().remove( node );
            remove( (M) entry );
            entry = temp;
        }

        NewBSMode<M> last = getFirst();
        final LogicalDependency node = last.getLogicalDependency();
        node.getJustifier().getLogicalDependencies().remove( node );
        beliefSystem.delete( node, this, context );
    }

    public void clear(PropagationContext context) {
        // remove all, but don't allow the BeliefSystem to clean up, the FH is most likely going to be used else where
        NewBSMode<M> entry = getFirst();
        while (entry != null) {
            NewBSMode<M> temp =  entry.getNext(); // get next, as we are about to remove it
            final LogicalDependency<M> node = entry.getLogicalDependency();
            node.getJustifier().getLogicalDependencies().remove( node );
            remove( (M) entry );
            entry = temp;
        }
    }

    public void setWorkingMemoryAction(WorkingMemoryAction wmAction) {
        this.wmAction = wmAction;
    }

    public WorkingMemoryAction getWorkingMemoryAction() {
        return this.wmAction;
    }

    @Override
    public boolean isNegated() {
        return negCounter>0;
    }

    @Override
    public boolean isDecided() { return !isConflicting(); }

    @Override
    public boolean isConflicting() { return conflicts.size()>0; }

    @Override
    public boolean isPositive() {
        return negCounter == 0 && posCounter > 0;
    }

    // todo: the following block of code (public enum MODE ..) is exactly the same as in JTMSBeliefSetImpl,
    //      we should consider a different implementation?
    public enum MODE {
        POSITIVE( "pos" ),
        NEGATIVE( "neg" );

        private String string;
        MODE( String string ) {
            this.string = string;
        }

        public String toExternalForm() {
            return this.string;
        }

        @Override
        public String toString() {
            return this.string;
        }

        public String getId() {
            return this.string;
        }

        public static NewBSBeliefSet.MODE resolve(Object id ) {
            if ( id == null ) {
                return null;
            } else if( NEGATIVE == id || NEGATIVE.getId().equalsIgnoreCase( id.toString() ) ) {
                return NEGATIVE;
            } else if( POSITIVE == id || POSITIVE.getId().equalsIgnoreCase( id .toString()) ) {
                return POSITIVE;
            } else {
                return null;
            }
        }
    }
}
