package deepdoop.datalog;

import java.util.List;

public class Functional extends Predicate {
	String _valueType;

	public Functional(String name, List<String> keyTypes, String valueType) {
		super(name, keyTypes);
		_valueType = valueType;
	}
	public Functional(String name) {
		super(name);
	}

	@Override
	public void setTypes(List<String> types) {
		_valueType = types.remove(types.size() - 1);
		_types = types;
	}

	@Override
	public String toString() {
		StringJoiner joiner = new StringJoiner(" x ");
		for (String s : _types) joiner.add(s);
		return _name + "/" + _types.size() + " (" + joiner + " -> " + _valueType + ")";
	}
}