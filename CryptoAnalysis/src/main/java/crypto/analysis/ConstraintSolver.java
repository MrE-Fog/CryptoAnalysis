package crypto.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import crypto.DSL.CryptSLArithmeticConstraint;
import crypto.DSL.CryptSLComparisonConstraint;
import crypto.DSL.CryptSLConstraint;
import crypto.DSL.CryptSLConstraint.LogOps;
import soot.jimple.ArithmeticConstant;
import crypto.DSL.CryptSLValueConstraint;
import crypto.DSL.ISLConstraint;
import za.co.wstoop.jatalog.DatalogException;
import za.co.wstoop.jatalog.Expr;
import za.co.wstoop.jatalog.Jatalog;

public class ConstraintSolver {

	Jatalog jat;
	List<Expr> pastExpressions;
	
	public ConstraintSolver() {
		jat = new Jatalog();
		pastExpressions = new ArrayList<Expr>();
	}
	
	
	public boolean evaluate(CryptSLComparisonConstraint comp) {
		
		
		return true;
	}
	
	public boolean evaluate(CryptSLArithmeticConstraint comp) {
		
		
		return true;
	}
	
	public boolean evaluate(CryptSLValueConstraint valueCons, String actualValue) {
		if (actualValue == null || actualValue.isEmpty()) {
			return false;
		}
		try {
			jat.fact("value", "algActual", actualValue.toLowerCase());
			for (String allowedValue : valueCons.getValueRange()) {
				jat.fact("value", "algAllowed", allowedValue.toLowerCase());
			}
			jat.rule(Expr.expr("ValueCons", "A", "C"), Expr.expr("value", "A", "B"), Expr.expr("value", "C", "D"), Expr.eq("B", "D"));
			Collection<Map<String, String>> answers = jat.executeAll("ValueCons(algActual, algAllowed)?");
			for (Map<String, String> answer : answers) {
				answer.get("");
			}
		} catch (DatalogException e) {
			return false;
		}
		return false;
	}

	public boolean evaluate(CryptSLConstraint cons) {
		Boolean left = evaluate(cons.getLeft());
		Boolean right = evaluate(cons.getRight());
		LogOps ops = cons.getOperator();
		if (ops.equals(LogOps.and)) {
			return left && right; 
		} else if (ops.equals(LogOps.or)) {
			return left || right;
		} else if (ops.equals(LogOps.implies)) {
			if (!left) {
				return true;
			} else {
				return right;
			}
		} else if (ops.equals(LogOps.eq)) {
			return left.equals(right);
		}
		return false;
	}


	public Boolean evaluate(ISLConstraint cons) {
		if (cons instanceof CryptSLComparisonConstraint) {
			return evaluate((CryptSLComparisonConstraint) cons);
		} else if (cons instanceof CryptSLArithmeticConstraint) {
			return evaluate((CryptSLArithmeticConstraint) cons);
		}
		
		return false;
	}

	
}