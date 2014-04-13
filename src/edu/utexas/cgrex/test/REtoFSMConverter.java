package edu.utexas.cgrex.test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;

import soot.SootMethod;
import dk.brics.automaton.Automaton;
import dk.brics.automaton.RegExp;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import edu.utexas.cgrex.automaton.AutoEdge;
import edu.utexas.cgrex.automaton.AutoState;
import edu.utexas.cgrex.automaton.RegAutoState;
import edu.utexas.cgrex.automaton.RegAutomaton;
import edu.utexas.cgrex.utils.StringUtil;

/**
 * Converting a regular expression into our regFSM.
 * Notice it may not handle .* properly.
 * @author yufeng
 *
 */
public class REtoFSMConverter implements Serializable{
	
	Map<State, RegAutoState> jsaToAutostate = new HashMap<State,RegAutoState>();
	
	//Regular expression -> JSA FSM -> our RegFSM
	public void doConvert(String reg, 	Map<String, SootMethod> uidToMethMap) {
		///System.out.println("begin to convert...." + reg);
		//step 1. Constructing a reg without .*
		//String cleanReg = reg.replace(".*", "");
		reg = StringEscapeUtils.unescapeJava(reg);
		//step 1. Constructing a reg without .*
		RegExp r = new RegExp(reg);
		Automaton auto = r.toAutomaton(); 
		RegAutomaton regAuto = new RegAutomaton();
		Set<RegAutoState> finiteStates = new HashSet<RegAutoState>();	
		Set<State> states = auto.getStates();
		int number = 1;
				
		for (State s : states) {
			// Map<State, Edge>
			Map<AutoEdge, Set<AutoState>> incomeStates = new HashMap<AutoEdge, Set<AutoState>>(); 
			RegAutoState mystate = new RegAutoState(number++, false, false);
			mystate.setOutgoingStatesInv(incomeStates);

			//use number to represent state id.
			jsaToAutostate.put(s, mystate);
		}
	
		for (State s : states) {
			RegAutoState fsmState = jsaToAutostate.get(s);
			 // Map<State, Edge>
			Map<AutoState,Set<AutoEdge>> outgoingStates = new HashMap<AutoState,Set<AutoEdge>>();
		
			if (s.isAccept()) {
				fsmState.setFinalState();
				regAuto.addFinalState(fsmState);
			} else {//normal states.
				
			}
				
			if (s.equals(auto.getInitialState())) {
				fsmState.setInitState();
				regAuto.addInitState(fsmState);
			}		
					
			for (Transition t : s.getTransitions()) {
				RegAutoState tgtState = jsaToAutostate.get(t.getDest());
				Map tgtIncome = tgtState.getOutgoingStatesInv();
				//using edge label as id.
				String unicode = StringUtil.appendChar(t.getMin(), new StringBuilder(""));
				String shortName = ".";

				if (uidToMethMap.get(unicode) != null) {
					shortName = uidToMethMap.get(unicode).getName();
				}
				AutoEdge outEdge = new AutoEdge(shortName);
				outEdge.setShortName(shortName);
				
				Set<RegAutoState> tmpin = new HashSet<RegAutoState>();
				tmpin.add(fsmState);
				tgtIncome.put(outEdge, tmpin);

				Set<AutoEdge> tmpout = new HashSet<AutoEdge>();
				tmpout.add(outEdge);
				outgoingStates.put(tgtState, tmpout);
			}
			fsmState.setOutgoingStates(outgoingStates);
			finiteStates.add(fsmState);
			
			regAuto.addStates(fsmState);
		}	
		
		//dump current result.
		System.out.println("dump regular graph.");
		regAuto.dump();	
		
	}
	
	String appendChar(char c, StringBuilder b) {
		if (c >= 0x21 && c <= 0x7e && c != '\\' && c != '"')
			b.append(c);
		else {
			b.append("\\u");
			String s = Integer.toHexString(c);
			if (c < 0x10)
				b.append("000").append(s);
			else if (c < 0x100)
				b.append("00").append(s);
			else if (c < 0x1000)
				b.append("0").append(s);
			else
				b.append(s);
		}
		return b.toString();
	}
	
	public static void main(String[] args) {
		String reg = "(c|x).*ab";
		new REtoFSMConverter().doConvert(reg, new HashMap());
	}
}
