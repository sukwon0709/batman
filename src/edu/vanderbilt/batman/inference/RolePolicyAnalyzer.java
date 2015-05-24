package edu.vanderbilt.batman.inference;

import edu.vanderbilt.batman.model.*;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.FileOutputStream;
import java.util.*;

public class RolePolicyAnalyzer {

    public static Set<RoleAuthPolicy> inferRoleAuthPolicy(List<WebSession> population) {

        HashMap<UserRole, List<WebSession>> roleSessionGroup = WebSessionRetriever.retrieveByRole(population);

        Set<RoleAuthPolicy> policySet = new HashSet<RoleAuthPolicy>();

        for (UserRole role : roleSessionGroup.keySet()) {
            RoleAuthPolicy policy = new RoleAuthPolicy(role);

            // identify the set of sensitive operations: including parameter prop path & response prop path
            // also, it looks like role-based constraints are gathered here.
            Set<SQLOperation> privileges = SQLOperationAnalyzer.analyze(role, roleSessionGroup.get(role));
            policy.addPrivileges(privileges);
            inferOperationConstraints(policy);

            policySet.add(policy);
        }
        return policySet;
    }

    /**
     * looks like this is inferring user-specific constraints.
     *
     * @param policy
     */
    private static void inferOperationConstraints(RoleAuthPolicy policy) {

        // finds user specific values for variables in SQL response and web response.
        for (SQLOperation operation : policy.getPrivileges()) {
            // infer specific value constraint:
            operation.addSpecificValueConsts(inferUserSpecificVariableConstraints(policy.getRole(), operation));
            operation.addSpecificValueConsts(inferSpecificVariableConstraintsWithinResponse(policy.getRole(), operation));
        }

        // finds constraints on variables from previous interaction's web responses to variables from web requests.
        for (SQLOperation operation : policy.getPrivileges()) {
            // infer ownership constraint:
            operation.addOwnershipConsts(inferUserOwnershipConstraints(policy.getRole(), operation));
        }
    }


    /**
     * finds user specific values for web response variables that comply with propagation constraints.
     *
     * @param role
     * @param operation
     * @return
     */
    private static Set<UserSpecificVariableConstraint> inferSpecificVariableConstraintsWithinResponse(UserRole role, SQLOperation operation) {

        Set<UserSpecificVariableConstraint> constraints = new HashSet<UserSpecificVariableConstraint>();

        if (!operation.getQuerySkeleton().startsWith("SELECT")) {  // only infer for SELECT queries.
            //System.out.println("JUMP: " + operation.getQuerySkeleton());
            return constraints;
        }

        HashMap<UserIdentity, List<SQLSample>> sqlSamplesByUser = SQLOperationAnalyzer.groupMarkedSampleByUser(role);

        List<Set<Variable>> userSamples = new ArrayList<Set<Variable>>();

        boolean print = false;

        //if (!operation.toString() .contains("comments.text")) return new HashSet<UserSpecificVariableConstraint>();
        if (operation.getQuerySkeleton().equals("SELECT blogdata.post_id, blogdata.subject, blogdata.message, blogusername.user, blogdata.datetime, blogusername.id, blogdata.user_id FROM blogdata, blogusername WHERE blogdata.user_id = blogusername.id")
                && operation.getRequestKeys().contains("GET-/minibloggie/blog.php")) {
            print = true;
        }

        for (UserIdentity user : sqlSamplesByUser.keySet()) {

            List<SQLSample> samples = SQLOperationAnalyzer.retrieveSampleByOperation(sqlSamplesByUser.get(user), operation);

            //YUan add: the user has not performed this operation
            if (samples.size() == 0) continue;

            if (print) System.out.println("userid:" + user.getUserId());

            List<Set<Variable>> respVars = new ArrayList<Set<Variable>>();  // use resp prop path as context;

            for (SQLSample sample : samples) {

                HashMap<String, List<String>> webRespVars = WebResponseAnalyzer.getWebResponseValues(sample.getWebResponse());
                Set<Variable> vars = new HashSet<Variable>();

                for (PropagationPathConstraint c : operation.getAttributes()) {
                    String destVar = c.getDestVariable().getName();             // only look at link/form, not text node.
                    if (destVar.contains("text"))
                        continue;                   // text cannot be passed along to next interaction.
                    String srcVar = c.getSrcVariable().getName();

                    //if (print) System.out.println("PropPath: " + c.getConstraint());

                    if (!webRespVars.containsKey(destVar)) {
                        //System.err.println("Web response doesn't contain the dest variable: " + destVar);
                        continue;  // next prop path..
                    }
                    if (!sample.getSqlResponse().getValues().containsKey(srcVar)) {
                        //System.err.println("SQL response doesn't contain the src variable: " + srcVar);
                        continue;  // next prop path..
                    }
                    List<String> sqlValues = sample.getSqlResponse().getValues().get(srcVar);
                    if (sqlValues.size() == 0) {
                        //System.err.println("SQL response doesn't contain the src variable: " + srcVar);
                        continue;  // next prop path..
                    }

                    // figures out which rows from database view are reflected on the response.
                    Set<Integer> rowsToAdd = new HashSet<Integer>();
                    for (String webRespVar : webRespVars.get(destVar)) {
                        for (int i = 0; i < sqlValues.size(); i++) {
                            if (sqlValues.get(i).equals(webRespVar)) {  // EXACT_MATCH: add this row.
                                //System.out.println("match: " + webRespVar + " " +  sqlValues.get(i));
                                rowsToAdd.add(i);
                            }
                        }
                    }
                    if (rowsToAdd.isEmpty()) {
                        //System.err.println("Couldn't find the sql resp value for web resp");
                        continue;
                    }
                    //System.out.println(rowsToAdd);

                    for (String name : sample.getSqlResponse().getValues().keySet()) {
                        List<String> colVars = sample.getSqlResponse().getValues().get(name);
                        if (colVars.size() == 0) continue;
                        if (colVars.size() != sqlValues.size()) {
                            //System.err.println("SQL response has alignment problem..." + name + " " + srcVar);
                            continue;
                        }
                        Variable var = new Variable(c.getConstraint(), name);      // use Prop Path as context;
                        for (Integer row : rowsToAdd) {
                            var.addValue(colVars.get(row));
                        }
                        vars.add(var);
                    }
                } // end propagation path loop

                if (vars.size() == 0) {
                    //System.err.println("Empty query parameters.");
                    if (print) System.out.println(sample.getSqlQuery().getQueryStr());
                    continue;
                }
                if (print) {
                    //System.out.println("New Sample: ");
                    for (Variable var : vars) {
                        //System.out.println(var.getName());
                        //System.out.println(var.getValues());
                    }
                }
                respVars.add(vars);
            }

            if (print) System.out.println("Sample size: " + respVars.size());
            Set<ConsistentValueConstraint> consistentConsts = InferenceEngine.inferConsistentValueConstraint(respVars);

            if (print) {
                System.out.println("ConsistentConst size: " + consistentConsts.size());
                System.out.println(consistentConsts);
            }

            //System.out.println("after consistent filer: queryParas:" + queryParas.toString());

            Set<Variable> consistentVars = new HashSet<Variable>();
            for (ConsistentValueConstraint constraint : consistentConsts) {
                //System.out.println("parameter: " + constraint.getVariable().nameWithContext() + " value: " + constraint.getValue());
                Variable var = new Variable(constraint.getVariable());

                //var.addValue(constraint.getValue()); //YUan fixed
                consistentVars.add(var);
            }
            //YUan add
            if (consistentVars.size() > 0) {
                userSamples.add(consistentVars);
            }
        }

        if (print) {
            for (Set<Variable> user : userSamples) {
                System.out.println("a user");
                for (Variable var : user)
                    System.out.println("after consistency check: user var:" + var.getName() + " = " + var.getValues().toString());
            }
        }

        constraints = InferenceEngine.inferSpecificValueConstraint(userSamples);

        if (print) {
            System.out.println(constraints);
        }
        return constraints;
    }

    /**
     * for a query skeleton, this finds all of user specific values for some of parameters.
     *
     * @param role
     * @param operation
     * @return
     */
    private static Set<UserSpecificVariableConstraint> inferUserSpecificVariableConstraints(UserRole role, SQLOperation operation) {

        Set<UserSpecificVariableConstraint> constraints = new HashSet<UserSpecificVariableConstraint>();

		/*
        if (operation.getParameterPropPaths().isEmpty()) {
			return constraints;
		}		
		Set<String> destVars = new HashSet<String>();
		for (PropagationPathConstraint path: operation.getParameterPropPaths()){
			destVars.add(path.getDestVariable().getName());
		}*/

        HashMap<UserIdentity, List<SQLSample>> sqlSamplesByUser = SQLOperationAnalyzer.groupMarkedSampleByUser(role);

        List<Set<Variable>> userSamples = new ArrayList<Set<Variable>>();

        boolean print = false;

        //if (!operation.toString() .contains("comments.text")) return new HashSet<UserSpecificVariableConstraint>();
        if (operation.getQuerySkeleton().equals("SELECT users.email FROM users WHERE users.user_id = {STRING}")
                && operation.getRequestKeys().contains("POST-/scarf/showpaper.php")) {
            //print = true;
        }

        for (UserIdentity user : sqlSamplesByUser.keySet()) {

            List<SQLSample> samples = SQLOperationAnalyzer.retrieveSampleByOperation(sqlSamplesByUser.get(user), operation);

            List<Set<Variable>> queryParas = new ArrayList<Set<Variable>>();

            //YUan add: the user has not performed this operation
            if (samples.size() == 0) continue;

            if (print) System.out.println("userid:" + user.getUserId());

            for (SQLSample sample : samples) {
                Set<Variable> vars = new HashSet<Variable>();
                //System.out.println("sample.getSqlQuery():" + sample.getSqlQuery().toString());
                //System.out.println("sample.getSqlQuery().getValues():" + sample.getSqlQuery().getValues().toString());
                for (String name : sample.getSqlQuery().getValues().keySet()) {
                    //System.out.println("name:" + name);
                    //Variable var = new Variable(operation.getOperationSignature(), name);  // context: operation signature.

						/*
						if (!destVars.contains(name)) {
							continue;
						}*/

                    Variable var = new Variable(operation.getQuerySkeleton(), name);
                    var.addValue(sample.getSqlQuery().getValues().get(name));
                    vars.add(var);
                }

                if (vars.size() == 0) {
                    //System.err.println("Empty query parameters.");
                    if (print) System.out.println(sample.getSqlQuery().getQueryStr());
                    continue;
                }

                //if (!sample.getWebRequest().getRequestKey().equals("GET-/securephoto/")
                //		&& !sample.getWebRequest().getRequestKey().equals("GET-/securephoto/users/login.php")
                //		&& !sample.getWebRequest().getRequestKey().equals("GET-/securephoto/users/sample.php"))
                queryParas.add(vars);
            }


            if (print) System.out.println("Sample size: " + queryParas.size());

            Set<ConsistentValueConstraint> consistentConsts = InferenceEngine.inferConsistentValueConstraint(queryParas);

            if (print) {
                System.out.println("ConsistentConst size: " + consistentConsts.size());
                System.out.println(consistentConsts);
            }

            //System.out.println("after consistent filer: queryParas:" + queryParas.toString());

            Set<Variable> consistentVars = new HashSet<Variable>();
            for (ConsistentValueConstraint constraint : consistentConsts) {
                //System.out.println("parameter: " + constraint.getVariable().nameWithContext() + " value: " + constraint.getValue());
                Variable var = new Variable(constraint.getVariable());

                //var.addValue(constraint.getValue()); //YUan fixed
                consistentVars.add(var);

            }
            //YUan add
            if (consistentVars.size() > 0) {
                userSamples.add(consistentVars);
            }
        }

        if (print) {
            for (Set<Variable> user : userSamples) {
                System.out.println("a user");
                for (Variable var : user)
                    System.out.println("after consistency check: user var:" + var.getName() + " = " + var.getValues().toString());
            }
        }

        constraints = InferenceEngine.inferSpecificValueConstraint(userSamples);

        if (print) {
            System.out.println(constraints);
        }
        return constraints;
    }


    private static Set<UserOwnershipConstraint> inferUserOwnershipConstraints(UserRole role, SQLOperation operation) {

        Set<UserOwnershipConstraint> constraints = new HashSet<UserOwnershipConstraint>();

        if (operation.getParameterPropPaths().isEmpty()) {    // parameter prop path exists.
            return constraints;
        }

        boolean print = false;
        if (operation.getRequestKeys().contains("GET-/minibloggie/del.php")
                && operation.getQuerySkeleton().equals("DELETE FROM blogdata WHERE blogdata.post_id = {STRING}")) {
            print = true;
        }

        List<SQLSample> samples = SQLOperationAnalyzer.retrieveSampleByOperation(SQLOperationAnalyzer.getMarkedSampleByRole(role), operation);

        List<Set<Variable>> webRequestVarSamples = new ArrayList<Set<Variable>>();
        List<Set<Variable>> webResponseVarSamples = new ArrayList<Set<Variable>>();
        //List<Set<Variable>> sqlResponseVarSamples = new ArrayList<Set<Variable>>();

        for (SQLSample sample : samples) {

            Set<Variable> webRequestVars = new HashSet<Variable>();
            for (PropagationPathConstraint constraint : operation.getParameterPropPaths()) {
                String name = constraint.getSrcVariable().getName();
                String value = sample.getWebRequest().getAllParameters().get(name);
                Variable var = new Variable(constraint.getSrcVariable());   /// here, should clear variable value.
                var.getValues().clear();
                var.addValue(value);
                webRequestVars.add(var);
            }

            Set<Variable> webResponseVars = new HashSet<Variable>();
            //Set<Variable> sqlResponseVars = new HashSet<Variable>();

            Set<SQLOperation> checkedOps = new HashSet<SQLOperation>();

            // finds variables from previous interactions' web responses
            for (SQLSample previousSample : sample.getSamplesInPreviousInteractions()) {
                if (!previousSample.isSQLOperationSet()) continue;
                SQLOperation previousOperation = previousSample.getSqlOperation();

                if (print) System.out.println(previousOperation.toString());

                if (checkedOps.contains(previousOperation)) continue;
                // require previous operation is bound to user and has propatate path to web response.
                if (previousOperation.getSpecificValueConsts().isEmpty() || previousOperation.getAttributes().isEmpty()) {
                    //if (previousOperation.getAttributes().isEmpty()) {
                    if (print) {
                        if (previousOperation.getSpecificValueConsts().isEmpty()) {
                            System.out.println("SpecificValueConsts is empty!");
                        }
                        if (previousOperation.getAttributes().isEmpty()) {
                            System.out.println("RespPropPath is empty!");
                        }
                    }
                    continue;
                }

                if (print) System.out.println("here!");

                for (PropagationPathConstraint constraint : previousOperation.getAttributes()) {
					/*
					String sqlRespVarName = constraint.getSrcVariable().getName();
					List<String> sqlRespVarValues = previousSample.getSqlResponse().getValues().get(sqlRespVarName);
					Variable sqlRespVar = new Variable(constraint.getSrcVariable());
					for (String value: sqlRespVarValues) {
						sqlRespVar.addValue(value);
					}
					sqlResponseVars.add(sqlRespVar);*/

                    String xpath = constraint.getDestVariable().getName();
                    Variable webRespVar = new Variable(constraint.getDestVariable());
                    for (Variable var : WebResponseAnalyzer.getWebResponseVariablesByXpath(previousSample.getWebResponse(), xpath)) {
                        webRespVar.addValues(var.getValues());
                    }
                    webResponseVars.add(webRespVar);
                }
                checkedOps.add(previousOperation);
            }
            if (print) {
                System.out.println("WebRequestVars: ");
                for (Variable var : webRequestVars) {
                    System.out.println(var.getName() + " " + var.getValues());
                }
                System.out.println("WebResponseVars: ");
                for (Variable var : webResponseVars) {
                    System.out.println(var.getName() + " " + var.getValues());
                }
            }

            webRequestVarSamples.add(webRequestVars);
            webResponseVarSamples.add(webResponseVars);
            //sqlResponseVarSamples.add(sqlResponseVars);
        }

        //TODO: match variable name!

        constraints.addAll(InferenceEngine.inferMembershipConstraint(webResponseVarSamples,
                webRequestVarSamples, InferenceEngine.MODE_EXACT_MATCH));
        if (print) System.out.println(constraints);
        return constraints;
    }

    public static void output(String file, Set<RoleAuthPolicy> policies) {
        try {
            Element root = new Element("RolePolicy");
            for (RoleAuthPolicy policy : policies) {
                root.addContent(policy.genXMLElement());
            }
            Document doc = new Document(root);
            XMLOutputter serializer = new XMLOutputter(Format.getPrettyFormat());
            serializer.output(doc, new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
