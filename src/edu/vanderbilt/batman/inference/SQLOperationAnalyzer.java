package edu.vanderbilt.batman.inference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.vanderbilt.batman.model.InteractionSignature;
import edu.vanderbilt.batman.model.PropagationPathConstraint;
import edu.vanderbilt.batman.model.SQLOperation;
import edu.vanderbilt.batman.model.SQLQueryResponsePair;
import edu.vanderbilt.batman.model.SQLSample;
import edu.vanderbilt.batman.model.UserIdentity;
import edu.vanderbilt.batman.model.UserRole;
import edu.vanderbilt.batman.model.WebInteraction;
import edu.vanderbilt.batman.model.WebSession;

/**
 * This class is designed to retrieve the set of samples grouped by SQL operations.
 * @author xli
 *
 */
public class SQLOperationAnalyzer {
	
	private static HashMap<UserRole, List<SQLSample>> markedSqlSamples = new HashMap<UserRole, List<SQLSample>>();
	
	public static Set<SQLOperation> analyze(UserRole role, List<WebSession> sessions) {
		
		if (!markedSqlSamples.containsKey(role)) {
			markedSqlSamples.put(role, new ArrayList<SQLSample>());
		}
		
		Set<SQLOperation> operations = new HashSet<SQLOperation>();
		
		System.out.println("Analyzing for role: " + role.getRole());
		
		HashMap<InteractionSignature, List<SQLSample>> sqlSamplesByInteractionSignature = groupSampleByInteractionSignature(sessions);
		
		//HashMap<InteractionSignature, List<WebInteraction>> interactionGroups = WebInteractionRetriever.retrieveBySignature(sessions);
		
		for (InteractionSignature intSignature: sqlSamplesByInteractionSignature.keySet()) {
			// response prop path: 
			
			Set<PropagationPathConstraint> responsePropPaths = 
					WebInteractionAnalyzer.inferResponsePropagatePath(sqlSamplesByInteractionSignature.get(intSignature));
			
			if (intSignature.getSqlQuerySkeleton().startsWith("SELECT")) {   // omit sensitive ops without resp prop path.
				if (responsePropPaths.isEmpty()){
					//System.out.println("omit sensitive ops without resp prop path.");
					//System.out.println(intSignature.getSqlQuerySkeleton());
					continue;
				}
			}
			
			// define sql operation:
			SQLOperation operation = new SQLOperation(intSignature.getSqlQuerySkeleton());
			if (!responsePropPaths.isEmpty()) {
				operation.addAttributes(responsePropPaths);
			}
			operation.genOperationSignature();      // define this operation!
			
			// parameter prop path:
			Set<PropagationPathConstraint> parameterPropPaths = WebInteractionAnalyzer.inferParameterPropagatePath(sqlSamplesByInteractionSignature.get(intSignature));
			
			if (operations.contains(operation)) {
				for (SQLOperation op: operations) {
					if (op.equals(operation)) {
						op.addRequestKey(intSignature.getWebRequestKey());
						op.addParameterPropPaths(parameterPropPaths);
						break;
					}
				}
			} else {
				operation.addRequestKey(intSignature.getWebRequestKey());    // add web request key.
				operation.addParameterPropPaths(parameterPropPaths);
				operations.add(operation);
			}
			
			
			// mark sample with operation for later.
			for (SQLSample sample: sqlSamplesByInteractionSignature.get(intSignature)) {
				sample.setSqlOperation(operation);
				markedSqlSamples.get(role).add(sample);
			}
		}
		return operations;
	}
	
	
	private static HashMap<InteractionSignature, List<SQLSample>> groupSampleByInteractionSignature(List<WebSession> sessions) {
		int lookAhead = 2;  // change to look ahead 2 interactions.
		HashMap<InteractionSignature, List<SQLSample>> sqlSamples = new HashMap<InteractionSignature, List<SQLSample>>();
		
		for (WebSession session: sessions) {
			List<List<SQLSample>> sqlSamplesInPreviousInteractions = new ArrayList<List<SQLSample>>();
			
			for (WebInteraction interaction: session.getInteractions()) {     // in order to preserve the list of sql samples in last interaction.
				
				String requestKey = interaction.getRequest().getRequestKey();
				List<SQLSample> sqlSamplesInCurInteraction = new ArrayList<SQLSample>();
				
				for (SQLQueryResponsePair pair: interaction.getSqlQueryResponsePairs()) {
					String querySkeleton = pair.getSqlQuery().getSkeleton();
					InteractionSignature signature = new InteractionSignature(requestKey, querySkeleton);
					if (!sqlSamples.containsKey(signature)) {
						sqlSamples.put(signature, new ArrayList<SQLSample>());
					}
					SQLSample sample = new SQLSample(interaction.getUser(), interaction.getRequest(), interaction.getResponse(), pair.getSqlQuery(), pair.getSqlResponse());
					
					for (int i = sqlSamplesInPreviousInteractions.size()-lookAhead; i < sqlSamplesInPreviousInteractions.size(); i++) {
						if (i < 0) continue;
						sample.addSamplesInPreviousInteractions(sqlSamplesInPreviousInteractions.get(i));  // be careful with order.
					}
					
					sqlSamplesInCurInteraction.add(sample);
					sqlSamples.get(signature).add(sample);
				}
				// update.
				sqlSamplesInPreviousInteractions.add(sqlSamplesInCurInteraction);
			}
		}
		return sqlSamples;
	}
	
	
	public static List<SQLSample> getMarkedSampleByRole(UserRole role) {
		if (!markedSqlSamples.containsKey(role)) {
			return new ArrayList<SQLSample>();
		}
		return markedSqlSamples.get(role);
	}
	
	
	public static HashMap<UserIdentity, List<SQLSample>> groupMarkedSampleByUser(UserRole role) {
		
		HashMap<UserIdentity, List<SQLSample>> samplesByUser = new HashMap<UserIdentity, List<SQLSample>>();
		
		for (SQLSample sample: markedSqlSamples.get(role)) {
			UserIdentity user = sample.getUser();
			if (!samplesByUser.containsKey(user)) {
				samplesByUser.put(user, new ArrayList<SQLSample>());
			}
			samplesByUser.get(user).add(sample);
		}
		return samplesByUser;
	}
	
	
	public static List<SQLSample> retrieveSampleByOperation(List<SQLSample> sqlSamples, SQLOperation operation) {
		List<SQLSample> samples = new ArrayList<SQLSample>();
		for (SQLSample sample: sqlSamples) {
			if (sample.isSQLOperationSet() && sample.getSqlOperation().equals(operation)) {
				samples.add(sample);
			}
		}
		return samples;
	}

}
