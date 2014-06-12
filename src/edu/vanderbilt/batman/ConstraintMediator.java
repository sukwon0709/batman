package edu.vanderbilt.batman;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import edu.vanderbilt.batman.model.GenericSQLQueryProfile;
import edu.vanderbilt.batman.model.PropagationPathConstraint;
import edu.vanderbilt.batman.model.RoleAuthPolicy;
import edu.vanderbilt.batman.model.SQLOperation;
import edu.vanderbilt.batman.model.UserOwnershipConstraint;
import edu.vanderbilt.batman.model.UserRole;
import edu.vanderbilt.batman.model.UserSpecificVariableConstraint;

// INFERENCE::
// review response prop path by source code (file); figure out how to debug efficiently. more than 1 src var (substring vs. exact match)

// minibloggie, securephoto: ownership different from membership before!!
// events: one action missing: DELETE from admin;

// bloggit, scarf: new trace.
// crawler vs. simulator: leave for later.

// TESTING:: state prep.

// crawl, test JSP apps!!

//Coverage: php, (jsp), relative increase ratio. over baseline (wget)


public class ConstraintMediator {
	
	static BufferedWriter bw = null;

	public static void main(String[] args) {
		
		String working_dir = "/Users/xli/Desktop/trace/Batman/";
		//String project = "scarf";
		//String project = "securephoto";
		//String project = "events";
		//String project = "bloggit";
		String project = "minibloggie";
		String trace_dir = working_dir + project + "/";
		
		try {
			bw = new BufferedWriter(new FileWriter(trace_dir + project + "_comparison"));
			
			String file1 = trace_dir + project + "_RolePolicy";          // user simulator.
			String file2 = trace_dir + "freeze_simulate/" + project + "_RolePolicy";  // crawler.
		    //compareRoleAuthPolicy(file1, file2);
			
			//printRoleAuthPolicy(file1);
			
			String file3 = trace_dir + project + "_SQLQueryConstraints";   // expeller constraint.
			compareGenericQueryProfile(file3, file1); // file3: expeller; file1: batman.
			
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// little helper function.
	private static void log(String message) {
		try {
			System.out.println(message);
			bw.write(message + "\n");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Data structure: HashMap<Integer, HashMap<String, Set<GenericSQLQueryProfile>>>
	// role --> query skeleton --> request key --> constraints.
	
	public static void printRoleAuthPolicy(String file) {
		
		Set<RoleAuthPolicy> policySet = loadRoleAuthPolicy(file);
		
		HashMap<Integer, HashMap<String, Set<SQLOperation>>> operationByQuery = new HashMap<Integer, HashMap<String, Set<SQLOperation>>>();
		
		for (RoleAuthPolicy policy: policySet) {
			int r = policy.getRole().getRole();
			operationByQuery.put(r, new HashMap<String, Set<SQLOperation>>());
			
			for (SQLOperation operation: policy.getPrivileges()) {
				String query = operation.getQuerySkeleton();
				if (!operationByQuery.get(r).containsKey(query)) {
					operationByQuery.get(r).put(query, new HashSet<SQLOperation>());
				}
				operationByQuery.get(r).get(query).add(operation);
			}
		}
		
		// print query Profiles.
		for (int role: operationByQuery.keySet()) {
			System.out.println("Role: " + role);
			
			for (String query: operationByQuery.get(role).keySet()) {
				System.out.println(" Query Skeleton: " + query);
				
				for (SQLOperation operation: operationByQuery.get(role).get(query)) {
					System.out.println("   Operation with Attributes : ");
					for (PropagationPathConstraint constraint: operation.getAttributes()) {
						System.out.println("      " + constraint.toString());
					}
					System.out.println("      RequestKey: " + operation.getRequestKeys());
				}
			}
		}
	}
	
	// file1: SQLQueryConstraints file; file2: Role Policy file.
	public static void compareGenericQueryProfile(String file1, String file2) {
			
		HashMap<Integer, HashMap<String, HashMap<String, GenericSQLQueryProfile>>> queryProfile1 = loadSQLQueryConstraints(file1);
		HashMap<Integer, HashMap<String, HashMap<String, GenericSQLQueryProfile>>> queryProfile2 = translate(loadRoleAuthPolicy(file2));
		compareQueryProfile(queryProfile1, queryProfile2);
	}
	
		
	public static void compareQueryProfile(HashMap<Integer, HashMap<String, HashMap<String, GenericSQLQueryProfile>>> queryProfile1, 
										   HashMap<Integer, HashMap<String, HashMap<String, GenericSQLQueryProfile>>> queryProfile2) {
		
		Set<Integer> roleSet1 = queryProfile1.keySet();
		Set<Integer> roleSet2 = queryProfile2.keySet();
		Set<Integer> commonRoleSet = compareTwoSet(roleSet1, roleSet2, "Role");
		
		for (int role: commonRoleSet) {
			
			log("Comparing role: " + role);
			HashMap<String, HashMap<String, GenericSQLQueryProfile>> profileMap1 = queryProfile1.get(role);
			HashMap<String, HashMap<String, GenericSQLQueryProfile>> profileMap2 = queryProfile2.get(role);
			
			Set<String> querySet1 = profileMap1.keySet();
			Set<String> querySet2 = profileMap2.keySet();		
			Set<String> commonQuerySet = compareTwoSet(querySet1, querySet2, "  QuerySkeleton");
			
			for (String query: commonQuerySet) {
				
				log("  Comparing query: " + query);
				
				HashMap<String, GenericSQLQueryProfile> profileSet1 = profileMap1.get(query);
				HashMap<String, GenericSQLQueryProfile> profileSet2 = profileMap2.get(query);
				
				Set<String> keySet1 = profileSet1.keySet();
				Set<String> keySet2 = profileSet2.keySet();				
				Set<String> commonKeySet = compareTwoSet(keySet1, keySet2, "    RequestKey");
				
				for (String key: commonKeySet) {
					
					log("    Comparing request key: " + key);
					
					GenericSQLQueryProfile profile1 = profileSet1.get(key);
					GenericSQLQueryProfile profile2 = profileSet2.get(key);
					
					Set<PropagationPathConstraint> propPathSet1 = profile1.getParameterPropPaths();
					Set<PropagationPathConstraint> propPathSet2 = profile2.getParameterPropPaths();
					compareTwoSet(propPathSet1, propPathSet2, "      PropagationPath");
					
					Set<UserSpecificVariableConstraint> specificSet1 = profile1.getSpecificValueConsts();
					Set<UserSpecificVariableConstraint> specificSet2 = profile2.getSpecificValueConsts();
					compareTwoSet(specificSet1, specificSet2, "      UserSpecificValue");
					
					//Set<UserOwnershipConstraint> memberSet1 = profile1.getOwnershipConsts();
					//if (memberSet1.size() != 0) 
					//System.out.println("      MembershipConsts size: " + memberSet1.size());
					//Set<UserOwnershipConstraint> memberSet2 = profile2.getOwnershipConsts();
					//compareTwoSet(memberSet1, memberSet2);
				}
			}
		}
	}
	
	
	
	private static HashMap<Integer, HashMap<String, HashMap<String, GenericSQLQueryProfile>>> loadSQLQueryConstraints(String file) {
		
		HashMap<Integer, HashMap<String, HashMap<String, GenericSQLQueryProfile>>> queryProfiles = new HashMap<Integer, HashMap<String, HashMap<String, GenericSQLQueryProfile>>>();
		
		try {
			SAXBuilder parser = new SAXBuilder();
			Document doc = parser.build(new File(file));
			for (Object o: doc.getContent()) {
				Element root = (Element) o;
				if (!root.getName().equals("SQLQueryConstraints")) continue;
				for (Object r: root.getContent()) {
					if (!(r instanceof Element)) continue;
					Element roleElement = (Element) r;
					if (!roleElement.getName().equals("Role")) continue;
					int role = Integer.parseInt(roleElement.getAttributeValue("Role"));
					queryProfiles.put(role, new HashMap<String, HashMap<String, GenericSQLQueryProfile>>());
					
					for (Object q: roleElement.getContent()) {
						if (!(q instanceof Element)) continue;
						Element queryElement = (Element) q;
						if (!queryElement.getName().equals("QuerySkeleton")) continue;
						String query = queryElement.getAttributeValue("QuerySkeleton");
						queryProfiles.get(role).put(query, new HashMap<String, GenericSQLQueryProfile>());
						
						for (Object c: queryElement.getContent()) {
							if (!(c instanceof Element)) continue;
							Element constElement = (Element) c;
							if (!constElement.getName().equals("SQLQueryConstraint")) continue;
						
							String requestKey = constElement.getAttributeValue("RequestKey");
							GenericSQLQueryProfile profile = new GenericSQLQueryProfile(query, requestKey);
							
							profile.loadXMLElementFromQueryConstraints(constElement);
							queryProfiles.get(role).get(query).put(requestKey, profile);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return queryProfiles;
	}
	
	// return the common set.
	public static <T> Set<T> compareTwoSet(Set<T> set1, Set<T> set2, String attribute) {
		
		Set<T> commonSet = new HashSet<T>(set1);
		Set<T> copySet1 = new HashSet<T>(set1);
		Set<T> copySet2 = new HashSet<T>(set2);
		commonSet.retainAll(set2);
		copySet1.removeAll(commonSet);
		copySet2.removeAll(commonSet);
		
		//System.out.println("ATTRIBUTE: " + attribute);
		if (copySet1.size() != 0) {
			log(attribute + ": Set1 - Set2 contains: ");
		}
		for (T key: copySet1) {
			log("  " + attribute + " " + key.toString() + "\n");
		}
		if (copySet2.size() != 0) {
			log(attribute + ": Set2 - Set1 contains: ");
		}
		for (T key: copySet2) {
			log("  " + attribute + " " + key.toString() + "\n");
		}
		//System.out.println("    Common key size: " + commonSet.size());
		return commonSet;
	}
	
	
	private static HashMap<Integer, HashMap<String, HashMap<String, GenericSQLQueryProfile>>> translate(Set<RoleAuthPolicy> policySet) {
		
		HashMap<Integer, HashMap<String, HashMap<String, GenericSQLQueryProfile>>> queryProfiles = new HashMap<Integer, HashMap<String, HashMap<String, GenericSQLQueryProfile>>>();
		
		for (RoleAuthPolicy policy: policySet) {
			int r = policy.getRole().getRole();
			queryProfiles.put(r, new HashMap<String, HashMap<String, GenericSQLQueryProfile>>());
			
			for (SQLOperation operation: policy.getPrivileges()) {
				String query = operation.getQuerySkeleton();
				if (!queryProfiles.get(r).containsKey(query)) {
					queryProfiles.get(r).put(query, new HashMap<String, GenericSQLQueryProfile>());
				}
				
				for (String key: operation.getRequestKeys()) {
					GenericSQLQueryProfile profile = new GenericSQLQueryProfile(query, key);
					
					profile.getSpecificValueConsts().addAll(operation.getSpecificValueConsts());
					
					for (PropagationPathConstraint constraint: operation.getParameterPropPaths()) {
						if (constraint.getSrcVariable().getContext().equals(key)) {
							profile.getParameterPropPaths().add(constraint);
						}
					}
					for (UserOwnershipConstraint constraint: operation.getOwnershipConsts()) {
						if (constraint.getMemberVariable().getContext().equals(key)) {
							profile.getOwnershipConsts().add(constraint);
						}
					}
					queryProfiles.get(r).get(query).put(key, profile);
				}
			}
		}
		return queryProfiles;
	}
	
	
	public static void compareRoleAuthPolicy(String file1, String file2) {
		try {
			Set<RoleAuthPolicy> policySet1 = loadRoleAuthPolicy(file1);
			Set<RoleAuthPolicy> policySet2 = loadRoleAuthPolicy(file2);
			HashMap<Integer, HashMap<String, HashMap<String, GenericSQLQueryProfile>>> queryProfile1 = translate(policySet1);
			HashMap<Integer, HashMap<String, HashMap<String, GenericSQLQueryProfile>>> queryProfile2 = translate(policySet2);
			
			compareQueryProfile(queryProfile1, queryProfile2);	
			//comparePolicySet(policySet1, policySet2);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private static Set<RoleAuthPolicy> loadRoleAuthPolicy(String file) {
		Set<RoleAuthPolicy> policySet = new HashSet<RoleAuthPolicy>();
		try {
			SAXBuilder parser = new SAXBuilder();
			Document doc = parser.build(new File(file));
			for (Object o: doc.getContent()) {
				Element root = (Element) o;
				if (!root.getName().equals("RolePolicy")) continue;
				for (Object s: root.getContent()) {
					if (!(s instanceof Element)) continue;
					Element element = (Element) s;
					int role = Integer.parseInt(element.getAttributeValue("Role"));
					RoleAuthPolicy policy = new RoleAuthPolicy(new UserRole(role));
					policy.parseXMLElement(element);
					policySet.add(policy);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
		return policySet;
	}
	
	
	private static void comparePolicySet(Set<RoleAuthPolicy> policySet1, Set<RoleAuthPolicy> policySet2) {
		
		for (RoleAuthPolicy policy1: policySet1) {
			for (RoleAuthPolicy policy2: policySet2) {
				if (policy1.getRole().equals(policy2.getRole())) {
					
					System.out.println("Role: " + policy1.getRole().getRole());
					System.out.println("Policy1 privielge size: " + policy1.getPrivileges().size());
					System.out.println("Policy2 privielge size: " + policy2.getPrivileges().size());
					System.out.println("Policy1 contains but Policy2 doesn't: ");
					Set<SQLOperation> commonOps = new HashSet<SQLOperation>();
					for (SQLOperation operation: policy1.getPrivileges()) {
						if (!policy2.getPrivileges().contains(operation)) {
							System.out.println(operation.getOperationSignature());
						} else {
							commonOps.add(operation);
						}
					}
					System.out.println("Policy2 contains but Policy1 doesn't: ");
					for (SQLOperation operation: policy2.getPrivileges()) {
						if (!policy1.getPrivileges().contains(operation)) {
							System.out.println(operation.getOperationSignature());
						}
					}
					System.out.println("Both Policy1 and Policy2 contains: ");
					for (SQLOperation operation: commonOps) {
						System.out.println(operation.getOperationSignature());
					}
				}
			}
		}
	}

}
