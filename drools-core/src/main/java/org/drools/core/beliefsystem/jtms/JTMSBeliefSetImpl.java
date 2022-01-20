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

package org.drools.core.beliefsystem.jtms;

import org.drools.core.beliefsystem.BeliefSystem;
import org.drools.core.common.InternalFactHandle;
import org.drools.core.common.LogicalDependency;
import org.drools.core.common.WorkingMemoryAction;
import org.drools.core.spi.PropagationContext;
import org.drools.core.util.LinkedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class JTMSBeliefSetImpl<M extends JTMSMode<M>> extends LinkedList<M> implements JTMSBeliefSet<M> {

    private BeliefSystem<M> beliefSystem;

    private WorkingMemoryAction wmAction;

    private InternalFactHandle rootHandle;

    private int posCounter = 0;
    private int negCounter = 0;

    // FAI-687
    private Map<String, Object> conflicts;

    public JTMSBeliefSetImpl(BeliefSystem<M> beliefSystem, InternalFactHandle rootHandle) {
        this.beliefSystem = beliefSystem;
        this.rootHandle = rootHandle;
        // FAI-687
        this.conflicts = new HashMap<>();
    }

    // FAI-687
    public void addConflict(String negCommandId, String posCommandId){
        ArrayList<String> posCommands = new ArrayList<String>();
        if (conflicts.get(negCommandId) != null) {
            posCommands = (ArrayList<String>) conflicts.get(negCommandId);
            this.conflicts.remove(negCommandId);
        }
        posCommands.add(posCommandId);
        this.conflicts.put(negCommandId, posCommands);
    }
    // FAI-687
    public void removeConflict_NegCommand(String negCommandId){
        if (conflicts.get(negCommandId) != null) {
            this.conflicts.remove(negCommandId);
        }
    }
    // FAI-687
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
    // FAI-687
    public ArrayList<String> getConflicts(String negCommandId){
        ArrayList<String> posCommands = new ArrayList<String>();
        if (conflicts.get(negCommandId) != null) {
            posCommands = (ArrayList<String>) conflicts.get(negCommandId);
        }
        return posCommands;
    }
    // FAI-687
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
    // FAI-687
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

    public JTMSBeliefSetImpl() {
    }

    @Override
    public void add(M node ) {
        JTMSMode mode = node;
        String value = mode.getValue();
        boolean neg = MODE.NEGATIVE.getId().equals( value );
        if ( neg ) {
            super.addLast( node ); //we add negatives to end
            negCounter++;
        } else {
            super.addFirst( node ); // we add positied to start
            posCounter++;
        }
    }
    
    @Override
    public void remove(M node ) {
        super.remove(node);

        JTMSMode mode = node;
        String value = mode.getValue();

        boolean neg = MODE.NEGATIVE.getId().equals( value );
        if ( neg ) {
            negCounter--;
        } else {
            posCounter--;
        }

    }    
    
    public BeliefSystem getBeliefSystem() {
        return beliefSystem;
    }

    public InternalFactHandle getFactHandle() {
        return this.rootHandle;
    }
    
    @Override
    public boolean isNegated() {
        // FAI-687
        //return posCounter == 0 && negCounter > 0;
        return negCounter > 0;
    }

    @Override
    public boolean isDecided() {
        return !isConflicting();
    }

    @Override
    public boolean isConflicting() {
        return posCounter > 0 && negCounter > 0;
    }

    @Override
    public boolean isPositive() {
        return negCounter == 0 && posCounter > 0;
    }    
    
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

        public static MODE resolve( Object id ) {
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

    public void cancel(PropagationContext context) {        
        // get all but last, as that we'll do via the BeliefSystem, for cleanup
        // note we don't update negative, conflict counters. It's needed for the last cleanup operation
        JTMSMode<M> entry = getFirst();
        while (entry != getLast()) {
            JTMSMode<M> temp = entry.getNext(); // get next, as we are about to remove it
            final LogicalDependency<M> node =  entry.getLogicalDependency();
            node.getJustifier().getLogicalDependencies().remove( node );
            remove( (M) entry );
            entry = temp;
        }

        JTMSMode<M> last = getFirst();
        final LogicalDependency node = last.getLogicalDependency();
        node.getJustifier().getLogicalDependencies().remove( node );
        beliefSystem.delete( node, this, context );
    }
    
    public void clear(PropagationContext context) { 
        // remove all, but don't allow the BeliefSystem to clean up, the FH is most likely going to be used else where
        JTMSMode<M> entry = getFirst();
        while (entry != null) {
            JTMSMode<M> temp =  entry.getNext(); // get next, as we are about to remove it
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
}
