/* ////////////////////////////////////////////////////////////////////////
 * Concept.java - This class defines a concept represented by one or more 
 *   vocabularies (strings).
 *
 *   Copyright (C) 2013-2013    Yun-Tung Lau
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *   
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * ////////////////////////////////////////////////////////////////////////
 *
 */
//*************************************************************************

package chinese;

import java.util.Vector;

/** 
 * This class defines a concept represented by one or more vocabularies (strings).
 */
public class Concept {

  /** The vocabulary strings for this concept. */
  public String[] vocabs;
  
  /** Constructor with a string for this concept. */
  public Concept(String str) {
    this.vocabs = new String[1];
    this.vocabs[0] = str;
  }

  /** Constructor with an array of strings for this concept. */
  public Concept(String[] vocabs) {
    this.vocabs = vocabs;
  }

  /** Returns a string representation of this object. */
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (String s : vocabs) {
      if (sb.length() > 0) sb.append(';');
      sb.append(s);
    }
    return sb.toString();
  }

}
