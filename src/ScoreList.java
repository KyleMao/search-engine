/**
 * This class implements the document score list data structure and provides methods for accessing
 * and manipulating them.
 *
 * Copyright (c) 2015, Carnegie Mellon University. All Rights Reserved.
 */

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

    System.out.println("sorting...");
    Collections.sort(scores, new Comparator<ScoreListEntry>() {
      @Override
      public int compare(ScoreListEntry s1, ScoreListEntry s2) {
        // Sort based on score first
        if (s1.score < s2.score) {
          return 1;
        } else if (s1.score > s2.score){
          return -1;
        } else {
          return 0;
        }
      }
    });
  }

}
