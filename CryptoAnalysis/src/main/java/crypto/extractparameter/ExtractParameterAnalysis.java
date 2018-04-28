package crypto.extractparameter;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Sets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import crypto.analysis.CryptoScanner;
import crypto.boomerang.CogniCryptIntAndStringBoomerangOptions;
import crypto.rules.CryptSLMethod;
import crypto.typestate.CryptSLMethodToSootMethod;
import crypto.typestate.LabeledMatcherTransition;
import crypto.typestate.SootBasedStateMachineGraph;
import heros.utilities.DefaultValueMap;
import java_cup.symbol_set;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.toolkits.ide.icfg.BiDiInterproceduralCFG;
import typestate.finiteautomata.MatcherTransition;
import wpds.impl.Weight.NoWeight;

public class ExtractParameterAnalysis {

	private Map<Statement,SootMethod> allCallsOnObject;
	private Collection<LabeledMatcherTransition> events = Sets.newHashSet();
	private CryptoScanner cryptoScanner;
	private Multimap<CallSiteWithParamIndex, Statement> collectedValues = HashMultimap.create();
	private DefaultValueMap<AdditionalBoomerangQuery, AdditionalBoomerangQuery> additionalBoomerangQuery = new DefaultValueMap<AdditionalBoomerangQuery, AdditionalBoomerangQuery>() {
		@Override
		protected AdditionalBoomerangQuery createItem(AdditionalBoomerangQuery key) {
			return key;
		}
	};

	public ExtractParameterAnalysis(CryptoScanner cryptoScanner, Map<Statement, SootMethod> allCallsOnObject, SootBasedStateMachineGraph fsm) {
		this.cryptoScanner = cryptoScanner;
		this.allCallsOnObject = allCallsOnObject;
		for(MatcherTransition m : fsm.getAllTransitions()) {
			if(m instanceof LabeledMatcherTransition) {
				this.events.add((LabeledMatcherTransition) m );
			}
		}
	}

	public void run() {
		for(Entry<Statement, SootMethod> callSiteWithCallee : allCallsOnObject.entrySet()) {
			Statement callSite = callSiteWithCallee.getKey();
			SootMethod declaredCallee = callSiteWithCallee.getValue();
			if(callSite.isCallsite()){
				for(LabeledMatcherTransition e : events) {
					if(e.matches(declaredCallee)) {
						injectQueryAtCallSite(e.label(),callSite);
					}
				}
			}
		}
		for (AdditionalBoomerangQuery q : additionalBoomerangQuery.keySet()) {
//			if (reports != null) {
//				reports.boomerangQueryStarted(query, q);
//			}
			q.solve();
//			if (reports != null) {
//				reports.boomerangQueryFinished(query, q);
//			}
		}
	}
	public Multimap<CallSiteWithParamIndex, Statement> getCollectedValues() {
		return collectedValues;
	}
	private void injectQueryAtCallSite(List<CryptSLMethod> list, Statement callSite) {
		if(!callSite.isCallsite())
			return;
		for(CryptSLMethod matchingDescriptor : list){
			for(SootMethod m : CryptSLMethodToSootMethod.v().convert(matchingDescriptor)){
				SootMethod method = callSite.getUnit().get().getInvokeExpr().getMethod();
				if (!m.equals(method))
					continue;
				{
					int index = 0;
					for(Entry<String, String> param : matchingDescriptor.getParameters()){
						if(!param.getKey().equals("_")){
							soot.Type parameterType = method.getParameterType(index);
							if(parameterType.toString().equals(param.getValue())){
								addQueryAtCallsite(param.getKey(), callSite, index);
							}
						}
						index++;
					}
				}
			}
		}
	}

	public void addQueryAtCallsite(final String varNameInSpecification, final Statement stmt, final int index) {
		if(!stmt.isCallsite())
			return;
		Value parameter = stmt.getUnit().get().getInvokeExpr().getArg(index);
		if (!(parameter instanceof Local)) {
			collectedValues.put(
					new CallSiteWithParamIndex(stmt, new Val(parameter, stmt.getMethod()), index, varNameInSpecification), stmt);
			return;
		}
		AdditionalBoomerangQuery query = additionalBoomerangQuery
				.getOrCreate(new AdditionalBoomerangQuery(stmt, new Val((Local) parameter, stmt.getMethod())));
		query.addListener(new QueryListener() {
			@Override
			public void solved(AdditionalBoomerangQuery q, Table<Statement, Val, NoWeight> res) {
				for (Cell<Statement, Val, NoWeight> v : res.cellSet()) {
					collectedValues.put(new CallSiteWithParamIndex(stmt, v.getColumnKey(), index, varNameInSpecification),
							v.getRowKey());
				}
			}
		});
	}

	public void addAdditionalBoomerangQuery(AdditionalBoomerangQuery q, QueryListener listener) {
		AdditionalBoomerangQuery query = additionalBoomerangQuery.getOrCreate(q);
		query.addListener(listener);
	}

	public class AdditionalBoomerangQuery extends BackwardQuery {
		public AdditionalBoomerangQuery(Statement stmt, Val variable) {
			super(stmt, variable);
		}

		protected boolean solved;
		private List<QueryListener> listeners = Lists.newLinkedList();
		private Table<Statement, Val, NoWeight> res;

		public void solve() {
			Boomerang boomerang = new Boomerang(new CogniCryptIntAndStringBoomerangOptions()) {
				@Override
				public BiDiInterproceduralCFG<Unit, SootMethod> icfg() {
					return ExtractParameterAnalysis.this.cryptoScanner.icfg();
				}
			};
			boomerang.solve(this);
			boomerang.debugOutput();
			// log("Solving query "+ accessGraph + " @ " + stmt);
			res = boomerang.getResults(this);
			for (QueryListener l : Lists.newLinkedList(listeners)) {
				l.solved(this, res);
			}
			solved = true;
		}

		public void addListener(QueryListener q) {
			if (solved) {
				q.solved(this, res);
				return;
			}
			listeners.add(q);
		}

		private ExtractParameterAnalysis getOuterType() {
			return ExtractParameterAnalysis.this;
		}
	}

	public static interface QueryListener {
		public void solved(AdditionalBoomerangQuery q, Table<Statement, Val, NoWeight> res);
	}
	


}