package edu.utexas.cgrex.automaton;

import java.util.HashSet;
import java.util.Set;

public class AnnotTwoStepsWrapper {
	// if the set contains nothing, meaning it can go forward anyway
	Set<AutoEdge> firstStep = new HashSet<AutoEdge>();

	Set<AutoEdge> secondStep = new HashSet<AutoEdge>();

	public AnnotTwoStepsWrapper() {
	}

	public AnnotTwoStepsWrapper(Set<AutoEdge> firstStep,
			Set<AutoEdge> secondStep) {
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

	public boolean addFirstStep(AutoEdge edge) {
		return firstStep.add(edge);
	}

	public boolean addFirstStep(Set<AutoEdge> edges) {
		return firstStep.addAll(edges);
	}

	public boolean addSecondStep(AutoEdge edge) {
		return secondStep.add(edge);
	}

	public boolean addSecondStep(Set<AutoEdge> edges) {
		return secondStep.addAll(edges);
	}

	public void clearFirstStep() {
		firstStep.clear();
	}

	public void clearSecondStep() {
		secondStep.clear();
	}

	public boolean isEmpty() {
		return firstStep.isEmpty() && secondStep.isEmpty();
	}

	public boolean firstStepIsEmpty() {
		return firstStep.isEmpty();
	}

	public boolean secondStepIsEmpty() {
		return secondStep.isEmpty();
	}

}