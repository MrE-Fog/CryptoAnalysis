package crypto.rules;

import java.util.ArrayList;
import java.util.List;

import typestate.interfaces.ICryptSLPredicateParameter;

public class CryptSLPredicate extends CryptSLLiteral implements java.io.Serializable {

	private static final long serialVersionUID = 1L;
	private final ICryptSLPredicateParameter baseObject;
	private final String predName;
	private final List<ICryptSLPredicateParameter> parameters;
	private final boolean negated;
	
	public CryptSLPredicate(ICryptSLPredicateParameter baseObject, String name, List<ICryptSLPredicateParameter> variables, Boolean not) {
		this.baseObject = baseObject;
		this.predName = name;
		this.parameters = variables;
		this.negated = not;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((predName == null) ? 0 : predName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof CryptSLPredicate)) {
			return false;
		}
		CryptSLPredicate other = (CryptSLPredicate) obj;
		if (predName == null) {
			if (other.predName != null) {
				return false;
			}
		} else if (!predName.equals(other.predName)) {
			return false;
		}
		return true;
	}

	/**
	 * @return the baseObject
	 */
	public ICryptSLPredicateParameter getBaseObject() {
		return baseObject;
	}

	/**
	 * @return the predName
	 */
	public String getPredName() {
		return predName;
	}

	/**
	 * @return the parameters
	 */
	public List<ICryptSLPredicateParameter> getParameters() {
		return parameters;
	}

	/**
	 * @return the negated
	 */
	public Boolean isNegated() {
		return negated;
	}
	
	public String toString() {
		StringBuilder predSB = new StringBuilder();
		predSB.append("P:");
		if (negated) {
			predSB.append("!");
		}
		predSB.append(predName);
		predSB.append("(");
		
		for (ICryptSLPredicateParameter parameter : parameters) {
			predSB.append(parameter);
			predSB.append(",");
		}
		predSB.append(")");
		
		
		return predSB.toString();
	}

	@Override
	public List<String> getInvolvedVarNames() {
		List<String> varNames = new ArrayList<String>();
		if (predName.equals("neverTypeOf")) {
			varNames.add(parameters.get(0).getName());
		} else {
		for (ICryptSLPredicateParameter var : parameters) {
			if (!("_".equals(var.getName()) || "this".equals(var.getName()))) {
				varNames.add(var.getName());
			}
		}
		}
		if(getBaseObject() != null)
			varNames.add(getBaseObject().getName());
		return varNames;
	}
	
	public CryptSLPredicate setNegated(boolean negated){
		if (negated == this.negated) {
			return this;
		} else {
			return new CryptSLPredicate(baseObject, predName, parameters, negated);
		}
	}
}
