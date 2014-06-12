package edu.vanderbilt.batman.inference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.vanderbilt.batman.model.EqualityConstraint;
import edu.vanderbilt.batman.model.UserOwnershipConstraint;
import edu.vanderbilt.batman.model.ConsistentValueConstraint;
import edu.vanderbilt.batman.model.UserSpecificVariableConstraint;
import edu.vanderbilt.batman.model.Variable;

/**
 * This class is designed to infer the constraints from given samples. TODO: refactor later. summarize constraint types.
 * @author xli
 *
 */
public class InferenceEngine {
	
	public static int MODE_EXACT_MATCH = 0;
	public static int MODE_SUBSTRING_MATCH = 1;
	
	
	/**
	 * Infer the user consistent parameter constraint that the value of a variable is always the same.
	 * @param samples
	 * @return
	 */
	public static Set<ConsistentValueConstraint> inferConsistentValueConstraint(List<Set<Variable>> samples) {
		Set<ConsistentValueConstraint> constraints = new HashSet<ConsistentValueConstraint>();
		if (samples.size() < 2) 
			{
				//System.out.println("<2 samples");
				return constraints;  // need more samples for confidence.
			}		
		
		HashMap<Variable, String> merged = new HashMap<Variable, String>();
		boolean print = false;
		
		for (int i = 0; i < samples.size(); i++) {
			HashMap<Variable, String> intersect = new HashMap<Variable, String>();
			
			Set<Variable> sample = samples.get(i);
			
			for (Variable var: sample) {
				if (var.getValues().size() < 1) continue;   // not necessarily one value.
				boolean changed = false;
				String value = var.getValues().get(0);
				for (String v: var.getValues()){
					if (!v.equals(value)) {
						changed = true;
						break;
					}
				}
				if (changed)  continue;
				if (i == 0 || (merged.containsKey(var) && merged.get(var).equals(value))) {
					intersect.put(var, value);
				}
			}
			
			if (print) {
				System.out.println("Sample: " + i);
				for (Variable var: intersect.keySet()) {
					System.out.println("intersect: " + var.getName() + " " + intersect.get(var));
				}
			}
			
			merged.clear();
			merged.putAll(intersect);
		}
		
		for (Variable var: merged.keySet()) {
			constraints.add(new ConsistentValueConstraint(var, merged.get(var)));
		}
		return constraints;
	}
	
	
	/**
	 * Infer the user bound parameter constraint that the value of a variable is specific to a user.
	 * @param samples
	 * @return
	 */
	public static Set<UserSpecificVariableConstraint> inferSpecificValueConstraint(List<Set<Variable>> samples) {
		
		Set<UserSpecificVariableConstraint> constraints = new HashSet<UserSpecificVariableConstraint>();
		if (samples.size() < 2)  return constraints;    // need at least two samples (e.g., users) for differential analysis.
		
		HashMap<Variable, Set<String>> merged = new HashMap<Variable, Set<String>>();
		
		//debug;
		/*
		System.out.println("inferSpecificValueConstraint: samples.size(): " + samples.size());
		for (Set<Variable> element: samples){
			
			for (Variable var: element)
				System.out.println("inferSpecificValueConstraint var: " + var.toString());
		}
		*/
		
		for (int i = 0; i < samples.size(); i++) {
			
			HashMap<Variable, Set<String>> intersect = new HashMap<Variable, Set<String>>();
			Set<Variable> sample = samples.get(i);
			//System.out.println("samples.get(i).size() " + sample.size());
			
			for (Variable var: sample) {	
				
				if (var.getValues().size() < 1) continue;			
				
				String value = var.getValues().get(0);
				boolean changed = false;
				for (String v: var.getValues()){
					if (!v.equals(value)) {
						changed = true;
						break;
					}
				}
				if (changed)  continue;
				//System.out.println("var.getValues().get(0) " + var.getValues().get(0));
				
				if (i == 0 || (merged.containsKey(var) && !merged.get(var).contains(value))) {
					intersect.put(var, new HashSet<String>());
					intersect.get(var).add(value);
					if (i != 0) {
						intersect.get(var).addAll(merged.get(var));
					}
				}
			}
			merged.clear();
			merged.putAll(intersect);
		}
		
		for (Variable var: merged.keySet()) {
			for (String value: merged.get(var)) {
				var.getValues().add(value);     // add value for testing.
			}
			constraints.add(new UserSpecificVariableConstraint(var));
		}
		if (constraints.size()>0){
			for (UserSpecificVariableConstraint userconstraint: constraints) {
				//System.out.println("inferSpecificValueConstraint var name: " + userconstraint.getVariable());
			}
		}
		
		
		return constraints;
	}
	
	
	/**
	 * Infer the membership constraint that the value of a variable always belongs to a set.
	 * @param responses
	 * @param samples
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Set<UserOwnershipConstraint> inferMembershipConstraint(List<Set<Variable>> ownerSamples, 
																		 List<Set<Variable>> memberSamples, int mode) {
		
		Set<UserOwnershipConstraint> constraints = new HashSet<UserOwnershipConstraint>();
		
		if (ownerSamples.size() != memberSamples.size()) {
			System.err.println("Sample size doesn't match! owner sample size: " + ownerSamples.size() + " member sample size: " + memberSamples.size());
			return constraints;
		}
		
		HashMap<Variable, Set<String>> memberVarValues = new HashMap<Variable, Set<String>>();
		
		HashSet<UserOwnershipConstraint> blacklist = new HashSet<UserOwnershipConstraint>();
		
		for (int i = 0; i < ownerSamples.size(); i++) {
			Set<Variable> ownerSample  = ownerSamples.get(i);
			Set<Variable> memberSample  = memberSamples.get(i);
			
			Set<UserOwnershipConstraint> intersect = new HashSet<UserOwnershipConstraint>();
			
			for (Variable memberVar: memberSample) {

				if (memberVar.getValues().size() != 1) continue; // early continue;
				
				String memberValue = memberVar.getValues().get(0);
				for (Variable ownerVar: ownerSample) {
					
					UserOwnershipConstraint constraint = new UserOwnershipConstraint(ownerVar, memberVar);
					if (blacklist.contains(constraint)) continue;
					
					boolean contain = false;
					if (mode == MODE_EXACT_MATCH) {
						if (ownerVar.getValues().contains(memberValue)) {
							contain = true;
						}
					} else if (mode == MODE_SUBSTRING_MATCH) {
						Set<String> mstrSet = new HashSet<String>(Arrays.asList(memberValue.split(" ")));
						for (String value: ownerVar.getValues()) {
							Set<String> ostrSet = new HashSet<String>(Arrays.asList(value.split(" ")));
							// all tokens in ostrs occur in mstrs
							if (mstrSet.containsAll(ostrSet)) {
								contain = true;
								break;
							}
							/*
							if (memberValue.contains(value)) {
								contain = true;
								break;
							} */
						}
					}
					
					if (!contain) {
						blacklist.add(constraint);
					}
					
					if (contain) {
						if (!memberVarValues.containsKey(memberVar)) {
							memberVarValues.put(memberVar, new HashSet<String>());
						}
						memberVarValues.get(memberVar).add(memberValue);
						
						// relax the constraint: not necessarily appear in every sample.
						for (String value: memberVarValues.get(memberVar)) {
							constraint.getMemberVariable().addValue(value);      // add value for testing.
						}
						intersect.add(constraint);
						
						/*
						if (i == 0 || constraints.contains(constraint)) {
							for (String value: memberVarValues.get(memberVar)) {
								constraint.getMemberVariable().addValue(value);      // add value for testing.
							}
							intersect.add(constraint);
						}*/
						//if (print) System.out.println(intersect);
					}
				}
			}
			constraints.removeAll(blacklist);
			constraints.removeAll(intersect);    // NOTE: constraint contains updated member values.
			constraints.addAll(intersect);
		}
		return constraints;
	}
	
	/**
	 * Infer the equality constraint: the value of variable1 is always equal to that of variable2, given two set of samples.
	 * @param sampleSet1
	 * @param sampleSet2
	 * @return a set of equality constraints.
	 */
	public static Set<EqualityConstraint> inferEqualityConstraint(List<Set<Variable>> sampleSet1, 
			                                                      List<Set<Variable>> sampleSet2) {
		
		Set<EqualityConstraint> constraints = new HashSet<EqualityConstraint>();
		// early termination.
		if (sampleSet1.size() != sampleSet2.size()) {
			System.err.println("Sample size doesn't match!!");
			return constraints;
		}
		
		for (int i = 0; i < sampleSet1.size(); i++ ) {
			Set<Variable> sample1 = sampleSet1.get(i);
			Set<Variable> sample2 = sampleSet2.get(i);
			
			Set<EqualityConstraint> intersect = new HashSet<EqualityConstraint>();
			for (Variable var1: sample1) {
				if (var1.getValues().size() != 1) continue;
				String value1 = var1.getValues().get(0);
				for (Variable var2: sample2) {
					if (var2.getValues().size() != 1) continue;
					String value2 = var2.getValues().get(0);
					
					if (value1.equals(value2)) {
						EqualityConstraint constraint = new EqualityConstraint(var1, var2);
						
						if (i == 0 || constraints.contains(constraint)) {
							intersect.add(constraint);
						}
					}
				}
			}
			
			constraints.clear();
			constraints.addAll(intersect);
		}
		return constraints;
	}
}