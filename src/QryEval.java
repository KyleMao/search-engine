/**
 * QryEval illustrates the architecture for the portion of a search engine that evaluates queries.
 * It is a template for class homework assignments, so it emphasizes simplicity over efficiency. It
 * implements an unranked Boolean retrieval model, however it is easily extended to other retrieval
 * models. For more information, see the ReadMe.txt file.
 *
 * Copyright (c) 2015, Carnegie Mellon University. All Rights Reserved.
 */

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class QryEval {

  private static String usage = "Usage:  java " + System.getProperty("sun.java.command")
      + " paramFile\n\n";
  private static int MAX_RESULT = 100;

  // The index file reader is accessible via a global variable. This
  // isn't great programming style, but the alternative is for every
  // query operator to store or pass this value, which creates its
  // own headaches.

  public static IndexReader READER;
  public static DocLengthStore dls;

  // Create and configure an English analyzer that will be used for
  // query parsing.

  public static EnglishAnalyzerConfigurable analyzer = new EnglishAnalyzerConfigurable(
      Version.LUCENE_43);
  static {
    analyzer.setLowercase(true);
    analyzer.setStopwordRemoval(true);
    analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
  }

  /**
   * @param args The only argument is the path to the parameter file.
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {

    // must supply parameter file
    if (args.length < 1) {
      fatalError(usage);
    }

    long startTime = System.currentTimeMillis();

    Map<String, String> params = readParam(args[0]);

    // open the index
    READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));
    if (READER == null) {
      fatalError(usage);
    }

    // read the retrieval algorithm
    RetrievalModel model = getModel(params);
    if (model == null) {
      fatalError("Unidentified retrieval algorithm!");
    }

    // create the output file
    File evalOut = new File(params.get("trecEvalOutputPath"));
    if (!evalOut.exists()) {
      evalOut.createNewFile();
    }
    BufferedWriter writer = new BufferedWriter(new FileWriter(evalOut.getAbsoluteFile()));

    // for relevance feedback
    QryEvalFb queryFb = null;
    if (params.containsKey("fb") && params.get("fb").equals("true")) {
      queryFb = new QryEvalFb(params, model);
    }

    // perform the queries
    Scanner in = new Scanner(new BufferedReader(new FileReader(params.get("queryFilePath"))));
    while (in.hasNextLine()) {
      String qLine = in.nextLine();
      String queryId = qLine.substring(0, qLine.indexOf(':'));
      String query = qLine.substring(qLine.indexOf(':') + 1);
      Qryop qTree = parseQuery(query, model);
      QryResult result = null;
      if (params.containsKey("fb") && params.get("fb").equals("true")) {
        result = queryFb.evaluate(qTree, queryId, query);
      } else {
        result = qTree.evaluate(model);
      }
      writeResults(writer, queryId, result);
    }
    in.close();
    writer.close();

    // for relevance feedback
    if (params.containsKey("fb") && params.get("fb").equals("true")) {
      queryFb.finish();
    }

    // print running time and memory usage
    long endTime = System.currentTimeMillis();
    System.out.println("Running Time: " + (endTime - startTime) + " ms");
    printMemoryUsage(false);
  }

  /**
   * parseQuery converts a query string into a query tree.
   * 
   * @param qString A string containing a query
   * @param r The retrieval model for the query
   * @return currentOp
   * @throws IOException
   */
  protected static Qryop parseQuery(String qString, RetrievalModel r) throws IOException {

    Qryop currentOp = null;
    Stack<Qryop> stack = new Stack<Qryop>();

    // Add a default query operator to an unstructured query. This
    // is a tiny bit easier if unnecessary whitespace is removed.
    qString = qString.trim();
    // Add default operator for different retrieval models
    if (r instanceof RetrievalModelUnrankedBoolean || r instanceof RetrievalModelRankedBoolean) {
      qString = "#OR(" + qString + ")";
    } else if (r instanceof RetrievalModelIndri) {
      qString = "#AND(" + qString + ")";
    } else if (r instanceof RetrievalModelBM25) {
      qString = "#SUM(" + qString + ")";
    }

    // Tokenize the query.
    StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);
    String token = null;

    // Each pass of the loop processes one token. To improve
    // efficiency and clarity, the query operator on the top of the
    // stack is also stored in currentOp.
    while (tokens.hasMoreTokens()) {

      token = tokens.nextToken();

      if (token.matches("[ ,(\t\n\r]")) {
        // Ignore most delimiters.
      } else if (token.equalsIgnoreCase("#and")) {
        currentOp = new QryopSlAnd();
        stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#or")) {
        currentOp = new QryopSlOr();
        stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#syn")) {
        currentOp = new QryopIlSyn();
        stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#sum")) {
        currentOp = new QryopSlSum();
        stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#wand")) {
        currentOp = new QryopSlWand();
        stack.push(currentOp);
      } else if (token.equalsIgnoreCase("#wsum")) {
        currentOp = new QryopSlWsum();
        stack.push(currentOp);
      } else if (token.toLowerCase().startsWith("#near")) {
        currentOp = new QryopIlNear(Integer.parseInt(token.substring(token.indexOf('/') + 1)));
        stack.push(currentOp);
      } else if (token.toLowerCase().startsWith("#window")) {
        currentOp = new QryopIlWindow(Integer.parseInt(token.substring(token.indexOf('/') + 1)));
        stack.push(currentOp);
      } else if (token.startsWith(")")) { // Finish current query operator.
        // If the current query operator is not an argument to
        // another query operator (i.e., the stack is empty when it
        // is removed), we're done (assuming correct syntax - see
        // below). Otherwise, add the current operator as an
        // argument to the higher-level operator, and shift
        // processing back to the higher-level operator.
        stack.pop();
        if (stack.empty())
          break;
        Qryop arg = currentOp;
        if (arg.args.size() > 0) {
          currentOp = stack.peek();
          currentOp.add(arg);
        }
      } else if (isNumeric(token) && (currentOp != null) && (currentOp.needWeight())) {
        currentOp.addWeight(Double.parseDouble(token));
      } else {
        // Lexical processing of the token before creating the query term, and check to see whether
        // the token specifies a particular field (e.g., apple.title).
        String[] termAndField = token.split("\\.");
        String term;
        String field;
        if (termAndField.length > 2) {
          System.err.println("Error: Invalid query term.");
          return null;
        } else if (termAndField.length == 2) {
          term = termAndField[0];
          field = termAndField[1];
          if (!(field.equalsIgnoreCase("url") || field.equalsIgnoreCase("keywords")
              || field.equalsIgnoreCase("title") || field.equalsIgnoreCase("body")
              || field.equalsIgnoreCase("inlink"))) {
            field = "body";
            term = token;
          }
        } else {
          term = termAndField[0];
          field = "body";
        }

        String[] processedToken = tokenizeQuery(term);
        if (processedToken.length > 1) {
          System.err.println("Error: Invalid query term.");
          return null;
        } else if (processedToken.length > 0) {
          currentOp.add(new QryopIlTerm(processedToken[0], field));
        } else if (!currentOp.needWeight()) {
          currentOp.removeWeight();
        }
      }
    }

    // A broken structured query can leave unprocessed tokens on the
    // stack, so check for that.
    if (tokens.hasMoreTokens()) {
      System.err.println("Error:  Query syntax is incorrect.  " + qString);
      return null;
    }

    return currentOp;
  }


  /**
   * Given a query string, returns the terms one at a time with stopwords removed and the terms
   * stemmed using the Krovetz stemmer.
   * 
   * Use this method to process raw query terms.
   * 
   * @param query String containing query
   * @return Array of query tokens
   * @throws IOException
   */
  static String[] tokenizeQuery(String query) throws IOException {

    TokenStreamComponents comp = analyzer.createComponents("dummy", new StringReader(query));
    TokenStream tokenStream = comp.getTokenStream();

    CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
    tokenStream.reset();

    List<String> tokens = new ArrayList<String>();
    while (tokenStream.incrementToken()) {
      String term = charTermAttribute.toString();
      tokens.add(term);
    }
    return tokens.toArray(new String[tokens.size()]);
  }

  /**
   * Read in the parameter file. One parameter per line in format of key=value.
   * 
   * @param paramPath
   * @return A map of parameters for the search engine
   * @throws IOException
   */
  private static Map<String, String> readParam(String paramPath) throws IOException {

    Map<String, String> params = new HashMap<String, String>();
    Scanner scan = new Scanner(new File(paramPath));
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split("=");
      params.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());
    scan.close();

    // parameters required for this example to run
    if (!(params.containsKey("indexPath") && params.containsKey("queryFilePath")
        && params.containsKey("trecEvalOutputPath") && params.containsKey("retrievalAlgorithm"))) {
      fatalError("Error: Parameters were missing.");
    }

    return params;
  }

  /*
   * Get the retrieval model with parameters.
   * 
   * @param params A map of parameters for the search engine
   * 
   * @return A retrieval model, or null if no model matched
   * 
   * @throws IOException
   */
  private static RetrievalModel getModel(Map<String, String> params) throws IOException {

    String modelName = params.get("retrievalAlgorithm");
    RetrievalModel model = null;

    if (modelName.equals("UnrankedBoolean")) {
      model = new RetrievalModelUnrankedBoolean();
    } else if (modelName.equals("RankedBoolean")) {
      model = new RetrievalModelRankedBoolean();
    } else if (modelName.equals("Indri")) {
      model = new RetrievalModelIndri();
      model.setParameter("mu", Integer.parseInt(params.get("Indri:mu")));
      model.setParameter("lambda", Double.parseDouble(params.get("Indri:lambda")));
      dls = new DocLengthStore(READER);
    } else if (modelName.equals("BM25")) {
      model = new RetrievalModelBM25();
      model.setParameter("b", Double.parseDouble(params.get("BM25:b")));
      model.setParameter("k_1", Double.parseDouble(params.get("BM25:k_1")));
      model.setParameter("k_3", Double.parseDouble(params.get("BM25:k_3")));
      dls = new DocLengthStore(READER);
    }

    return model;
  }

  /**
   * Write the query results into file.
   * 
   * @param queryId ID of the query
   * @param result Result of the query
   * @throws IOException
   */
  static void writeResults(BufferedWriter writer, String queryId, QryResult result)
      throws IOException {

    if (result.docScores.scores.size() < 1) {
      writer.write(queryId + " Q0 dummy 1 0 zexim\n");
    } else {
      DocScore docScore = new DocScore(result);
      for (int i = 0; i < docScore.scores.size() && i < MAX_RESULT; i++) {
        String line =
            String.format("%s Q0 %s %d %f zexim\n", queryId, docScore.getExternalDocid(i), i + 1,
                docScore.getDocidScore(i));
        writer.write(line);
      }
    }
  }

  /**
   * Write an error message and exit. This can be done in other ways, but I wanted something that
   * takes just one statement so that it is easy to insert checks without cluttering the code.
   * 
   * @param message The error message to write before exiting.
   * @return void
   */
  static void fatalError(String message) {
    System.err.println(message);
    System.exit(1);
  }

  /**
   * Get the external document id for a document specified by an internal document id. If the
   * internal id doesn't exists, returns null.
   * 
   * @param iid The internal document id of the document.
   * @throws IOException
   */
  static String getExternalDocid(int iid) throws IOException {
    Document d = QryEval.READER.document(iid);
    String eid = d.get("externalId");
    return eid;
  }

  /**
   * Finds the internal document id for a document specified by its external id, e.g.
   * clueweb09-enwp00-88-09710. If no such document exists, it throws an exception.
   * 
   * @param externalId The external document id of a document.s
   * @return An internal doc id suitable for finding document vectors etc.
   * @throws Exception
   */
  static int getInternalDocid(String externalId) throws Exception {
    Query q = new TermQuery(new Term("externalId", externalId));

    IndexSearcher searcher = new IndexSearcher(QryEval.READER);
    TopScoreDocCollector collector = TopScoreDocCollector.create(1, false);
    searcher.search(q, collector);
    ScoreDoc[] hits = collector.topDocs().scoreDocs;

    if (hits.length < 1) {
      throw new Exception("External id not found.");
    } else {
      return hits[0].doc;
    }
  }

  /*
   * Check whether a String is a number.
   * 
   * @param str The String to be tested.
   */
  @SuppressWarnings("unused")
  private static boolean isNumeric(String str) {
    try {
      double d = Double.parseDouble(str);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  /**
   * Print a message indicating the amount of memory used. The caller can indicate whether garbage
   * collection should be performed, which slows the program but reduces memory usage.
   * 
   * @param gc If true, run the garbage collector before reporting.
   * @return void
   */
  public static void printMemoryUsage(boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc) {
      runtime.gc();
    }

    System.out.println("Memory used:  "
        + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
  }

}
