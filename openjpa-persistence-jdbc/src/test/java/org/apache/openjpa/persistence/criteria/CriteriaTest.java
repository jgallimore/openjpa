/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.openjpa.persistence.criteria;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.QueryBuilder;

import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.sql.DBDictionary;
import org.apache.openjpa.lib.jdbc.AbstractJDBCListener;
import org.apache.openjpa.lib.jdbc.JDBCEvent;
import org.apache.openjpa.lib.jdbc.ReportingSQLException;
import org.apache.openjpa.persistence.test.SQLListenerTestCase;


/**
 * Generic utility to run Criteria tests. 
 * 
 * Provides facility to compare the target SQL generated by good old JPQL
 * and newly minted Criteria.
 *  
 * Loads a domain model. 
 * 
 */
public abstract class CriteriaTest extends SQLListenerTestCase {
    QueryBuilder cb;
    EntityManager em;
    
    public void setUp(Object...props) {
            super.setUp(
                CLEAR_TABLES,
                Account.class, 
                Address.class, 
                CompUser.class,
                Contact.class,
                Contractor.class, 
                Course.class, 
                CreditCard.class,
                Customer.class, 
                Department.class, 
                Employee.class,
                Exempt.class, 
                FemaleUser.class,
                FrequentFlierPlan.class, 
                Item.class,
                LineItem.class, 
                Manager.class, 
                MaleUser.class,
                Movie.class, 
                Order.class, 
                Person.class,
                Phone.class, 
                Photo.class, 
                Product.class,
                Semester.class, 
                Student.class, 
                Transaction.class,
                TransactionHistory.class,
                VideoStore.class);
            
        setDictionary();
        
        em = emf.createEntityManager();
        cb = emf.getQueryBuilder();
    }
    
    public final void tearDown() {
        // important: do nothing
    }

    void setDictionary() {
        JDBCConfiguration conf = (JDBCConfiguration) emf.getConfiguration();
        DBDictionary dict = conf.getDBDictionaryInstance();
        dict.requiresCastForComparisons = false;
        dict.requiresCastForMathFunctions = false;
    }

    void assertEquivalence(CriteriaQuery c, String jpql, String[] paramNames,
            Object[] params) {
        Query cQ = em.createQuery(c);
        for (int i = 0; i < paramNames.length; i++) {
            cQ.setParameter(paramNames[i], params[i]);
        }
        Query jQ = em.createQuery(jpql);
        for (int i = 0; i < paramNames.length; i++) {
            jQ.setParameter(paramNames[i], params[i]);
        }
        executeAndAssert(cQ, jQ);
    }

    /**
     * Executes the given CriteriaQuery and JPQL string and compare their
     * respective SQLs for equality.
     */

    void assertEquivalence(CriteriaQuery c, String jpql, Object[] params) {
        Query cQ = em.createQuery(c);
        for (int i = 0; i < params.length; i++) {
            cQ.setParameter(i + 1, params[i]);
        }
        
        Query jQ = em.createQuery(jpql);
        for (int i = 0; i < params.length; i++) {
            jQ.setParameter(i + 1, params[i]);
        }
        
        executeAndAssert(cQ, jQ);
    }

    void assertEquivalence(CriteriaQuery c, String jpql) {
        executeAndAssert(em.createQuery(c), em.createQuery(jpql));
    }

    void executeAndAssert(Query cQ, Query jQ) {
        List<String>[] sqls = new ArrayList[2];
        if (!execute(cQ, jQ, sqls)) {
            fail("Invalid SQL for Criteria;[" + sqls[1] + "]. \r\n"
                    + "Expeceted [" + sqls[0] + "]");
        }
        assertEquals(sqls[0].size(), sqls[1].size());
        for (int i = 0; i < sqls[0].size(); i++)
           assertEquals(sqls[0].get(i), sqls[1].get(i));
    }

    /**
     * Execute the two given queries. The first query originated from a JPQL
     * string must be well-formed. The second query originated from a Criteria
     * is being tested.
     * 
     * @param sqls The target SQL for the queries will be filled-in the given
     *            array.
     * @return true if both queries execute successfully.
     */
    boolean execute(Query cQ, Query jQ, List<String>[] sqls) {
        sql.clear();
        List jList = jQ.getResultList();
        sqls[0] = new ArrayList<String>(sql);

        sql.clear();
        try {
            List cList = cQ.getResultList();
        } catch (PersistenceException e) {
            e.printStackTrace();
            sqls[1] = new ArrayList<String>(sql);
            sqls[1].add(extractSQL(e));
            return false;
        }
        sqls[1] = new ArrayList<String>(sql);

        return true;
    }

    String extractSQL(PersistenceException e) {
        Throwable t = e.getCause();
        if (t instanceof ReportingSQLException)
            return ((ReportingSQLException) t).getSQL();
        return null;
    }
    
    public class MyListener extends AbstractJDBCListener {
        public final List<String> sql = new ArrayList<String>();

        @Override
        public void beforeExecuteStatement(JDBCEvent event) {
            if (event.getSQL() != null) {
                sql.add(event.getSQL());
            }
        }
    }

}
