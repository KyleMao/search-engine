/**
 * This class implements the document score list data structure and provides methods for accessing
 * and manipulating them.
 *
 * Copyright (c) 2015, Carnegie Mellon University. All Rights Reserved.
 */

import java.io.IOException;
import java.util.*;

public class ScoreList {

  // A little utility class to create a <docid, score> object.

  protected class ScoreListEntry {
    private int docid;
    private double score;

    private ScoreListEntry(int docid, double score) {
      this.docid = docid;
      this.score = score;
    }
  }

  List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

  /**
   * Append a document score to a score list.
   * 
   * @param docid An internal document id.
   * @param score The document's score.
   * @return void
   */
  public void add(int docid, double score) {
    scores.add(new ScoreListEntry(docid, score));
  }

  /**
   * Get the n'th document id.
   * 
   * @param n The index of the requested document.
   * @return The internal document id.
   */
  public int getDocid(int n) {
    return this.scores.get(n).docid;
  }

  /**
   * Get the score of the n'th document.
   * 
   * @param n The index of the requested document score.
   * @return The document's score.
   */
  public double getDocidScore(int n) {
    return this.scores.get(n).score;
  }

  /**
   * Sort the matching documents by their scores, in descending order. The external document id is a
   * secondary sort key.
   */
  public void sort() {

    Collections.sort(scores, new Comparator<ScoreListEntry>() {
      @Override
      public int compare(ScoreListEntry s1, ScoreListEntry s2) {
        // Sort based on score first
        int scoreCmp = ((Double) s2.score).compareTo((Double) s1.score);
        if (scoreCmp != 0) {
          return scoreCmp;
        }

        // Sort based on external document id
        int externalIdCmp = 1;
        try {
          externalIdCmp =
              QryEval.getExternalDocid(s1.docid).compareTo(QryEval.getExternalDocid(s2.docid));
        } catch (IOException e) {
          e.printStackTrace();
        }
        return externalIdCmp;
      }
    });
  }

}
