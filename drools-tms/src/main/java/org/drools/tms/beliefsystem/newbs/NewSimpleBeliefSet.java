package org.drools.tms.beliefsystem.newbs;

import org.drools.tms.beliefsystem.BeliefSet;
import org.drools.tms.beliefsystem.BeliefSystem;
import org.drools.tms.SimpleMode;
import org.drools.core.common.InternalFactHandle;
import org.drools.tms.LogicalDependency;
import org.drools.core.common.WorkingMemoryAction;
import org.drools.core.common.PropagationContext;
import org.drools.core.util.LinkedList;
import org.drools.core.util.LinkedListEntry;
import org.drools.tms.beliefsystem.chainbs.BeliefSystemC;

public class NewSimpleBeliefSet extends LinkedList<SimpleMode> implements BeliefSet<SimpleMode> {
    protected BeliefSystemC beliefSystem;

    protected InternalFactHandle fh;

    protected WorkingMemoryAction wmAction;

    public NewSimpleBeliefSet(BeliefSystemC beliefSystem, InternalFactHandle fh) {
        this.beliefSystem = beliefSystem;
        this.fh = fh;
    }

    public NewSimpleBeliefSet() {
    }


    public BeliefSystem getBeliefSystem() {
        return (BeliefSystem) beliefSystem;
    }

    public InternalFactHandle getFactHandle() {
        return this.fh;
    }

    public void cancel(PropagationContext context) {
        // get all but last, as that we'll do via the BeliefSystem, for cleanup
        SimpleMode entry = getFirst();
        while (entry != getLast()) {
            SimpleMode temp = entry.getNext(); // get next, as we are about to remove it
            final LogicalDependency<SimpleMode> node = entry.getObject();
            node.getJustifier().getLogicalDependencies().remove( node );
            remove( entry );
            entry = temp;
        }

        LinkedListEntry last = getFirst();
        final LogicalDependency<SimpleMode> node = (LogicalDependency) last.getObject();
        node.getJustifier().getLogicalDependencies().remove( node );
        beliefSystem.delete( node, this, context );
    }

    public void clear(PropagationContext context) {
        // remove all, but don't allow the BeliefSystem to clean up, the FH is most likely going to be used else where
        SimpleMode entry = getFirst();
        while (entry != null) {
            SimpleMode temp = entry.getNext(); // get next, as we are about to remove it
            final LogicalDependency<SimpleMode> node = entry.getObject();
            node.getJustifier().getLogicalDependencies().remove( node );
            remove( entry );
            entry = temp;
        }
    }

    public WorkingMemoryAction getWorkingMemoryAction() {
        return wmAction;
    }

    public void setWorkingMemoryAction(WorkingMemoryAction wmAction) {
        this.wmAction = wmAction;
    }

    @Override
    public boolean isNegated() {
        return false;
    }

    @Override
    public boolean isDecided() {
        return true;
    }

    @Override
    public boolean isConflicting() {
        return false;
    }

    @Override
    public boolean isPositive() {
        return ! isEmpty();
    }
}