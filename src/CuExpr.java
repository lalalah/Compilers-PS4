import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//*************** TO DO check related operators against figures 9************************


public abstract class CuExpr {
	protected String text = "";
	protected String methodId = null;
	protected String cText = "";
	protected String name = "";
	protected String castType = "";
	private CuType type = null;
	public void add(List<CuType> pt, List<CuExpr> es) {}
	public final CuType getType(CuContext context) throws NoSuchTypeException {
		if(type == null) { type = calculateType(context); }
		Helper.P("return expression type " + type);
		return type;
	}
	protected CuType calculateType(CuContext context) throws NoSuchTypeException { return null;};
	@Override public String toString() {return text;}
	
	public String toC() {
		return cText;
	}
	
	public String construct(){
		return name;
	}
	
	public String getCastType(){
		return castType;
	}
	
	public boolean isFunCall () {
		return false;
	}
	protected CuType binaryExprType(CuContext context, String leftId, String methodId, CuType rightType) throws NoSuchTypeException {
		//System.out.println("in binaryExprType, begin");
		//System.out.println("leftid is " + leftId + ", methodid is " + methodId + ",right type is " + rightType.id);
		// get the functions of left class
		CuClass cur_class = context.mClasses.get(leftId);
		if (cur_class == null) {
			//System.out.println("didn't find this class in class context");
			throw new NoSuchTypeException();
		}
		Map<String, CuTypeScheme> funcs =  cur_class.mFunctions;
		// check the method typescheme
		CuTypeScheme ts = funcs.get(methodId);
		if (ts == null ) {
			//System.out.println("didn't find this method in current class");
			throw new NoSuchTypeException();
		}
		Helper.ToDo("we know there is only one parameter for now");
		CuType tR = null;
		for (String mystr : ts.data_tc.keySet()) {
			tR = ts.data_tc.get(mystr);
		}
		/** if this method exists, kindcontext is <>, and type scheme matches with input */
		if (!rightType.isSubtypeOf(tR)) {
			throw new NoSuchTypeException();
		}
		//System.out.println("in binaryExprType, end");
		return ts.data_t;
	}

	protected CuType unaryExprType(CuContext context, String id, String methodId) throws NoSuchTypeException {
		// get the functions of left class
		CuClass cur_class = context.mClasses.get(id);
		if (cur_class == null) {
			throw new NoSuchTypeException();
		}
		Map<String, CuTypeScheme> funcs = cur_class.mFunctions;
		// check the method typescheme
		CuTypeScheme ts = funcs.get(methodId);
		if (ts==null) {
			throw new NoSuchTypeException();
		}
		return ts.data_t;
	}
	
	protected Boolean isTypeOf(CuContext context, CuType t) {
		return this.getType(context).isSubtypeOf(t);
	}
    protected Boolean isTypeOf(CuContext context, CuType t, Map<String, CuType> map) {
        CuType type = this.getType(context);
        //System.out.println("in isType of, before type check, type is " + type.toString());
        if (type == null)
        	type.calculateType(context);
        //System.out.println("in isType of, after type check, type is " + type.toString());
        
        t.plugIn(map);
        //System.out.println("t type is " + t.type.toString());
        Helper.P(String.format("this=%s<%s> parent %s, that=%s<%s>, map=%s", type,type.map,type.parentType, t,t.map, map));
        return type.isSubtypeOf(t);
    }
}

class AndExpr extends CuExpr{
	private CuExpr left, right;
	public AndExpr(CuExpr e1, CuExpr e2) {
		left = e1;
		right = e2;
//		super.desiredType = CuType.bool;
		super.methodId = "add";
		super.text = String.format("%s . %s < > ( %s )", left.toString(), super.methodId, right.toString());
		
		
		
	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		//right should pass in a type
		return binaryExprType(context, left.getType(context).id, super.methodId, right.getType(context));
	}
	
	@Override
	public String toC() {
		String temp = Helper.getVarName();
		
		super.cText = temp+".value";
		super.castType = "Boolean";
		String leftToC = left.toC();
		String rightToC = right.toC();
		String leftC = left.construct();
		String rightC = right.construct();
		
		name += "\n" + leftC + rightC;
		
		super.name += String.format("Boolean* %s = (Boolean*) malloc(sizeof(Boolean));\n"
				+ "%s->nrefs = 0;\n"
				+ "%s->value=", temp, temp, temp);
		super.name += String.format("((%s*)%s)->value && ((%s*)%s)->value;\n", "Boolean", leftToC, "Boolean", rightToC);			

		/*if (leftC.equals("") && rightC.equals("")){
		//both are variables
		super.name += String.format("Boolean %s;\n%s.value=", temp, temp);
		super.name += String.format("((%s*)%s)->value && ((%s*)%s)->value;\n", "Boolean", left.toC(), "Boolean", right.toC());			
	}
	else if (leftC.equals("") && !rightC.equals("")) { 
		//right is Boolean
		leftCastType = "(" + right.getCastType() + "*)";			
		super.name += String.format("Boolean %s;\n%s.value=", temp, temp);
		super.name += String.format("(%s %s)->value && %s.value;\n", leftCastType, left.toC(), right.toC());
	}
	else if (!leftC.equals("") && rightC.equals("")) {
		//left is Boolean
		rightCastType = "(" + left.getCastType() + "*)";
		super.name += String.format("Boolean %s;\n%s.value=", temp, temp);
		super.name += String.format("%s.value && (%s %s)->value;\n", left.toC(), rightCastType, right.toC());
	}
	else {
		//both are Booleans
		super.name += String.format("Boolean %s;\n%s.value=", temp, temp);
		super.name += String.format("%s.value && %s.value;\n", left.toC(), right.toC());
	}*/
		
		return super.toC();
	}
}

class AppExpr extends CuExpr {
	CuExpr left;
	CuExpr right;
	public AppExpr(CuExpr e1, CuExpr e2) {
		this.left = e1;
		this.right = e2;
		super.text = e1.toString() + " ++ " + e2.toString();
	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		CuType t1 = left.calculateType(context);
		CuType t2 = right.calculateType(context);
		if (!t1.isIterable() || !t2.isIterable()) {
			throw new NoSuchTypeException();
		}
		CuType type = CuType.commonParent(t1.type, t2.type);
		return new Iter(type);
		/*CuType type = CuType.commonParent(left.getType(context), right.getType(context));
		if (type.isIterable()) return type;
		if (type.isBottom()) return new Iter(CuType.bottom);
		Helper.ToDo("Bottom <: Iterable<Bot>?"); */
	}
	
	@Override
	public String toC() {
		String leftToC = left.toC(), rightToC = right.toC();
		castType = "Iterable";
		
		name += left.construct();
		name += right.construct();
		name += "concatenate((Iterable*)" + leftToC + ", (Iterable*) " + rightToC + ");\n";
		
		cText = leftToC;
		return super.toC();
	}
}

class BrkExpr extends CuExpr {
	private List<CuExpr> val;
	public BrkExpr(List<CuExpr> es){
		this.val = es;
		super.text=Helper.printList("[", val, "]", ",");
		
	}
	@Override protected CuType calculateType(CuContext context) {
		//System.out.println("in bracket expression, start");
		if (val == null || val.isEmpty()) return new Iter(CuType.bottom);
		CuType t = val.get(0).getType(context);
		//System.out.println("type id is " + t.id);
		for (int i = 0; i+1 < val.size(); i++) {
			t = CuType.commonParent(val.get(i).getType(context), val.get(i+1).getType(context));
		} // find the common parent type of all expressions here
		
		//System.out.println("in bracket expression end");
		
		return new Iter(t);
	}
	
	@Override
	public String toC() {
		String eToC;
		
		ArrayList<String> tempNameArr=new ArrayList<String>();	
		ArrayList<String> tempDataArr=new ArrayList<String>();
		for (CuExpr e : val) {
			eToC = e.toC();
			name += e.construct();
			tempNameArr.add(Helper.getVarName());
			tempDataArr.add(eToC);
		}
		tempNameArr.add("null");

		for (int i = val.size() - 1; i >= 0; i--) {
			name += "Iterable* " + tempNameArr.get(i) + "=(Iterable*) malloc(sizeof(Iterable));\n"
					+ tempNameArr.get(i) + "->nref=1;\n" 
					+ tempNameArr.get(i) + "->value=" + tempDataArr.get(i) + ";\n"
					+ tempNameArr.get(i) + "->additional=" + tempNameArr.get(i + 1) + ";\n" 
					+ tempNameArr.get(i) + "->gen=NULL;\n" 
					+ tempNameArr.get(i)+ "->concat=NULL;\n";
		}	
			
		cText = tempNameArr.get(0);
		
		super.castType = "Iterable";
		return super.toC();
	}

}

class CBoolean extends CuExpr{
	Boolean val;
	public CBoolean(Boolean b){
		val=b;
		super.text=b.toString();
		
		}
	@Override protected CuType calculateType(CuContext context) {
		if (val == null) { throw new NoSuchTypeException();}
		return CuType.bool;
	}
	
	@Override
	public String toC() {
		super.castType = "Boolean";
		String temp = Helper.getVarName();
		super.cText = temp;
		
		if (val)			
			super.name = String.format("Boolean* %s = (Boolean *) malloc(sizeof(Boolean));\n"
					+ "(%s->nrefs) = 0;\n"
					+ "%s->value = %d;\n", temp, temp, temp, 1);
		else
			super.name = String.format("Boolean* %s = (Boolean *) malloc(sizeof(Boolean));\n"
					+ "(%s->nrefs)++;\n"
					+ "%s.value = %d;\n", temp, temp, temp, 0);
	
		return super.toC();
	}
}

class CInteger extends CuExpr {
	Integer val;
	public CInteger(Integer i){
		val=i;
		super.text=i.toString();
		
	}
	@Override protected CuType calculateType(CuContext context) {
		if (val == null) { throw new NoSuchTypeException();}
		return CuType.integer;
	}
	
	@Override
	public String toC() {
		String temp = Helper.getVarName();
		super.name = String.format("Integer* %s = (Integer*) malloc(sizeof(Integer));\n"
				+ "(%s->nrefs) = 0;\n"
				+ "%s->value = %d;\n", temp, temp, temp, val);		
		super.cText = temp;
		super.castType = "Integer";
		return super.toC();
	}
}

class CString extends CuExpr {
	String val;
	public CString(String s){
		val=s;
		super.text=s;
		
	}
	@Override protected CuType calculateType(CuContext context) {
		if (val == null) { throw new NoSuchTypeException();}
		return CuType.string;
	}
	
	@Override
	public String toC() {
		String temp = Helper.getVarName();
		super.name = String.format("String* %s = (String *) malloc(sizeof(String));\n"
				+ "%s->value = (char*) malloc(sizeof(%s));\n"
				+ "(%s->nrefs) = 0;\n"
				+ "mystrcpy(%s->value, %s);\n", temp, temp, val, temp, temp, val);	
		
		super.cText = temp;
		super.castType = "String";
		
		return super.toC();
	}
}

class DivideExpr extends CuExpr{
	private CuExpr left, right;
	public DivideExpr(CuExpr e1, CuExpr e2) {
		left = e1;
		right = e2;
		super.methodId = "divide";
		super.text = String.format("%s . %s < > ( %s )", left.toString(), super.methodId, right.toString());
	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		return binaryExprType(context, left.getType(context).id, super.methodId, right.getType(context));
	}
	/*
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		if (!left.getType(context).isInteger() || !right.getType(context).isInteger())
			throw new NoSuchTypeException();
		return CuType.integer;
	}
	 */
	
	@Override
	public String toC() {
String temp = Helper.getVarName();
		
		super.cText = temp;
		super.castType = "Integer";
		String leftToC = left.toC();
		String rightToC = right.toC();
		String leftC = left.construct();
		String rightC = right.construct();
		
		name += "\n" + leftC + rightC;
		
		super.name += String.format("Integer* %s  = (Integer*) malloc(sizeof(Integer));\n"
				+ "%s->nrefs = 0;\n"
				+ "%s->value=", temp, temp, temp);
		super.name += String.format("((%s*)%s)->value / ((%s*)%s)->value;\n", "Integer", leftToC, "Integer", rightToC);			

		/*if (leftC.equals("") && rightC.equals("")){
			//both are variables
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("((%s*)%s)->value / ((%s*)%s)->value;\n", "Integer", left.toC(), "Integer", right.toC());			
		}
		else if (leftC.equals("") && !rightC.equals("")) { 
			//right is number
			leftCastType = "(" + right.getCastType() + "*)";			
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("(%s %s)->value / %s.value;\n", leftCastType, left.toC(), right.toC());
		}
		else if (!leftC.equals("") && rightC.equals("")) {
			//left is number
			rightCastType = "(" + left.getCastType() + "*)";
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("%s.value / (%s %s)->value;\n", left.toC(), rightCastType, right.toC());
		}
		else {
			//both are numbers
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("%s.value / %s.value;\n", left.toC(), right.toC());
		}*/
		return super.toC();
	}
}

class EqualExpr extends CuExpr{
	private CuExpr left, right;
	private String method2= null;
	public EqualExpr(CuExpr e1, CuExpr e2, Boolean eq) {
		left = e1;
		right = e2;
		super.methodId = "equals";
		
		if (eq) {
			super.text = String.format("%s . %s < > ( %s )", left.toString(), super.methodId, right.toString());
		}
		else {
			method2 = "negate";
			super.text = String.format("%s . %s < > ( %s ) . negate ( )", left.toString(), super.methodId, right.toString());
		}
	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		CuType t = binaryExprType(context, left.getType(context).id, super.methodId, right.getType(context));
		if (method2 != null) {
			CuClass cur_class = context.mClasses.get(t.id);
			return cur_class.mFunctions.get(method2).data_t;
		}
		return t;
	}
	/*
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		if (!left.equals(right))
			throw new NoSuchTypeException();
		return CuType.bool;
	} */
	
	@Override
	public String toC() {
		castType = "Boolean";
		String leftToC = left.toC();
		String rightToC = right.toC();
		String leftC = left.construct();
		String rightC = right.construct();
		
		String leftCastType = ""; 
		String rightCastType = "";
		
		name += "\n" + leftC + rightC;
		
		if (leftC.equals("") && rightC.equals("")){
			leftCastType = "(" + Helper.cVarType.get(leftToC) + "*)";
			rightCastType = "(" + Helper.cVarType.get(rightToC) + "*)";
		}
		else if (leftC.equals("") && !rightC.equals("")) { 
			leftCastType = "(" + right.getCastType() + "*)";
			rightCastType = "(" + right.getCastType() + "*)";
		}
		else if (!leftC.equals("") && rightC.equals("")) {
			rightCastType = "(" + left.getCastType() + "*)";
			leftCastType = "(" + left.getCastType() + "*)";
		}
		else {
			leftCastType =  "(" + left.getCastType() + "*)";
			rightCastType = "(" + right.getCastType() + "*)";
		}
		
		if (!leftCastType.equals("(String*)") && !rightCastType.equals("(String*)")) {
			if (method2 == null)
				super.cText = String.format(
						"(%s %s)->value == (%s %s)->value", leftCastType,
						leftToC, rightCastType, rightToC);
			else
				super.cText = String.format(
						"(%s %s)->value != (%s %s)->value", leftCastType,
						leftToC, rightCastType, rightToC);
		}
		else {
			if (method2 == null)
				super.cText = String.format(
						"(mystrcmp(((%s %s)->value), ((%s %s)->value)) ? 1 : 0)", leftCastType,
						leftToC, rightCastType, rightToC);
			else
				super.cText = String.format(
						"(mystrcmp(((%s %s)->value), ((%s %s)->value)) ? 0 : 1)", leftCastType,
						leftToC, rightCastType, rightToC);
		
		}
		
		
		/*if (leftC.equals("") && rightC.equals("")){
			leftCastType = "(" + Helper.cVarType.get(left.toC()) + "*)";
			rightCastType = "(" + Helper.cVarType.get(right.toC()) + "*)";
			
			if (!Helper.cVarType.get(left.toC()).equals("String") && !Helper.cVarType.get(right.toC()).equals("String")) {
				if (eq)
					super.cText = String.format(
							"(%s %s)->value == (%s %s)->value", leftCastType,
							left.toC(), rightCastType, right.toC());
				else
					super.cText = String.format(
							"(%s %s)->value != (%s %s)->value", leftCastType,
							left.toC(), rightCastType, right.toC());
			}
			else {
				if (eq)
					super.cText = String.format(
							"*((%s %s)->value) == *((%s %s)->value)", leftCastType,
							left.toC(), rightCastType, right.toC());
				else
					super.cText = String.format(
							"*((%s %s)->value) != *((%s %s)->value)", leftCastType,
							left.toC(), rightCastType, right.toC());
			
			}
			
		}
		else if (leftC.equals("") && !rightC.equals("")) { 
			//right's type is known
			leftCastType = "(" + right.getCastType() + "*)";
			
			if (!right.castType.equals("String")) {			
				if (eq)
					super.cText = String.format("(%s %s)->value == %s.value",
							leftCastType, left.toC(), right.toC());
				else
					super.cText = String.format("(%s %s)->value != %s.value",
							leftCastType, left.toC(), right.toC());
			}
			else {
				if (eq)
					super.cText = String.format("*((%s %s)->value) == *(%s.value)",
							leftCastType, left.toC(), right.toC());
				else
					super.cText = String.format("*((%s %s)->value) != *(%s.value)",
							leftCastType, left.toC(), right.toC());
			}
		}
		else if (!leftC.equals("") && rightC.equals("")) {
			//left's type is known
			rightCastType = "(" + left.getCastType() + "*)";
			
			if (!left.castType.equals("String")) {			
				if (eq)
					super.cText = String.format("%s.value == (%s %s)->value",
							left.toC(), rightCastType, right.toC());
				else
					super.cText = String.format("%s.value != (%s %s)->value",
							left.toC(), rightCastType, right.toC());
			}
			else {
				if (eq)
					super.cText = String.format("*(%s.value) == *((%s %s)->value)", left.toC(), rightCastType, right.toC());
				else
					super.cText = String.format("*(%s.value) != *((%s %s)->value)", left.toC(), rightCastType, right.toC());
				
			}
		}
		else {
			//both types are not known
			if (!left.castType.equals("String") && !right.castType.equals("String")) {
				if (eq)
					super.cText = String.format("%s.value == %s.value",
							left.toC(), right.toC());
				else
					super.cText = String.format("%s.value != %s.value",
							left.toC(), right.toC());
			}
			else
			{
				if (eq)
					super.cText = String.format("*(%s.value) == *(%s.value)",
							left.toC(), right.toC());
				else
					super.cText = String.format("*(%s.value) != *(%s.value)",
							left.toC(), right.toC());			
			}
		}*/
		return super.toC();
	}
}

class GreaterThanExpr extends CuExpr{
	private CuExpr left, right;
	boolean b;
	public GreaterThanExpr(CuExpr e1, CuExpr e2, Boolean strict) {
		left = e1;
		right = e2;
		b = strict;
		super.methodId = "greaterThan";
		Helper.ToDo("strict boolean??");
		super.text = String.format("%s . %s < > ( %s , %s )", left.toString(), super.methodId, right.toString(), strict);
	}

	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		boolean b1 = left.isTypeOf(context, CuType.integer) && right.isTypeOf(context, CuType.integer);
		boolean b2 = left.isTypeOf(context, CuType.bool) && right.isTypeOf(context, CuType.bool);
		if ((!b1) && (!b2))
			throw new NoSuchTypeException();
		return CuType.bool;
	}
	
	@Override
	public String toC() {
		castType = "Boolean";
		String leftToC = left.toC();
		String rightToC = right.toC();
		String leftC = left.construct();
		String rightC = right.construct();
		
		String leftCastType = ""; 
		String rightCastType = "";
		
		name += "\n" + leftC + rightC;
		
		leftCastType = "(" + Helper.cVarType.get(leftToC) + "*)";
		rightCastType = "(" + Helper.cVarType.get(rightToC) + "*)";
		
		if (b)
			super.cText = String.format("(%s %s)->value > (%s %s)->value", leftCastType, leftToC, rightCastType, rightToC);
		else
			super.cText = String.format("(%s %s)->value >= (%s %s)->value", leftCastType, leftToC, rightCastType, rightToC);

		/*if (leftC.equals("") && rightC.equals("")){
			leftCastType = "(" + Helper.cVarType.get(left.toC()) + "*)";
			rightCastType = "(" + Helper.cVarType.get(right.toC()) + "*)";
			if (strict)
				super.cText = String.format("(%s %s)->value > (%s %s)->value", leftCastType, left.toC(), rightCastType, right.toC());
			else
				super.cText = String.format("(%s %s)->value >= (%s %s)->value", leftCastType, left.toC(), rightCastType, right.toC());
		}
		else if (leftC.equals("") && !rightC.equals("")) { 
			//right is number
			leftCastType = "(" + right.getCastType() + "*)";
			
			if (strict)
				super.cText = String.format("(%s %s)->value > %s.value", leftCastType, left.toC(), right.toC());
			else
				super.cText = String.format("(%s %s)->value >= %s.value", leftCastType, left.toC(), right.toC());
		}
		else if (!leftC.equals("") && rightC.equals("")) {
			//left is number
			rightCastType = "(" + left.getCastType() + "*)";
			
			if (strict)
				super.cText = String.format("%s.value > (%s %s)->value", left.toC(), rightCastType, right.toC());
			else
				super.cText = String.format("%s.value >= (%s %s)->value", left.toC(), rightCastType, right.toC());
		}
		else {
			//both are numbers
			if (strict)
				super.cText = String.format("%s.value > %s.value", left.toC(), right.toC());
			else
				super.cText = String.format("%s.value >= %s.value", left.toC(), right.toC());
		}*/		
		return super.toC();
	}
}

class LessThanExpr extends CuExpr{
	private CuExpr left, right;
	boolean b;
	public LessThanExpr(CuExpr e1, CuExpr e2, Boolean strict) {
		left = e1;
		right = e2;
		b = strict;
		super.methodId = "lessThan";
		super.text = String.format("%s . %s < > ( %s, %s )", left.toString(), super.methodId, right.toString(), strict);
		
		
	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		boolean b1 = left.isTypeOf(context, CuType.integer) && right.isTypeOf(context, CuType.integer);
		boolean b2 = left.isTypeOf(context, CuType.bool) && right.isTypeOf(context, CuType.bool);
		if ((!b1) && (!b2))
			throw new NoSuchTypeException();
		return CuType.bool;
	}
	
	@Override
	public String toC() {
		castType = "Boolean";
		String leftToC = left.toC();
		String rightToC = right.toC();
		String leftC = left.construct();
		String rightC = right.construct();
		
		String leftCastType = ""; 
		String rightCastType = "";
		
		name += "\n" + leftC + rightC;
		
		leftCastType = "(" + Helper.cVarType.get(leftToC) + "*)";
		rightCastType = "(" + Helper.cVarType.get(rightToC) + "*)";
		if (b)
			super.cText = String.format("(%s %s)->value < (%s %s)->value", leftCastType, leftToC, rightCastType, rightToC);
		else
			super.cText = String.format("(%s %s)->value <= (%s %s)->value", leftCastType, leftToC, rightCastType, rightToC);

		/*if (leftC.equals("") && rightC.equals("")) {
			leftCastType = "(" + Helper.cVarType.get(left.toC()) + "*)";
			rightCastType = "(" + Helper.cVarType.get(right.toC()) + "*)";
			if (strict)
				super.cText = String.format("(%s %s)->value < (%s %s)->value", leftCastType, left.toC(), rightCastType, right.toC());
			else
				super.cText = String.format("(%s %s)->value <= (%s %s)->value", leftCastType, left.toC(), rightCastType, right.toC());
		}
		else if (leftC.equals("") && !rightC.equals("")) { 
			//right is number
			leftCastType = "(" + right.getCastType() + "*)";
			
			if (strict)
				super.cText = String.format("(%s %s)->value < %s.value", leftCastType, left.toC(), right.toC());
			else
				super.cText = String.format("(%s %s)->value <= %s.value", leftCastType, left.toC(), right.toC());
		}
		else if (!leftC.equals("") && rightC.equals("")) {
			//left is number
			rightCastType = "(" + left.getCastType() + "*)";
			
			if (strict)
				super.cText = String.format("%s.value < (%s %s)->value", left.toC(), rightCastType, right.toC());
			else
				super.cText = String.format("%s.value <= (%s %s)->value", left.toC(), rightCastType, right.toC());
		}
		else {
			//both are numbers
			if (strict)
				super.cText = String.format("%s.value < %s.value", left.toC(), right.toC());
			else
				super.cText = String.format("%s.value <= %s.value", left.toC(), right.toC());
		}*/
		return super.toC();
	}
}

class MinusExpr extends CuExpr{
	private CuExpr left, right;
	public MinusExpr(CuExpr e1, CuExpr e2) {
		left = e1;
		right = e2;
		super.methodId = "minus";
		super.text = String.format("%s . %s < > ( %s )", left.toString(), super.methodId, right.toString());
		
	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		return binaryExprType(context, left.getType(context).id, super.methodId, right.getType(context));
	}
	/*
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		if (!left.getType(context).isInteger() || !right.getType(context).isInteger())
			throw new NoSuchTypeException();
		return CuType.integer;
	}*/
	
	@Override
	public String toC() {
String temp = Helper.getVarName();
		
		super.cText = temp;
		super.castType = "Integer";
		String leftToC = left.toC();
		String rightToC = right.toC();
		String leftC = left.construct();
		String rightC = right.construct();
		
		//String leftCastType = ""; 
		//String rightCastType = "";
		
		name += "\n" + leftC + rightC;

		super.name += String.format("Integer* %s  = (Integer*) malloc(sizeof(Integer));\n"
				+ "%s->nrefs = 0;\n"
				+ "%s->value=", temp, temp, temp);
		super.name += String.format("((%s*)%s)->value - ((%s*)%s)->value;\n", "Integer", leftToC, "Integer", rightToC);			

		/*if (leftC.equals("") && rightC.equals("")){
			//both are variables
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("((%s*)%s)->value - ((%s*)%s)->value;\n", "Integer", left.toC(), "Integer", right.toC());			
		}
		else if (leftC.equals("") && !rightC.equals("")) { 
			//right is number
			leftCastType = "(" + right.getCastType() + "*)";			
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("(%s %s)->value - %s.value;\n", leftCastType, left.toC(), right.toC());
		}
		else if (!leftC.equals("") && rightC.equals("")) {
			//left is number
			rightCastType = "(" + left.getCastType() + "*)";
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("%s.value - (%s %s)->value;\n", left.toC(), rightCastType, right.toC());
		}
		else {
			//both are numbers
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("%s.value - %s.value;\n", left.toC(), right.toC());
		}*/
		return super.toC();
	}
}

class ModuloExpr extends CuExpr{
	private CuExpr left, right;
	public ModuloExpr(CuExpr e1, CuExpr e2) {
		left = e1;
		right = e2;
		super.methodId = "modulo";
		super.text = String.format("%s . %s < > ( %s )", left.toString(), super.methodId, right.toString());
		
		
	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		return binaryExprType(context, left.getType(context).id, super.methodId, right.getType(context));
	}
	/*
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		if (!left.getType(context).isInteger() || !right.getType(context).isInteger())
			throw new NoSuchTypeException();
		return CuType.integer;
	}*/
	
	@Override
	public String toC() {
		String temp = Helper.getVarName();
		
		super.cText = temp;
		super.castType = "Integer";
		String leftToC = left.toC();
		String rightToC = right.toC();
		String leftC = left.construct();
		String rightC = right.construct();
		String integer = "Integer";
		
		name += "\n" + leftC + rightC;
		
		super.name += String.format("Integer* %s  = (Integer*) malloc(sizeof(Integer));\n"
				+ "%s->nrefs = 0;\n"
				+ "%s->value=", temp, temp, temp);
		super.name += String.format("((%s*)%s)->value %% ((%s*)%s)->value;\n", "Integer", leftToC, "Integer", rightToC);			
		/*if (leftC.equals("") && rightC.equals("")){
			//both are variables
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("((%s*)%s)->value % ((%s*)%s)->value;\n", "Integer", left.toC(), "Integer", right.toC());			
		}
		else if (leftC.equals("") && !rightC.equals("")) { 
			//right is number
			leftCastType = "(" + right.getCastType() + "*)";			
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("(%s %s)->value % %s.value;\n", leftCastType, left.toC(), right.toC());
		}
		else if (!leftC.equals("") && rightC.equals("")) {
			//left is number
			rightCastType = "(" + left.getCastType() + "*)";
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("%s.value % (%s %s)->value;\n", left.toC(), rightCastType, right.toC());
		}
		else {
			//both are numbers
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("%s.value % %s.value;\n", left.toC(), right.toC());
		}*/
		return super.toC();
	}
}

class NegateExpr extends CuExpr{
	private CuExpr val;
	public NegateExpr(CuExpr e) {
		val = e;
		super.methodId = "negate";
		super.text = String.format("%s . %s < > ( )", val.toString(), super.methodId);

	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		return unaryExprType(context, val.getType(context).id, super.methodId);
	}
	/*
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		if (!val.getType(context).isBoolean())
			throw new NoSuchTypeException();
		return CuType.bool;
	}*/
	
	@Override
	public String toC() {
		String temp = Helper.getVarName();
		
		super.cText = temp+".val";
		super.castType = "Boolean";
		String valToC = val.toC();
		String eC = val.construct();		
		
		name += "\n" + eC;	
		
		name += String.format("Boolean* %s = (Boolean*) malloc(sizeof(Boolean));\n"
				+ "%s->nrefs = 0;\n"
				+ "%s->value=", temp, temp, temp);
		name += String.format("!(((%s*)%s)->value);\n", "Boolean", valToC);

/*		if(eC.equals(""))
		{
			name += String.format("Boolean %s;\n%s.value=", temp, temp);
			name += String.format("!(((%s*)%s)->value);\n", "Boolean", e.toC());
		}
		else
		{
			name += String.format("Boolean %s;\n%s.value=", temp, temp);
			name += String.format("!(%s.value);\n", e.toC());
		}*/

		return super.toC();
	}
}

class NegativeExpr extends CuExpr{
	private CuExpr val;
	public NegativeExpr(CuExpr e) {
		val = e;
		super.methodId = "negative";
		super.text = String.format("%s . %s < > ( )", val.toString(), super.methodId);

	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		return unaryExprType(context, val.getType(context).id, super.methodId);
	}
	/*
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		if (!val.getType(context).isInteger())
			throw new NoSuchTypeException();
		return CuType.integer;
	}*/
	
	@Override
	public String toC() {
		String temp = Helper.getVarName();
		
		super.cText = temp;
		super.castType = "Integer";
		String valToC = val.toC();
		String eC = val.construct();		
		
		name += "\n" + eC;
		
		name += String.format("Integer* %s  = (Integer*) malloc(sizeof(Integer));\n"
				+ "%s->nrefs = 0;\n"
				+ "%s->value=", temp, temp, temp);
		name += String.format("-(((%s*)%s)->value);\n", "Integer", valToC);	
		
		/*if(eC.equals(""))
		{
			name += String.format("Integer* %s  = (Integer*) malloc(sizeof(Integer));\n"
					+ "%s->value=", temp, temp);
			name += String.format("-(((%s*)%s)->value);\n", "Integer", e.toC());
		}
		else
		{
			name += String.format("Integer %s;\n%s.value=", temp, temp);
			name += String.format("-(%s.value);\n", e.toC());
		}*/
		return super.toC();
	}
}

class OnwardsExpr extends CuExpr{
	private CuExpr val;
	boolean inclusive;
	public OnwardsExpr(CuExpr e, Boolean inclusiveness) {
		val = e;
		inclusive = inclusiveness;
		super.methodId = "onwards";
		super.text = String.format("%s . %s < > ( %s )", val.toString(), super.methodId, inclusiveness);
	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		return binaryExprType(context, val.getType(context).id, super.methodId, CuType.bool);
	}
	
	@Override
	public String toC() {
		castType = "Iterable";		
		String valToC = val.toC();
		
		if (val.getCastType().equals("Boolean"))
		{
			if (val.toString().equals("true")) {
				cText = "NULL";
			}
			else {
				if(inclusive) {
					String iter = Helper.getVarName();
					String iterTemp = Helper.getVarName(), falseTemp = Helper.getVarName(), trueTemp = Helper.getVarName();
					name += String.format("Boolean* %s = (Boolean*) x3malloc(sizeof(Boolean));\n"
							+ "%s->value = 1;\n"
							+ "%s->nrefs = 0;\n",
							trueTemp, trueTemp, trueTemp);
					name += String.format("Boolean* %s = (Boolean*) x3malloc(sizeof(Boolean));\n"
							+ "%s->value = 0;\n"
							+ "%s->nrefs = 0;\n",
							falseTemp, falseTemp, falseTemp);
					
					name +=  "Iterable* " + iterTemp + "(Iterable*) x3malloc(sizeof(Iterable));\n"
							+ iterTemp + "->nref = 1;\n"
							+ iterTemp + "->value = " + trueTemp + ";\n"
							+ iterTemp + "->additional = NULL;\n"
							+ iterTemp + "->gen = NULL;\n"
							+ iterTemp + "->concat = NULL;\n";
										
					name +=  "Iterable* " + iter + "(Iterable*) malloc(sizeof(Iterable));\n"
							+ iter + "->nref = 1;\n"
							+ iter + "->value = " + falseTemp + ";\n"
							+ iter + "->additional =" + iterTemp + ";\n"
							+ iter + "->gen = NULL;\n"
							+ iter + "->concat = NULL;\n";
					
					cText = iter;
				}
				
				else {
					String iter = Helper.getVarName();
					String temp = Helper.getVarName();
					name += String.format("Boolean* %s = (Boolean*) x3malloc(sizeof(Boolean));\n"
							+ "%s->value = 1;\n"
							+ "%s->nrefs = 0;\n",
							temp, temp, temp);
					
					name +=  "Iterable* " + iter + "(Iterable*) malloc(sizeof(Iterable));\n"
							+ iter + "->nref = 1;\n"
							+ iter + "->value = " + temp + ";\n"
							+ iter + "->additional = NULL;\n"
							+ iter + "->gen = NULL;\n"
							+ iter + "->concat = NULL;\n";
					
					cText = iter;
				}
			}
				
			
		}
		
		else {
			if(inclusive) {
				name += val.construct();
				String iter = Helper.getVarName();
		
				name += "Iterable* " + iter + " = (Iterable*) malloc(sizeof(Iterable));\n"
						+ iter + "->nref = 1;\n"
						+ iter + "->value = " + valToC + ";\n"
						+ iter + "->additional = NULL;\n"
						+ iter + "->gen = &" + val.getCastType() + "_onwards;\n"
						+ iter + "->concat = NULL;\n";
		
				cText = valToC;
			}
			else {
				String temp = Helper.getVarName();
				String iter = Helper.getVarName();
												
				int i = (Integer.parseInt(val.toString())) + 1;
				name += "Integer* " + temp + " = (Integer*) malloc(sizeof(Integer));\n"
						+ temp + "->value = " + i + ";\n";					
					
				
				name +=  "Iterable* " + iter + "(Iterable*) malloc(sizeof(Iterable));\n"
						+ iter + "->nref = 1;\n"
						+ iter + "->value = " + temp + ";\n"
						+ iter + "->additional = NULL;\n"
						+ iter + "->gen = &" + val.getCastType() + "_onwards;\n"
						+ iter + "->concat = NULL;\n";
				
				cText = iter;
			}
		}
		return super.toC();
	}
}

class OrExpr extends CuExpr{
	private CuExpr left, right;
	public OrExpr(CuExpr e1, CuExpr e2) {
		left = e1;
		right = e2;
		super.methodId = "or";
		super.text = String.format("%s . %s < > ( %s )", left.toString(), super.methodId, right.toString());

	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		return binaryExprType(context, left.getType(context).id, super.methodId, right.getType(context));
	}
	
	@Override
	public String toC() {

		String temp = Helper.getVarName();
		
		super.cText = temp+".value";
		super.castType = "Boolean";
		String leftToC = left.toC();
		String rightToC = right.toC();
		String leftC = left.construct();
		String rightC = right.construct();
		
		name += "\n" + leftC + rightC;
		
		super.name += String.format("Boolean* %s  = (Boolean*) malloc(sizeof(Boolean));\n"
				+ "%s->nrefs = 0;\n"
				+ "%s->value=", temp, temp, temp);
		super.name += String.format("((%s*)%s)->value || ((%s*)%s)->value;\n", "Boolean", leftToC, "Boolean", rightToC);	
		
		/*if (leftC.equals("") && rightC.equals("")){
			//both are variables
			super.name += String.format("Boolean* %s  = (Boolean*) malloc(sizeof(Boolean));\n"
					+ "%s->nrefs = 0;\n"
					+ "%s->value=", temp, temp, temp);
			super.name += String.format("((%s*)%s)->value || ((%s*)%s)->value;\n", "Boolean", left.toC(), "Boolean", right.toC());			
		}
		else if (leftC.equals("") && !rightC.equals("")) { 
			//right is Boolean
			leftCastType = "(" + right.getCastType() + "*)";			
			super.name += String.format("Boolean %s;\n%s.value=", temp, temp);
			super.name += String.format("(%s %s)->value || %s.value;\n", leftCastType, left.toC(), right.toC());
		}
		else if (!leftC.equals("") && rightC.equals("")) {
			//left is Boolean
			rightCastType = "(" + left.getCastType() + "*)";
			super.name += String.format("Boolean %s;\n%s.value=", temp, temp);
			super.name += String.format("%s.value || (%s %s)->value;\n", left.toC(), rightCastType, right.toC());
		}
		else {
			//both are Booleans
			super.name += String.format("Boolean %s;\n%s.value=", temp, temp);
			super.name += String.format("%s.value || %s.value;\n", left.toC(), right.toC());
		}*/
		return super.toC();
	}
}

class PlusExpr extends CuExpr{
	private CuExpr left, right;
	public PlusExpr(CuExpr e1, CuExpr e2) {
		left = e1;
		right = e2;
		super.methodId = "plus";
		super.text = String.format("%s . %s < > ( %s )", left.toString(), super.methodId, right.toString());
	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		//System.out.println("in plus expr begin");
		return binaryExprType(context, left.getType(context).id, super.methodId, right.getType(context));
	}
	
	@Override
	public String toC() {
		String temp = Helper.getVarName();
		
		super.cText = temp;
		super.castType = "Integer";
		String leftToC = left.toC();
		String rightToC = right.toC();
		String leftC = left.construct();
		String rightC = right.construct();
		
		name += "\n" + leftC + rightC;
		
		super.name += String.format("Integer* %s  = (Integer*) malloc(sizeof(Integer));\n"
				+ "%s->nrefs = 0;\n"
				+ "%s->value=", temp, temp, temp);
		super.name += String.format("((%s*)%s)->value + ((%s*)%s)->value;\n", "Integer", leftToC, "Integer", rightToC);			
		
		/*
		if (leftC.equals("") && rightC.equals("")){
			//both are variables
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("((%s*)%s)->value + ((%s*)%s)->value;\n", "Integer", left.toC(), "Integer", right.toC());			
		}
		else if (leftC.equals("") && !rightC.equals("")) { 
			//right is number
			leftCastType = "(" + right.getCastType() + "*)";			
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("(%s %s)->value + %s.value;\n", leftCastType, left.toC(), right.toC());
		}
		else if (!leftC.equals("") && rightC.equals("")) {
			//left is number
			rightCastType = "(" + left.getCastType() + "*)";
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("%s.value + (%s %s)->value;\n", left.toC(), rightCastType, right.toC());
		}
		else {
			//both are numbers
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("%s.value + %s.value;\n", left.toC(), right.toC());
		}*/
		return super.toC();
	}
}

class ThroughExpr extends CuExpr{
	private CuExpr left, right;
	boolean bLow, bUp;
	public ThroughExpr(CuExpr e1, CuExpr e2, Boolean low, Boolean up) {
		left = e1;
		right = e2;
		bLow = low;
		bUp = up;
		super.methodId = "through";
		super.text = String.format("%s . %s < > ( %s , %s , %s )", left.toString(), methodId, right.toString(), low, up);
	}

	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		boolean b1 = left.isTypeOf(context, CuType.integer) && right.isTypeOf(context, CuType.integer);
		boolean b2 = left.isTypeOf(context, CuType.bool) && right.isTypeOf(context, CuType.bool);
		if ((!b1) && (!b2))
			throw new NoSuchTypeException();
		if (b1)
			return new Iter(CuType.integer);
		else
			return new Iter(CuType.bool);
	}
	
	@Override
	public String toC() {
		castType = "Iterable";
		String leftToC = left.toC(), rightToC = right.toC();
		String iter = Helper.getVarName();
		
		if(bLow && bUp)	{
			if(left.getCastType().equals("Boolean")) {
				//true..true
				if(left.toString().equals("true") && right.toString().equals("true"))
				{
					String temp = Helper.getVarName();
					name += String.format("Boolean* %s = (Boolean*) x3malloc(sizeof(Boolean));\n"
							+ "%s->value = 1;\n"
							+ "%s->nrefs = 0;\n",
							temp, temp, temp);
					
					name +=  "Iterable* " + iter + "(Iterable*) malloc(sizeof(Iterable));\n"
							+ iter + "->nref = 1;\n"
							+ iter + "->value = " + temp + ";\n"
							+ iter + "->additional = NULL;\n"
							+ iter + "->gen = NULL;\n"
							+ iter + "->concat = NULL;\n";
					
					cText = iter;
				}
				
				//false..true
				else if (left.toString().equals("false") && right.toString().equals("true"))
				{
					String iterTemp = Helper.getVarName(), falseTemp = Helper.getVarName(), trueTemp = Helper.getVarName();
					name += String.format("Boolean* %s = (Boolean*) x3malloc(sizeof(Boolean));\n"
							+ "%s->value = 1;\n"
							+ "%s->nrefs = 0;\n",
							trueTemp, trueTemp, trueTemp);
					name += String.format("Boolean* %s = (Boolean*) x3malloc(sizeof(Boolean));\n"
							+ "%s->value = 0;\n"
							+ "%s->nrefs = 0;\n",
							falseTemp, falseTemp, falseTemp);
					
					name +=  "Iterable* " + iterTemp + "(Iterable*) malloc(sizeof(Iterable));\n"
							+ iterTemp + "->nref = 1;\n"
							+ iterTemp + "->value = " + trueTemp + ";\n"
							+ iterTemp + "->additional = NULL;\n"
							+ iterTemp + "->gen = NULL;\n"
							+ iterTemp + "->concat = NULL;\n";
										
					name +=  "Iterable* " + iter + "(Iterable*) malloc(sizeof(Iterable));\n"
							+ iter + "->nref = 1;\n"
							+ iter + "->value = " + falseTemp + ";\n"
							+ iter + "->additional =" + iterTemp + ";\n"
							+ iter + "->gen = NULL;\n"
							+ iter + "->concat = NULL;\n";
					
					cText = iter;
				}
				
				//true..false
				else if (left.toString().equals("true") && right.toString().equals("false"))
				{
					cText = "NULL";
				}
				
				//false..false
				else
				{
					String temp = Helper.getVarName();
					name += String.format("Boolean* %s = (Boolean*) x3malloc(sizeof(Boolean));\n"
							+ "%s->value = 0;\n"
							+ "%s->nrefs = 0;\n",
							temp, temp, temp);
					
					name +=  "Iterable* " + iter + "(Iterable*) malloc(sizeof(Iterable));\n"
							+ iter + "->nref = 1;\n"
							+ iter + "->value = " + temp + ";\n"
							+ iter + "->additional = NULL;\n"
							+ iter + "->gen = NULL;\n"
							+ iter + "->concat = NULL;\n";
					
					cText = iter;
				}
			}
			
			else {
				name += left.construct();
				name += right.construct();
			
				name +=  "Iterable* " + iter + "(Iterable*) malloc(sizeof(Iterable));\n"
						+ iter + "->nref = 1;\n"
						+ iter + "->value = " + leftToC + ";\n"
						+ iter + "->additional = " + rightToC + ";\n"
						+ iter + "->gen = &" + left.getCastType() + "_through;\n"
						+ iter + "->concat = NULL;\n";
				
				cText = iter;
			}
		}
		else if (bUp){
			
			if(left.getCastType().equals("Boolean")) {
				
				//true<.true; true<.false; false<.false
				if ((left.toString().equals("True")) 
					|| (left.toString().equals("false") && right.toString().equals("false")) )
				{
					cText = "NULL";
				}
				
				//false<.true
				else
				{
					String temp = Helper.getVarName();
					name += String.format("Boolean* %s = (Boolean*) x3malloc(sizeof(Boolean));\n"
							+ "%s->value = 1;\n"
							+ "%s->nrefs = 0;\n",
							temp, temp, temp);
					
					name +=  "Iterable* " + iter + "(Iterable*) malloc(sizeof(Iterable));\n"
							+ iter + "->nref = 1;\n"
							+ iter + "->value = " + temp + ";\n"
							+ iter + "->additional = NULL;\n"
							+ iter + "->gen = NULL;\n"
							+ iter + "->concat = NULL;\n";
					
					cText = iter;
				}
			}
			
			else {
				String temp = Helper.getVarName();
				int i = (Integer.parseInt(left.toString())) + 1;
				name += "Integer* " + temp + " = (Integer*) malloc(sizeof(Integer));\n"
						+ temp + "->value = " + i + ";\n";					
					
				name += right.construct();
				
				name +=  "Iterable* " + iter + "(Iterable*) malloc(sizeof(Iterable));\n"
						+ iter + "->nref = 1;\n"
						+ iter + "->value = " + temp + ";\n"
						+ iter + "->additional = " + rightToC + ";\n"
						+ iter + "->gen = &" + left.getCastType() + "_through;\n"
						+ iter + "->concat = NULL;\n";
				
				cText = iter;
			}
		}
		else if (bLow) {
			
			if(left.getCastType().equals("Boolean")) {
				
				//true.<true; true.<false; false.<false
				if ((left.toString().equals("True")) 
					|| (left.toString().equals("false") && right.toString().equals("false")) )
				{
					cText = "NULL";
				}
				
				//false.<true
				else
				{
					String temp = Helper.getVarName();
					name += String.format("Boolean* %s = (Boolean*) x3malloc(sizeof(Boolean));\n"
							+ "%s->value = 0;\n"
							+ "%s->nrefs = 0;\n",
							temp, temp, temp);
					
					name +=  "Iterable* " + iter + "(Iterable*) malloc(sizeof(Iterable));\n"
							+ iter + "->nref = 1;\n"
							+ iter + "->value = " + temp + ";\n"
							+ iter + "->additional = NULL;\n"
							+ iter + "->gen = NULL;\n"
							+ iter + "->concat = NULL;\n";
					
					cText = iter;
				}
			}
			
			else {
				String temp = Helper.getVarName();
				name += left.construct();
								
				int i = (Integer.parseInt(left.toString())) - 1;
				name += "Integer* " + temp + " = (Integer*) malloc(sizeof(Integer));\n"
						+ temp + "->value = " + i + ";\n";					
					
				
				name +=  "Iterable* " + iter + "(Iterable*) malloc(sizeof(Iterable));\n"
						+ iter + "->nref = 1;\n"
						+ iter + "->value = " + leftToC + ";\n"
						+ iter + "->additional = " + temp + ";\n"
						+ iter + "->gen = &" + left.getCastType() + "_through;\n"
						+ iter + "->concat = NULL;\n";
				
				cText = iter;
			}					
		}
		else {
			
			if(left.getCastType().equals("Boolean")) {
				//true<<true; true<<false; false<<false; false<<true
				cText = "NULL";
			}
				
			else {
				String temp1 = Helper.getVarName(), temp2 = Helper.getVarName();
				//name += left.construct();
								
				int i = (Integer.parseInt(left.toString())) + 1;
				name += "Integer* " + temp1 + " = (Integer*) malloc(sizeof(Integer));\n"
						+ temp1 + "->value = " + i + ";\n";					
				
				i = (Integer.parseInt(left.toString())) - 1;
				name += "Integer* " + temp2 + " = (Integer*) malloc(sizeof(Integer));\n"
						+ temp2 + "->value = " + i + ";\n";					
					
				
				name +=  "Iterable* " + iter + "(Iterable*) malloc(sizeof(Iterable));\n"
						+ iter + "->nref = 1;\n"
						+ iter + "->value = " + temp1 + ";\n"
						+ iter + "->additional = " + temp2 + ";\n"
						+ iter + "->gen = &" + left.getCastType() + "_through;\n"
						+ iter + "->concat = NULL;\n";
				
				cText = iter;
			}
		}
		return super.toC();
	}
}

class TimesExpr extends CuExpr{
	private CuExpr left, right;
	public TimesExpr(CuExpr e1, CuExpr e2) {
		this.left = e1;
		this.right = e2;
		super.methodId = "times";
		super.text = String.format("%s . %s < > ( %s )", left.toString(), super.methodId, right.toString());

	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
		return binaryExprType(context, left.getType(context).id, super.methodId, right.getType(context));
	}
	
	@Override
	public String toC() {
		String temp = Helper.getVarName();
		
		super.cText = temp;
		super.castType = "Integer";
		String leftToC = left.toC();
		String rightToC = right.toC();
		String leftC = left.construct();
		String rightC = right.construct();
		
		name += "\n" + leftC + rightC;
		
		super.name += String.format("Integer* %s  = (Integer*) malloc(sizeof(Integer));\n"
				+ "%s->nrefs = 0;\n"
				+ "%s->value=", temp, temp, temp);
		super.name += String.format("((%s*)%s)->value * ((%s*)%s)->value;\n", "Integer", leftToC, "Integer", rightToC);			

		/*if (leftC.equals("") && rightC.equals("")){
			//both are variables
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("((%s*)%s)->value * ((%s*)%s)->value;\n", "Integer", left.toC(), "Integer", right.toC());			
		}
		else if (leftC.equals("") && !rightC.equals("")) { 
			//right is number
			leftCastType = "(" + right.getCastType() + "*)";			
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("(%s %s)->value * %s.value;\n", leftCastType, left.toC(), right.toC());
		}
		else if (!leftC.equals("") && rightC.equals("")) {
			//left is number
			rightCastType = "(" + left.getCastType() + "*)";
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("%s.value * (%s %s)->value;\n", left.toC(), rightCastType, right.toC());
		}
		else {
			//both are numbers
			super.name += String.format("Integer %s;\n%s.value=", temp, temp);
			super.name += String.format("%s.value * %s.value;\n", left.toC(), right.toC());
		}*/
		return super.toC();
	}
}

class VarExpr extends CuExpr{// e.vv<tao1...>(e1,...)
	private CuExpr val;
	private String method;
	private List<CuType> types;
	List<CuExpr> es;
	public VarExpr(CuExpr e, String var, List<CuType> pt, List<CuExpr> es) {		
		this.val = e;
		this.method = var;
		this.types = pt;
		this.es = es;
		super.text = String.format("%s . %s %s %s", this.val.toString(), this.method, 
				Helper.printList("<", this.types, ">", ","), Helper.printList("(", this.es, ")", ","));
	}
	@Override public boolean isFunCall () {
		return true;
	}
	@Override protected CuType calculateType(CuContext context) throws NoSuchTypeException {
//System.out.println("in VarExp, begin");
        CuType tHat = val.getType(context); // 1st line in Figure 5 exp
//System.out.println("t_hat is " + tHat.id);
        CuClass cur_class = context.mClasses.get(tHat.id);
        if (cur_class == null) {
        	throw new NoSuchTypeException();
        }
        CuTypeScheme ts = cur_class.mFunctions.get(method);
        if (ts == null) {
        	throw new NoSuchTypeException();
        }
        //System.out.println("got this function");
        
        if (ts.data_kc.size() != types.size()) throw new NoSuchTypeException();
        Map<String, CuType> mapping = new HashMap<String, CuType>();
        Helper.P(String.format("kc=%s. types=%s. eMap=%s",ts.data_kc, types,val.calculateType(context).map));
        for (int i = 0; i < types.size(); i++) {
        	mapping.put(ts.data_kc.get(i), types.get(i));
        }
        mapping.putAll(val.getType(context).map); // add mapping from the expression that owns this method
        //added by Yinglei
        if (ts.data_tc.size() != es.size()) throw new NoSuchTypeException();
        List<CuType> tList = new ArrayList<CuType>();
        for (CuType ct : ts.data_tc.values()) {
        	tList.add(ct);
        }   
        for (int i = 0; i < es.size(); i++) {
        	if (!es.get(i).isTypeOf(context, tList.get(i), mapping)) {
        		//System.out.println(es.get(i).toString() + " doesnt match " + tList.get(i).toString() );
        		throw new NoSuchTypeException();
        	}
        }        	
        //System.out.println("in VarExp, end");
        ts.data_t.plugIn(mapping);
        Helper.P(String.format("%s returns %s<%s>, mapping %s", this, ts.data_t,ts.data_t.map, mapping));
        return ts.data_t;
	}
	
	@Override
		public String toC() {
		castType = Helper.cVarType.get(val+"_"+method);
		int offset = 0;									//to be modified when class definition becomes clearer
		String tempName = Helper.getVarName();
		String fptr = Helper.getVarName(), fptrArg = "", tempCastType = "";
		String classType = Helper.cVarType.get(val.toC()) + "*";
		name += "\n";
		name += String.format("%s this%s = (%s) %s;\n", classType, tempName, classType, val);
		String temp = "", expToC = "";
		if (es == null)
		{
			temp = String.format("((%s) this%s)", classType, tempName);
			fptrArg = "(" + classType + ")";
		}
		else {
			temp += String.format("((%s) this%s, ", classType, tempName);
			fptrArg = "(" + classType + ", ";
			for (CuExpr exp : es) {
				expToC = exp.toC();
				super.name += exp.construct();
				tempCastType = exp.getCastType();
				if (tempCastType.equals(""))
					tempCastType = Helper.cVarType.get(expToC);
				temp += "(" + tempCastType + "*)" + expToC + ", ";
				fptrArg += tempCastType + "*, ";
			}
			int j = temp.lastIndexOf(", ");
			if (j > 1)
				temp = temp.substring(0, j);
			j = 0;
			j = fptrArg.lastIndexOf(", ");
			if (j > 1)
				fptrArg = fptrArg.substring(0, j);
			temp += ")";
			fptrArg += ")";
		}
		
		name += String.format("void* (*%s) %s = (((%s) &%s)[0])[%d];	//unsure of this! needs testing\n", 	//unsure of this! needs testing				
								/*Helper.cVarType.get(var),*/ fptr, fptrArg, classType, val.toString(), offset);
		super.cText = String.format("(*%s) %s", fptr, temp);
		
			return super.toC();
		}

}
class VcExp extends CuExpr {
	private String val; 
	private List<CuType> types;
	private List<CuExpr> es;
	public VcExp(String v, List<CuType> pt, List<CuExpr> e){
		//System.out.println("in VcExp constructor, begin");
		this.val=v;
		this.types=pt;
		this.es=e;
		
		super.text=val.toString()+Helper.printList("<", types, ">", ",")+Helper.printList("(", es, ")", ",");
Helper.P("VcExp= "+text);
		//System.out.println("in VcExp constructor, end");
	}
	@Override public boolean isFunCall () {
		return true;
	}
	@Override protected CuType calculateType(CuContext context)  throws NoSuchTypeException{
		//System.out.println("in VcExp, begin val is " + val);
		//type check each tao_i // check tao in scope
		for (CuType ct : types) {
			ct.calculateType(context);
		}      
		
        if (context.getFunction(val) == null) throw new NoSuchTypeException();
        // check each es 
        TypeScheme cur_ts = (TypeScheme) context.getFunction(val);
        List<CuType> tList = new ArrayList<CuType>();
        for (CuType cur_type : cur_ts.data_tc.values()) {
            tList.add(cur_type);
        }
        Map<String, CuType> mapping = new HashMap<String, CuType>();
        for (int i = 0; i < types.size(); i++) {
        	mapping.put(cur_ts.data_kc.get(i), types.get(i));
        }
  Helper.P(String.format("mapping=%s. types=%s. data_kc=%s ", mapping, types, cur_ts.data_kc));
  		//added by Yinglei
        if (es.size() != cur_ts.data_tc.size()) throw new NoSuchTypeException();
        for (int i = 0; i < es.size(); i++) {
            if (!es.get(i).isTypeOf(context, tList.get(i), mapping)) {
            	//System.out.println(es.get(i).toString() + " doesnt match " + tList.get(i).toString() );
            	throw new NoSuchTypeException();
            }	
        }
        //System.out.println("in VcExp, end");
        cur_ts.data_t.plugIn(mapping);
 Helper.P(String.format("VcExp return %s<%s>", cur_ts.data_t, cur_ts.data_t.map));
        return cur_ts.data_t;
	}
	
	@Override
	public String toC() {
		castType = Helper.cVarType.get(val);
		String temp = "", tempCastType = "", expToC = "";
		if (es == null)
			temp = "()";
		temp += "(";
		for (CuExpr exp : es) {
			expToC = exp.toC();
			super.name += exp.construct();
			tempCastType = exp.getCastType();
			if(tempCastType.equals("")) tempCastType = Helper.cVarType.get(expToC);
			temp += "(" + tempCastType + "*)" + expToC + ", ";
		}
		int j = temp.lastIndexOf(", ");
		if (j > 1) temp = temp.substring(0, j);
		temp += ")";
		
		String objectName = Helper.getVarName();
		super.name += String.format("%s* %s = (%s*) malloc(sizeof(%s));\n"
				+ "(&%s)[0] = %s;	//pointer to vtable\n"
				+ "((int*) &%s)[1] = 0;		//pointer to nrefs\n",
				val, objectName, val, val, objectName, Helper.cClassVtablePtr.get(val), objectName);
		
		j = 2;
		
		for (CuExpr exp : es) {
			expToC = exp.toC();
			tempCastType = exp.getCastType();
			if(tempCastType.equals("")) tempCastType = Helper.cVarType.get(expToC);
			super.name += String.format("((" + tempCastType + "*) &%s)[%d] = " + expToC + ";\n", objectName, j++);
		}
		
		super.name += "\n"+Helper.cClassStats.get(val) + "\n";
		super.cText= objectName;
		return super.toC();
	}
}

class VvExp extends CuExpr{
	private String val;
	private List<CuType> types = new ArrayList<CuType>();
	private List<CuExpr> es = null;
	
	public VvExp(String str){
		val = str;
		super.text=str;
	}
	
	@Override public void add(List<CuType> pt, List<CuExpr> e){
		types = pt;
		es = e;
		super.text += Helper.printList("<", pt, ">", ",")+Helper.printList("(", es, ")", ",");
	}
	
	@Override public boolean isFunCall () {
		if (es == null)
			return false;
		else
			return true;
	}

	@Override protected CuType calculateType(CuContext context) {
		//System.out.println(String.format("in VvExp %s, begin %s", text, val));
		if (es == null) return context.getVariable(val);
		//else, it will be the same as in VcExp
        // check tao in scope
		//System.out.println("not a variable, checking function context");
        if (context.getFunction(val) == null) throw new NoSuchTypeException();
        //System.out.println("got this function from function context");
		//type check each tao_i // check tao in scope
		for (CuType ct : types) {
			ct.calculateType(context);
		}     
        // check each es 
        TypeScheme cur_ts = (TypeScheme) context.getFunction(val);
        List<CuType> tList = new ArrayList<CuType>();
        for (CuType cur_type : cur_ts.data_tc.values()) {
        	if(cur_type.id.equals("Iterable"))
        		cur_type.type = Helper.getTypeForIterable(cur_type.text);
            tList.add(cur_type);
        }
        Map<String, CuType> mapping = new HashMap<String, CuType>();
        for (int i = 0; i < types.size(); i++) {
        	mapping.put(cur_ts.data_kc.get(i), types.get(i));
        }
        Helper.P("VvExp MAPPING "+mapping);
		//added by Yinglei
		if (cur_ts.data_tc.size() != es.size()) throw new NoSuchTypeException();
        for (int i = 0; i < es.size(); i++) {
        	//System.out.println(es.get(i).toString());
            if (!es.get(i).isTypeOf(context, tList.get(i), mapping)) {
            	//System.out.println("type mismatch, " + "es is " + es.get(i).toString() + "tListgeti is " + tList.get(i).toString() );
                throw new NoSuchTypeException();
            }
Helper.P(String.format("calculated %s", es.get(i)));
        }
        cur_ts.data_t.plugIn(mapping);
Helper.P(String.format("VvExp returns %s<%s>", cur_ts.data_t, cur_ts.data_t.map));
        return cur_ts.data_t;
	}
	
	@Override
	public String toC() {
		if(es==null)
		{
			super.cText = val;
			super.castType = Helper.cVarType.get(val);
		}
		else
		{
			super.castType = Helper.cVarType.get(val);
			super.name += "\n";
			
			String temp = "", tempName = "", expToC = "";
			String tempCastType = "";
			if (es == null)
				temp = "()";
			temp += "(";
			for (CuExpr exp : es) {
				expToC = exp.toC();
				tempName = exp.construct();
				tempCastType = exp.getCastType();
				if(tempCastType.equals("")) tempCastType = Helper.cVarType.get(expToC);
				temp += "(" + tempCastType + "*)" + expToC + ", ";
				super.name += tempName;
			}
			int j = temp.lastIndexOf(", ");
			if (j > 1) temp = temp.substring(0, j);
			temp += ")";
			
			super.cText=val.toString()+temp;			
		}
		return super.toC();
	}
	
}
