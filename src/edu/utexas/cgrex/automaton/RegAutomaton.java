package edu.utexas.cgrex.automaton;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// refsm is the master fsm
public class RegAutomaton extends Automaton {

	Map<AutoState, Set<AutoEdge>> annotOneStep = new HashMap<AutoState, Set<AutoEdge>>();
	
	Map<AutoState, AnnotTwoStepWrapper> annotTwoSteps = new HashMap<AutoState, AnnotTwoStepWrapper>();
	
	class AnnotTwoStepWrapper {
		
		Set<AutoEdge> firstStep = new HashSet<AutoEdge>();
		
		Set<AutoEdge> secondStep = new HashSet<AutoEdge>();
		
		public AnnotTwoStepWrapper(Set<AutoEdge> firstStep, Set<AutoEdge> secondStep) {
			this.firstStep = firstStep;
			this.secondStep = secondStep;
		}
		
		public void setFirstStep(Set<AutoEdge> firstStep) {
			this.firstStep = firstStep;
		}
		
		public void setSecondStep(Set<AutoEdge> secondStep) {
			this.secondStep = secondStep;
		}
		
		public Set<AutoEdge> getFirstStep() {
			return this.firstStep;
		}
		
		public Set<AutoEdge> getSecondStep() {
			return this.secondStep;
		}
	}
	
	// find() method returns a map that maps each state in refsm with a dot edge
	// to a set of edges that must be followed to reach the one of the final
	// state in refsm
	public void buildOneStepAnnot() {
		// Map<AutoState, Set<AutoEdge>> opts = new HashMap<AutoState,
		// Set<AutoEdge>>();
		for (AutoState s : states) {
			// we want to optimize for the states with cycle
			// so we only care about states that have (.*) edge
			RegAutoState currState = (RegAutoState) s;

			// // we can replace this by hasCycleEdge() if the only possible
			// // cycle is dot edge
			// if (currState.hasCycleEdge()) {
			// // we do not regard final state as a qualified state
			// // and do not create an keyEdges set for the final state
			// // but we can change this freely by removing the following two
			// // LOC
			// if (currState.isFinalState())
			// continue;
			//
			// Set<AutoEdge> keyEdges = new HashSet<AutoEdge>();
			// opts.put(currState, keyEdges);
			//
			// for (AutoEdge e : currState.getOutgoingStatesInvKeySet()) {
			// AutoEdge eg = (AutoEdge) e;
			// if (!eg.isDotEdge())
			// keyEdges.add(eg);
			// else if (currState.outgoingStatesInvLookup(eg).equals(
			// currState)) {
			// keyEdges.clear();
			// break;
			// }
			// }
			// }

			if (currState.isFinalState())
				continue;

			Set<AutoEdge> keyEdges = new HashSet<AutoEdge>();
			annotOneStep.put(currState, keyEdges);

			for (AutoEdge eg : currState.getOutgoingStatesInvKeySet()) {
				if (!eg.isDotEdge())
					keyEdges.add(eg);
				else {
					Set<AutoState> out = currState.outgoingStatesInvLookup(eg);
					for (AutoState os : out) {
						if (!os.equals(currState)) {
							// System.out.println("***" + os + " " + currState);
							keyEdges.clear();
							break;
						}
					}
				}
			}

		}
//		test();
		// System.out.println(opts);
	}
	
	protected void test() {
		StringBuilder b = new StringBuilder("info\n");
		b.append(annotOneStep);
		

		try {
			BufferedWriter bufw = new BufferedWriter(new FileWriter(
					"sootOutput/debug"));

			bufw.write(b.toString());

			bufw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public Map<AutoState, Set<AutoEdge>> getOneStepAnnot() {
		return annotOneStep;
	}
	
	public void dumpFindOneStep() {
		for (AutoState s: states) {
			System.out.println("STATE: " + s.getId());
			Set<AutoEdge> keyEdges = annotOneStep.get(s);
			if (keyEdges == null) {
				assert(s.isFinalState() == true);
				System.out.println("This is FINAL state!!");
			} else {
				System.out.println("One step must go through following edges: ");
				System.out.println(keyEdges);
			}
		}
	}
}
