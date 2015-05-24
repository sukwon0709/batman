package edu.vanderbilt.batman;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.vanderbilt.batman.inference.RolePolicyAnalyzer;
import edu.vanderbilt.batman.model.RoleAuthPolicy;
import edu.vanderbilt.batman.model.SQLOperation;
import edu.vanderbilt.batman.model.TestVector;
import edu.vanderbilt.batman.model.WebSession;
import edu.vanderbilt.batman.testing.TestGenerator;
import edu.vanderbilt.batman.util.TraceMerger;
import edu.vanderbilt.batman.util.UserManager;


/**
 * This class is designed to implement the workflow for constraint inference.
 * @author xli
 *
 */
class ConstraintAnalyzer {
	
	public static void main(String args[]) {
		
		String working_dir = "/home/soh/trace/Batman/";
		//String project = "scarf";
		//String project = "securephoto";
		//String project = "events";
	    //String project = "bloggit";
		//String project = "minibloggie";
	    String project = "wackopicko";
		//String project = "jspblog";
		
		try {
			ConstraintAnalyzer.run(working_dir, project);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * The workflow of constraint inference and test generation (Offline).
	 * @throws Exception
	 */
	public static void run(String working_dir, String project) throws Exception {
		String trace_dir = working_dir + project + "/";
		// Step I: parse the raw traces.
		ArrayList<WebSession> sessions = TraceMerger.merge(working_dir, project);
		
		
		// Step II: policy inference
		Set<RoleAuthPolicy> policySet = RolePolicyAnalyzer.inferRoleAuthPolicy(sessions);
		RolePolicyAnalyzer.output(trace_dir + project + "_RolePolicy", policySet);
		
		// statistics:
		Set<SQLOperation> operations = new HashSet<SQLOperation>();
		int uniqueConst = 0;
		int ownerConst = 0;
		for (RoleAuthPolicy policy: policySet) {
			System.out.println("Role: " + policy.getRole().getRole());
			System.out.println("Privilege set size: " + policy.getPrivileges().size());
			operations.addAll(policy.getPrivileges());
			for (SQLOperation operation: policy.getPrivileges()) {
				uniqueConst += operation.getSpecificValueConsts().size();
				ownerConst += operation.getOwnershipConsts().size();
			}
		}
		System.out.println("Unique Const size: " + uniqueConst);
		System.out.println("Owner Const size: " + ownerConst);
		System.out.println("Operation set size: " + operations.size());
		
		
		// Step III: generate test cases. in future, change to online testing!!
		String login_file = trace_dir + project + "_LoginProfile";
		UserManager.loadUsers(login_file);
		List<TestVector> testCases = TestGenerator.generateTestCases(policySet);  // rely on SQLOperationAnalyzer.markedSqlSamples.
		TestGenerator.output(trace_dir + project + "_TestProfile", testCases);
		
	}
	
}