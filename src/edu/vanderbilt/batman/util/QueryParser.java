package edu.vanderbilt.batman.util;

import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.IntervalExpression;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitor;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Union;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import edu.vanderbilt.batman.model.SQLQuery;

/**
 * This class is designed to parse SQL query statement into SQLQuery object by calling JSqlParser library.
 * @author xli
 *
 */

//Yuan: The design of this class needs some further thought

public class QueryParser implements StatementVisitor, SelectVisitor, ExpressionVisitor, ItemsListVisitor, FromItemVisitor, SelectItemVisitor
{
	private SQLQuery query;
	private CCJSqlParserManager pm;
	
	private enum Clause { NONE, WHERE, ITEM_LIST, SELECT_MAIN, SELECT_SUB };
	
	private Clause mode = Clause.NONE;
	private String parsed_expression = "";
	
	private String[] unhandledKeyword = {"COMMIT", "BEGIN", "LIMIT", "limit"};
	
	private HashMap<String, String> aliasList = new HashMap<String, String>();
	//private boolean debug_mode = true;
	private boolean debug_mode = false;
	
	//Set true to tokenize constants
	//Ex: all strings "abc", "ace", etc. will go to {STRING}
	private Boolean TokenizeConstants = true;
	
	//Yuan dup code; need to resolve
	public QueryParser(String querystr) {
		query = new SQLQuery(querystr);
		pm = new CCJSqlParserManager();
	}

	public QueryParser() {
		query = new SQLQuery();
		pm = new CCJSqlParserManager();
	}

	public SQLQuery getSQLQuery() {
		return query;
	}
	
	/*
	 * Methods needed for Parser
	 */
	public boolean checkNonAsciiExists(String s) {
		CharsetEncoder asciiEncoder = Charset.forName("US-ASCII").newEncoder(); // or "ISO-8859-1" for ISO Latin 1
		return !asciiEncoder.canEncode(s);
	}
	
	/*
	 * Main interface
	 */
	public SQLQuery parseSQLQuery(String str) throws JSQLParserException {
		query.clear();		
		query.setQueryStr(str);
		Statement statement;
		
		//Yuan add: filterSQLQuery returns null for unsupported SQL keywords
		//System.out.println("Before filter:" + str);
		String filteredStr = filterSQLQuery(str);
		//System.out.println("After filter:" + filteredStr);
		
		try {
			if (filteredStr!=null){
				statement = pm.parse(new StringReader(filteredStr));
				statement.accept(this);
				query.setSkeleton(parsed_expression);
			} else
				query.setType(SQLQuery.SQL_QUERY_TYPE_UNDEFINED);
		} catch (JSQLParserException e) {
			// TODO Change to return an error SQLQuery object
			//System.err.println("For query '"+str+"'");
			//e.printStackTrace();
			throw e;
		}
		
		
		//Debug
		if(debug_mode ) {
			System.out.println("Parsed Query:");
			System.out.println(parsed_expression);
			
			System.out.print("\nTables:\n\t");
			for(String t : query.getTables())
				System.out.print(t+"  ");
			if(query.getTables().isEmpty())
				System.out.print("(none)");
			
			if(query.getType() == SQLQuery.SQL_QUERY_TYPE_SELECT) {
				System.out.println("\nSelect Fields:");
				for(String f : query.getSelectFields())
					System.out.println("\t"+f);
				System.out.println("\nConditions:");
			} else System.out.println("\nField/Values:");
			
			for(String f : query.getFields())
				System.out.println("\t"+f+" : "+query.getValues().get(f));
			if(query.getFields().isEmpty())
				System.out.print("\t(none)");
		}
		return query;
	}
	
	private String filterSQLQuery(String str) {
		for (String uhkeyword: unhandledKeyword) {
			if (str.contains(uhkeyword) && (uhkeyword.equalsIgnoreCase("limit"))){
				String[] splits = str.split(uhkeyword);
				return splits[0]; //Yuan: this needs to be changed later for handling "LIMIT" in a more reliable way..
			} else if (str.contains(uhkeyword))
				return null;
		}
		return str;
	
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.jsqlparser.statement.StatementVisitor
	 * 
	 * Visit methods for StatementVisitor
	 */
	
	@Override
	public void visit(Select select) {
		if (debug_mode) System.out.println("Parsing Select " + select.toString());
		query.setType(SQLQuery.SQL_QUERY_TYPE_SELECT);
		mode = Clause.SELECT_MAIN;
		parsed_expression = "";
		select.getSelectBody().accept(this);
	}


	@Override
	public void visit(Delete delete) {
		if (debug_mode) System.out.println("Parsing Delete " + delete.toString());
		query.setType(SQLQuery.SQL_QUERY_TYPE_DELETE);
		delete.getTable().accept(this);
		parsed_expression = "DELETE FROM " + delete.getTable().getName() +" WHERE ";
		mode = Clause.WHERE;
		delete.getWhere().accept(this);
	}


	@Override
	public void visit(Update update) {
		if (debug_mode) System.out.println("Parsing Update " + update.toString());
		query.setType(SQLQuery.SQL_QUERY_TYPE_UPDATE);
		update.getTable().accept(this);
		
		for(Object o : update.getColumns() ) {
			if(o instanceof Column) {				
				parsed_expression = "";
				Column col = (Column) o;
				col.accept(this);
				query.getFields().add(parsed_expression);
			}
		}
		
		parsed_expression = "UPDATE "+update.getTable().getName()+" SET ";
		@SuppressWarnings("rawtypes")
		Iterator it_val = update.getExpressions().iterator();
		Iterator<String> it_col = query.getFields().iterator();
		Boolean first = true;
		while(it_val.hasNext() && it_col.hasNext()) {
			Object o = it_val.next();			
			if(o instanceof Expression) {
				if(first) first = false;
				else parsed_expression += ", ";
				Expression exp = (Expression) o;
				String pe_tmp = parsed_expression;
				parsed_expression = "";
				String col = it_col.next();
				exp.accept(this);
				String value = exp.toString();
				if (value.contains("'")) {
					query.getValues().put(col, filterValue(value));
				}				
				parsed_expression = pe_tmp + col+" = "+parsed_expression;
			}
		}
		
		parsed_expression += " WHERE ";
		
		if(it_val.hasNext() || it_col.hasNext()) {
			System.err.println("Warning: mismatch in number of columns and values.");
		}
		
		mode = Clause.WHERE;
		update.getWhere().accept(this);
	}


	@Override
	public void visit(Insert insert) {
		if (debug_mode) System.out.println("Parsing Insert " + insert.toString());
		query.setType(SQLQuery.SQL_QUERY_TYPE_INSERT);
		insert.getTable().accept(this);
		
		parsed_expression = "INSERT INTO "+insert.getTable().getName();
		
		String pe_tmp = parsed_expression;
		Boolean first = true;
		//Add fields
		//Yuan debug here (what the expression is for?)
		
		if (insert.getColumns()!=null) {
			//no col information specified; default col is used
			pe_tmp += " (";
			for(Object o : insert.getColumns() ) {
				if(o instanceof Column) {
					if(first) first = false;
					else pe_tmp += ", ";
					
					parsed_expression = "";
					Column col = (Column) o;
					col.accept(this);
					query.getFields().add(parsed_expression);
					pe_tmp += parsed_expression;
				}
			}
			parsed_expression = pe_tmp + ") VALUES (";
		} else {
			parsed_expression = pe_tmp + " VALUES (";
		}
		mode = Clause.ITEM_LIST;
		insert.getItemsList().accept(this); //Values
		//No where
		parsed_expression += ")";
	}


	@Override
	public void visit(Replace replace) {
		//TODO Finalize syntax matching
		//Like INSERT but will overwrite if already exists
		query.setType(SQLQuery.SQL_QUERY_TYPE_REPLACE);
		replace.getTable().accept(this);
		
		parsed_expression = "REPLACE ...";
		
		String pe_tmp = parsed_expression;
		Boolean first = true;
		for(Object o : replace.getColumns() ) {
			if(o instanceof Column) {
				if(first) first = false;
				else pe_tmp += ", ";
				
				parsed_expression = "";
				Column col = (Column) o;
				col.accept(this);
				query.getFields().add(parsed_expression);
				pe_tmp += parsed_expression;
			}
		}
		parsed_expression = pe_tmp;
		
		//TODO Process values
		mode = Clause.ITEM_LIST;
		if(replace.getItemsList()!= null) {
			
		} else if(replace.getExpressions()!=null) {
			
		}
		
	}


	@Override
	public void visit(Drop drop) {
		System.out.println(parsed_expression);
		//Do nothing for now
		query.setType(SQLQuery.SQL_QUERY_TYPE_ADMINISTRATIVE);
	}


	@Override
	public void visit(Truncate truncate) {
		//Do nothing for now
		query.setType(SQLQuery.SQL_QUERY_TYPE_ADMINISTRATIVE);
		
	}


	@Override
	public void visit(CreateTable createTable) {
		//Do nothing for now
		query.setType(SQLQuery.SQL_QUERY_TYPE_ADMINISTRATIVE);
	}

	
	/*
	 * (non-Javadoc)
	 * @see net.sf.jsqlparser.statement.select.SelectVisitor
	 * 
	 * Visit methods for SelectVisitor
	 */
	
	@SuppressWarnings("rawtypes")
	@Override
	public void visit(PlainSelect plainSelect) {
		if (debug_mode) System.out.println("Parsing PlainSelect " + plainSelect.toString());
		parsed_expression += "SELECT ";
		String pe_bak = parsed_expression;
		
		//Process FROM out of order, to make sure we know tables
		parsed_expression = " FROM ";
		plainSelect.getFromItem().accept(this);
		//System.out.println("after from " + parsed_expression);
		
		if (plainSelect.getJoins() != null) {
			for (Iterator joinsIt = plainSelect.getJoins().iterator(); joinsIt.hasNext();) {
				parsed_expression += ", ";
				Join join = (Join) joinsIt.next();
				join.getRightItem().accept(this);
			}
		}
		String pe_from = parsed_expression;
		//System.out.println("after join " + parsed_expression);
		
		parsed_expression = pe_bak;
		Boolean first = true;
		for(Iterator it = plainSelect.getSelectItems().iterator(); it.hasNext(); ) {
			Object o = it.next();
			if(o instanceof SelectItem) {
				if(first) first = false;
				else parsed_expression += ", ";
				
				SelectItem si = (SelectItem) o;
				si.accept(this);
			} else {
				System.out.println("Unknown column of type:"+o.getClass().toString());
			}
		}
		//System.out.println("after select " + parsed_expression);
		
		parsed_expression += pe_from;
		
		parsed_expression += " WHERE ";
		
		mode = Clause.WHERE;
		if(plainSelect.getWhere() != null)
			plainSelect.getWhere().accept(this);
		//System.out.println("after where " + parsed_expression);
		if(parsed_expression.endsWith(" WHERE ")) 
			parsed_expression += "1";
	}

	@Override
	public void visit(Union union) {
		// TODO Figure out Unions later.
		// We'll probably have to change SQLQuery class
		System.err.println("Unimplemented: UNION");
		
		/* Example code
		for (Iterator iter = union.getPlainSelects().iterator(); iter.hasNext();) {
			PlainSelect plainSelect = (PlainSelect) iter.next();
			visit(plainSelect);
		}
		 */
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.sf.jsqlparser.expression.ExpressionVisitor
	 * 
	 * Visit methods for ExpressionVisitor
	 */

	@Override
	public void visit(NullValue nullValue) {
		parsed_expression += "NULL";
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void visit(Function function) {
		if (debug_mode) System.out.println("Parsing Function " + function.toString());
		parsed_expression += function.getName() + "(";
		
		//So it doesn't blow up on functions like NOW()
		if(function.getParameters() != null) {
			for(Iterator iter = function.getParameters().getExpressions().iterator(); iter.hasNext(); ) {
				Object o = iter.next();
				
				if(o instanceof Expression) {
					Expression exp = (Expression) o;
					exp.accept(this);
					if(iter.hasNext()) parsed_expression += ",";
				}
			}
		}
		parsed_expression += ")";
	}
	
	@Override
	public void visit(IntervalExpression intervalExpression) {
		//We could put this as constant type, but this is an expression
		//To put to signature, perhaps fix the units
		if (debug_mode) System.out.println("Parsing IntervalExpression " + intervalExpression.toString());
		parsed_expression += "INTERVAL ";
		intervalExpression.getNum().accept(this);
		parsed_expression += " ";
		parsed_expression += intervalExpression.getUnits();
	}
	
	@Override
	public void visit(InverseExpression inverseExpression) {
		if (debug_mode) System.out.println("Parsing InverseExpression " + inverseExpression.toString());
		Expression exp = inverseExpression.getExpression();
		
		if(!(exp instanceof DoubleValue || exp instanceof LongValue)) {		
			parsed_expression += "-";
		}
		
		inverseExpression.getExpression().accept(this);
	}

	@Override
	public void visit(JdbcParameter jdbcParameter) {
		parsed_expression += "?";
	}

	@Override
	public void visit(DoubleValue doubleValue) {
		if (debug_mode) System.out.println("Parsing DoubleValue " + doubleValue.toString());
		parsed_expression += tokenize(doubleValue.toString(), "{NUMERIC}");
	}

	@Override
	public void visit(LongValue longValue) {
		if (debug_mode) System.out.println("Parsing LongValue " + longValue.toString());
		parsed_expression += tokenize(longValue.toString(), "{NUMERIC}");
	}

	@Override
	public void visit(DateValue dateValue) {
		if (debug_mode) System.out.println("Parsing DateValue " + dateValue.toString());
		parsed_expression += tokenize(dateValue.toString(), "{DATE}");
	}

	@Override
	public void visit(TimeValue timeValue) {
		if (debug_mode) System.out.println("Parsing TimeValue " + timeValue.toString());
		parsed_expression += tokenize(timeValue.toString(), "{TIME}");
	}

	@Override
	public void visit(TimestampValue timestampValue) {
		if (debug_mode) System.out.println("Parsing TimestampValue " + timestampValue.toString());
		parsed_expression += tokenize(timestampValue.toString(), "{TIMESTAMP}");
	}

	@Override
	public void visit(Parenthesis parenthesis) {
		if (debug_mode) System.out.println("Parsing Parenthesis " + parsed_expression);
		parsed_expression += "(";
		parenthesis.getExpression().accept(this);
		parsed_expression += ")";
	}

	@Override
	public void visit(StringValue stringValue) {
		if (debug_mode) System.out.println("Parsing StringValue " + stringValue.toString());
		parsed_expression += tokenize(stringValue.toString(), "{STRING}");
	}

	@Override
	public void visit(Addition addition) {
		visitBinaryExpression(addition, " + ");
	}

	@Override
	public void visit(Division division) {
		visitBinaryExpression(division, " / ");
	}

	@Override
	public void visit(Multiplication multiplication) {
		visitBinaryExpression(multiplication, " * ");
	}

	@Override
	public void visit(Subtraction subtraction) {
		visitBinaryExpression(subtraction, " - ");
	}

	@Override
	public void visit(AndExpression andExpression) {
		visitBinaryExpression(andExpression, " AND ");
	}

	@Override
	public void visit(OrExpression orExpression) {
		visitBinaryExpression(orExpression, " OR ");
	}

	@Override
	public void visit(Between between) {
		// TODO Between Auto-generated method stub
		//Convert to > and < ?
		System.err.println("Unimplemented: BETWEEN");
	}

	@Override
	public void visit(EqualsTo equalsTo) {
		visitRelativeExpression(equalsTo, " = ");
	}

	@Override
	public void visit(GreaterThan greaterThan) {
		visitRelativeExpression(greaterThan, " > ");
	}

	@Override
	public void visit(GreaterThanEquals greaterThanEquals) {
		visitRelativeExpression(greaterThanEquals, " >= ");
	}

	@Override
	public void visit(InExpression inExpression) {
		if (debug_mode) System.out.println("Parsing InExpression " + inExpression.toString());
		if(mode == Clause.WHERE) {
			String pe_bak = new String(parsed_expression);
			parsed_expression = "";
			inExpression.getLeftExpression().accept(this);
			String col = parsed_expression;
			parsed_expression = "";
			
			Clause mode_bak = mode;
			mode = Clause.ITEM_LIST;
			inExpression.getItemsList().accept(this);
			mode = mode_bak;
			
			query.getFields().add(col);
			query.getValues().put(col, " IN "+parsed_expression);
			parsed_expression = pe_bak + col + " IN " + parsed_expression;
		} else
		System.err.println("Unimplemented: InExpression");
	}

	@Override
	public void visit(IsNullExpression isNullExpression) {
		if (debug_mode) System.out.println("Parsing IsNullExpression " + isNullExpression.toString());
		parsed_expression += (isNullExpression.isNot())? "ISNOTNULL(" : "ISNULL(";
		isNullExpression.getLeftExpression().accept(this);
		parsed_expression += ")";
	}

	@Override
	public void visit(LikeExpression likeExpression) {
		if (debug_mode) System.out.println("Parsing LikeExpression " + likeExpression.toString());
		visitRelativeExpression(likeExpression, " LIKE ");
	}

	//Less than...
	@Override
	public void visit(MinorThan minorThan) {
		visitRelativeExpression(minorThan, " < ");
	}

	@Override
	public void visit(MinorThanEquals minorThanEquals) {
		visitRelativeExpression(minorThanEquals, " <= ");
	}

	@Override
	public void visit(NotEqualsTo notEqualsTo) {
		visitRelativeExpression(notEqualsTo, " != ");
	}

	@Override
	public void visit(Column tableColumn) {
		if (debug_mode) System.out.println("Parsing TableColumn " + tableColumn.toString());
		String col = tableColumn.getWholeColumnName();
		
		//Undo aliasing (only in WHERE clause)
		if(aliasList.containsKey(col) && mode == Clause.WHERE)
			col = aliasList.get(col);
		
		//Make sure is TABLE.COLUMN
		if(!col.contains("."))
			col = query.getTables().first() + "." + col; // Here: use first table for JOIN.
			//col = query.getTables().last() + "." + col;
		
		//Remove the tic marks (`)
		col = col.replace("`", "");
		
		parsed_expression += col;
	}

	@Override
	public void visit(SubSelect subSelect) {
		if (debug_mode) System.out.println("Parsing SubSelect " + subSelect.toString());
		Clause mode_bak = mode;
		mode = Clause.SELECT_SUB;
		subSelect.getSelectBody().accept(this);
		mode = mode_bak;
	}

	@Override
	public void visit(CaseExpression caseExpression) {
		System.err.println("Unimplemented: CASE");
	}

	@Override
	public void visit(WhenClause whenClause) {
		System.err.println("Unimplemented: WHEN (in CASE)");
	}

	@Override
	public void visit(ExistsExpression existsExpression) {
		if (debug_mode) System.out.println("Parsing ExistsExpression " + existsExpression.toString());
		parsed_expression += existsExpression.getStringExpression() + " ";
		existsExpression.getRightExpression().accept(this);
	}

	@Override
	public void visit(AllComparisonExpression allComparisonExpression) {
		// TODO AllComparison Auto-generated method stub
		//Not sure what this is... IN ALL?
		System.err.println("Unimplemented: AllComparisonExpression");
	}

	@Override
	public void visit(AnyComparisonExpression anyComparisonExpression) {
		// TODO AnyComparison Auto-generated method stub
		//Not sure what this is... IN ANY?
		System.err.println("Unimplemented: AnyComparisonExpression");
	}

	@Override
	public void visit(Concat concat) {
		if (debug_mode) System.out.println("Parsing Concat " + concat.toString());
		visitBinaryExpression(concat, " || ");
	}

	@Override
	public void visit(Matches matches) {
		visitBinaryExpression(matches, " @@ ");
	}

	@Override
	public void visit(BitwiseAnd bitwiseAnd) {
		visitBinaryExpression(bitwiseAnd, " & ");
	}

	@Override
	public void visit(BitwiseOr bitwiseOr) {
		visitBinaryExpression(bitwiseOr, " | ");
	}

	@Override
	public void visit(BitwiseXor bitwiseXor) {
		visitBinaryExpression(bitwiseXor, " ^ ");
	}

	/* 
	 * (non-Javadoc)
	 * @see net.sf.jsqlparser.statement.FromItemVisitor
	 * 
	 * Visit methods for FromItemVisitor
	 */
	
	
	//The ExpressionList visitor does double duty (at least)
	@Override
	public void visit(ExpressionList expressionList) {
		if (debug_mode) System.out.println("Processing tables as ExpressionList " + expressionList.toString());
		String pe_tmp = parsed_expression;
		@SuppressWarnings("rawtypes")
		Iterator iter = expressionList.getExpressions().iterator();
		Iterator<String> it_col = query.getFields().iterator();
		
		//while(iter.hasNext() && (mode!=Clause.ITEM_LIST || it_col.hasNext()) ) {
		while(iter.hasNext()) {
			//System.out.println("HERE!!");
			Object o = iter.next();
			if(mode == Clause.ITEM_LIST)
				parsed_expression = "";
			
			if(o instanceof Expression) {
				Expression exp = (Expression) o;
				if(mode == Clause.ITEM_LIST) {
					if (it_col.hasNext()) {
						String col = it_col.next();
						String value = exp.toString();
						if (value.contains("'")) {
							query.getValues().put(col, filterValue(value));
						}
					}
				}
				exp.accept(this);
				if(iter.hasNext()) parsed_expression += ", ";
				if(mode == Clause.ITEM_LIST)
					pe_tmp += parsed_expression;
				
			} else {
				System.err.println("Unprocessed type: "+o.getClass().toString());
			}
		}
		
		if(mode == Clause.ITEM_LIST) {
			parsed_expression = pe_tmp;
			if(iter.hasNext() || it_col.hasNext()){
				System.err.println("Warning: mismatched columns and values.");
			}
		}
	}

	@Override
	public void visit(Table tableName) {
		if (debug_mode) System.out.println("Processing tables as Table " + tableName.toString());
		query.getTables().add(tableName.getName().replace("`", ""));
		parsed_expression += tableName.getName().replace("`", "");
	}

	@Override
	public void visit(SubJoin subjoin) {
		if (debug_mode) System.out.println("Processing tables as SubJoin " + subjoin.toString());
		//Should recurse just giving us the Table
		subjoin.getLeft().accept(this);
		subjoin.getJoin().getRightItem().accept(this);
	}

	/*
	 * (non-Javadoc)
	 * @see net.sf.jsqlparser.statement.SelectItemsVisitor
	 * 
	 * Visit methods for SelectItemsVisitor
	 */
	
	@Override
	public void visit(AllColumns allColumns) {
		if (debug_mode) System.out.println("Parsing AllColumns " + allColumns.toString());
		if(mode == Clause.SELECT_MAIN)
			query.getSelectFields().add(query.getTables().first() + ".*");
		
		parsed_expression += "*";
	}

	@Override
	public void visit(AllTableColumns allTableColumns) {
		if (debug_mode) System.out.println("Parsing AllTableColumns " + allTableColumns.toString());
		if(mode == Clause.SELECT_MAIN)
			query.getSelectFields().add(allTableColumns.getTable().getWholeTableName().replace("`", "")+".*");
		
		parsed_expression += allTableColumns.getTable().getWholeTableName()+".*";
	}

	@Override
	public void visit(SelectExpressionItem selectExpressionItem) {
		if (debug_mode) System.out.println("Parsing SelectExpressionItem " + selectExpressionItem.toString());
		String pe_bak = parsed_expression;
		parsed_expression = "";
		
		String alias = selectExpressionItem.getAlias();
		if(alias != null && !alias.equals("")) {
			//aliasList.put(alias, parsed_expression);
			selectExpressionItem.setExpression(new Column(new Table(),alias));
		}
		
		selectExpressionItem.getExpression().accept(this);
		if(mode == Clause.SELECT_MAIN)
			query.getSelectFields().add(parsed_expression);
		
		parsed_expression = pe_bak + parsed_expression;
		//System.out.println("after alias: " + parsed_expression);
	}
	
	/*
	 * Helper methods for the ExpressionVisitor part
	 * Reduce redundancy in the binary expressions
	 */
	
	public void visitRelativeExpression(BinaryExpression binaryExpression, String op) {
		if (debug_mode) System.out.println("Parsing RelativeExpression " + binaryExpression.toString() + " op: " + op);
		if(mode == Clause.WHERE) {
			String pe_bak = new String(parsed_expression);
			parsed_expression = "";
			binaryExpression.getLeftExpression().accept(this);
			String col = parsed_expression;
			parsed_expression = "";
			binaryExpression.getRightExpression().accept(this);
			query.getFields().add(col);
			String value = binaryExpression.getRightExpression().toString();
			if (value.contains("'")) {
				query.getValues().put(col, filterValue(value));
			}
			parsed_expression = pe_bak + col + op + parsed_expression;
		} else visitBinaryExpression(binaryExpression, op);
	}

	public void visitBinaryExpression(BinaryExpression binaryExpression, String op) {
		if (debug_mode) System.out.println("Parsing BinaryExpression " + binaryExpression.toString() + " op: " + op);
		binaryExpression.getLeftExpression().accept(this);
		parsed_expression += op;
		binaryExpression.getRightExpression().accept(this);
	}
	
	
	private String filterValue(String value) {
		if (value.charAt(0) == '\'' && value.charAt(value.length()-1) == '\'') {
			return value.substring(1, value.length()-1);
		}
		return value;
	}
	

	String tokenize(String in, String literal_type) {
		String in1 = in.replaceFirst("^'(.+)'$", "$1");
		in1 = in1.replaceFirst("^\"(.+)\"$", "$1");
		//String in2 = in1.trim().replace("\n", "");
		
		if(TokenizeConstants) {
			return literal_type;
		}
		
		//System.out.println("No match. Returned "+in);
		//Otherwise, leave as-is
		return in1;
	}
	
	/*
	 * Testing function
	 */
	
	public static void main(String args[]) throws Exception {
		QueryParser p = new QueryParser();
		p.debug_mode = true;
		Vector<String> tests = new Vector<String>();
		// scarf test case: LEFT JOIN.
		tests.add("INSERT INTO admin VALUES ('','gPSMpG@e.com', '522a76e39af4ddd45880cd26ada37b35')");
		tests.add("SELECT pictures.id, pictures.title, pictures.filename, pictures.price, pictures.user_id, pictures.tag, pictures.high_quality, users.login,  UNIX_TIMESTAMP(pictures.created_on) as created_on_unix from pictures, users where pictures.id = '8' and pictures.user_id = users.id limit 1");
		//tests.add("SELECT session_id, name, UNIX_TIMESTAMP(starttime) AS starttime, UNIX_TIMESTAMP(starttime + INTERVAL duration MINUTE) AS endtime, CONCAT(firstname, ' ', lastname) AS fullname FROM sessions LEFT JOIN users ON sessions.user_id = users.user_id ORDER BY starttime");
		//tests.add("SELECT * FROM events WHERE (year >= 2012 AND month > 05) OR (year >= 2012 AND month = 05 AND day >= 29) OR (year_end >= 2012 AND month_end = 05 AND day_end >= 29) OR (year > 2012) OR (year_show >= 2012 AND month_show > 05)OR (year_show >= 2012 AND month_show = 05 AND day_show >= 29) OR (year_show > 2012) ORDER BY year, month, day LIMIT 0, 10");
		//tests.add("SELECT  News.* ,  `Departments`.`Description` as `dl_Description` ,  CONCAT(Employees.FirstName,' ',Employees.LastName) AS Employee_Display   FROM `News`  LEFT JOIN `openIT`.`News_Departments`   ON `openIT`.`News_Departments`.`NewsID`=`News`.`NewsID`  LEFT JOIN `openIT`.`Departments`   ON `openIT`.`Departments`.`ID`=`News_Departments`.`DepartmentID`    LEFT JOIN `openIT`.`Employees`   ON `openIT`.`Employees`.`EmployeeID`=`News`.`EmployeeID`    WHERE ( (ExpirationTime IS NULL OR ExpirationTime > CAST('2012-05-30 15:57:28' AS DATETIME)) )  AND ( News.Flag != 1 ) AND ( (News_Departments.NewsID IS NULL) )  GROUP BY News.NewsID    ORDER BY NewsID DESC, ExpirationTime  ");
		//tests.add("SELECT MAX( `order` ) as max FROM `papers` WHERE session_id = '136'");
		//tests.add("INSERT INTO papers (title, abstract, pdf, pdfname, session_id, `order`) VALUES ('(1) Security Analysis of India\\'s Electronic Voting Machines', 'Abstract: (1) Security Analysis of India\\'s Electronic Voting Machines', '', '', '157', '1')");
		//tests.add("INSERT INTO `users` (`id`, `login`, `password`, `firstname`, `lastname`, `salt`, `tradebux`, `created_on`, `last_login_on`) VALUES (NULL, 'anonymous_XD', SHA1('123456NDY2'), 'FRKdE', 'sKjjG', 'NDY2','100', NOW(), NOW())");
		//tests.add("INSERT INTO papers SET title='A paper', abstract='abs', pdf='none', pdfname='Nothing', session_id='132', `order`='1'");
		//tests.add("SELECT user FROM blogusername WHERE user=\'admin\'                    AND password LIKE BINARY \'5f4dcc3b5aa765d61d8327deb882cf99\'");
		//tests.add("UPDATE users SET username='admin', pwd='123456', active=2 WHERE uid=10");
		for(String test : tests) {
			System.out.println("INPUT:\n"+test);
			//System.out.println(test);
			try {
				p.parseSQLQuery(test);
				System.out.println(p.getSQLQuery().getValues());
		        System.out.println(p.getSQLQuery().getFields());
				//System.out.println(p.getQueryParameters(test));
			} catch (Exception e){
				e.printStackTrace();
			}
			System.out.println("\n------------------------------\n");
		}
	}
}