package deepdoop.datalog;

import java.util.List;

public class FunctionalElement extends PredicateElement {

	IExpr _valueExpr;

	public FunctionalElement(String name, String stage, List<IExpr> keyExprs, IExpr valueExpr) {
		super(name, stage, keyExprs);
		_valueExpr = valueExpr;
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(", ");
		for (IExpr e : _exprs) joiner.add(e.toString());
		return _name + "[" + joiner + "] = " + _valueExpr;
	}
}