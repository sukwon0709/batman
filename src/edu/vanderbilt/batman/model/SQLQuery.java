package edu.vanderbilt.batman.model;

import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import lombok.Getter;
import lombok.Setter;

public class SQLQuery {
        public final static int SQL_QUERY_TYPE_SELECT = 0;
        public final static int SQL_QUERY_TYPE_INSERT = 1;
        public final static int SQL_QUERY_TYPE_UPDATE = 2;
        public final static int SQL_QUERY_TYPE_DELETE = 3;
        public final static int SQL_QUERY_TYPE_REPLACE = 4;
        public final static int SQL_QUERY_TYPE_ADMINISTRATIVE = -2;
        public final static int SQL_QUERY_TYPE_UNDEFINED = -1;

        @Getter @Setter private String queryStr;
        
        //Yuan change
        
        @Getter @Setter private int type;      // 0: SELECT; 1: INSERT; 2: UPDATE;  3: DELETE; 
        @Getter @Setter private SortedSet<String> tables;      // the first/default table.
        @Getter @Setter private Vector<String> selectFields;

        @Getter @Setter private Vector<String> fields;
        @Getter @Setter private HashMap<String, String> values;  // the observed values.
        
        @Getter @Setter private String skeleton = "";
        
        // For logging purpose
        //@Getter @Setter private String session = null;
        @Getter @Setter private String script = null;
        
        public SQLQuery (String query, String spt) {
        	queryStr = query;
        	script = spt;
        }

        //Yuan add --> Xiaowei please double check
        public SQLQuery(SQLQuery q){
        	queryStr = q.queryStr;
            type = q.type;
            tables = new TreeSet<String>(q.tables);
            fields = new Vector<String>(q.fields);
            selectFields = new Vector<String>(q.selectFields);
            values = new HashMap<String, String>(q.values);
            skeleton = q.getSkeleton();
        }
        public SQLQuery(String qstr) {
        		queryStr = qstr;
                type = SQLQuery.SQL_QUERY_TYPE_UNDEFINED;
                tables = new TreeSet<String>();
                fields = new Vector<String>();
                selectFields = new Vector<String>();
                values = new HashMap<String, String>();
        }

        public SQLQuery() {

            type = SQLQuery.SQL_QUERY_TYPE_UNDEFINED;
            tables = new TreeSet<String>();
            fields = new Vector<String>();
            selectFields = new Vector<String>();
            values = new HashMap<String, String>();
    }

        public void clear(){
                type = SQLQuery.SQL_QUERY_TYPE_UNDEFINED;
                tables.clear();
                fields.clear();
                selectFields.clear();
                values.clear();
                queryStr=null;
        }
        
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("[QUERY][" + queryStr + "]");
			builder.append("[SCRIPT][" + script + "]");
			/*
			if (session == null || session.equals("")) {
				builder.append("[SESSION][null]\n");
			} else {
				builder.append("[SESSION][" + session + "]\n");
			}*/
			return builder.toString();
		}

}