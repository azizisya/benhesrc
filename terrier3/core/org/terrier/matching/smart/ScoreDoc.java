package org.terrier.matching.smart;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/** Expert: Returned by low-level search implementations.
 * @see TopDocs */
public class ScoreDoc implements java.io.Serializable {
  /** Expert: The score of this document for the query. */
  public double score;

  /** Expert: A hit document's number.
   * @see Searcher#doc(int)
   */
  public int doc;
  
  public short mask;
//  public 
  /** Expert: Constructs a ScoreDoc. */
  public ScoreDoc(int doc, double score) {
    this.doc = doc;
    this.score = score;
  }
  
  public ScoreDoc(int doc, double score, short mask) {
	    this.doc = doc;
	    this.score = score;
	    this.mask = mask;
	  }
  
}
