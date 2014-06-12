package edu.vanderbilt.batman.model;

import lombok.Getter;

/**
 * This class is used to extract features for grouping web interactions.
 * @author xli
 *
 */
public class InteractionSignature {
	
	@Getter private String webRequestKey;
	@Getter private String sqlQuerySkeleton;
	
	public InteractionSignature(String key, String skeleton) {
		webRequestKey = key;
		sqlQuerySkeleton = skeleton;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof InteractionSignature) {
			InteractionSignature signature = (InteractionSignature)o;
			return webRequestKey.equals(signature.getWebRequestKey()) && sqlQuerySkeleton.equals(signature.getSqlQuerySkeleton());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return 0;  // void function, so use equals() instead for key comparison.
	}
}
