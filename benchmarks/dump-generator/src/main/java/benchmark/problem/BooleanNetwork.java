package benchmark.problem;

import com.sun.management.HotSpotDiagnosticMXBean;

import javax.management.MBeanServer;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.*;

/**
 * Boolean network (BN) represents a basic model of N interacting entities, each state of each represented by a boolean
 * value. Each entity can asynchronously update its value (create a successor) based on the values of other entities.
 *
 * Every BN will have a 2^N associated {@link State} objects connected depending on the behaviour of update functions
 * for individual variables. Once the whole state space is populated, we should be able to dump the heap
 * and get a well defined graph that we can explore using OQL.
 *
 * The network is intentionally implemented very poorly in terms of memory consumption, because we want to use
 * it for testing relationships between objects in heap dumps, not to create performant code.
 */
public abstract class BooleanNetwork {

    public static void dumpHeap(String filePath, boolean live) throws IOException {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(
                server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
        mxBean.dumpHeap(filePath, live);
    }

    public final HashMap<State, State> stateSpace = new HashMap<>();
    private final int numVars;
    private final int numStates;

    protected BooleanNetwork(int numVars) {
        if (numVars > 30) throw new IllegalArgumentException("Cannot create Boolean network with 2^"+numVars+" states.");
        this.numVars = numVars;
        this.numStates = 1 << numVars;
        initStateSpace();
    }

    abstract boolean getNewValueForVariable(State state, int variable);

    public void initStateSpace() {
        for (int stateLiteral = 0; stateLiteral < this.numStates; stateLiteral++) {
            State state = literalToState(stateLiteral);
            for (int var = 0; var < numVars; var++) {
                boolean newValue = getNewValueForVariable(state, var);
                if (newValue != state.getValue(var)) {  // if the value is different, we have a new state
                    State successor = state.flipValue(var);
                    stateSpace.putIfAbsent(successor, successor);
                    successor = stateSpace.get(successor);
                    state.addSuccessor(successor);
                }
            }
        }
    }

    private State literalToState(int literal) {
        boolean[] values = new boolean[numVars];
        for (int bit = 0; bit < numVars; bit++) {
            values[bit] = ((literal >> bit) & 1) == 1;
        }
        State created = new State(values);
        stateSpace.putIfAbsent(created, created);
        return stateSpace.get(created);
    }


    /**
     * A state of a boolean network holds an array of boolean values representing the values of individual
     * variables.
     *
     * It also has a list of successor states that can be updated when needed.
     *
     * Two states are equal if they share the same variable valuation.
     */
    public static class State {

        private final boolean[] values;
        private final List<State> successors = new ArrayList<>();

        private State(boolean[] values) {
            this.values = values;
        }

        public boolean getValue(int variable) {
            return this.values[variable];
        }

        public State flipValue(int variable) {
            boolean[] newValues = this.values.clone();
            newValues[variable] = !newValues[variable];
            return new State(newValues);
        }

        public void addSuccessor(State state) {
            for (State s : successors) {
                if (s.equals(state)) return;
            }
            this.successors.add(state);
        }

        @Override
        public String toString() {
            return Arrays.toString(this.values);
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            State state = (State) object;
            return Arrays.equals(values, state.values);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(values);
        }

    }

}
