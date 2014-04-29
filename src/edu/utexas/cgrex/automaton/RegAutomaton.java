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
		System.out.println("In RegAutomaton buildTwoStepAnnot method....");
		LinkedList<AutoState> workList = new LinkedList<AutoState>();
		Set<AutoState> annotated = new HashSet<AutoState>();

		// annotate the last second states (the states right before the final
		// states)
		// for these states, we only fill the first step set
		for (AutoState fs : finalStates) {
			for (AutoState s : fs.getIncomingStatesKeySet()) {
				if (s.isFinalState)
					continue;

				annotated.add(s);

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
				// add more elements in the worklist
				for (AutoState prev : s.getIncomingStatesKeySet()) {
					if (!annotated.contains(prev))
						workList.add(prev);
				}
			}
		}

		// for (AutoState s: states) {
		// workList.add(s);
		// }

		// a worklist algorithm to annotate all the other states
		while (!workList.isEmpty()) {

			AutoState head = workList.poll();

			if (annotated.contains(head))
				continue;

			// dependency check
			boolean restart = false;
			for (AutoState next : head.getOutgoingStatesKeySet()) {
				if (!next.equals(head) && !annotated.contains(next)) {
					// if (!annotated.contains(next)) {
					restart = true;
					break;
				}
			}
			if (restart) {
				workList.add(head);
				continue;
			}

			System.out.println("******Annotating state " + head);

			if (head.isFinalState)
				continue;

			annotated.add(head);

			AnnotTwoStepsWrapper keyEdges = new AnnotTwoStepsWrapper();
			annotTwoSteps.put(head, keyEdges);
			System.out.println("*****Before annotating first steps "
					+ keyEdges.getFirstStep());
			// annotate the first step
			for (AutoEdge eg : head.getOutgoingStatesInvKeySet()) {
				if (!eg.isDotEdge())
					keyEdges.addFirstStep(eg);
				else {
					Set<AutoState> out = head.outgoingStatesInvLookup(eg);
					for (AutoState os : out) {
						if (!os.equals(head)) {
							keyEdges.clearFirstStep();
							break;
						}
					}
				}
			}
			System.out.println("*****After annotating first steps: "
					+ keyEdges.getFirstStep());
			System.out.println("*****Before annotating second steps: "
					+ keyEdges.getSecondStep());
			// annotate the second step
			for (AutoState next : head.getOutgoingStatesKeySet()) {
				if (next.equals(head))
					continue;
				assert (annotTwoSteps.containsKey(next));
				// System.out.println("head: " + head);
				// System.out.println("next: " + next);
				keyEdges.addSecondStep(annotTwoSteps.get(next).getFirstStep());
			}
			System.out.println("*****After annotating second steps "
					+ keyEdges.getSecondStep());
			// add more elements in the worklist
			for (AutoState prev : head.getIncomingStatesKeySet()) {
				if (!annotated.contains(prev))
					workList.add(prev);
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
