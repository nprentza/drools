package org.drools.tms.beliefsystem.newbs;

import org.drools.core.beliefsystem.Mode;
import org.drools.core.util.AbstractBaseLinkedListNode;
import org.drools.tms.LogicalDependency;
import org.drools.tms.beliefsystem.BeliefSystem;
import org.drools.tms.beliefsystem.ModedAssertion;

public class NewBSMode<M extends NewBSMode<M>> extends AbstractBaseLinkedListNode<M> implements ModedAssertion<M> {
    private BeliefSystem<M> bs;
    private String value; // "pos" or "neg"
    private String commandId;
    private LogicalDependency<M> dep;
    private Mode nextMode;

    public NewBSMode(String value, BeliefSystem bs) {
        this.value = value;
        this.bs = bs;
    }
    public NewBSMode(String value, String commandId, BeliefSystem bs) {
        this.value = value;
        this.commandId = commandId;
        this.bs = bs;
    }
    public NewBSMode(String value, BeliefSystem bs,  Mode nextMode) {
        this.value = value;
        this.bs = bs;
        this.nextMode = nextMode;
    }
    public NewBSMode(String value, String commandId, BeliefSystem bs,  Mode nextMode) {
        this.value = value;
        this.commandId = commandId;
        this.bs = bs;
        this.nextMode = nextMode;
    }
    @Override
    public BeliefSystem getBeliefSystem() {
        return bs;
    }

    public String getValue() {
        return value;
    }

    public String getCommandId(){return commandId;}

    public LogicalDependency<M> getLogicalDependency() {
        return dep;
    }

    public void setLogicalDependency(LogicalDependency<M> dep) {
        this.dep = dep;
    }

    public Mode getNextMode() {
        return nextMode;
    }
}
