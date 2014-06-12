package edu.vanderbilt.batman.inference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.vanderbilt.batman.model.ConsistentValueConstraint;
import edu.vanderbilt.batman.model.EqualityConstraint;
import edu.vanderbilt.batman.model.UserOwnershipConstraint;
import edu.vanderbilt.batman.model.PropagationPathConstraint;
import edu.vanderbilt.batman.model.SQLSample;
import edu.vanderbilt.batman.model.Variable;

public class WebInteractionAnalyzer {
	
	// infer the propagate paths from sql responses to web responses.
	public static Set<PropagationPathConstraint> inferResponsePropagatePath(List<SQLSample> samples) {
		
		//processWebResponses(samples);
		
		Set<PropagationPathConstraint> constraints = new HashSet<PropagationPathConstraint>();
		
		List<Set<Variable>> webRespSamples = new ArrayList<Set<Variable>>();
		List<Set<Variable>> sqlRespSamples = new ArrayList<Set<Variable>>();
		
		boolean print = false;
		
		for (SQLSample sample: samples) {
			
			if (sample.getUser().getRole().getRole() == 2 &&
					sample.getSqlQuery().getSkeleton().equals("SELECT blogdata.post_id, blogdata.subject, blogdata.message, blogusername.user, blogdata.datetime, blogusername.id, blogdata.user_id FROM blogdata, blogusername WHERE blogdata.user_id = blogusername.id")) {
				//print = true;
				if (print) System.out.println(sample.getWebRequest().getRequestKey());
				if (print) System.out.println(sample.getSqlQuery().getQueryStr());
			}
			
			Set<Variable> webRespVars = WebResponseAnalyzer.getWebResponseVariables(sample.getWebResponse());
			//Set<Variable> webRespVars = sample.getWebResponse().getDynamicVariables();
			
			if (print) {
				System.out.println("Web Response Var:");
				for (Variable var: webRespVars) {
					System.out.println(var.getName() + " " + var.getValues());
				}
			}
			
			Set<Variable> sqlRespVars = new HashSet<Variable>();
			boolean sqlResponseEmpty = true;   // assume
			for (String name: sample.getSqlResponse().getValues().keySet()) {
				Variable var = new Variable(sample.getSqlResponse().getSqlquery().getSkeleton(), name);  // context: sql query skeleton.
				for (String value: sample.getSqlResponse().getValues().get(name)) {
					var.addValue(value);
				}
				sqlRespVars.add(var);
				if (var.getValues().size() != 0) sqlResponseEmpty = false;
			}
			
			if (print) {
				System.out.println("SQL Response Var:");
				for (Variable var: sqlRespVars) {
					System.out.println(var.getName() + " " + var.getValues());
				}
			}
			
			if (!sqlResponseEmpty) {
				webRespSamples.add(webRespVars);
				sqlRespSamples.add(sqlRespVars);
			}
		}
		
		Set<UserOwnershipConstraint> consts = InferenceEngine.inferMembershipConstraint(sqlRespSamples, webRespSamples, InferenceEngine.MODE_SUBSTRING_MATCH);
		
		if (print) {
			System.out.println("Membershipconst: ");
			for (UserOwnershipConstraint c: consts) {
				System.out.println(c.toString());
			}
		}
		
		// merge membership const to generate propagate path const.
		HashMap<String, Set<UserOwnershipConstraint>> constsByXpath = new HashMap<String, Set<UserOwnershipConstraint>>();
		for (UserOwnershipConstraint constraint: consts) {
			if (!constsByXpath.containsKey(constraint.getMemberVariable().getName())) {
				constsByXpath.put(constraint.getMemberVariable().getName(), new HashSet<UserOwnershipConstraint>());			
			}
			constsByXpath.get(constraint.getMemberVariable().getName()).add(constraint);
		}
		
		// for each xpath (dest var), only one src.
		for (String xpath: constsByXpath.keySet()) {
			
			HashMap<String, Set<Variable>> srcVarsByContext = new HashMap<String, Set<Variable>>();
			for (UserOwnershipConstraint constraint: constsByXpath.get(xpath)) {
				String index = constraint.getMemberVariable().getContext();
				if (!srcVarsByContext.containsKey(index)) {
					srcVarsByContext.put(index, new HashSet<Variable>());
				}
				srcVarsByContext.get(index).add(constraint.getOwnerVariable());
			}
			
			Set<Variable> srcVars = new HashSet<Variable>();
			boolean initial = true;
			for (String context: srcVarsByContext.keySet()) {
				if (initial){
					srcVars.addAll(srcVarsByContext.get(context));
					initial = false;
				} else {
					srcVars.retainAll(srcVarsByContext.get(context));
				}
			}
			
			if (srcVars.size() > 1) {
				if (print) System.err.println("One destination xpath should have only one source! src var: " + srcVars);
				if (print) {
					System.out.println(xpath);
					for (UserOwnershipConstraint constraint: constsByXpath.get(xpath)) {
						System.out.println(constraint.toString());
					}
				}
				continue;
			}
			
			for (Variable srcVar: srcVars) {
				Variable dest = new Variable(xpath, xpath);   // use xpath as both context and name.
				PropagationPathConstraint constraint = new PropagationPathConstraint(srcVar, dest);
				constraints.add(constraint);
			}
		}
		
		if (print) {
			System.out.println("Response propagate path: ");
			for (PropagationPathConstraint c: constraints) {
				System.out.println(c.toString());
			}
		}
		
		/*
		HashMap<Variable, Set<UserOwnershipConstraint>> constsBySqlRespVariable = new HashMap<Variable, Set<UserOwnershipConstraint>>();
		for (UserOwnershipConstraint constraint: consts) {
			if (!constsBySqlRespVariable.containsKey(constraint.getOwnerVariable())) {
				constsBySqlRespVariable.put(constraint.getOwnerVariable(), new HashSet<UserOwnershipConstraint>());			
			}
			constsBySqlRespVariable.get(constraint.getOwnerVariable()).add(constraint);
		}
		
		for (Variable sqlRespVariable: constsBySqlRespVariable.keySet()) {
			// merge xpath and prune:
			String xpath = "";
			boolean merged = true;
			for (UserOwnershipConstraint constraint: constsBySqlRespVariable.get(sqlRespVariable)) {
				if (xpath.equals("")) {
					xpath = constraint.getMemberVariable().getName();    // general xpath.
				} else if (!constraint.getMemberVariable().getName().equals(xpath)) {
					merged = false;
					break;
				}
			}
			if (merged && !xpath.equals("")) {
				Variable dest = new Variable(xpath, xpath);   // use xpath as both context and name.
				PropagationPathConstraint constraint = new PropagationPathConstraint(sqlRespVariable, dest);
				constraints.add(constraint);
			}
		}
		*/
		return constraints;
	}
	
	
	private static void processWebResponses(List<SQLSample> samples) {
		
		List<Set<Variable>> respVarSamples = new ArrayList<Set<Variable>>();
		
		for (SQLSample sample: samples) {
			Set<Variable> webRespVars = WebResponseAnalyzer.getWebResponseVariables(sample.getWebResponse());
			sample.getWebResponse().getDynamicVariables().addAll(webRespVars);
			respVarSamples.add(webRespVars);
		}
		
		Set<ConsistentValueConstraint> consistentRespConsts = InferenceEngine.inferConsistentValueConstraint(respVarSamples);
		Set<Variable> consistentRespVars = new HashSet<Variable>();
		for (ConsistentValueConstraint c: consistentRespConsts) {
			consistentRespVars.add(c.getVariable());
		}
		
		for (SQLSample sample: samples) {
			sample.getWebResponse().getDynamicVariables().removeAll(consistentRespVars);
		}
	}
	
	
	
	// infer the propagate paths from web request parameters to sql query parameters.
	public static Set<PropagationPathConstraint> inferParameterPropagatePath(List<SQLSample> samples) {
		
		Set<PropagationPathConstraint> constraints = new HashSet<PropagationPathConstraint>();
		
		boolean print = false;
		
		List<Set<Variable>> webRequestSamples = new ArrayList<Set<Variable>>();
		List<Set<Variable>> sqlQuerySamples = new ArrayList<Set<Variable>>();
		for (SQLSample sample: samples) {
			if (sample.getSqlQuery().getSkeleton().equals("DELETE FROM blogdata WHERE blogdata.post_id = {STRING}")) {
				//print = true;
			}
			
			Set<Variable> webRequestVars = new HashSet<Variable>();
			HashMap<String, String> parameters = sample.getWebRequest().getAllParameters();
			for (String name: parameters.keySet()) {
				if (parameters.get(name)!= null) {
					Variable var = new Variable(sample.getWebRequest().getRequestKey(), name);    // context: request key.
					var.addValue(parameters.get(name));
					webRequestVars.add(var);
				}
			}
			webRequestSamples.add(webRequestVars);
			if (print) {
				System.out.println("Web Request Var:");
				for (Variable var: webRequestVars) {
					System.out.println(var.getName() + " " + var.getValues());
				}
			}
			
			Set<Variable> sqlQueryVars = new HashSet<Variable>();
			for (String name: sample.getSqlQuery().getValues().keySet()) {
				Variable var = new Variable(sample.getSqlQuery().getSkeleton(), name);  // context: query skeleton
				var.addValue(sample.getSqlQuery().getValues().get(name));
				sqlQueryVars.add(var);
			}
			sqlQuerySamples.add(sqlQueryVars);
			if (print) {
				System.out.println("SQL Query Var:");
				for (Variable var: sqlQueryVars) {
					System.out.println(var.getName() + " " + var.getValues());
				}
			}
			
		}
		
		// strict match
		Set<EqualityConstraint> equalityConsts = InferenceEngine.inferEqualityConstraint(webRequestSamples, sqlQuerySamples);
		for (EqualityConstraint equalityConst: equalityConsts) {
			PropagationPathConstraint constraint = new PropagationPathConstraint(equalityConst.getLeftSideVariable(), equalityConst.getRightSideVariable());
			constraints.add(constraint);
		}
		
		return constraints;
	}
	
	
}
