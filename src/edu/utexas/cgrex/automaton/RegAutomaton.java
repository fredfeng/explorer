package edu.utexas.cgrex.automaton;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// refsm is the master fsm
public class RegAutomaton extends Automaton {

	// turn off debug before push to git
	private boolean debug = false;

	Map<AutoState, Set<AutoEdge>> annotOneStep = new HashMap<AutoState, Set<AutoEdge>>();

	Map<AutoState, AnnotTwoStepsWrapper> annotTwoSteps = new HashMap<AutoState, AnnotTwoStepsWrapper>();

	// this method returns a map that maps each state in refsm with a dot edge
	// to a set of edges that must be followed to reach the one of the final
	// state in refsm
	public void buildOneStepAnnot() {
		// Map<AutoState, Set<AutoEdge>> opts = new HashMap<AutoState,
		// Set<AutoEdge>>();
		for (AutoState s : states) {
			// we want to optimize for the states with cycle
			// so we only care about states that have (.*) edge
			RegAutoState currState = (RegAutoState) s;

			// we do not need to annotate final states
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
	}

	public void buildTwoStepAnnot() {
		if (debug)
			System.out.println("In RegAutomaton buildTwoStepAnnot method....");
		// LinkedList<AutoState> workList = new LinkedList<AutoState>();

		// this is annotating the first step
		for (AutoState s : states) {
			// we want to optimize for the states with cycle
			// so we only care about states that have (.*) edge

			// we do not need to annotate final states
			if (s.isFinalState())
				continue;

			AnnotTwoStepsWrapper keyEdges = new AnnotTwoStepsWrapper();
			annotTwoSteps.put(s, keyEdges);

			// annotate the first step
			for (AutoEdge eg : s.getOutgoingStatesInvKeySet()) {
				if (!eg.isDotEdge())
					keyEdges.addFirstStep(eg);
				else {
					Set<AutoState> out = s.outgoingStatesInvLookup(eg);
					for (AutoState os : out) {
						if (!os.equals(s)) {
							keyEdges.clearFirstStep();
							break;
						}
					}
				}
			}

		}

		// this is annotating the second step
		long count = 0;
		Set<AutoState> annotated = new HashSet<AutoState>();
		for (AutoState s : states) {
			count++;
			assert (count < 1000);

			if (s.isFinalState())
				continue;

			if (annotated.contains(s))
				continue;

			annotated.add(s);

			boolean jumpToNext = false;
			for (AutoState fs : finalStates) {
				if (s.getOutgoingStatesKeySet().contains(fs)) {
					jumpToNext = true;
					break;
				}
			}
			if (jumpToNext)
				continue;

			assert (annotTwoSteps.containsKey(s));

			AnnotTwoStepsWrapper keyEdges = annotTwoSteps.get(s);
			for (AutoState next : s.getOutgoingStatesKeySet()) {
				if (next.equals(s))
					continue;

				assert (annotTwoSteps.containsKey(next));

				keyEdges.addSecondStep(annotTwoSteps.get(next).getFirstStep());
			}
		}
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

	public Map<AutoState, AnnotTwoStepsWrapper> getTwoStepAnnot() {
		return annotTwoSteps;
	}

	public void dumpFindOneStep() {
		for (AutoState s : states) {
			System.out.println("STATE: " + s.getId());
			Set<AutoEdge> keyEdges = annotOneStep.get(s);
			if (keyEdges == null) {
				assert (s.isFinalState() == true);
				System.out.println("This is FINAL state!!");
			} else {
				System.out
						.println("One step must go through following edges: ");
				System.out.println(keyEdges);
			}
		}
	}

	public void dumpFindTwoStep() {
		for (AutoState s : states) {
			System.out.println("STATE: " + s.getId());
			AnnotTwoStepsWrapper keyEdges = annotTwoSteps.get(s);
			if (keyEdges == null) {
				assert (s.isFinalState() == true);
				System.out.println("This is FINAL state!!");
			} else {
				System.out
						.println("One step must go through following edges: ");
				System.out.println("The first step is: ");
				System.out.println(keyEdges.getFirstStep());
				System.out.println("The second step is: ");
				System.out.println(keyEdges.getSecondStep());
			}
		}
	}
}
