package org.basex.test.examples;

import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;
import org.xmldb.api.*;

/**
 * This class serves an example for executing XPath requests with the XML:DB
 * API.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author BaseX Team
 */
public final class XMLDBQuery {
  /** Database driver. */
  private static final String DRIVER = "org.basex.api.xmldb.BXDatabase";
  /** Name of the referenced database. */
  private static final String DBNAME = "xmldb:basex://localhost:1984/input";
  /** Sample query. */
  private static final String QUERY = "//li";

  /** Private constructor. */
  private XMLDBQuery() { }  

  /**
   * Main method of the example class.
   * @param args (ignored) command-line arguments
   * @throws Exception exception
   */
  public static void main(final String[] args) throws Exception {
    Collection coll = null;
    try {
      Class<?> c = Class.forName(DRIVER);

      Database db = (Database) c.newInstance();
      // Register the database.
      DatabaseManager.registerDatabase(db);
      // Receive the database.
      coll = DatabaseManager.getCollection(DBNAME);
      // Receive the XPath query service.
      XPathQueryService service = (XPathQueryService)
        coll.getService("XPathQueryService", "1.0");
      // Execute the query and receives all results.
      ResourceSet set = service.query(QUERY);
      // Create and loop through a result iterator.
      ResourceIterator iter = set.getIterator();
      while(iter.hasMoreResources()) {
        // Receive the next results.
        Resource res = iter.nextResource();
        // Writing the result to the console.
        System.out.println((String) res.getContent());
      }
    } catch(XMLDBException ex) {
      System.err.println("XML:DB Exception occured " + ex.errorCode);
    } finally {
      if(coll != null) coll.close();
    }
  }
}
